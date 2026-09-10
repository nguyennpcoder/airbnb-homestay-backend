package com.project.airbnb.controller;

import com.project.airbnb.entity.DanhGia;
import com.project.airbnb.entity.DanhGiaHuuIch;
import com.project.airbnb.entity.DatCho;
import com.project.airbnb.entity.Phong;
import com.project.airbnb.repository.DanhGiaRepository;
import com.project.airbnb.repository.DanhGiaHuuIchRepository;
import com.project.airbnb.repository.DatChoRepository;
import com.project.airbnb.repository.PhongRepository;
import com.project.airbnb.repository.NguoiDungRepository;
import lombok.Data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/danh-gia")
public class
DanhGiaController {
    private static final Logger log = LoggerFactory.getLogger(DanhGiaController.class);

    private final DanhGiaRepository danhGiaRepository;
    private final DanhGiaHuuIchRepository danhGiaHuuIchRepository;
    private final PhongRepository phongRepository;
    private final DatChoRepository datChoRepository;
    private final NguoiDungRepository nguoiDungRepository;

    public DanhGiaController(DanhGiaRepository danhGiaRepository, DanhGiaHuuIchRepository danhGiaHuuIchRepository,
            PhongRepository phongRepository, DatChoRepository datChoRepository, NguoiDungRepository nguoiDungRepository) {
        this.danhGiaRepository = danhGiaRepository;
        this.danhGiaHuuIchRepository = danhGiaHuuIchRepository;
        this.phongRepository = phongRepository;
        this.datChoRepository = datChoRepository;
        this.nguoiDungRepository = nguoiDungRepository;
    }

    @GetMapping("/phong/{maPhong}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ReviewResponse>> getByPhong(@PathVariable Long maPhong, @RequestParam(defaultValue = "0") Long userId) {
        List<DanhGia> entities = danhGiaRepository.findByPhong_MaPhong(maPhong);

        List<ReviewResponse> result = entities.stream().map(dg -> {
            ReviewResponse dto = new ReviewResponse();
            dto.setMaDanhGia(dg.getMaDanhGia());
            if (dg.getPhong() != null) dto.setMaSanPham(dg.getPhong().getMaPhong());
            dto.setDiemSo(dg.getDiemSo() != null ? dg.getDiemSo().doubleValue() : 0.0);
            dto.setBinhLuan(dg.getBinhLuan());
            dto.setPhanHoi(dg.getPhanHoi());
            dto.setNgayTao(dg.getNgayTao());
            dto.setSoLuotHuuIch(dg.getSoLuotHuuIch() != null ? dg.getSoLuotHuuIch() : 0);

            if (userId > 0) {
                dto.setIsHuuIchByCurrentUser(danhGiaHuuIchRepository.existsByDanhGia_MaDanhGiaAndNguoiDung_MaNguoiDung(dg.getMaDanhGia(), userId));
            } else {
                dto.setIsHuuIchByCurrentUser(false);
            }

            if (dg.getKhach() != null) {
                ReviewUser user = new ReviewUser();
                user.setMaNguoiDung(dg.getKhach().getMaNguoiDung());

                String displayName = "";
                if (dg.getKhach().getHo() != null && !dg.getKhach().getHo().isEmpty()
                        && dg.getKhach().getTen() != null && !dg.getKhach().getTen().isEmpty()) {
                    displayName = (dg.getKhach().getHo() + " " + dg.getKhach().getTen()).trim();
                } else if (dg.getKhach().getHoTen() != null && !dg.getKhach().getHoTen().isEmpty()) {
                    displayName = dg.getKhach().getHoTen();
                }
                if (displayName.isEmpty() && dg.getKhach().getEmail() != null) {
                    String email = dg.getKhach().getEmail();
                    displayName = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
                }
                user.setHoTen(displayName);
                user.setAvatarUrl(dg.getKhach().getUrlAnhDaiDien());
                user.setUrlAnhDaiDien(dg.getKhach().getUrlAnhDaiDien());
                user.setThanhPho(dg.getKhach().getThanhPhoSong());
                dto.setKhach(user);
            }

            if (dg.getPhong() != null && dg.getPhong().getChuNha() != null) {
                ReviewUser host = new ReviewUser();
                host.setMaNguoiDung(dg.getPhong().getChuNha().getMaNguoiDung());
                String hostName = "";
                if (dg.getPhong().getChuNha().getHo() != null && !dg.getPhong().getChuNha().getHo().isEmpty()
                        && dg.getPhong().getChuNha().getTen() != null && !dg.getPhong().getChuNha().getTen().isEmpty()) {
                    hostName = (dg.getPhong().getChuNha().getHo() + " " + dg.getPhong().getChuNha().getTen()).trim();
                } else if (dg.getPhong().getChuNha().getHoTen() != null && !dg.getPhong().getChuNha().getHoTen().isEmpty()) {
                    hostName = dg.getPhong().getChuNha().getHoTen();
                }
                if (hostName.isEmpty() && dg.getPhong().getChuNha().getEmail() != null) {
                    String email = dg.getPhong().getChuNha().getEmail();
                    hostName = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
                }
                host.setHoTen(hostName);
                host.setAvatarUrl(dg.getPhong().getChuNha().getUrlAnhDaiDien());
                host.setUrlAnhDaiDien(dg.getPhong().getChuNha().getUrlAnhDaiDien());
                dto.setChuNha(host);
            }

            dto.setDiemSachSe(dg.getDiemSachSe() != null ? dg.getDiemSachSe().doubleValue() : null);
            dto.setDiemChinhXac(dg.getDiemChinhXac() != null ? dg.getDiemChinhXac().doubleValue() : null);
            dto.setDiemNhanPhong(dg.getDiemNhanPhong() != null ? dg.getDiemNhanPhong().doubleValue() : null);
            dto.setDiemGiaoTiep(dg.getDiemGiaoTiep() != null ? dg.getDiemGiaoTiep().doubleValue() : null);
            dto.setDiemViTri(dg.getDiemViTri() != null ? dg.getDiemViTri().doubleValue() : null);
            dto.setDiemGiaTri(dg.getDiemGiaTri() != null ? dg.getDiemGiaTri().doubleValue() : null);

            if (dg.getDatCho() != null) {
                dto.setMaDatCho(dg.getDatCho().getMaDatCho());
                dto.setNgayNhanPhong(dg.getDatCho().getNgayNhanPhong());
                dto.setNgayTraPhong(dg.getDatCho().getNgayTraPhong());
            }

            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/host/{hostId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ReviewResponse>> getByHost(@PathVariable Long hostId,
            @RequestParam(defaultValue = "0") Long userId) {
        List<DanhGia> entities = danhGiaRepository.findByPhong_ChuNha_MaNguoiDung(hostId);

        List<ReviewResponse> result = entities.stream().map(dg -> {
            ReviewResponse dto = new ReviewResponse();
            dto.setMaDanhGia(dg.getMaDanhGia());
            if (dg.getPhong() != null) dto.setMaSanPham(dg.getPhong().getMaPhong());
            dto.setDiemSo(dg.getDiemSo() != null ? dg.getDiemSo().doubleValue() : 0.0);
            dto.setBinhLuan(dg.getBinhLuan());
            dto.setPhanHoi(dg.getPhanHoi());
            dto.setNgayTao(dg.getNgayTao());
            dto.setSoLuotHuuIch(dg.getSoLuotHuuIch() != null ? dg.getSoLuotHuuIch() : 0);
            dto.setIsHuuIchByCurrentUser(userId != null && userId > 0
                    && danhGiaHuuIchRepository.existsByDanhGia_MaDanhGiaAndNguoiDung_MaNguoiDung(dg.getMaDanhGia(), userId));

            if (dg.getKhach() != null) {
                ReviewUser user = new ReviewUser();
                user.setMaNguoiDung(dg.getKhach().getMaNguoiDung());
                String displayName = (dg.getKhach().getHoTen() != null) ? dg.getKhach().getHoTen() : "";
                user.setHoTen(displayName);
                user.setAvatarUrl(dg.getKhach().getUrlAnhDaiDien());
                user.setUrlAnhDaiDien(dg.getKhach().getUrlAnhDaiDien());
                user.setThanhPho(dg.getKhach().getThanhPhoSong());
                dto.setKhach(user);
            }

            if (dg.getPhong() != null && dg.getPhong().getChuNha() != null) {
                ReviewUser host = new ReviewUser();
                host.setMaNguoiDung(dg.getPhong().getChuNha().getMaNguoiDung());
                String hostName = (dg.getPhong().getChuNha().getHoTen() != null) ? dg.getPhong().getChuNha().getHoTen() : "";
                host.setHoTen(hostName);
                host.setAvatarUrl(dg.getPhong().getChuNha().getUrlAnhDaiDien());
                host.setUrlAnhDaiDien(dg.getPhong().getChuNha().getUrlAnhDaiDien());
                dto.setChuNha(host);
            }

            dto.setDiemSachSe(dg.getDiemSachSe() != null ? dg.getDiemSachSe().doubleValue() : null);
            dto.setDiemChinhXac(dg.getDiemChinhXac() != null ? dg.getDiemChinhXac().doubleValue() : null);
            dto.setDiemNhanPhong(dg.getDiemNhanPhong() != null ? dg.getDiemNhanPhong().doubleValue() : null);
            dto.setDiemGiaoTiep(dg.getDiemGiaoTiep() != null ? dg.getDiemGiaoTiep().doubleValue() : null);
            dto.setDiemViTri(dg.getDiemViTri() != null ? dg.getDiemViTri().doubleValue() : null);
            dto.setDiemGiaTri(dg.getDiemGiaTri() != null ? dg.getDiemGiaTri().doubleValue() : null);

            if (dg.getDatCho() != null) {
                dto.setMaDatCho(dg.getDatCho().getMaDatCho());
                dto.setNgayNhanPhong(dg.getDatCho().getNgayNhanPhong());
                dto.setNgayTraPhong(dg.getDatCho().getNgayTraPhong());
            }

            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/user/{userId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ReviewResponse>> getByUser(@PathVariable Long userId) {
        List<DanhGia> entities = danhGiaRepository.findByKhach_MaNguoiDung(userId);

        List<ReviewResponse> result = entities.stream().map(dg -> {
            ReviewResponse dto = new ReviewResponse();
            dto.setMaDanhGia(dg.getMaDanhGia());
            if (dg.getPhong() != null) {
                dto.setMaSanPham(dg.getPhong().getMaPhong());
                dto.setTieuDePhong(dg.getPhong().getTieuDe());
                dto.setUrlAnhPhong(dg.getPhong().getUrlAnhChinh());
            }
            dto.setDiemSo(dg.getDiemSo() != null ? dg.getDiemSo().doubleValue() : 0.0);
            dto.setBinhLuan(dg.getBinhLuan());
            dto.setPhanHoi(dg.getPhanHoi());
            dto.setNgayTao(dg.getNgayTao());
            dto.setSoLuotHuuIch(dg.getSoLuotHuuIch() != null ? dg.getSoLuotHuuIch() : 0);
            dto.setIsHuuIchByCurrentUser(danhGiaHuuIchRepository.existsByDanhGia_MaDanhGiaAndNguoiDung_MaNguoiDung(dg.getMaDanhGia(), userId));
            if (dg.getDatCho() != null) {
                dto.setMaDatCho(dg.getDatCho().getMaDatCho());
            }
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/phong/{maPhong}/category-averages")
    public ResponseEntity<CategoryAveragesResponse> getCategoryAverages(@PathVariable Long maPhong) {
        List<DanhGia> reviews = danhGiaRepository.findByPhong_MaPhong(maPhong);

        if (reviews.isEmpty()) {
            return ResponseEntity.ok(new CategoryAveragesResponse());
        }

        double avgSachSe = reviews.stream()
                .filter(r -> r.getDiemSachSe() != null)
                .mapToDouble(r -> r.getDiemSachSe().doubleValue())
                .average()
                .orElse(0.0);

        double avgChinhXac = reviews.stream()
                .filter(r -> r.getDiemChinhXac() != null)
                .mapToDouble(r -> r.getDiemChinhXac().doubleValue())
                .average()
                .orElse(0.0);

        double avgNhanPhong = reviews.stream()
                .filter(r -> r.getDiemNhanPhong() != null)
                .mapToDouble(r -> r.getDiemNhanPhong().doubleValue())
                .average()
                .orElse(0.0);

        double avgGiaoTiep = reviews.stream()
                .filter(r -> r.getDiemGiaoTiep() != null)
                .mapToDouble(r -> r.getDiemGiaoTiep().doubleValue())
                .average()
                .orElse(0.0);

        double avgViTri = reviews.stream()
                .filter(r -> r.getDiemViTri() != null)
                .mapToDouble(r -> r.getDiemViTri().doubleValue())
                .average()
                .orElse(0.0);

        double avgGiaTri = reviews.stream()
                .filter(r -> r.getDiemGiaTri() != null)
                .mapToDouble(r -> r.getDiemGiaTri().doubleValue())
                .average()
                .orElse(0.0);

        CategoryAveragesResponse response = new CategoryAveragesResponse();
        response.setDiemSachSe(avgSachSe > 0 ? avgSachSe : null);
        response.setDiemChinhXac(avgChinhXac > 0 ? avgChinhXac : null);
        response.setDiemNhanPhong(avgNhanPhong > 0 ? avgNhanPhong : null);
        response.setDiemGiaoTiep(avgGiaoTiep > 0 ? avgGiaoTiep : null);
        response.setDiemViTri(avgViTri > 0 ? avgViTri : null);
        response.setDiemGiaTri(avgGiaTri > 0 ? avgGiaTri : null);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody CreateReviewRequest req) {
        try {
        Phong sp = phongRepository.findById(req.getPhongId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        if (req.getMaDatCho() != null) {
            var booking = datChoRepository.findById(req.getMaDatCho())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt chỗ"));
            boolean isCompleted = booking.getTrangThaiDatCho() == DatCho.TrangThaiDatCho.hoan_thanh;
            boolean isPastConfirmed = booking.getTrangThaiDatCho() == DatCho.TrangThaiDatCho.da_xac_nhan
                    && ((booking.getNgayTraPhong() != null
                            && booking.getNgayTraPhong().isBefore(java.time.LocalDate.now())) ||
                            (booking.getNgayDat() != null && booking.getNgayDat().isBefore(java.time.LocalDate.now())));

            if (!isCompleted && !isPastConfirmed) {
                return ResponseEntity.badRequest()
                        .body(java.util.Map.of("message", "Chỉ đánh giá sau khi hoàn thành đặt chỗ"));
            }
            boolean exists = danhGiaRepository.findByPhong_MaPhong(sp.getMaPhong()).stream()
                    .anyMatch(r -> r.getDatCho() != null && r.getDatCho().getMaDatCho().equals(req.getMaDatCho())
                            && r.getKhach() != null && r.getKhach().getMaNguoiDung().equals(req.getMaKhach()));
            if (exists)
                return ResponseEntity.badRequest().body(java.util.Map.of("message", "Bạn đã đánh giá đặt chỗ này"));
        }

        DanhGia dg = new DanhGia();
        dg.setPhong(sp);
        dg.setLoaiPhong(sp.getLoaiPhong());
        com.project.airbnb.entity.NguoiDung userRef = new com.project.airbnb.entity.NguoiDung();
        userRef.setMaNguoiDung(req.getMaKhach());
        dg.setKhach(userRef);
        if (req.getMaDatCho() != null) {
            DatCho dc = new DatCho();
            dc.setMaDatCho(req.getMaDatCho());
            dg.setDatCho(dc);
        }

        if (req.getDiemSachSe() != null) dg.setDiemSachSe(BigDecimal.valueOf(req.getDiemSachSe()));
        if (req.getDiemChinhXac() != null) dg.setDiemChinhXac(BigDecimal.valueOf(req.getDiemChinhXac()));
        if (req.getDiemNhanPhong() != null) dg.setDiemNhanPhong(BigDecimal.valueOf(req.getDiemNhanPhong()));
        if (req.getDiemGiaoTiep() != null) dg.setDiemGiaoTiep(BigDecimal.valueOf(req.getDiemGiaoTiep()));
        if (req.getDiemViTri() != null) dg.setDiemViTri(BigDecimal.valueOf(req.getDiemViTri()));
        if (req.getDiemGiaTri() != null) dg.setDiemGiaTri(BigDecimal.valueOf(req.getDiemGiaTri()));

        double overallScore = req.calculateOverallScore();
        dg.setDiemSo(BigDecimal.valueOf(overallScore));
        dg.setBinhLuan(req.getBinhLuan());

        DanhGia saved = danhGiaRepository.save(dg);

        List<DanhGia> spReviews = danhGiaRepository.findByPhong_MaPhong(sp.getMaPhong());
        double spAvg = spReviews.stream().mapToDouble(r -> r.getDiemSo().doubleValue()).average().orElse(0.0);
        sp.setDiemTrungBinh(BigDecimal.valueOf(spAvg));
        sp.setSoLuongDanhGia(spReviews.size());
        sp.setDuocKhachYeuThich(spReviews.size() > 0);
        phongRepository.save(sp);

        Long hostId = sp.getChuNha().getMaNguoiDung();
        Double hostAvg = danhGiaRepository.getAverageRatingByHost(hostId);
        Long hostCount = danhGiaRepository.countByPhong_ChuNha_MaNguoiDung(hostId);

        com.project.airbnb.entity.NguoiDung host = sp.getChuNha();
        host.setDiemDanhGiaHost(hostAvg != null ? BigDecimal.valueOf(hostAvg) : null);
        host.setSoLuongDanhGiaHost(hostCount != null ? hostCount.intValue() : 0);
        nguoiDungRepository.save(host);

        return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Review create error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"));
        }
    }

    @PostMapping("/{reviewId}/reply")
    @Transactional
    public ResponseEntity<?> reply(@PathVariable Long reviewId, @RequestBody ReplyRequest req,
            @RequestParam(required = false) Long userId) {
        try {
            DanhGia dg = danhGiaRepository.findById(reviewId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));
            requireHost(userId, dg);

            dg.setPhanHoi(req.getNoiDung());
            danhGiaRepository.save(dg);

            return ResponseEntity.ok(java.util.Map.of("message", "Đã phản hồi đánh giá", "reviewId", reviewId));
        } catch (Exception e) {
            log.error("Lỗi phản hồi đánh giá: {}", e.getMessage(), e);
            return ResponseEntity.status(403).body(java.util.Map.of("message", e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"));
        }
    }

    @PutMapping("/{reviewId}/reply")
    @Transactional
    public ResponseEntity<?> updateReply(@PathVariable Long reviewId, @RequestBody ReplyRequest req,
            @RequestParam(required = false) Long userId) {
        try {
            DanhGia dg = danhGiaRepository.findById(reviewId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));
            requireHost(userId, dg);

            dg.setPhanHoi(req.getNoiDung());
            danhGiaRepository.save(dg);

            return ResponseEntity.ok(java.util.Map.of("message", "Đã cập nhật phản hồi", "reviewId", reviewId));
        } catch (Exception e) {
            log.error("Lỗi cập nhật phản hồi: {}", e.getMessage(), e);
            return ResponseEntity.status(403).body(java.util.Map.of("message", e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"));
        }
    }

    @DeleteMapping("/{reviewId}/reply")
    @Transactional
    public ResponseEntity<?> deleteReply(@PathVariable Long reviewId,
            @RequestParam(required = false) Long userId) {
        try {
            DanhGia dg = danhGiaRepository.findById(reviewId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));
            requireHost(userId, dg);

            dg.setPhanHoi(null);
            danhGiaRepository.save(dg);

            return ResponseEntity.ok(java.util.Map.of("message", "Đã xóa phản hồi", "reviewId", reviewId));
        } catch (Exception e) {
            log.error("Lỗi xóa phản hồi: {}", e.getMessage(), e);
            return ResponseEntity.status(403).body(java.util.Map.of("message", e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"));
        }
    }

    // Chỉ chủ nhà (la_chu_nha) của chính phòng đó mới được phản hồi/chỉnh sửa/xóa
    private void requireHost(Long userId, DanhGia dg) {
        if (userId == null) {
            throw new RuntimeException("Cần đăng nhập để thực hiện thao tác này");
        }
        com.project.airbnb.entity.NguoiDung user = nguoiDungRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        if (!Boolean.TRUE.equals(user.getLaChuNha())) {
            throw new RuntimeException("Chỉ chủ nhà mới có quyền phản hồi đánh giá");
        }
        if (dg.getPhong() == null || dg.getPhong().getChuNha() == null
                || !dg.getPhong().getChuNha().getMaNguoiDung().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền phản hồi đánh giá này");
        }
    }

    @PostMapping("/{reviewId}/huu-ich")
    @Transactional
    public ResponseEntity<?> markHuuIch(@PathVariable Long reviewId, @RequestParam(defaultValue = "0") Long userId) {
        try {
            DanhGia dg = danhGiaRepository.findById(reviewId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

            var existingHelpful = danhGiaHuuIchRepository.findByDanhGia_MaDanhGiaAndNguoiDung_MaNguoiDung(reviewId, userId);
            if (existingHelpful.isPresent()) {
                danhGiaHuuIchRepository.delete(existingHelpful.get());
                int newCount = Math.max(0, (dg.getSoLuotHuuIch() != null ? dg.getSoLuotHuuIch() : 0) - 1);
                dg.setSoLuotHuuIch(newCount);
                danhGiaRepository.save(dg);
                return ResponseEntity.ok(java.util.Map.of(
                    "message", "Đã bỏ đánh dấu hữu ích",
                    "reviewId", reviewId,
                    "huuIch", false,
                    "soLuotHuuIch", newCount
                ));
            } else {
                DanhGiaHuuIch helpful = new DanhGiaHuuIch();
                helpful.setDanhGia(dg);
                helpful.setNguoiDung(nguoiDungRepository.findById(userId).orElseThrow());
                danhGiaHuuIchRepository.save(helpful);
                int newCount = (dg.getSoLuotHuuIch() != null ? dg.getSoLuotHuuIch() : 0) + 1;
                dg.setSoLuotHuuIch(newCount);
                danhGiaRepository.save(dg);
                return ResponseEntity.ok(java.util.Map.of(
                    "message", "Đã đánh dấu hữu ích",
                    "reviewId", reviewId,
                    "huuIch", true,
                    "soLuotHuuIch", newCount
                ));
            }
        } catch (Exception e) {
            log.error("Lỗi đánh dấu hữu ích: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"));
        }
    }

    @Data
    public static class ReplyRequest {
        private String noiDung;
    }

    @Data
    public static class CreateReviewRequest {
        private Long phongId;
        private Long maKhach;
        private Long maDatCho;
        private double diemSo;
        private Double diemSachSe;
        private Double diemChinhXac;
        private Double diemNhanPhong;
        private Double diemGiaoTiep;
        private Double diemViTri;
        private Double diemGiaTri;
        private String binhLuan;

        public double calculateOverallScore() {
            if (diemSachSe != null && diemChinhXac != null && diemNhanPhong != null
                    && diemGiaoTiep != null && diemViTri != null && diemGiaTri != null) {
                return (diemSachSe + diemChinhXac + diemNhanPhong + diemGiaoTiep + diemViTri + diemGiaTri) / 6.0;
            }
            return diemSo;
        }
    }

    @Data
    public static class CategoryAveragesResponse {
        private Double diemSachSe;
        private Double diemChinhXac;
        private Double diemNhanPhong;
        private Double diemGiaoTiep;
        private Double diemViTri;
        private Double diemGiaTri;
    }

    @Data
    public static class ReviewUser {
        private Long maNguoiDung;
        private String hoTen;
        private String avatarUrl;
        private String urlAnhDaiDien;
        private String thanhPho;
    }

    @Data
    public static class ReviewResponse {
        private Long maDanhGia;
        private Long maSanPham;
        private double diemSo;
        private String binhLuan;
        private String phanHoi;
        private LocalDateTime ngayTao;
        private ReviewUser khach;
        private ReviewUser chuNha;
        private Integer soLuotHuuIch;
        private Boolean isHuuIchByCurrentUser;
        private Double diemSachSe;
        private Double diemChinhXac;
        private Double diemNhanPhong;
        private Double diemGiaoTiep;
        private Double diemViTri;
        private Double diemGiaTri;
        private Long maDatCho;
        private java.time.LocalDate ngayNhanPhong;
        private java.time.LocalDate ngayTraPhong;
        private String tieuDePhong;
        private String urlAnhPhong;
    }
}
