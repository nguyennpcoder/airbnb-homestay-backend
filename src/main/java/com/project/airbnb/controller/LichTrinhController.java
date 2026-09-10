package com.project.airbnb.controller;

import com.project.airbnb.entity.LichTrinh;
import com.project.airbnb.repository.LichTrinhRepository;
import com.project.airbnb.repository.DatChoRepository;
import lombok.Data;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/lich-trinh")
public class LichTrinhController {

    private final LichTrinhRepository lichTrinhRepository;
    private final DatChoRepository datChoRepository;

    public LichTrinhController(LichTrinhRepository lichTrinhRepository,
            DatChoRepository datChoRepository) {
        this.lichTrinhRepository = lichTrinhRepository;
        this.datChoRepository = datChoRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<LichTrinhResponse>> getLichTrinhDays(@RequestParam Long phongId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {
        List<LichTrinh> list = lichTrinhRepository
                .findByPhong_MaPhongAndNgayBetween(phongId, tuNgay, denNgay);

        List<com.project.airbnb.entity.DatCho> bookings = datChoRepository.findByPhong_MaPhong(phongId);

        List<LichTrinhResponse> dto = new java.util.ArrayList<>();
        LocalDate current = tuNgay;
        while (!current.isAfter(denNgay)) {
            final LocalDate d = current;
            LichTrinhResponse r = new LichTrinhResponse();
            r.setNgay(d);

            LichTrinh manual = list.stream()
                    .filter(e -> e.getNgay().equals(d))
                    .findFirst()
                    .orElse(null);

            if (manual != null) {
                r.setMaLichTrinh(manual.getMaLichTrinh());
                r.setGiaGhiDe(manual.getGiaGhiDe());
            }

            boolean hasActiveBooking = bookings.stream().anyMatch(
                    b -> (b.getTrangThaiDatCho() == com.project.airbnb.entity.DatCho.TrangThaiDatCho.cho_xac_nhan ||
                            b.getTrangThaiDatCho() == com.project.airbnb.entity.DatCho.TrangThaiDatCho.da_xac_nhan ||
                            b.getTrangThaiDatCho() == com.project.airbnb.entity.DatCho.TrangThaiDatCho.hoan_thanh)
                            && b.getNgayNhanPhong() != null && b.getNgayTraPhong() != null
                            && !d.isBefore(b.getNgayNhanPhong()) && d.isBefore(b.getNgayTraPhong()));

            // Check manual block first, then fall back to booking check
            boolean manuallyBlocked = manual != null && Boolean.FALSE.equals(manual.getConKhaDung());
            r.setConKhaDung(!manuallyBlocked && !hasActiveBooking);
            dto.add(r);
            current = current.plusDays(1);
        }

        return ResponseEntity.ok(dto);
    }

    @Data
    public static class LichTrinhResponse {
        private Long maLichTrinh;
        private LocalDate ngay;
        private Boolean conKhaDung;
        private java.math.BigDecimal giaGhiDe;
    }
}
