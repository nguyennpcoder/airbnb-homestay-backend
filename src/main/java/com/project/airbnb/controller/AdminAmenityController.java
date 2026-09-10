package com.project.airbnb.controller;

import com.project.airbnb.entity.DanhMucTienNghi;
import com.project.airbnb.entity.NguoiDung;
import com.project.airbnb.entity.NhomTienNghi;
import com.project.airbnb.repository.DanhMucTienNghiRepository;
import com.project.airbnb.repository.NguoiDungRepository;
import com.project.airbnb.repository.NhomTienNghiRepository;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/host/amenities")
@Transactional
public class AdminAmenityController {
    private static final Logger log = LoggerFactory.getLogger(AdminAmenityController.class);

    private final NhomTienNghiRepository nhomRepo;
    private final DanhMucTienNghiRepository catalogRepo;
    private final NguoiDungRepository nguoiDungRepository;

    public AdminAmenityController(NhomTienNghiRepository nhomRepo,
            DanhMucTienNghiRepository catalogRepo,
            NguoiDungRepository nguoiDungRepository) {
        this.nhomRepo = nhomRepo;
        this.catalogRepo = catalogRepo;
        this.nguoiDungRepository = nguoiDungRepository;
    }

    private NguoiDung getHost(Long hostId) {
        return nguoiDungRepository.findById(hostId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ nhà"));
    }

    // ========== NHÓM TIỆN NGHI ==========

    @GetMapping("/groups")
    @Transactional(readOnly = true)
    public ResponseEntity<List<NhomTienNghi>> getGroups(@RequestParam Long hostId) {
        return ResponseEntity.ok(nhomRepo.findByChuNha_MaNguoiDungOrderByThuTuAsc(hostId));
    }

    @PostMapping("/groups")
    public ResponseEntity<?> createGroup(@RequestBody GroupRequest req) {
        try {
            if (req.getHostId() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Thiếu mã chủ nhà"));
            }
            if (req.getThuTu() != null && req.getThuTu() < 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Thứ tự không được là số âm"));
            }
            NguoiDung host = getHost(req.getHostId());
            NhomTienNghi g = new NhomTienNghi();
            g.setTenNhom(req.getTenNhom());
            g.setChuNha(host);
            g.setThuTu(req.getThuTu() != null ? req.getThuTu() : 0);
            g.setHoatDong(true);
            return ResponseEntity.ok(nhomRepo.save(g));
        } catch (Exception e) {
            log.error("createGroup error", e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/groups/{id}")
    public ResponseEntity<?> updateGroup(@PathVariable Long id, @RequestBody GroupRequest req) {
        return nhomRepo.findById(id).map(g -> {
            if (req.getTenNhom() != null) g.setTenNhom(req.getTenNhom());
            if (req.getThuTu() != null) {
                if (req.getThuTu() < 0) {
                    return ResponseEntity.badRequest().body((Object) Map.of("message", "Thứ tự không được là số âm"));
                }
                g.setThuTu(req.getThuTu());
            }
            return ResponseEntity.ok((Object) nhomRepo.save(g));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/groups/{id}/toggle")
    @Transactional
    public ResponseEntity<?> toggleGroup(@PathVariable Long id) {
        return nhomRepo.findById(id).map(g -> {
            boolean newState = !Boolean.TRUE.equals(g.getHoatDong());
            g.setHoatDong(newState);
            nhomRepo.save(g);
            List<DanhMucTienNghi> items = catalogRepo.findByNhom_MaNhomTienNghi(id);
            items.forEach(c -> c.setHoatDong(newState));
            catalogRepo.saveAll(items);
            return ResponseEntity.ok(Map.of("hoatDong", newState));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ========== DANH MỤC TIỆN NGHI ==========

    @GetMapping("/catalog")
    @Transactional(readOnly = true)
    public ResponseEntity<List<DanhMucTienNghi>> getCatalog(
            @RequestParam Long hostId,
            @RequestParam(required = false) Long groupId) {
        List<DanhMucTienNghi> list;
        if (groupId != null) {
            list = catalogRepo.findAllByHostAndGroupWithNhom(hostId, groupId);
        } else {
            list = catalogRepo.findAllByHostWithNhom(hostId);
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/catalog/active")
    @Transactional(readOnly = true)
    public ResponseEntity<List<DanhMucTienNghi>> getCatalogActive(
            @RequestParam Long hostId,
            @RequestParam(required = false) Long groupId) {
        List<DanhMucTienNghi> list;
        if (groupId != null) {
            list = catalogRepo.findAllActiveByHostWithNhom(hostId).stream()
                    .filter(c -> c.getNhom() != null && c.getNhom().getMaNhomTienNghi().equals(groupId))
                    .toList();
        } else {
            list = catalogRepo.findAllActiveByHostWithNhom(hostId);
        }
        return ResponseEntity.ok(list);
    }

    @PostMapping("/catalog")
    public ResponseEntity<?> createCatalog(@RequestBody CatalogRequest req) {
        try {
            if (req.getHostId() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Thiếu mã chủ nhà"));
            }
            NhomTienNghi nhom = nhomRepo.findById(req.getMaNhomTienNghi())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm tiện nghi"));
            if (!Boolean.TRUE.equals(nhom.getHoatDong())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Không thể thêm tiện ích vào nhóm đang bị ẩn. Vui lòng hiển thị nhóm trước."));
            }
            NguoiDung host = getHost(req.getHostId());
            DanhMucTienNghi c = new DanhMucTienNghi();
            c.setNhom(nhom);
            c.setChuNha(host);
            c.setTen(req.getTen());
            c.setBieuTuong(req.getBieuTuong());
            c.setMoTa(req.getMoTa());
            c.setHoatDong(true);
            return ResponseEntity.ok(catalogRepo.save(c));
        } catch (Exception e) {
            log.error("createCatalog error", e);
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/catalog/{id}")
    public ResponseEntity<?> updateCatalog(@PathVariable Long id, @RequestBody CatalogRequest req) {
        return catalogRepo.findById(id).map(c -> {
            if (req.getMaNhomTienNghi() != null) {
                NhomTienNghi nhom = nhomRepo.findById(req.getMaNhomTienNghi())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm tiện nghi"));
                c.setNhom(nhom);
            }
            if (req.getTen() != null) c.setTen(req.getTen());
            if (req.getBieuTuong() != null) c.setBieuTuong(req.getBieuTuong());
            if (req.getMoTa() != null) c.setMoTa(req.getMoTa());
            return ResponseEntity.ok(catalogRepo.save(c));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/catalog/{id}/toggle")
    public ResponseEntity<?> toggleCatalog(@PathVariable Long id) {
        return catalogRepo.findById(id).map(c -> {
            boolean wantEnable = !Boolean.TRUE.equals(c.getHoatDong());
            if (wantEnable) {
                NhomTienNghi nhom = c.getNhom();
                if (nhom != null && !Boolean.TRUE.equals(nhom.getHoatDong())) {
                    return ResponseEntity.badRequest().body((Object) Map.of(
                            "message", "Nhóm tiện nghi \"" + nhom.getTenNhom() + "\" đang bị ẩn. Vui lòng hiển thị nhóm trước khi hiển thị tiện ích này."
                    ));
                }
            }
            c.setHoatDong(wantEnable);
            catalogRepo.save(c);
            return ResponseEntity.ok((Object) Map.of("hoatDong", c.getHoatDong()));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/catalog/{id}")
    public ResponseEntity<?> deleteCatalog(@PathVariable Long id) {
        return catalogRepo.findById(id).map(c -> {
            c.setHoatDong(false);
            catalogRepo.save(c);
            return ResponseEntity.ok(Map.of("hidden", true));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ========== DTOs ==========

    @Data
    public static class GroupRequest {
        private String tenNhom;
        private Integer thuTu;
        private Long hostId;
    }

    @Data
    public static class CatalogRequest {
        private Long maNhomTienNghi;
        private String ten;
        private String bieuTuong;
        private String moTa;
        private Long hostId;
    }
}
