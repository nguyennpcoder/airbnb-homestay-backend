package com.project.airbnb.controller;

import com.project.airbnb.dto.PhongDTO;
import com.project.airbnb.entity.Phong;
import com.project.airbnb.entity.NguoiDung;
import com.project.airbnb.repository.HinhAnhRepository;
import com.project.airbnb.repository.NguoiDungRepository;
import com.project.airbnb.repository.PhongRepository;
import com.project.airbnb.repository.TienNghiPhongRepository;
import com.project.airbnb.repository.NhomTienNghiRepository;
import com.project.airbnb.repository.DanhMucTienNghiRepository;
import com.project.airbnb.service.ResponseCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping({ "/api/public/phong" })
public class PhongController {
    private static final Logger log = LoggerFactory.getLogger(PhongController.class);

    private final PhongRepository phongRepository;
    private final HinhAnhRepository hinhAnhRepository;
    private final TienNghiPhongRepository tienNghiPhongRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final NhomTienNghiRepository nhomTienNghiRepository;
    private final DanhMucTienNghiRepository danhMucTienNghiRepository;
    private final ResponseCache responseCache;

    private static final long ROOM_TTL_MS = 60_000L;
    private static final String ROOM_CACHE_CONTROL = "public, max-age=60, stale-while-revalidate=300";

    public PhongController(PhongRepository phongRepository,
            HinhAnhRepository hinhAnhRepository,
            TienNghiPhongRepository tienNghiPhongRepository,
            NguoiDungRepository nguoiDungRepository,
            NhomTienNghiRepository nhomTienNghiRepository,
            DanhMucTienNghiRepository danhMucTienNghiRepository,
            ResponseCache responseCache) {
        this.phongRepository = phongRepository;
        this.hinhAnhRepository = hinhAnhRepository;
        this.tienNghiPhongRepository = tienNghiPhongRepository;
        this.nguoiDungRepository = nguoiDungRepository;
        this.nhomTienNghiRepository = nhomTienNghiRepository;
        this.danhMucTienNghiRepository = danhMucTienNghiRepository;
        this.responseCache = responseCache;
    }

    private Optional<Long> getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserDetails) {
                String email = ((UserDetails) principal).getUsername();
                if (email != null) {
                    return nguoiDungRepository.findByEmail(email)
                            .map(NguoiDung::getMaNguoiDung);
                }
            }
        }
        return Optional.empty();
    }

    @GetMapping("/{id}")
    @Transactional
    public ResponseEntity<PhongDTO> getPhongById(@PathVariable Long id) {
        Optional<Long> currentUserId = getCurrentUserId();

        // A logged-in host may be previewing a room that is not public yet.
        // Never share that response through the cache.
        if (currentUserId.isPresent()) {
            return loadRoom(id, currentUserId.get())
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }

        // The page waits for this call before it can render the hero image.
        // It used to run 6 sequential queries on every visit (~2.4s); cache
        // the assembled DTO briefly so the next visitors get it instantly.
        Optional<PhongDTO> room = responseCache.getOrLoad(
                "public-phong:v2:" + id,
                ROOM_TTL_MS,
                () -> loadRoom(id, null));

        return room
                .map(dto -> ResponseEntity.ok()
                        .header(HttpHeaders.CACHE_CONTROL, ROOM_CACHE_CONTROL)
                        .body(dto))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Loads a room and builds its DTO. Must run inside a transaction. */
    private Optional<PhongDTO> loadRoom(Long id, Long currentUserId) {
        phongRepository.incrementViews(id);
        return phongRepository.findById(id)
                .map(phong -> {
                    if (Boolean.TRUE.equals(phong.getBiKhoa())) {
                        return null;
                    }
                    boolean isHost = currentUserId != null
                            && currentUserId.equals(phong.getChuNha().getMaNguoiDung());
                    if (!isHost && phong.getTrangThai() != Phong.TrangThaiPhong.hoat_dong) {
                        return null;
                    }
                    log.info("Loading room {}: {}", id, phong.getTieuDe());
                    var hinhAnhs = hinhAnhRepository.findByPhong_MaPhongOrderByThuTuAsc(id);
                    var amenities = tienNghiPhongRepository.findByPhong_MaPhongOrderByNhomAscTenAsc(id);
                    PhongDTO dto = PhongDTO.fromEntity(phong, hinhAnhs, amenities);

                    // Filter amenities by host's active groups/catalogs
                    Long hostId = phong.getChuNha() != null ? phong.getChuNha().getMaNguoiDung() : null;
                    if (hostId != null) {
                        java.util.Set<String> activeGroups = new java.util.HashSet<>(nhomTienNghiRepository.findActiveGroupNamesByHost(hostId));
                        java.util.Set<String> hiddenCatalogs = new java.util.HashSet<>(danhMucTienNghiRepository.findHiddenCatalogNamesByHost(hostId));
                        dto.setTienNghi(PhongDTO.filterTienNghi(dto.getTienNghi(), activeGroups, hiddenCatalogs));
                    }

                    return dto;
                });
    }

    @GetMapping("/yeu-thich")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PhongDTO>> getLovedByGuests() {
        List<PhongDTO> dtoList = phongRepository.findLovedByGuests().stream()
                .map(phong -> {
                    PhongDTO dto = PhongDTO.fromEntity(phong);
                    Long hostId = phong.getChuNha() != null ? phong.getChuNha().getMaNguoiDung() : null;
                    if (hostId != null) {
                        java.util.Set<String> activeGroups = new java.util.HashSet<>(nhomTienNghiRepository.findActiveGroupNamesByHost(hostId));
                        java.util.Set<String> hiddenCatalogs = new java.util.HashSet<>(danhMucTienNghiRepository.findHiddenCatalogNamesByHost(hostId));
                        dto.setTienNghi(PhongDTO.filterTienNghi(dto.getTienNghi(), activeGroups, hiddenCatalogs));
                    }
                    return dto;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }
}
