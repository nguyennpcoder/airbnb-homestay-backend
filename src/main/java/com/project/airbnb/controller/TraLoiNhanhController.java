package com.project.airbnb.controller;

import com.project.airbnb.entity.NguoiDung;
import com.project.airbnb.entity.TraLoiNhanh;
import com.project.airbnb.repository.NguoiDungRepository;
import com.project.airbnb.service.TraLoiNhanhService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tra-loi-nhanh")
public class TraLoiNhanhController {

    private final TraLoiNhanhService service;
    private final NguoiDungRepository nguoiDungRepository;

    public TraLoiNhanhController(TraLoiNhanhService service, NguoiDungRepository nguoiDungRepository) {
        this.service = service;
        this.nguoiDungRepository = nguoiDungRepository;
    }

    @GetMapping("/chu-nha/{maChuNha}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<TraLoiNhanh>> listByHost(@PathVariable Long maChuNha) {
        return ResponseEntity.ok(service.getByChuNha(maChuNha));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> detail(@PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        try {
            Long maChuNha = Long.parseLong(body.get("maChuNha").toString());
            String noiDung = (String) body.get("noiDung");
            String phimTat = (String) body.get("phimTat");

            if (noiDung == null || noiDung.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Nội dung là bắt buộc"));
            }
            if (phimTat == null || phimTat.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Phím tắt là bắt buộc"));
            }

            NguoiDung chuNha = nguoiDungRepository.findById(maChuNha)
                    .orElseThrow(() -> new RuntimeException("Host not found"));

            TraLoiNhanh traLoi = new TraLoiNhanh();
            traLoi.setNoiDung(noiDung.trim());
            traLoi.setPhimTat(phimTat.trim());
            traLoi.setChuNha(chuNha);

            TraLoiNhanh saved = service.create(traLoi);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            TraLoiNhanh updated = new TraLoiNhanh();
            if (body.containsKey("noiDung")) updated.setNoiDung((String) body.get("noiDung"));
            if (body.containsKey("phimTat")) updated.setPhimTat((String) body.get("phimTat"));

            TraLoiNhanh saved = service.update(id, updated);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
