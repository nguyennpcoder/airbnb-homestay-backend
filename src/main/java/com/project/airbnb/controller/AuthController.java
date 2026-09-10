package com.project.airbnb.controller;

import com.project.airbnb.dto.*;
import com.project.airbnb.entity.NguoiDung;
import com.project.airbnb.service.AuthService;
import com.project.airbnb.repository.NguoiDungRepository;
import com.project.airbnb.service.OtpService;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")

public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final OtpService otpService;
    private final NguoiDungRepository nguoiDungRepository;
    private final com.project.airbnb.repository.OtpVerificationRepository otpVerificationRepository;

    public AuthController(AuthService authService, OtpService otpService,
            NguoiDungRepository nguoiDungRepository,
            com.project.airbnb.repository.OtpVerificationRepository otpVerificationRepository) {
        this.authService = authService;
        this.otpService = otpService;
        this.nguoiDungRepository = nguoiDungRepository;
        this.otpVerificationRepository = otpVerificationRepository;
    }

    @Value("${app.debug-expose-otp:false}")
    private boolean debugExposeOtp;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.authenticate(request);
        setJwtCookie(response, authResponse.getToken());
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/social-login")
    public ResponseEntity<AuthResponse> socialLogin(@Valid @RequestBody SocialLoginRequest request,
            HttpServletResponse response) {
        try {
            log.info("Social login attempt - Provider: {}, Email: {}", request.getProvider(), request.getEmail());
            AuthResponse authResponse = authService.handleSocialLogin(request);
            setJwtCookie(response, authResponse.getToken());
            log.info("Social login successful - User ID: {}, Email: {}", authResponse.getMaNguoiDung(),
                    authResponse.getEmail());
            return ResponseEntity.ok(authResponse);
        } catch (Exception e) {
            log.error("Social login failed - Provider: {}, Email: {}, Error: {}",
                    request.getProvider(), request.getEmail(), e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("jwt", null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok(Map.of("message", "Đã đăng xuất"));
    }

    private void setJwtCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("jwt", token);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        // cookie.setSecure(true); // Enable in production with HTTPS
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        response.addCookie(cookie);
    }

    @PostMapping("/otp/send")
    public ResponseEntity<Map<String, Object>> sendOtp(@Valid @RequestBody OtpRequest request) {
        // Normalize and validate email
        String normalizedEmail = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
        if (normalizedEmail == null || !normalizedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            Map<String, Object> bad = new HashMap<>();
            bad.put("message", "Email không hợp lệ");
            return ResponseEntity.badRequest().body(bad);
        }

        // If email đã tồn tại: bắt buộc số điện thoại phải khớp; nếu chưa tồn tại: cho
        // phép gửi OTP để đăng ký mới
        var existingOpt = nguoiDungRepository.findByEmail(normalizedEmail);
        if (existingOpt.isPresent()) {
            // Check if account is locked
            if (Boolean.TRUE.equals(existingOpt.get().getBiKhoa())) {
                Map<String, Object> locked = new HashMap<>();
                locked.put("locked", true);
                locked.put("message", "Tài khoản đã bị khóa. Vui lòng liên hệ admin@airbnb.com.vn để được hỗ trợ mở lại.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(locked);
            }

            String savedPhone = existingOpt.get().getSoDienThoai();
            if (savedPhone != null && !savedPhone.isEmpty()) {
                java.util.function.Function<String, String> normalize = s -> s == null ? ""
                        : s.replaceAll("[^0-9]", "");
                String reqDigits = normalize.apply(request.getSoDienThoai());
                String savedDigits = normalize.apply(savedPhone);
                boolean match = reqDigits.equals(savedDigits) || reqDigits.endsWith(savedDigits)
                        || savedDigits.endsWith(reqDigits);
                if (!match) {
                    Map<String, Object> bad = new HashMap<>();
                    bad.put("message", "Số điện thoại không khớp với tài khoản email này");
                    return ResponseEntity.badRequest().body(bad);
                }
            }
        }

        String otp = otpService.sendOtp(request.getSoDienThoai(), normalizedEmail);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "OTP đã được gửi đến email của bạn");
        if (debugExposeOtp) {
            response.put("otp", otp);
        }
        response.put("email", normalizedEmail);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/otp/latest")
    public ResponseEntity<Map<String, String>> getLatestOtp(@RequestParam String email) {
        var otpList = otpVerificationRepository
                .findByEmailAndLoaiOrderByThoiGianTaoDesc(email.trim().toLowerCase(),
                        com.project.airbnb.entity.OtpVerification.Loai.email);
        Map<String, String> result = new HashMap<>();
        if (otpList != null && !otpList.isEmpty()) {
            result.put("otp", otpList.get(0).getOtpCode());
        } else {
            result.put("otp", "");
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/validate-email-phone")
    public ResponseEntity<Map<String, Object>> validateEmailPhone(@Valid @RequestBody OtpRequest request) {
        Map<String, Object> result = new HashMap<>();
        String normalizedEmail = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
        var existingOpt = normalizedEmail == null ? java.util.Optional.<com.project.airbnb.entity.NguoiDung>empty()
                : nguoiDungRepository.findByEmail(normalizedEmail);
        if (existingOpt.isPresent()) {
            String savedPhone = existingOpt.get().getSoDienThoai();
            if (savedPhone != null && !savedPhone.isEmpty()) {
                java.util.function.Function<String, String> normalize = s -> s == null ? ""
                        : s.replaceAll("[^0-9]", "");
                String reqDigits = normalize.apply(request.getSoDienThoai());
                String savedDigits = normalize.apply(savedPhone);
                boolean match = reqDigits.equals(savedDigits) ||
                        reqDigits.endsWith(savedDigits) ||
                        savedDigits.endsWith(reqDigits);
                if (!match) {
                    result.put("message", "Số điện thoại không khớp với tài khoản email này");
                    return ResponseEntity.badRequest().body(result);
                }
            }
        }
        // Email chưa tồn tại: coi như hợp lệ để tiến hành đăng ký
        result.put("valid", true);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        String normalizedEmail = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
        boolean isValid = otpService.verifyOtp(normalizedEmail, request.getOtpCode());
        Map<String, Object> response = new HashMap<>();
        if (isValid) {
            response.put("verified", true);
            response.put("message", "Xác thực thành công");
            response.put("soDienThoai", request.getSoDienThoai());
            response.put("email", normalizedEmail);
            boolean exists = normalizedEmail != null && nguoiDungRepository.findByEmail(normalizedEmail)
                    .map(u -> u.getSoDienThoai() != null && !u.getSoDienThoai().isEmpty())
                    .orElse(false);
            response.put("existingUser", exists);
        } else {
            response.put("verified", false);
            response.put("message", "Mã OTP không hợp lệ hoặc đã hết hạn");
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/complete-registration")
    public ResponseEntity<Map<String, Object>> completeRegistration(
            @RequestParam String soDienThoai,
            @Valid @RequestBody CompleteRegistrationRequest request) {
        NguoiDung nguoiDung = authService.completeRegistration(request, soDienThoai);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Đăng ký hoàn tất. Vui lòng kiểm tra email để xác nhận.");
        response.put("maNguoiDung", nguoiDung.getMaNguoiDung());
        response.put("email", nguoiDung.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/accept-community-commitment")
    public ResponseEntity<Map<String, String>> acceptCommunityCommitment(@RequestParam Long userId) {
        authService.acceptCommunityCommitment(userId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Đã chấp nhận cam kết cộng đồng");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestParam String token, HttpServletResponse response) {
        Map<String, Object> result = authService.verifyEmail(token);
        if (Boolean.TRUE.equals(result.get("verified")) && result.get("token") != null) {
            setJwtCookie(response, result.get("token").toString());
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestParam Long userId,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        authService.changePassword(userId, oldPassword, newPassword);
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Đổi mật khẩu thành công");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestParam String email) {
        String tempPassword = authService.forgotPassword(email);
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Mật khẩu tạm thời đã được gửi qua email");
        resp.put("tempPassword", tempPassword);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String tempPassword = body.get("tempPassword");
        String newPassword = body.get("newPassword");
        if (email == null || tempPassword == null || newPassword == null) {
            Map<String, String> err = new HashMap<>();
            err.put("message", "Thiếu thông tin bắt buộc");
            return ResponseEntity.badRequest().body(err);
        }
        authService.resetPassword(email.trim().toLowerCase(), tempPassword, newPassword);
        Map<String, String> resp = new HashMap<>();
        resp.put("message", "Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại.");
        return ResponseEntity.ok(resp);
    }
}
