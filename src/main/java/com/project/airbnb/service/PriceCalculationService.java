package com.project.airbnb.service;

import com.project.airbnb.entity.KhuyenMai;
import com.project.airbnb.entity.QuyDinhGia;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
public class PriceCalculationService {

    private final QuyDinhGiaService quyDinhGiaService;

    public PriceCalculationService(QuyDinhGiaService quyDinhGiaService) {
        this.quyDinhGiaService = quyDinhGiaService;
    }

    /**
     * Tỷ lệ phí dịch vụ (0-1) lấy từ quy_dinh_gia, mặc định 0.10.
     */
    public BigDecimal getServiceFeeRate() {
        QuyDinhGia qd = quyDinhGiaService.get();
        BigDecimal tyLe = qd.getTyLePhiDichVu();
        if (tyLe == null) {
            tyLe = new BigDecimal("10.00");
        }
        return tyLe.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    /**
     * Tính giá cho nơi lưu trú (theo đêm, theo người).
     */
    public PriceResult calculateLuuTru(
            BigDecimal giaMoiKhach,
            LocalDate ngayNhanPhong,
            LocalDate ngayTraPhong,
            int soNguoiLon,
            int soTreEm,
            int soEmBe,
            BigDecimal phiVeSinh,
            Long maKhuyenMai,
            KhuyenMaiService khuyenMaiService) {

        long nights = ChronoUnit.DAYS.between(ngayNhanPhong, ngayTraPhong);
        if (nights <= 0) nights = 1;

        QuyDinhGia qd = quyDinhGiaService.get();
        BigDecimal tyLeNguoiLon = qd.getTyLeNguoiLon().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal tyLeTreEm = qd.getTyLeTreEm().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        // Room cost
        BigDecimal adultsCost = giaMoiKhach
                .multiply(tyLeNguoiLon)
                .multiply(BigDecimal.valueOf(soNguoiLon))
                .multiply(BigDecimal.valueOf(nights));
        BigDecimal childrenCost = giaMoiKhach
                .multiply(tyLeTreEm)
                .multiply(BigDecimal.valueOf(soTreEm))
                .multiply(BigDecimal.valueOf(nights));
        BigDecimal roomCost = adultsCost.add(childrenCost);

        // Cleaning fee
        BigDecimal cleaningFee = phiVeSinh != null ? phiVeSinh : BigDecimal.ZERO;

        // Service fee (theo ty_le_phi_dich_vu trong quy_dinh_gia, mặc định 10%)
        BigDecimal serviceFeeRate = qd.getTyLePhiDichVu().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal serviceFee = roomCost.add(cleaningFee).multiply(serviceFeeRate).setScale(0, RoundingMode.HALF_UP);

        // Subtotal before discount
        BigDecimal subTotal = roomCost.add(cleaningFee).add(serviceFee);

        // Discount
        BigDecimal tienGiamGia = BigDecimal.ZERO;
        if (maKhuyenMai != null && khuyenMaiService != null) {
            KhuyenMai km = khuyenMaiService.findById(maKhuyenMai);
            tienGiamGia = khuyenMaiService.calculateDiscount(km, soNguoiLon, soTreEm, soEmBe, roomCost, nights, adultsCost, childrenCost);
        }

        // Total
        BigDecimal tongTien = subTotal.subtract(tienGiamGia).max(BigDecimal.ZERO);

        return new PriceResult(
                nights,
                roomCost,
                adultsCost,
                childrenCost,
                BigDecimal.ZERO, // infants cost
                cleaningFee,
                serviceFee,
                tienGiamGia,
                tongTien
        );
    }

    /**
     * Tính giá cho dịch vụ/trải nghiệm (theo người, không theo đêm).
     */
    public PriceResult calculateDichVu(
            BigDecimal giaMoiKhach,
            int soLuongKhach) {

        BigDecimal roomCost = giaMoiKhach.multiply(BigDecimal.valueOf(soLuongKhach));
        QuyDinhGia qd = quyDinhGiaService.get();
        BigDecimal serviceFeeRate = qd.getTyLePhiDichVu().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal serviceFee = roomCost.multiply(serviceFeeRate).setScale(0, RoundingMode.HALF_UP);
        BigDecimal tongTien = roomCost.add(serviceFee);

        return new PriceResult(
                1,
                roomCost,
                roomCost,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                serviceFee,
                BigDecimal.ZERO,
                tongTien
        );
    }

    public record PriceResult(
            long nights,
            BigDecimal roomCost,
            BigDecimal adultsCost,
            BigDecimal childrenCost,
            BigDecimal infantsCost,
            BigDecimal cleaningFee,
            BigDecimal serviceFee,
            BigDecimal tienGiamGia,
            BigDecimal tongTien
    ) {
        public Map<String, Object> toMap() {
            return Map.of(
                    "nights", nights,
                    "roomCost", roomCost,
                    "adultsCost", adultsCost,
                    "childrenCost", childrenCost,
                    "infantsCost", infantsCost,
                    "cleaningFee", cleaningFee,
                    "serviceFee", serviceFee,
                    "tienGiamGia", tienGiamGia,
                    "tongTien", tongTien
            );
        }
    }
}
