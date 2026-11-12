package com.project.airbnb.controller;

import com.project.airbnb.dto.QuyDinhGiaRequest;
import com.project.airbnb.entity.QuyDinhGia;
import com.project.airbnb.service.QuyDinhGiaService;
import com.project.airbnb.service.ResponseCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
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
    private static final String CACHE_KEY = "pricing-rules:v1";
    private static final long CACHE_TTL_MS = 5 * 60_000L;

    private final QuyDinhGiaService service;
    private final ResponseCache responseCache;

    public QuyDinhGiaController(QuyDinhGiaService service, ResponseCache responseCache) {
        this.service = service;
        this.responseCache = responseCache;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<QuyDinhGia> get() {
        // Loaded on every page via PricingRulesInit; it changes only when an
        // admin edits it, so a 5-minute cache removes a ~1.3s request.
        QuyDinhGia rules = responseCache.getOrLoad(CACHE_KEY, CACHE_TTL_MS, service::get);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=60, stale-while-revalidate=300")
                .body(rules);
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
            responseCache.invalidate(CACHE_KEY);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("update QuyDinhGia error", e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
