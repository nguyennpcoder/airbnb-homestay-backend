package com.project.airbnb.controller;

import com.project.airbnb.dto.KhuyenMaiRequest;
import com.project.airbnb.entity.KhuyenMai;
import com.project.airbnb.service.KhuyenMaiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/khuyen-mai")
@Transactional
public class KhuyenMaiController {
    private static final Logger log = LoggerFactory.getLogger(KhuyenMaiController.class);

    private final KhuyenMaiService service;

    public KhuyenMaiController(KhuyenMaiService service) {
        this.service = service;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<KhuyenMai>> list() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/available")
    @Transactional(readOnly = true)
    public ResponseEntity<List<KhuyenMai>> available() {
        return ResponseEntity.ok(service.findAvailable());
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> detail(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.findById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody KhuyenMaiRequest req) {
        try {
            if (req.getTenKhuyenMai() == null || req.getTenKhuyenMai().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Tên khuyến mãi là bắt buộc"));
            }
            if (req.getGiaTri() == null || req.getGiaTri().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Giá trị khuyến mãi phải lớn hơn 0"));
            }
            if (req.getLoaiGiamGia() != null && req.getLoaiGiamGia().equals("PHAN_TRAM")
                && req.getGiaTri().compareTo(BigDecimal.valueOf(100)) > 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Phần trăm giảm không được vượt quá 100%"));
            }
            KhuyenMai km = service.create(req);
            return ResponseEntity.ok(km);
        } catch (Exception e) {
            log.error("create KhuyenMai error", e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody KhuyenMaiRequest req) {
        try {
            KhuyenMai km = service.update(id, req);
            return ResponseEntity.ok(km);
        } catch (Exception e) {
            log.error("update KhuyenMai error", e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<?> toggle(@PathVariable Long id) {
        try {
            KhuyenMai km = service.toggle(id);
            return ResponseEntity.ok(km);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{id}/validate")
    @Transactional(readOnly = true)
    public ResponseEntity<?> validate(@PathVariable Long id) {
        try {
            boolean available = service.isAvailable(id);
            KhuyenMai km = service.findById(id);
            return ResponseEntity.ok(Map.of(
                "available", available,
                "soLuongToiDa", km.getSoLuongToiDa(),
                "soLuongDaDung", km.getSoLuongDaDung(),
                "message", available ? "Mã khuyến mãi còn hiệu lực" : "Mã khuyến mãi đã hết lượt sử dụng"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("available", false, "message", "Không tìm thấy khuyến mãi"));
        }
    }

    @PostMapping("/calculate")
    @Transactional(readOnly = true)
    public ResponseEntity<?> calculate(@RequestBody Map<String, Object> body) {
        try {
            Long kmId = body.get("maKhuyenMai") != null ? Long.valueOf(body.get("maKhuyenMai").toString()) : null;
            int soNguoiLon = body.get("soNguoiLon") != null ? Integer.parseInt(body.get("soNguoiLon").toString()) : 0;
            int soTreEm = body.get("soTreEm") != null ? Integer.parseInt(body.get("soTreEm").toString()) : 0;
            int soEmBe = body.get("soEmBe") != null ? Integer.parseInt(body.get("soEmBe").toString()) : 0;
            BigDecimal roomCost = new BigDecimal(body.get("roomCost").toString());
            long nights = body.get("nights") != null ? Long.parseLong(body.get("nights").toString()) : 1;

            // Nhan them adultsCost va childrenCost de tinh chinh xac
            BigDecimal adultsCost = body.get("adultsCost") != null ? new BigDecimal(body.get("adultsCost").toString()) : roomCost;
            BigDecimal childrenCost = body.get("childrenCost") != null ? new BigDecimal(body.get("childrenCost").toString()) : roomCost;

            KhuyenMai km = kmId != null ? service.findById(kmId) : null;
            BigDecimal discount = service.calculateDiscount(km, soNguoiLon, soTreEm, soEmBe, roomCost, nights, adultsCost, childrenCost);

            return ResponseEntity.ok(Map.of(
                "tienGiamGia", discount,
                "tongTienSauGiam", roomCost.subtract(discount)
            ));
        } catch (Exception e) {
            log.error("calculate discount error", e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
