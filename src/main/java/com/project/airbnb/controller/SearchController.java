package com.project.airbnb.controller;

import com.project.airbnb.dto.PhongDTO;
import com.project.airbnb.dto.SearchCriteria;
import com.project.airbnb.entity.Phong;
import com.project.airbnb.repository.DanhMucTienNghiRepository;
import com.project.airbnb.repository.NhomTienNghiRepository;
import com.project.airbnb.repository.PhongRepository;
import com.project.airbnb.service.ResponseCache;
import com.project.airbnb.service.SearchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
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

    /** Dated searches drive availability, so keep their cache very short. */
    private static final long SEARCH_TTL_MS = 45_000L;
    /**
     * Browse searches (homepage, city listings) change only when a host
     * publishes/edits a room. Five minutes keeps those pages consistently
     * fast without making edits invisible for long.
     */
    private static final long SEARCH_BROWSE_TTL_MS = 5 * 60_000L;
    private static final long LOCATIONS_TTL_MS = 10 * 60_000L;
    private static final String SEARCH_CACHE_CONTROL = "public, max-age=30, stale-while-revalidate=120";
    private static final String LOCATIONS_CACHE_CONTROL = "public, max-age=300, stale-while-revalidate=600";

    private final SearchService searchService;
    private final PhongRepository phongRepository;
    private final NhomTienNghiRepository nhomTienNghiRepository;
    private final DanhMucTienNghiRepository danhMucTienNghiRepository;
    private final ResponseCache responseCache;

    public SearchController(SearchService searchService,
            PhongRepository phongRepository,
            NhomTienNghiRepository nhomTienNghiRepository,
            DanhMucTienNghiRepository danhMucTienNghiRepository,
            ResponseCache responseCache) {
        this.searchService = searchService;
        this.phongRepository = phongRepository;
        this.nhomTienNghiRepository = nhomTienNghiRepository;
        this.danhMucTienNghiRepository = danhMucTienNghiRepository;
        this.responseCache = responseCache;
    }

    @GetMapping("/locations")
    public ResponseEntity<List<String>> getLocations() {
        // Used to run a full room search just to collect city names (2.3s).
        // A DISTINCT query returns the same list in a few milliseconds.
        List<String> locations = responseCache.getOrLoad(
                "search:locations:v1",
                LOCATIONS_TTL_MS,
                () -> phongRepository.findDistinctThanhPhoByLoaiPhong(Phong.LoaiPhong.noi_luu_tru));

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, LOCATIONS_CACHE_CONTROL)
                .body(locations);
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

        String cacheKey = buildCacheKey(criteria, imageLimit);
        long ttl = (criteria.getNgayNhan() != null || criteria.getNgayTra() != null)
                ? SEARCH_TTL_MS
                : SEARCH_BROWSE_TTL_MS;

        List<PhongDTO> results = responseCache.getOrLoad(cacheKey, ttl, () -> {
            List<PhongDTO> rooms = filterAmenities(searchService.search(criteria));

            // Homepage carousels only need the first few photos of each room.
            // Trim inside the loader so the cache stores the small payload.
            if (imageLimit != null && imageLimit >= 0) {
                rooms.forEach(dto -> {
                    var images = dto.getHinhAnhs();
                    if (images != null && images.size() > imageLimit) {
                        dto.setHinhAnhs(new java.util.ArrayList<>(images.subList(0, imageLimit)));
                    }
                });
            }
            return rooms;
        });

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, SEARCH_CACHE_CONTROL)
                .body(results);
    }

    private String buildCacheKey(SearchCriteria c, Integer imageLimit) {
        return "search:v2"
                + "|lp=" + text(c.getLoaiPhong())
                + "|tp=" + text(c.getThanhPho()).toLowerCase()
                + "|sk=" + text(c.getSoKhach())
                + "|nn=" + text(c.getNgayNhan())
                + "|nt=" + text(c.getNgayTra())
                + "|min=" + text(c.getMinPrice())
                + "|max=" + text(c.getMaxPrice())
                + "|tn=" + csv(c.getTienNghi())
                + "|lat=" + text(c.getMinLat()) + ":" + text(c.getMaxLat())
                + "|lng=" + text(c.getMinLng()) + ":" + text(c.getMaxLng())
                + "|lh=" + csv(c.getLoaiHinh())
                + "|img=" + text(imageLimit);
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private String csv(List<String> values) {
        return values == null ? "" : values.stream().sorted().collect(Collectors.joining(","));
    }

    private List<PhongDTO> filterAmenities(List<PhongDTO> rooms) {
        Set<String> globalActiveGroups = new HashSet<>(nhomTienNghiRepository.findActiveGroupNames());
        Set<String> globalHiddenCatalogs = new HashSet<>(danhMucTienNghiRepository.findHiddenCatalogNames());
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
