package com.project.airbnb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "hinh_anh")
@Data

@NoArgsConstructor
@AllArgsConstructor
public class HinhAnh {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_hinh_anh")
    private Long maHinhAnh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_phong", nullable = false)
    private Phong phong;

    @Column(name = "url_hinh_anh", nullable = false, length = 500)
    private String urlHinhAnh;

    @Column(name = "thu_tu")
    private Integer thuTu = 0;

    @Column(name = "la_anh_chinh")
    private Boolean laAnhChinh = false;

    @Column(name = "phan_loai_anh", length = 100)
    private String phanLoaiAnh; // 'phong_khach', 'bep', 'phong_ngu', 'phong_tam', 'ban_cong', 'khu_vuc_giat_la', 'anh_bo_sung'

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
    }
}
