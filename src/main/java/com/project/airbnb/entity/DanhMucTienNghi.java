package com.project.airbnb.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "danh_muc_tien_nghi")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DanhMucTienNghi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_tien_nghi")
    private Long maTienNghi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_nhom_tien_nghi", nullable = false)
    private NhomTienNghi nhom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_chu_nha")
    @Getter(AccessLevel.NONE)
    private NguoiDung chuNha;

    @Column(name = "ten", nullable = false, length = 200)
    private String ten;

    @Column(name = "bieu_tuong", length = 200)
    private String bieuTuong;

    @Column(name = "mo_ta", length = 255)
    private String moTa;

    @Column(name = "hoat_dong")
    private Boolean hoatDong = true;

    @JsonIgnore
    public NguoiDung getChuNha() { return chuNha; }
    public void setChuNha(NguoiDung chuNha) { this.chuNha = chuNha; }
}
