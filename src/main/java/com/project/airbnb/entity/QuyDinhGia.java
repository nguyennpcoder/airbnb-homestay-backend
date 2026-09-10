package com.project.airbnb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "quy_dinh_gia")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuyDinhGia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_quy_dinh")
    private Long maQuyDinh;

    @Column(name = "ty_le_nguoi_lon", nullable = false, precision = 5, scale = 2)
    private BigDecimal tyLeNguoiLon = new BigDecimal("100.00");

    @Column(name = "ty_le_tre_em", nullable = false, precision = 5, scale = 2)
    private BigDecimal tyLeTreEm = new BigDecimal("60.00");

    @Column(name = "ty_le_em_be", nullable = false, precision = 5, scale = 2)
    private BigDecimal tyLeEmBe = BigDecimal.ZERO;

    @Column(name = "ty_le_phi_dich_vu", nullable = false, precision = 5, scale = 2)
    private BigDecimal tyLePhiDichVu = new BigDecimal("10.00");

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
