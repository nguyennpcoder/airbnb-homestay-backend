package com.project.airbnb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "chinh_sach_huy")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChinhSachHoanTien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_chinh_sach")
    private Long maChinhSach;

    @Column(name = "ma", nullable = false, unique = true, length = 30)
    private String ma;

    @Column(name = "ten", nullable = false, length = 100)
    private String ten;

    @Column(name = "mo_ta", columnDefinition = "text")
    private String moTa;

    @Column(name = "ty_le_hoan_tien", nullable = false, precision = 5, scale = 2)
    private BigDecimal tyLeHoanTien = new BigDecimal("100.00");

    @Column(name = "so_ngay_truoc", nullable = false)
    private Integer soNgayTruoc = 1;

    @Column(name = "thu_tu", nullable = false)
    private Integer thuTu = 0;

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
