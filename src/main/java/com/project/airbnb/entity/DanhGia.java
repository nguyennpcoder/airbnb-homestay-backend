package com.project.airbnb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "danh_gia")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DanhGia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_danh_gia")
    private Long maDanhGia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_phong", nullable = false)
    private Phong phong;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_phong", nullable = false, length = 20)
    private Phong.LoaiPhong loaiPhong;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_khach", nullable = false)
    private NguoiDung khach;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_dat_cho")
    private DatCho datCho;

    @Column(name = "diem_so", nullable = false, precision = 2, scale = 1)
    private BigDecimal diemSo;

    @Column(name = "diem_sach_se", precision = 2, scale = 1)
    private BigDecimal diemSachSe;

    @Column(name = "diem_chinh_xac", precision = 2, scale = 1)
    private BigDecimal diemChinhXac;

    @Column(name = "diem_nhan_phong", precision = 2, scale = 1)
    private BigDecimal diemNhanPhong;

    @Column(name = "diem_giao_tiep", precision = 2, scale = 1)
    private BigDecimal diemGiaoTiep;

    @Column(name = "diem_vi_tri", precision = 2, scale = 1)
    private BigDecimal diemViTri;

    @Column(name = "diem_gia_tri", precision = 2, scale = 1)
    private BigDecimal diemGiaTri;

    @Column(name = "binh_luan", columnDefinition = "TEXT")
    private String binhLuan;

    @Column(name = "phan_hoi", columnDefinition = "TEXT")
    private String phanHoi;

    @Column(name = "so_luot_huu_ich")
    private Integer soLuotHuuIch = 0;

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
