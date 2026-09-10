package com.project.airbnb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "danh_gia_huu_ich", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"ma_danh_gia", "ma_nguoi_dung"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DanhGiaHuuIch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_huu_ich")
    private Long maHuuIch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_danh_gia", nullable = false)
    private DanhGia danhGia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_nguoi_dung", nullable = false)
    private NguoiDung nguoiDung;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
    }
}
