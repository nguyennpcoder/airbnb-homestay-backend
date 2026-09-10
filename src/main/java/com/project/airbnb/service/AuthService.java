package com.project.airbnb.service;

import com.project.airbnb.dto.AuthRequest;
import com.project.airbnb.dto.AuthResponse;
import com.project.airbnb.dto.CompleteRegistrationRequest;
import com.project.airbnb.dto.RegisterRequest;
import com.project.airbnb.dto.SocialLoginRequest;
import com.project.airbnb.entity.NguoiDung;
import com.project.airbnb.repository.NguoiDungRepository;
import com.project.airbnb.repository.EmailVerificationRepository;
import com.project.airbnb.security.JwtService;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service

public class AuthService {

    private final NguoiDungRepository nguoiDungRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final EmailService emailService;
    private final OtpService otpService;
    private final EmailVerificationRepository emailVerificationRepository;

    public AuthService(NguoiDungRepository nguoiDungRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            AuthenticationManager authenticationManager, UserDetailsService userDetailsService,
            EmailService emailService, OtpService otpService, EmailVerificationRepository emailVerificationRepository) {
        this.nguoiDungRepository = nguoiDungRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.emailService = emailService;
        this.otpService = otpService;
        this.emailVerificationRepository = emailVerificationRepository;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
        if (email != null && nguoiDungRepository.existsByEmail(email)) {
            throw new RuntimeException("Email đã tồn tại");
        }

        NguoiDung nguoiDung = new NguoiDung();

        nguoiDung.setEmail(email);
        nguoiDung.setMatKhauBam(passwordEncoder.encode(request.getMatKhau()));
        nguoiDung.setHoTen(request.getHoTen());
        nguoiDung.setSoDienThoai(request.getSoDienThoai());
        nguoiDung.setLaChuNha(request.getLaChuNha() != null ? request.getLaChuNha() : false);

        nguoiDung = nguoiDungRepository.save(nguoiDung);

        UserDetails userDetails = userDetailsService.loadUserByUsername(nguoiDung.getEmail());
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(token, nguoiDung.getEmail(), nguoiDung.getMaNguoiDung(),
                nguoiDung.getHoTen(), nguoiDung.getLaChuNha(), nguoiDung.getLaAdmin());
    }

    public AuthResponse authenticate(AuthRequest request) {
        String email = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        email,
                        request.getMatKhau()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        String token = jwtService.generateToken(userDetails);

        NguoiDung nguoiDung = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (Boolean.TRUE.equals(nguoiDung.getBiKhoa())) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }

