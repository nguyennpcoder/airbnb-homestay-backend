package com.project.airbnb.controller;

import com.project.airbnb.entity.ChinhSachHoanTien;
import com.project.airbnb.repository.ChinhSachHoanTienRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@RestController
public class ChinhSachHuyController {

    private final ChinhSachHoanTienRepository repo;

    public ChinhSachHuyController(ChinhSachHoanTienRepository repo) {
        this.repo = repo;
    }

    // ==================== PUBLIC ====================

    @GetMapping("/api/chinh-sach-huy")
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(repo.findAllByOrderByThuTuAsc());
    }

    @GetMapping("/api/chinh-sach-huy/{ma}")
    public ResponseEntity<?> getByMa(@PathVariable String ma) {
        Optional<ChinhSachHoanTien> policy = repo.findByMa(ma);
        if (policy.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(policy.get());
    }

    // ==================== ADMIN ====================

    @GetMapping("/api/admin/chinh-sach-huy")
    public ResponseEntity<?> adminList() {
        return ResponseEntity.ok(repo.findAllByOrderByThuTuAsc());
    }

    @PostMapping("/api/admin/chinh-sach-huy")
    public ResponseEntity<?> create(@RequestBody ChinhSachHoanTien req) {
        try {
            if (req.getMa() == null || req.getMa().isBlank() || req.getTen() == null || req.getTen().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Mã và tên chính sách không được bỏ trống"));
            }
            if (repo.findByMa(req.getMa()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Mã chính sách đã tồn tại"));
            }
            if (req.getTyLeHoanTien() == null || req.getTyLeHoanTien().compareTo(BigDecimal.ZERO) < 0
                    || req.getTyLeHoanTien().compareTo(new BigDecimal("100")) > 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Tỷ lệ hoàn tiền phải từ 0% đến 100%"));
            }
            req.setMaChinhSach(null);
            return ResponseEntity.ok(repo.save(req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"));
        }
    }

    @PutMapping("/api/admin/chinh-sach-huy/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ChinhSachHoanTien req) {
        try {
            ChinhSachHoanTien cs = repo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chính sách"));
            if (req.getTen() != null) cs.setTen(req.getTen());
            if (req.getMoTa() != null) cs.setMoTa(req.getMoTa());
            if (req.getTyLeHoanTien() != null) cs.setTyLeHoanTien(req.getTyLeHoanTien());
            if (req.getSoNgayTruoc() != null) cs.setSoNgayTruoc(req.getSoNgayTruoc());
            if (req.getThuTu() != null) cs.setThuTu(req.getThuTu());
            if (req.getMa() != null && !req.getMa().isBlank()) cs.setMa(req.getMa());
            return ResponseEntity.ok(repo.save(cs));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"));
        }
    }

    @DeleteMapping("/api/admin/chinh-sach-huy/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            repo.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Đã xóa chính sách hủy"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"));
        }
    }
}
