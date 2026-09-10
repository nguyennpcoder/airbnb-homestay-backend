package com.project.airbnb.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "thong_bao")
@Data
public class ThongBao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "nguoi_nhan_id")
    @JsonIgnoreProperties({ "matKhauBam", "emailDaXacNhan", "daChapNhanCamKetCongDong", "nhanTinNhanTiepThi" })
    private NguoiDung nguoiNhan;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "nguoi_gui_id")
    @JsonIgnoreProperties({ "matKhauBam", "emailDaXacNhan", "daChapNhanCamKetCongDong", "nhanTinNhanTiepThi" })
    private NguoiDung nguoiGui;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dat_cho_id")
    @JsonIgnoreProperties({ "khach", "phong" })
    private DatCho datCho;

    private String loaiThongBao;
    private String noiDung;
    private boolean daDoc;
    private String trangThai;

    private LocalDateTime ngayTao;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
        daDoc = false;
        if (trangThai == null) {
            trangThai = "PENDING";
        }
    }
}