        return new AuthResponse(token, nguoiDung.getEmail(), nguoiDung.getMaNguoiDung(),
                nguoiDung.getHoTen(), nguoiDung.getLaChuNha(), nguoiDung.getLaAdmin());
    }

    @Transactional
    public NguoiDung completeRegistration(CompleteRegistrationRequest request, String soDienThoai) {
        String normalizedEmail = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
        // Find user by phone number (created during OTP verification)
        NguoiDung nguoiDung = nguoiDungRepository.findBySoDienThoai(soDienThoai)
                .orElseGet(() -> {
                    NguoiDung newUser = new NguoiDung();
                    newUser.setSoDienThoai(soDienThoai);
                    return newUser;
                });

        // Update user information
        nguoiDung.setHo(request.getHo());
        nguoiDung.setTen(request.getTen());
        nguoiDung.setNgaySinh(request.getNgaySinh());
        nguoiDung.setHoTen(request.getHo() + " " + request.getTen());
        nguoiDung.setEmail(normalizedEmail);
        nguoiDung.setNhanTinNhanTiepThi(request.getNhanTinNhanTiepThi());

        // Set password if provided
        if (request.getMatKhau() != null && !request.getMatKhau().isEmpty()) {
            nguoiDung.setMatKhauBam(passwordEncoder.encode(request.getMatKhau()));
        }

        // Account is not activated until email is verified
        nguoiDung.setEmailDaXacNhan(false);

        // Save user
        nguoiDung = nguoiDungRepository.save(nguoiDung);

        // Do not send verification at this step. It will be sent after
        // the user accepts the community commitment.

        return nguoiDung;
    }

    @Transactional
    public void acceptCommunityCommitment(Long userId) {
        NguoiDung nguoiDung = nguoiDungRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        nguoiDung.setDaChapNhanCamKetCongDong(true);
        nguoiDungRepository.save(nguoiDung);

        // After acceptance, create email verification token and send verification email
        com.project.airbnb.entity.EmailVerification emailVerification = new com.project.airbnb.entity.EmailVerification();
        emailVerification.setMaNguoiDung(nguoiDung.getMaNguoiDung());
        emailVerification.setEmail(nguoiDung.getEmail());
        String verificationToken = UUID.randomUUID().toString();
        emailVerification.setToken(verificationToken);
        emailVerification.setThoiGianHetHan(java.time.LocalDateTime.now().plusHours(24));
        emailVerificationRepository.save(emailVerification);

        emailService.sendVerificationEmail(nguoiDung.getEmail(), verificationToken);
    }

    @Transactional
    public Map<String, Object> verifyEmail(String token) {
        Map<String, Object> result = new HashMap<>();

        com.project.airbnb.entity.EmailVerification verification = emailVerificationRepository
                .findByTokenAndDaSuDungFalseAndHetHanFalse(token)
                .orElse(null);

        if (verification == null) {
            result.put("verified", false);
            result.put("message", "Token không hợp lệ");
            return result;
        }

        // Check expiration
        if (verification.getThoiGianHetHan().isBefore(java.time.LocalDateTime.now())) {
            verification.setHetHan(true);
            emailVerificationRepository.save(verification);
            result.put("verified", false);
            result.put("message", "Token đã hết hạn");
            return result;
        }

        // Verify email and activate account
        NguoiDung nguoiDung = nguoiDungRepository.findById(verification.getMaNguoiDung())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        nguoiDung.setEmailDaXacNhan(true);
        nguoiDungRepository.save(nguoiDung);

        // Mark token as used
        verification.setDaSuDung(true);
        emailVerificationRepository.save(verification);

        // Send welcome email after verification
        emailService.sendWelcomeEmail(nguoiDung.getEmail(), nguoiDung.getHoTen());

        // Generate token for auto-login
        UserDetails userDetails = userDetailsService.loadUserByUsername(nguoiDung.getEmail());
        String tokenJwt = jwtService.generateToken(userDetails);

        result.put("verified", true);
        result.put("message", "Email đã được xác nhận thành công!");
        result.put("userId", nguoiDung.getMaNguoiDung());
        result.put("token", tokenJwt);

        return result;
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        NguoiDung user = nguoiDungRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        if (user.getMatKhauBam() == null || !passwordEncoder.matches(oldPassword, user.getMatKhauBam())) {
            throw new RuntimeException("Mật khẩu cũ không đúng");
        }
        user.setMatKhauBam(passwordEncoder.encode(newPassword));
        nguoiDungRepository.save(user);
    }

    @Transactional
    public String forgotPassword(String email) {
        NguoiDung user = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email này"));
        String temp = generateTempPassword();
        user.setMatKhauBam(passwordEncoder.encode(temp));
        nguoiDungRepository.save(user);

        // Reuse OTP email template to send temp password quickly
        emailService.sendOtpEmail(email, temp);
        return temp;
    }

    @Transactional
    public void resetPassword(String email, String tempPassword, String newPassword) {
        NguoiDung user = nguoiDungRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với email này"));
        if (user.getMatKhauBam() == null || !passwordEncoder.matches(tempPassword, user.getMatKhauBam())) {
            throw new RuntimeException("Mật khẩu tạm thời không đúng");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new RuntimeException("Mật khẩu mới phải có ít nhất 8 ký tự");
        }
        user.setMatKhauBam(passwordEncoder.encode(newPassword));
        nguoiDungRepository.save(user);
    }

    @Transactional
    public AuthResponse handleSocialLogin(SocialLoginRequest request) {
        String normalizedEmail = request.getEmail() == null ? null : request.getEmail().trim().toLowerCase();
        if (normalizedEmail == null || normalizedEmail.isEmpty()) {
            throw new RuntimeException("Email không hợp lệ");
        }

        NguoiDung user = nguoiDungRepository.findByEmail(normalizedEmail).orElseGet(() -> {
            NguoiDung nguoiDung = new NguoiDung();
            nguoiDung.setEmail(normalizedEmail);
            nguoiDung.setMatKhauBam(passwordEncoder.encode(UUID.randomUUID().toString()));
            nguoiDung.setEmailDaXacNhan(true);
            nguoiDung.setDaChapNhanCamKetCongDong(true);
            return nguoiDung;
        });

        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            user.setHoTen(request.getDisplayName());
            var parts = request.getDisplayName().trim().split("\\s+");
            if (parts.length > 1) {
                user.setHo(String.join(" ", java.util.Arrays.copyOf(parts, parts.length - 1)));
                user.setTen(parts[parts.length - 1]);
            } else {
                user.setTen(request.getDisplayName().trim());
            }
        }

        if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
            user.setUrlAnhDaiDien(request.getAvatarUrl());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            if (user.getSoDienThoai() == null || user.getSoDienThoai().isBlank()) {
                user.setSoDienThoai(request.getPhoneNumber());
            }
        }

        if (Boolean.TRUE.equals(user.getBiKhoa())) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }

        user = nguoiDungRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(token, user.getEmail(), user.getMaNguoiDung(), user.getHoTen(), user.getLaChuNha(),
                user.getLaAdmin());
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
        java.util.Random r = new java.util.Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++)
            sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }
}
