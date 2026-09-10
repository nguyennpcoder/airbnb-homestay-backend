package com.project.airbnb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "xac_thuc_email")
@Data

@NoArgsConstructor
@AllArgsConstructor
public class EmailVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_xac_thuc")
    private Long maVerification;

    @Column(name = "ma_nguoi_dung", nullable = false)
    private Long maNguoiDung;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "token", nullable = false, unique = true, length = 255)
    private String token;

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
            thoiGianHetHan = LocalDateTime.now().plusHours(24); // Token expires in 24 hours
        }
    }
}

