package com.project.airbnb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "khuyen_mai")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KhuyenMai {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_khuyen_mai")
    private Long maKhuyenMai;

    @Column(name = "ten_khuyen_mai", nullable = false, length = 255)
    private String tenKhuyenMai;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_giam_gia", nullable = false, length = 20)
    private LoaiGiamGia loaiGiamGia;

    @Column(name = "gia_tri", nullable = false, precision = 10, scale = 2)
    private BigDecimal giaTri;

    @Enumerated(EnumType.STRING)
    @Column(name = "ap_cho", nullable = false, length = 20)
    private ApCho apCho;

    @Enumerated(EnumType.STRING)
    @Column(name = "dieu_kien_ap_dung", length = 20)
    private DieuKienApDung dieuKienApDung = DieuKienApDung.KHONG;

    @Column(name = "dieu_kien_ngay_bat_dau")
    private LocalDate dieuKienNgayBatDau;

    @Column(name = "dieu_kien_ngay_ket_thuc")
    private LocalDate dieuKienNgayKetThuc;

    @Column(name = "so_luong_toi_da", nullable = false)
    private Integer soLuongToiDa = 0;

    @Column(name = "so_luong_da_dung", nullable = false)
    private Integer soLuongDaDung = 0;

    @Column(name = "hoat_dong", nullable = false)
    private Boolean hoatDong = true;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
        ngayCapNhat = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        ngayCapNhat = LocalDateTime.now();
    }

    public enum LoaiGiamGia {
        PHAN_TRAM, SO_TIEN
    }

    public enum ApCho {
        NGUOI_LON, TRE_EM, DON_HANG
    }

    public enum DieuKienApDung {
        KHONG, CO_TRE_EM, CO_NGUOI_LON
    }
}
