package com.project.airbnb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "quy_dinh")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuyDinh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_quy_dinh")
    private Long maQuyDinh;

    @Column(name = "ma", nullable = false, unique = true, length = 50)
    private String ma;

    @Column(name = "tieu_de", nullable = false, length = 200)
    private String tieuDe;

    @Column(name = "noi_dung", columnDefinition = "text")
    private String noiDung;

    @Column(name = "doi_tuong", nullable = false, length = 20)
    private String doiTuong = "tat_ca";

    @Column(name = "nhom", nullable = false, length = 30)
    private String nhom = "chung";

    @Column(name = "thu_tu", nullable = false)
    private Integer thuTu = 0;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

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
