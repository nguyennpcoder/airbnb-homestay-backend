package com.project.airbnb.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class QuyDinhGiaRequest {
    private BigDecimal tyLeNguoiLon;
    private BigDecimal tyLeTreEm;
    private BigDecimal tyLePhiDichVu;
}
