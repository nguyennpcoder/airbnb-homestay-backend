package com.project.airbnb.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "nhom_tien_nghi")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class NhomTienNghi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_nhom_tien_nghi")
    private Long maNhomTienNghi;

    @Column(name = "ten_nhom", nullable = false, length = 150)
    private String tenNhom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_chu_nha")
    @Getter(AccessLevel.NONE)
    private NguoiDung chuNha;

    @Column(name = "thu_tu")
    private Integer thuTu;

    @Column(name = "hoat_dong")
    private Boolean hoatDong = true;

    @JsonIgnore
    public NguoiDung getChuNha() { return chuNha; }
    public void setChuNha(NguoiDung chuNha) { this.chuNha = chuNha; }
}
