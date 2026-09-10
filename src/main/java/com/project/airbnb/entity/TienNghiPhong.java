package com.project.airbnb.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tien_nghi_phong")
@Data

@NoArgsConstructor
@AllArgsConstructor
public class TienNghiPhong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_tien_nghi_phong")
    private Long maTienNghiPhong;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_phong", nullable = false)
    private Phong phong;

    @Column(name = "nhom", nullable = false, length = 150)
    private String nhom;

    @Column(name = "ten", nullable = false, length = 200)
    private String ten;

    @Column(name = "duoc_cung_cap")
    private Boolean duocCungCap = true;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;
}
