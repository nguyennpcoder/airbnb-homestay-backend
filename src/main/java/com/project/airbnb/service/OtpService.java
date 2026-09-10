package com.project.airbnb.service;

import com.project.airbnb.entity.OtpVerification;
import com.project.airbnb.repository.OtpVerificationRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service

public class OtpService {
    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private final OtpVerificationRepository otpRepository;
    private final EmailService emailService;

    public OtpService(OtpVerificationRepository otpRepository, EmailService emailService) {
        this.otpRepository = otpRepository;
        this.emailService = emailService;
    }

    public String generateOtp() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    @Transactional
    public String sendOtp(String soDienThoai, String email) {
        // Invalidate previous OTPs for this email
        otpRepository.findByEmailAndLoaiOrderByThoiGianTaoDesc(email, OtpVerification.Loai.email)
                .forEach(otp -> {
                    otp.setDaSuDung(true);
                    otp.setHetHan(true);
                    otpRepository.save(otp);
                });

        // Generate new OTP
        String otpCode = generateOtp();
        OtpVerification otp = new OtpVerification();
        otp.setSoDienThoai(soDienThoai);
        otp.setEmail(email);
        otp.setOtpCode(otpCode);
        otp.setLoai(OtpVerification.Loai.email);
        otp.setThoiGianHetHan(LocalDateTime.now().plusMinutes(10)); // Email OTP expires in 10 minutes
        otpRepository.save(otp);

        // Send OTP via Email
        emailService.sendOtpEmail(email, otpCode);

        log.info("OTP sent to email {}: {}", email, otpCode);

        return otpCode;
    }

    @Transactional
    public boolean verifyOtp(String email, String otpCode) {
        OtpVerification otp = otpRepository
                .findByEmailAndOtpCodeAndLoaiAndDaSuDungFalseAndHetHanFalse(
                        email, otpCode, OtpVerification.Loai.email)
                .orElse(null);

        if (otp == null) {
            return false;
        }

        // Check expiration
        if (otp.getThoiGianHetHan().isBefore(LocalDateTime.now())) {
            otp.setHetHan(true);
            otpRepository.save(otp);
            return false;
        }

        // Mark as used
        otp.setDaSuDung(true);
        otpRepository.save(otp);

        return true;
    }

    @Transactional
    public String sendEmailOtp(String email, String soDienThoai) {
        // Invalidate previous OTPs
        otpRepository.findByEmailAndLoaiOrderByThoiGianTaoDesc(email, OtpVerification.Loai.email)
                .forEach(otp -> {
                    otp.setDaSuDung(true);
                    otp.setHetHan(true);
                    otpRepository.save(otp);
                });

        // Generate new OTP
        String otpCode = generateOtp();
        OtpVerification otp = new OtpVerification();
        otp.setSoDienThoai(soDienThoai != null ? soDienThoai : "");
        otp.setEmail(email);
        otp.setOtpCode(otpCode);
        otp.setLoai(OtpVerification.Loai.email);
        otp.setThoiGianHetHan(LocalDateTime.now().plusMinutes(10)); // Email OTP expires in 10 minutes
        otpRepository.save(otp);

        // Will be sent via EmailService
        return otpCode;
    }
}
