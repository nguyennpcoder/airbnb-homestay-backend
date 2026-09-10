package com.project.airbnb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "phong")
@Data

@NoArgsConstructor
@AllArgsConstructor
public class Phong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_phong")
    private Long maPhong;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_chu_nha", nullable = false)
    private NguoiDung chuNha;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_phong", nullable = false, length = 20)
    private LoaiPhong loaiPhong;

    @Column(name = "tieu_de", nullable = false, length = 500)
    private String tieuDe;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "url_anh_chinh", length = 500)
    private String urlAnhChinh;

    @Column(name = "quoc_gia", length = 100)
    private String quocGia;

    @Column(name = "thanh_pho", length = 100)
    private String thanhPho;

    @Column(name = "quan_huyen", length = 100)
    private String quanHuyen;

    @Column(name = "phuong_xa", length = 100)
    private String phuongXa;

    @Column(name = "dia_chi_day_du", columnDefinition = "TEXT")
    private String diaChiDayDu;

    @Column(name = "gia_moi_khach", nullable = false, precision = 15, scale = 2)
    private BigDecimal giaMoiKhach;

    @Column(name = "tien_te", length = 10)
    private String tienTe = "VND";

    @Column(name = "gia_dat_toi_thieu", precision = 15, scale = 2)
    private BigDecimal giaDatToiThieu;

    @Column(name = "phi_ve_sinh", precision = 15, scale = 2)
    private BigDecimal phiVeSinh = BigDecimal.ZERO;

    @Column(name = "kinh_do", precision = 11, scale = 8)
    private BigDecimal kinhDo;

    @Column(name = "vi_do", precision = 11, scale = 8)
    private BigDecimal viDo;

    // Các trường dành riêng cho Nơi lưu trú
    @Column(name = "loai_bat_dong_san", length = 100)
    private String loaiBatDongSan;

    @Column(name = "so_phong_ngu")
    private Integer soPhongNgu;

    @Column(name = "so_giuong")
    private Integer soGiuong;

    @Column(name = "so_phong_tam", precision = 3, scale = 1)
    private BigDecimal soPhongTam;

    @Column(name = "so_khach_toi_da")
    private Integer soKhachToiDa;

    @Column(name = "so_dem_toi_thieu")
    private Integer soDemToiThieu = 1;

    // Các trường dành riêng cho Trải nghiệm/Dịch vụ
    @Column(name = "thoi_gian_tinh_bang_gio", precision = 5, scale = 2)
    private BigDecimal thoiGianTinhBangGio;

    @Column(name = "phan_loai", length = 100)
    private String phanLoai;

    @Column(name = "la_original")
    private Boolean laOriginal = false;

    @Column(name = "la_pho_bien")
    private Boolean laPhoBien = false;

    // Đánh giá và nhận xét
    @Column(name = "diem_trung_binh", precision = 3, scale = 2)
    private BigDecimal diemTrungBinh = BigDecimal.ZERO;

    @Column(name = "so_luong_danh_gia")
    private Integer soLuongDanhGia = 0;

    @Column(name = "duoc_khach_yeu_thich")
    private Boolean duocKhachYeuThich = false;

    @Column(name = "so_luot_xem")
    private Long soLuotXem = 0L;

    // Metadata
    @Column(name = "tien_nghi", columnDefinition = "TEXT")
    private String tienNghi;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", length = 20)
    private TrangThaiPhong trangThai = TrangThaiPhong.hoat_dong;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @Column(name = "bi_khoa")
    private Boolean biKhoa = false;

    @Column(name = "da_duyet")
    private Boolean daDuyet = false;

    @Column(name = "ly_do_tu_choi", columnDefinition = "TEXT")
    private String lyDoTuChoi;

    @Enumerated(EnumType.STRING)
    @Column(name = "chinh_sach_huy", length = 20)
    private ChinhSachHuy chinhSachHuy = ChinhSachHuy.LINH_HOAT;

    @Column(name = "pass_phong", length = 20)
    private String passPhong;

    @OneToMany(mappedBy = "phong", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private java.util.List<HinhAnh> hinhAnhs;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
        ngayCapNhat = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        ngayCapNhat = LocalDateTime.now();
    }

    public enum LoaiPhong {
        noi_luu_tru, trai_nghiem, dich_vu
    }

    public enum TrangThaiPhong {
        hoat_dong, khong_hoat_dong, cho_duyet
    }

    public enum ChinhSachHuy {
        LINH_HOAT, TRUNG_BINH, NGHIEM_NGAT
    }
}
