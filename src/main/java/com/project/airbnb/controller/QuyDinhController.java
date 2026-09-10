package com.project.airbnb.controller;

import com.project.airbnb.entity.QuyDinh;
import com.project.airbnb.repository.QuyDinhRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class QuyDinhController {

    private final QuyDinhRepository repo;

    public QuyDinhController(QuyDinhRepository repo) {
        this.repo = repo;
    }

    // ==================== PUBLIC ====================
    // ?nhom=gia_ca|hoan_tien|noi_quy|chung|giao_tiep
    // ?doiTuong=tat_ca|chu_nha|khach (lọc theo đối tượng)

    @GetMapping("/api/quy-dinh")
    public ResponseEntity<?> list(
            @RequestParam(required = false) String nhom,
            @RequestParam(required = false) String doiTuong) {
        List<QuyDinh> result;
        List<String> audiences = doiTuong != null && !doiTuong.isBlank()
                ? List.of(doiTuong, "tat_ca")
                : List.of("tat_ca", "chu_nha", "khach");
        if (nhom != null && !nhom.isBlank()) {
            result = repo.findByActiveTrueAndNhomAndDoiTuongInOrderByThuTuAsc(nhom, audiences);
        } else {
            result = repo.findByActiveTrueAndDoiTuongInOrderByThuTuAsc(audiences);
        }
        return ResponseEntity.ok(result);
    }

    // ==================== ADMIN ====================

    @GetMapping("/api/admin/quy-dinh")
    public ResponseEntity<?> adminList() {
        return ResponseEntity.ok(repo.findAllByOrderByThuTuAsc());
    }

    @PostMapping("/api/admin/quy-dinh")
    public ResponseEntity<?> create(@RequestBody QuyDinh req) {
        try {
            if (req.getMa() == null || req.getMa().isBlank() || req.getTieuDe() == null || req.getTieuDe().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Mã và tiêu đề không được bỏ trống"));
            }
            if (repo.findByMa(req.getMa()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Mã quy định đã tồn tại"));
            }
            req.setMaQuyDinh(null);
            return ResponseEntity.ok(repo.save(req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"));
        }
    }

    @PutMapping("/api/admin/quy-dinh/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody QuyDinh req) {
        try {
            QuyDinh qd = repo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy quy định"));
            if (req.getMa() != null && !req.getMa().isBlank()) qd.setMa(req.getMa());
            if (req.getTieuDe() != null) qd.setTieuDe(req.getTieuDe());
            if (req.getNoiDung() != null) qd.setNoiDung(req.getNoiDung());
            if (req.getDoiTuong() != null) qd.setDoiTuong(req.getDoiTuong());
            if (req.getNhom() != null) qd.setNhom(req.getNhom());
            if (req.getThuTu() != null) qd.setThuTu(req.getThuTu());
            if (req.getActive() != null) qd.setActive(req.getActive());
            return ResponseEntity.ok(repo.save(qd));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"));
        }
    }

    @DeleteMapping("/api/admin/quy-dinh/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            repo.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Đã xóa quy định"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"));
        }
    }
}
