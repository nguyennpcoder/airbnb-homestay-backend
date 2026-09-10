package com.project.airbnb.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tra_loi_nhanh")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TraLoiNhanh {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "noi_dung", nullable = false, columnDefinition = "TEXT")
    private String noiDung;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_chu", nullable = false)
    private NguoiDung chuNha;

    @Column(name = "phim_tat", nullable = false, length = 255)
    private String phimTat;
}
