package com.project.airbnb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lich_trinh")
@Data

@NoArgsConstructor
@AllArgsConstructor
public class LichTrinh {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_lich_trinh")
    private Long maLichTrinh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_phong", nullable = false)
    private Phong phong;

    @Column(name = "ngay", nullable = false)
    private LocalDate ngay;

    @Column(name = "gia_ghi_de", precision = 15, scale = 2)
    private BigDecimal giaGhiDe;

    @Column(name = "con_kha_dung", nullable = false)
    private Boolean conKhaDung = true;

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

    // Getters and Setters
    public Long getMaLichTrinh() {
        return maLichTrinh;
    }

    public void setMaLichTrinh(Long maLichTrinh) {
        this.maLichTrinh = maLichTrinh;
    }

    public Phong getPhong() {
        return phong;
    }

    public void setPhong(Phong phong) {
        this.phong = phong;
    }

    public LocalDate getNgay() {
        return ngay;
    }

    public void setNgay(LocalDate ngay) {
        this.ngay = ngay;
    }

    public BigDecimal getGiaGhiDe() {
        return giaGhiDe;
    }

    public void setGiaGhiDe(BigDecimal giaGhiDe) {
        this.giaGhiDe = giaGhiDe;
    }

    public Boolean getConKhaDung() {
        return conKhaDung;
    }

    public void setConKhaDung(Boolean conKhaDung) {
        this.conKhaDung = conKhaDung;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }

    public LocalDateTime getNgayCapNhat() {
        return ngayCapNhat;
    }

    public void setNgayCapNhat(LocalDateTime ngayCapNhat) {
        this.ngayCapNhat = ngayCapNhat;
    }
}
