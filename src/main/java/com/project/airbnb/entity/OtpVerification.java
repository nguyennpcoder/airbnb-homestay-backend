package com.project.airbnb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "xac_thuc_otp")
@Data

@NoArgsConstructor
@AllArgsConstructor
public class OtpVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_otp")
    private Long maOtp;

    @Column(name = "so_dien_thoai", length = 20)
    private String soDienThoai;

    @Column(name = "email")
    private String email;

    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai", nullable = false, length = 10)
    private Loai loai;

    @Column(name = "da_su_dung")
    private Boolean daSuDung = false;

    @Column(name = "het_han")
    private Boolean hetHan = false;

    @Column(name = "thoi_gian_tao", updatable = false)
    private LocalDateTime thoiGianTao;

    @Column(name = "thoi_gian_het_han", nullable = false)
    private LocalDateTime thoiGianHetHan;

    @PrePersist
    protected void onCreate() {
        thoiGianTao = LocalDateTime.now();
        if (thoiGianHetHan == null) {
            thoiGianHetHan = LocalDateTime.now().plusMinutes(5); // OTP expires in 5 minutes
        }
    }

    public enum Loai {
        phone, email
    }
}

