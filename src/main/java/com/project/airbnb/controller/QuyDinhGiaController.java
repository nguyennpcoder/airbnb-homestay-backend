package com.project.airbnb.controller;

import com.project.airbnb.dto.QuyDinhGiaRequest;
import com.project.airbnb.entity.QuyDinhGia;
import com.project.airbnb.service.QuyDinhGiaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/quy-dinh-gia")
@Transactional
public class QuyDinhGiaController {
    private static final Logger log = LoggerFactory.getLogger(QuyDinhGiaController.class);

    private final QuyDinhGiaService service;

    public QuyDinhGiaController(QuyDinhGiaService service) {
        this.service = service;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<QuyDinhGia> get() {
        return ResponseEntity.ok(service.get());
    }

    @PutMapping
    public ResponseEntity<?> update(@RequestBody QuyDinhGiaRequest req) {
        try {
            if (req.getTyLeNguoiLon() != null && (req.getTyLeNguoiLon().compareTo(BigDecimal.ZERO) < 0 || req.getTyLeNguoiLon().compareTo(new BigDecimal("100")) > 0)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Tỷ lệ người lớn phải từ 0% đến 100%"));
            }
            if (req.getTyLeTreEm() != null && (req.getTyLeTreEm().compareTo(BigDecimal.ZERO) < 0 || req.getTyLeTreEm().compareTo(new BigDecimal("100")) > 0)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Tỷ lệ trẻ em phải từ 0% đến 100%"));
            }
            QuyDinhGia updated = service.update(req);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("update QuyDinhGia error", e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
