package com.project.airbnb.dto;

import com.project.airbnb.entity.Phong;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class SearchCriteria {
    private Phong.LoaiPhong loaiPhong;
    private String thanhPho;
    private Integer soKhach;
    private LocalDate ngayNhan;
    private LocalDate ngayTra;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private List<String> tienNghi;
    private BigDecimal minLat;
    private BigDecimal maxLat;
    private BigDecimal minLng;
    private BigDecimal maxLng;
    private List<String> loaiHinh;
}
