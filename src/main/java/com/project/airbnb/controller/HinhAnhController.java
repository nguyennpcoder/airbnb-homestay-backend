package com.project.airbnb.controller;

import com.project.airbnb.entity.HinhAnh;
import com.project.airbnb.entity.Phong;
import com.project.airbnb.entity.NguoiDung;
import com.project.airbnb.repository.HinhAnhRepository;
import com.project.airbnb.repository.NguoiDungRepository;
import com.project.airbnb.repository.PhongRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/images")
public class HinhAnhController {

    private static final Logger log = LoggerFactory.getLogger(HinhAnhController.class);

    private final HinhAnhRepository hinhAnhRepository;
    private final PhongRepository phongRepository;
    private final NguoiDungRepository nguoiDungRepository;

    public HinhAnhController(HinhAnhRepository hinhAnhRepository, PhongRepository phongRepository,
            NguoiDungRepository nguoiDungRepository) {
        this.hinhAnhRepository = hinhAnhRepository;
        this.phongRepository = phongRepository;
        this.nguoiDungRepository = nguoiDungRepository;
    }

    /**
     * Check if the given user ID is the owner (chuNha) of the listing.
     */
    private boolean isHostOwner(Phong phong, Long hostId) {
        if (phong == null || phong.getChuNha() == null || hostId == null) return false;
        return phong.getChuNha().getMaNguoiDung().equals(hostId);
    }

    private boolean isStillHost(Long hostId) {
        if (hostId == null) return false;
        return nguoiDungRepository.findById(hostId)
                .map(NguoiDung::getLaChuNha)
                .orElse(false);
    }

    /**
     * Resolve the effective user ID for authorization.
     * Priority: hostId param from frontend > JWT auth > null.
     */
    private Long getCurrentUserId(Long hostId) {
        if (hostId != null) {
            return hostId;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof UserDetails) {
                String email = ((UserDetails) principal).getUsername();
                if (email != null) {
                    Optional<NguoiDung> user = nguoiDungRepository.findByEmail(email);
                    if (user.isPresent()) {
                        return user.get().getMaNguoiDung();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Upload an image for a listing (phong).
     * If laAnhChinh=true or phong has no cover yet, automatically set as cover (urlAnhChinh).
     */
    @PostMapping(value = "/phong/{phongId}", consumes = "multipart/form-data")
    public ResponseEntity<?> upload(@PathVariable Long phongId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "thuTu", required = false) Integer thuTu,
            @RequestParam(value = "laAnhChinh", required = false) Boolean laAnhChinh,
            @RequestParam(value = "phanLoaiAnh", required = false) String phanLoaiAnh,
            @RequestParam(value = "hostId", required = false) Long hostId) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "No file uploaded"));
            }
            Phong phong = phongRepository.findByIdWithChuNha(phongId).orElse(null);
            if (phong == null) {
                return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy phòng"));
            }
            Long effectiveHostId = getCurrentUserId(hostId);
            if (effectiveHostId != null && !isHostOwner(phong, effectiveHostId)) {
                return ResponseEntity.status(403).body(Map.of("message", "Bạn không có quyền chỉnh sửa ảnh của phòng này"));
            }
            if (effectiveHostId != null && !isStillHost(effectiveHostId)) {
                return ResponseEntity.status(403).body(Map.of("message", "Tài khoản không còn quyền Host"));
            }

            // Save file to disk
            Path dir = Paths.get("uploads", "product");
            java.nio.file.Files.createDirectories(dir);
            String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image";
            String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : ".jpg";
            String filename = "product-" + phongId + "-" + System.currentTimeMillis() + ext.toLowerCase();
            Path dest = dir.resolve(filename);
            try (java.io.InputStream in = file.getInputStream()) {
                java.nio.file.Files.copy(in, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            String url = "/uploads/product/" + filename;

            HinhAnh ha = new HinhAnh();
            ha.setPhong(phong);
            ha.setUrlHinhAnh(url);
            if (thuTu != null) {
                ha.setThuTu(thuTu);
            }
            if (laAnhChinh != null) {
                ha.setLaAnhChinh(laAnhChinh);
            }
            if (phanLoaiAnh != null) {
                ha.setPhanLoaiAnh(phanLoaiAnh);
            }
            hinhAnhRepository.save(ha);

            // Sync cover image: set as cover if flagged, or if phong has no cover yet
            if (Boolean.TRUE.equals(laAnhChinh)
                    || phong.getUrlAnhChinh() == null
                    || phong.getUrlAnhChinh().isEmpty()
                    || "FILE_SELECTED".equals(phong.getUrlAnhChinh())) {
                phong.setUrlAnhChinh(url);
                phongRepository.save(phong);
                log.info("Set cover image for phong {}: {}", phongId, url);
            }

            return ResponseEntity.ok(Map.of(
                    "url", url,
                    "maHinhAnh", ha.getMaHinhAnh(),
                    "thuTu", ha.getThuTu(),
                    "laAnhChinh", ha.getLaAnhChinh() != null && ha.getLaAnhChinh(),
                    "phanLoaiAnh", ha.getPhanLoaiAnh() != null ? ha.getPhanLoaiAnh() : ""));
        } catch (Exception e) {
            log.error("Upload failed for phong {}: {}", phongId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("message", "Upload failed", "error", e.getMessage()));
        }
    }

