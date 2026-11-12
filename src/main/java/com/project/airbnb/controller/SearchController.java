package com.project.airbnb.controller;

import com.project.airbnb.dto.PhongDTO;
import com.project.airbnb.dto.SearchCriteria;
import com.project.airbnb.entity.Phong;
import com.project.airbnb.repository.NhomTienNghiRepository;
import com.project.airbnb.repository.DanhMucTienNghiRepository;
import com.project.airbnb.service.SearchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/search")
public class SearchController {

    private final SearchService searchService;
    private final NhomTienNghiRepository nhomTienNghiRepository;
    private final DanhMucTienNghiRepository danhMucTienNghiRepository;

    public SearchController(SearchService searchService,
            NhomTienNghiRepository nhomTienNghiRepository,
            DanhMucTienNghiRepository danhMucTienNghiRepository) {
        this.searchService = searchService;
        this.nhomTienNghiRepository = nhomTienNghiRepository;
        this.danhMucTienNghiRepository = danhMucTienNghiRepository;
    }

    @GetMapping("/locations")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<List<String>> getLocations() {
        var criteria = SearchCriteria.builder().loaiPhong(Phong.LoaiPhong.noi_luu_tru).build();
        List<PhongDTO> rooms = searchService.search(criteria);
        List<String> locations = rooms.stream()
                .map(PhongDTO::getThanhPho)
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return ResponseEntity.ok(locations);
    }

    @GetMapping
    public ResponseEntity<List<PhongDTO>> search(
            @RequestParam(required = false) String loaiPhong,
            @RequestParam(required = false, name = "loaiSanPham") String loaiSanPham,
            @RequestParam(required = false) String thanhPho,
            @RequestParam(required = false) Integer soKhach,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngayNhan,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngayTra,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) List<String> tienNghi,
            @RequestParam(required = false) BigDecimal minLat,
            @RequestParam(required = false) BigDecimal maxLat,
            @RequestParam(required = false) BigDecimal minLng,
            @RequestParam(required = false) BigDecimal maxLng,
            @RequestParam(required = false) List<String> loaiHinh,
            @RequestParam(required = false) Integer imageLimit) {
        if (ngayNhan != null && ngayTra != null && !ngayNhan.isBefore(ngayTra)) {
            return ResponseEntity.badRequest().build();
        }

        String loaiRaw = StringUtils.hasText(loaiPhong) ? loaiPhong : loaiSanPham;

        SearchCriteria criteria = SearchCriteria.builder()
                .loaiPhong(parseLoaiPhong(loaiRaw))
                .thanhPho(thanhPho != null ? thanhPho.trim() : null)
                .soKhach(soKhach)
                .ngayNhan(ngayNhan)
                .ngayTra(ngayTra)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .tienNghi(normalizeList(tienNghi))
                .minLat(minLat)
                .maxLat(maxLat)
                .minLng(minLng)
                .maxLng(maxLng)
                .loaiHinh(normalizeList(loaiHinh))
                .build();

        List<PhongDTO> results = filterAmenities(searchService.search(criteria));

        // Homepage carousels only need the first few photos of each room.
        // Trimming here keeps the JSON small and skips serializing ~1000 image
        // rows, which matters a lot on a 0.1 CPU Render instance.
        if (imageLimit != null && imageLimit >= 0) {
            results.forEach(dto -> {
                var images = dto.getHinhAnhs();
                if (images != null && images.size() > imageLimit) {
                    dto.setHinhAnhs(new java.util.ArrayList<>(images.subList(0, imageLimit)));
                }
            });
        }

        return ResponseEntity.ok(results);
    }

    private List<PhongDTO> filterAmenities(List<PhongDTO> rooms) {
        // Cache per host to avoid repeated queries
        java.util.Map<Long, Set<String>> activeGroupsCache = new java.util.HashMap<>();
        java.util.Map<Long, Set<String>> hiddenCatalogsCache = new java.util.HashMap<>();

        rooms.forEach(dto -> {
            // We need the host ID from the room, but PhongDTO doesn't store it directly
            // The filtering will be done per-host using the cache
            // For search results, we use the global fallback (all groups active)
        });

        // For search results, use global active groups as fallback
        java.util.Set<String> globalActiveGroups = new HashSet<>(nhomTienNghiRepository.findActiveGroupNames());
        java.util.Set<String> globalHiddenCatalogs = new HashSet<>(danhMucTienNghiRepository.findHiddenCatalogNames());
        rooms.forEach(dto -> dto.setTienNghi(PhongDTO.filterTienNghi(dto.getTienNghi(), globalActiveGroups, globalHiddenCatalogs)));
        return rooms;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllExceptions(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error: " + ex.getMessage());
    }

    private Phong.LoaiPhong parseLoaiPhong(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String normalized = raw.trim();
        for (Phong.LoaiPhong type : Phong.LoaiPhong.values()) {
            if (type.name().equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return null;
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) {
            return null;
        }
        List<String> normalized = values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.toList());
        return normalized.isEmpty() ? null : normalized;
    }
}
