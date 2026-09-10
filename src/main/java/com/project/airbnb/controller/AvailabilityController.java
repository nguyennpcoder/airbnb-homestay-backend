package com.project.airbnb.controller;

import com.project.airbnb.entity.LichTrinh;
import com.project.airbnb.entity.Phong;
import com.project.airbnb.repository.LichTrinhRepository;
import com.project.airbnb.repository.PhongRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

    private final LichTrinhRepository lichTrinhRepository;
    private final PhongRepository phongRepository;

    public AvailabilityController(LichTrinhRepository lichTrinhRepository,
            PhongRepository phongRepository) {
        this.lichTrinhRepository = lichTrinhRepository;
        this.phongRepository = phongRepository;
    }

    @PostMapping("/range")
    @Transactional
    public ResponseEntity<?> updateRange(@RequestBody Map<String, Object> payload) {
        Long phongId = Long.valueOf(payload.get("phongId").toString());
        LocalDate tuNgay = LocalDate.parse(payload.get("tuNgay").toString());
        LocalDate denNgay = LocalDate.parse(payload.get("denNgay").toString());
        Boolean conKhaDung = Boolean.valueOf(payload.get("conKhaDung").toString());

        Phong phong = phongRepository.findById(phongId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

        LocalDate current = tuNgay;
        int count = 0;
        while (!current.isAfter(denNgay)) {
            final LocalDate date = current;
            LichTrinh record = lichTrinhRepository
                    .findByPhong_MaPhongAndNgay(phongId, date)
                    .orElseGet(() -> {
                        LichTrinh r = new LichTrinh();
                        r.setPhong(phong);
                        r.setNgay(date);
                        return r;
                    });
            record.setConKhaDung(conKhaDung);
            lichTrinhRepository.save(record);
            count++;
            current = current.plusDays(1);
        }

        return ResponseEntity.ok(Map.of(
                "updated", count,
                "conKhaDung", conKhaDung));
    }
}