    /**
     * Delete an image. Also removes the physical file and handles cover image fallback:
     * if the deleted image was the cover, the next image in order becomes cover.
     */
    @DeleteMapping("/{maHinhAnh}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long maHinhAnh,
            @RequestParam(value = "hostId", required = false) Long hostId) {
        HinhAnh ha = hinhAnhRepository.findByIdWithPhongAndChuNha(maHinhAnh).orElse(null);
        if (ha == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy hình ảnh"));
        }

        Phong phong = ha.getPhong();
        Long effectiveHostId = getCurrentUserId(hostId);
        if (effectiveHostId != null && !isHostOwner(phong, effectiveHostId)) {
            return ResponseEntity.status(403).body(Map.of("message", "Bạn không có quyền xóa ảnh của phòng này"));
        }
        if (effectiveHostId != null && !isStillHost(effectiveHostId)) {
            return ResponseEntity.status(403).body(Map.of("message", "Tài khoản không còn quyền Host"));
        }

        boolean wasCover = phong != null && ha.getUrlHinhAnh() != null
                && ha.getUrlHinhAnh().equals(phong.getUrlAnhChinh());

        // Delete physical file from disk
        try {
            if (ha.getUrlHinhAnh() != null) {
                Path filePath = Paths.get(".").resolve(ha.getUrlHinhAnh().substring(1)); // remove leading /
                java.nio.file.Files.deleteIfExists(filePath);
                log.info("Deleted physical file: {}", filePath);
            }
        } catch (Exception e) {
            log.warn("Could not delete physical file for image {}: {}", maHinhAnh, e.getMessage());
        }

        hinhAnhRepository.deleteById(maHinhAnh);

        String newCoverUrl = null;

        // Handle cover image fallback: if deleted image was the cover, find next image
        if (wasCover && phong != null) {
            List<HinhAnh> remaining = hinhAnhRepository.findByPhong_MaPhongOrderByThuTuAsc(phong.getMaPhong());
            if (!remaining.isEmpty()) {
                HinhAnh newCover = remaining.get(0);
                newCover.setLaAnhChinh(true);
                hinhAnhRepository.save(newCover);
                newCoverUrl = newCover.getUrlHinhAnh();
                log.info("Fallback cover for phong {} → image {}", phong.getMaPhong(), newCover.getMaHinhAnh());
            } else {
                newCoverUrl = null;
                log.info("No images left for phong {}, cleared cover", phong.getMaPhong());
            }
            phong.setUrlAnhChinh(newCoverUrl);
            phongRepository.save(phong);
        }

        return ResponseEntity.ok(Map.of(
                "deleted", true,
                "maHinhAnh", maHinhAnh,
                "wasCover", wasCover,
                "newCoverUrl", newCoverUrl != null ? newCoverUrl : ""));
    }

    /**
     * Set an image as the main/cover image.
     * Updates both hinh_anh.la_anh_chinh flags and phong.url_anh_chinh.
     */
    @PutMapping("/{maHinhAnh}/set-main")
    @Transactional
    public ResponseEntity<?> setMain(@PathVariable Long maHinhAnh,
            @RequestParam(value = "hostId", required = false) Long hostId) {
        HinhAnh ha = hinhAnhRepository.findByIdWithPhongAndChuNha(maHinhAnh).orElse(null);
        if (ha == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy hình ảnh"));
        }

        Phong phong = ha.getPhong();
        if (phong == null) {
            return ResponseEntity.status(400).body(Map.of("message", "Hình ảnh không thuộc phòng nào"));
        }
        Long effectiveHostId = getCurrentUserId(hostId);
        if (effectiveHostId != null && !isHostOwner(phong, effectiveHostId)) {
            return ResponseEntity.status(403).body(Map.of("message", "Bạn không có quyền chỉnh sửa ảnh của phòng này"));
        }
        if (effectiveHostId != null && !isStillHost(effectiveHostId)) {
            return ResponseEntity.status(403).body(Map.of("message", "Tài khoản không còn quyền Host"));
        }

        // Clear all laAnhChinh flags for this listing
        List<HinhAnh> allImages = hinhAnhRepository.findByPhong_MaPhongOrderByThuTuAsc(phong.getMaPhong());
        for (HinhAnh img : allImages) {
            if (Boolean.TRUE.equals(img.getLaAnhChinh())) {
                img.setLaAnhChinh(false);
                hinhAnhRepository.save(img);
            }
        }

        // Set the selected image as main
        ha.setLaAnhChinh(true);
        hinhAnhRepository.save(ha);

        // Update phong cover image
        phong.setUrlAnhChinh(ha.getUrlHinhAnh());
        phongRepository.save(phong);

        log.info("Set main image for phong {}: {}", phong.getMaPhong(), ha.getUrlHinhAnh());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "maHinhAnh", maHinhAnh,
                "urlHinhAnh", ha.getUrlHinhAnh()));
    }

    /**
     * Get all images for a listing.
     */
    @GetMapping("/phong/{phongId}")
    public ResponseEntity<?> getByPhong(@PathVariable Long phongId) {
        Phong phong = phongRepository.findById(phongId).orElse(null);
        if (phong == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy phòng"));
        }
        List<HinhAnh> images = hinhAnhRepository.findByPhong_MaPhongOrderByThuTuAsc(phongId);
        return ResponseEntity.ok(images.stream().map(h -> Map.of(
                "maHinhAnh", h.getMaHinhAnh(),
                "urlHinhAnh", h.getUrlHinhAnh(),
                "thuTu", h.getThuTu() != null ? h.getThuTu() : 0,
                "laAnhChinh", h.getLaAnhChinh() != null && h.getLaAnhChinh(),
                "phanLoaiAnh", h.getPhanLoaiAnh() != null ? h.getPhanLoaiAnh() : "")).toList());
    }
}
