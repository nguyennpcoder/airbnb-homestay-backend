package com.project.airbnb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "dat_cho")
@Data

@NoArgsConstructor
@AllArgsConstructor
public class DatCho {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_dat_cho")
    private Long maDatCho;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_khach", nullable = false)
    private NguoiDung khach;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_phong", nullable = false)
    private Phong phong;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_phong", nullable = false, length = 20)
    private Phong.LoaiPhong loaiPhong;

    // Cho nơi lưu trú
    @Column(name = "ngay_nhan_phong")
    private LocalDate ngayNhanPhong;

    @Column(name = "ngay_tra_phong")
    private LocalDate ngayTraPhong;

    // Cho trải nghiệm/dịch vụ
    @Column(name = "ngay_dat")
    private LocalDate ngayDat;

    @Column(name = "gio_bat_dau")
    private LocalTime gioBatDau;

    @Column(name = "gio_ket_thuc")
    private LocalTime gioKetThuc;

    @Column(name = "so_luong_khach", nullable = false)
    private Integer soLuongKhach = 1;

    @Column(name = "so_nguoi_lon")
    private Integer soNguoiLon;

    @Column(name = "so_tre_em")
    private Integer soTreEm;

    @Column(name = "so_em_be")
    private Integer soEmBe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_khuyen_mai")
    private KhuyenMai khuyenMai;

    @Column(name = "tien_giam_gia", precision = 15, scale = 2)
    private BigDecimal tienGiamGia = BigDecimal.ZERO;

    @Column(name = "gia_moi_khach", nullable = false, precision = 15, scale = 2)
    private BigDecimal giaMoiKhach;

    @Column(name = "tong_tien", nullable = false, precision = 15, scale = 2)
    private BigDecimal tongTien;

    @Column(name = "phi_ve_sinh", precision = 15, scale = 2)
    private BigDecimal phiVeSinh = BigDecimal.ZERO;

    @Column(name = "phi_dich_vu", precision = 15, scale = 2)
    private BigDecimal phiDichVu = BigDecimal.ZERO;

    @Column(name = "tien_te", length = 10)
    private String tienTe = "VND";

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_dat_cho", length = 20)
    private TrangThaiDatCho trangThaiDatCho = TrangThaiDatCho.cho_xac_nhan;

    @Column(name = "yeu_cau_dac_biet", columnDefinition = "TEXT")
    private String yeuCauDacBiet;

    @Column(name = "ly_do_huy", columnDefinition = "TEXT")
    private String lyDoHuy;

    @Column(name = "tien_hoan_lai", precision = 15, scale = 2)
    private BigDecimal tienHoanLai;

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

    public enum TrangThaiDatCho {
        cho_xac_nhan, da_xac_nhan, hoan_thanh, da_huy
    }

    @Transient
    private String lastMessage;

    @Transient
    private LocalDateTime lastMessageTime;

    @Transient
    private Boolean hasUnread;

}
