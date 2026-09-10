package com.project.airbnb.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class KhuyenMaiRequest {
    private String tenKhuyenMai;
    private String moTa;
    private String loaiGiamGia; // PHAN_TRAM | SO_TIEN
    private BigDecimal giaTri;
    private String apCho; // NGUOI_LON | TRE_EM | EM_BE | DON_HANG
    private String dieuKienApDung; // KHONG | CO_TRE_EM | CO_NGUOI_LON
    private LocalDate dieuKienNgayBatDau;
    private LocalDate dieuKienNgayKetThuc;
    private Integer soLuongToiDa;
    private Boolean hoatDong;
}
