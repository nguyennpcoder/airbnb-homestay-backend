package com.project.airbnb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "nguoi_dung")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NguoiDung {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_nguoi_dung")
    private Long maNguoiDung;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "mat_khau_bam", nullable = false)
    private String matKhauBam;

    @Column(name = "ho")
    private String ho;

    @Column(name = "ten")
    private String ten;

    @Column(name = "ngay_sinh")
    private java.time.LocalDate ngaySinh;

    @Column(name = "ho_ten")
    private String hoTen;

    @Column(name = "so_dien_thoai", length = 20)
    private String soDienThoai;

    @Column(name = "email_da_xac_nhan")
    private Boolean emailDaXacNhan = false;

    @Column(name = "da_chap_nhan_cam_ket_cong_dong")
    private Boolean daChapNhanCamKetCongDong = false;

    @Column(name = "nhan_tin_nhan_tiep_thi")
    private Boolean nhanTinNhanTiepThi = true;

    @Column(name = "la_chu_nha", nullable = false)
    private Boolean laChuNha = false;

    @Column(name = "la_admin", nullable = false)
    private Boolean laAdmin = false;

    @Column(name = "bi_khoa", nullable = false)
    private Boolean biKhoa = false;

    @Column(name = "url_anh_dai_dien", length = 500)
    private String urlAnhDaiDien;

    @Column(name = "xac_minh_danh_tinh")
    private Boolean xacMinhDanhTinh = false;

    @Column(name = "url_bang_chung_danh_tinh", length = 500)
    private String urlBangChungDanhTinh;

    @Column(name = "ngay_xac_minh")
    private LocalDateTime ngayXacMinh;

    @Column(name = "ly_do_khoa", columnDefinition = "TEXT")
    private String lyDoKhoa;

    @Column(name = "cong_viec", length = 100)
    private String congViec;

    @Column(name = "thanh_pho_song", length = 100)
    private String thanhPhoSong;

    @Column(name = "so_luong_danh_gia_host")
    private Integer soLuongDanhGiaHost = 0;

    @Column(name = "diem_danh_gia_host", precision = 3, scale = 2)
    private java.math.BigDecimal diemDanhGiaHost;

    @Column(name = "so_du", precision = 15, scale = 2)
    private java.math.BigDecimal soDu = java.math.BigDecimal.ZERO;

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
}
