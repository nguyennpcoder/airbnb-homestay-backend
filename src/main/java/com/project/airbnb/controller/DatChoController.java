package com.project.airbnb.controller;

import com.project.airbnb.dto.DatChoResponse;
import com.project.airbnb.entity.DatCho;
import com.project.airbnb.entity.Phong;
import com.project.airbnb.entity.TinNhan;
import com.project.airbnb.repository.DatChoRepository;
import com.project.airbnb.repository.PhongRepository;
import com.project.airbnb.repository.PaymentRepository;
import com.project.airbnb.repository.TinNhanRepository;
import lombok.Data;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/dat-cho")
public class DatChoController {
    private static final Logger log = LoggerFactory.getLogger(DatChoController.class);

    private final DatChoRepository datChoRepository;
    private final PhongRepository phongRepository;
    private final PaymentRepository paymentRepository;
    private final com.project.airbnb.repository.NguoiDungRepository nguoiDungRepository;
    private final com.project.airbnb.service.PaymentService paymentService;
    private final com.project.airbnb.repository.LichTrinhRepository lichTrinhRepository;
    private final com.project.airbnb.service.PriceCalculationService priceCalcService;
    private final com.project.airbnb.service.KhuyenMaiService khuyenMaiService;
    private final TinNhanRepository tinNhanRepository;

    public DatChoController(DatChoRepository datChoRepository, PhongRepository phongRepository,
            PaymentRepository paymentRepository,
            com.project.airbnb.repository.NguoiDungRepository nguoiDungRepository,
            com.project.airbnb.service.PaymentService paymentService,
            com.project.airbnb.repository.LichTrinhRepository lichTrinhRepository,
            com.project.airbnb.service.PriceCalculationService priceCalcService,
            com.project.airbnb.service.KhuyenMaiService khuyenMaiService,
            TinNhanRepository tinNhanRepository) {
        this.datChoRepository = datChoRepository;
        this.phongRepository = phongRepository;
        this.paymentRepository = paymentRepository;
        this.nguoiDungRepository = nguoiDungRepository;
        this.paymentService = paymentService;
        this.lichTrinhRepository = lichTrinhRepository;
        this.priceCalcService = priceCalcService;
        this.khuyenMaiService = khuyenMaiService;
        this.tinNhanRepository = tinNhanRepository;
    }

    @GetMapping("/user/{userId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<DatChoResponse>> getByUser(@PathVariable Long userId) {
        List<DatCho> results = datChoRepository.findByKhach_MaNguoiDung(userId);
        List<DatChoResponse> responses = results.stream().map(booking -> {
            var payments = paymentRepository.findByBooking_MaDatCho(booking.getMaDatCho());
            return DatChoResponse.fromEntity(booking, payments);
        }).toList();

        // Populate lastMessage, lastMessageTime, hasUnread for each booking
        if (!responses.isEmpty()) {
            List<Long> bookingIds = responses.stream().map(DatChoResponse::getMaDatCho).toList();

            // Batch query unread counts
            List<Object[]> unreadCounts = tinNhanRepository.countUnreadByBookingIds(bookingIds, userId);
            Map<Long, Long> unreadMap = unreadCounts.stream()
                    .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

            // Query last message for each booking
            for (DatChoResponse resp : responses) {
                Long bookingId = resp.getMaDatCho();
                List<TinNhan> lastMessages = tinNhanRepository.findLastMessageByBooking(bookingId, PageRequest.of(0, 1));
                if (!lastMessages.isEmpty()) {
                    TinNhan lastMsg = lastMessages.get(0);
                    resp.setLastMessage(lastMsg.getNoiDung());
                    resp.setLastMessageTime(lastMsg.getNgayTao());
                }
                resp.setHasUnread(unreadMap.getOrDefault(bookingId, 0L) > 0);
            }
        }

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/host/{hostId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<DatChoResponse>> getByHost(@PathVariable Long hostId) {
        List<DatCho> results = datChoRepository.findByPhong_ChuNha_MaNguoiDung(hostId);
        return ResponseEntity.ok(results.stream().map(booking -> {
            var payments = paymentRepository.findByBooking_MaDatCho(booking.getMaDatCho());
            return DatChoResponse.fromEntity(booking, payments);
        }).toList());
    }

    @GetMapping("/phong/{phongId}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<DatChoResponse>> getByPhong(@PathVariable Long phongId) {
        List<DatCho> results = datChoRepository.findByPhong_MaPhong(phongId);
        return ResponseEntity.ok(results.stream().map(booking -> {
            var payments = paymentRepository.findByBooking_MaDatCho(booking.getMaDatCho());
            return DatChoResponse.fromEntity(booking, payments);
        }).toList());
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<DatChoResponse> getById(@PathVariable Long id) {
        return datChoRepository.findById(id)
                .map(booking -> {
                    var payments = paymentRepository.findByBooking_MaDatCho(booking.getMaDatCho());
                    return DatChoResponse.fromEntity(booking, payments);
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/preview-refund")
    @Transactional(readOnly = true)
    public ResponseEntity<?> previewRefund(@PathVariable Long id) {
        return datChoRepository.findById(id)
                .map(booking -> {
                    log.info("previewRefund booking {} status={} tongTien={} phiDV={} phiVS={} ngayNhan={} phong={}",
                            booking.getMaDatCho(), booking.getTrangThaiDatCho(),
                            booking.getTongTien(), booking.getPhiDichVu(), booking.getPhiVeSinh(),
                            booking.getNgayNhanPhong(), booking.getPhong() != null ? "loaded" : "NULL");

                    BigDecimal refundAmount = BigDecimal.ZERO;
                    if (booking.getTrangThaiDatCho() == DatCho.TrangThaiDatCho.da_xac_nhan ||
                            booking.getTrangThaiDatCho() == DatCho.TrangThaiDatCho.hoan_thanh) {
                        BigDecimal tongTien = booking.getTongTien() != null ? booking.getTongTien() : BigDecimal.ZERO;
                        BigDecimal serviceFee = booking.getPhiDichVu() != null ? booking.getPhiDichVu() : BigDecimal.ZERO;
                        BigDecimal cleaningFee = booking.getPhiVeSinh() != null ? booking.getPhiVeSinh() : BigDecimal.ZERO;
                        BigDecimal nightCost = tongTien.subtract(serviceFee).subtract(cleaningFee);
                        if (nightCost.compareTo(BigDecimal.ZERO) < 0) nightCost = BigDecimal.ZERO;

                        String policyName = "LINH_HOAT";
                        try {
                            if (booking.getPhong() != null && booking.getPhong().getChinhSachHuy() != null) {
                                policyName = booking.getPhong().getChinhSachHuy().name();
                            }
                        } catch (Exception e) {
                            log.warn("Cannot load policy for booking {}: {}", id, e.getMessage());
                        }

                        LocalDate checkInDate = booking.getNgayNhanPhong();
                        long daysUntilCheckIn = checkInDate != null
                                ? java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), checkInDate) : 30;

                        BigDecimal nightPct = BigDecimal.ZERO;
                        switch (policyName) {
                            case "LINH_HOAT": if (daysUntilCheckIn >= 0) nightPct = BigDecimal.ONE; break;
                            case "TRUNG_BINH": if (daysUntilCheckIn >= 0) nightPct = new BigDecimal("0.5"); break;
                            case "NGHIEM_NGAT": nightPct = BigDecimal.ZERO; break;
                            default: if (daysUntilCheckIn >= 0) nightPct = BigDecimal.ONE; break;
                        }
                        refundAmount = nightCost.multiply(nightPct).setScale(0, java.math.RoundingMode.HALF_UP).add(cleaningFee);
                        log.info("previewRefund calc: nightCost={} policy={} days={} nightPct={} refund={}",
                                nightCost, policyName, daysUntilCheckIn, nightPct, refundAmount);
                    } else if (booking.getTrangThaiDatCho() == DatCho.TrangThaiDatCho.cho_xac_nhan) {
                        refundAmount = booking.getTongTien();
                    }
                    return ResponseEntity.ok(java.util.Map.of("refundAmount", refundAmount));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> create(@RequestBody CreateBookingRequest req) {
        try {
            Phong sp = phongRepository.findById(req.getPhongId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng với ID: " + req.getPhongId()));

            BigDecimal total;
            BigDecimal cleaningFee = BigDecimal.ZERO;
            BigDecimal serviceFee = BigDecimal.ZERO;
            BigDecimal tienGiamGia = BigDecimal.ZERO;
            Long appliedKmId = null;

            if (sp.getLoaiPhong() == Phong.LoaiPhong.noi_luu_tru) {
                if (req.getNgayNhanPhong() == null || req.getNgayTraPhong() == null) {
                    return ResponseEntity.badRequest().body(java.util.Map.of("message", "Thiếu ngày nhận/trả"));
                }
                long nights = ChronoUnit.DAYS.between(req.getNgayNhanPhong(), req.getNgayTraPhong());
                if (nights <= 0) {
                    return ResponseEntity.badRequest().body(java.util.Map.of("message", "Khoảng ngày không hợp lệ"));
                }

                // Check for overlapping active bookings
                java.util.List<DatCho> existing = datChoRepository.findByPhong_MaPhong(req.getPhongId());
                boolean hasOverlap = existing.stream().anyMatch(b ->
                        b.getMaDatCho() != null
                        && (b.getTrangThaiDatCho() == DatCho.TrangThaiDatCho.cho_xac_nhan
                            || b.getTrangThaiDatCho() == DatCho.TrangThaiDatCho.da_xac_nhan)
                        && b.getNgayNhanPhong() != null && b.getNgayTraPhong() != null
                        && req.getNgayNhanPhong().isBefore(b.getNgayTraPhong())
                        && req.getNgayTraPhong().isAfter(b.getNgayNhanPhong()));
                if (hasOverlap) {
                    return ResponseEntity.status(409).body(java.util.Map.of("message", "Ngày này đã được đặt. Vui lòng chọn ngày khác."));
                }

                // Check for host-blocked dates in LichTrinh
                java.util.List<com.project.airbnb.entity.LichTrinh> blockedDates =
                        lichTrinhRepository.findByPhong_MaPhongAndNgayBetween(
                                req.getPhongId(), req.getNgayNhanPhong(), req.getNgayTraPhong().minusDays(1));
                boolean hasBlockedDate = blockedDates.stream().anyMatch(r -> !Boolean.TRUE.equals(r.getConKhaDung()));
                if (hasBlockedDate) {
                    return ResponseEntity.status(409).body(java.util.Map.of("message", "Một số ngày trong khoảng đã bị chặn. Vui lòng chọn ngày khác."));
                }
                int adults = req.getSoNguoiLon() != null ? req.getSoNguoiLon() : 1;
                int children = req.getSoTreEm() != null ? req.getSoTreEm() : 0;
                int infants = req.getSoEmBe() != null ? req.getSoEmBe() : 0;
                BigDecimal phiVeSinh = sp.getPhiVeSinh() != null ? sp.getPhiVeSinh() : BigDecimal.ZERO;

                var priceResult = priceCalcService.calculateLuuTru(
                        sp.getGiaMoiKhach(), req.getNgayNhanPhong(), req.getNgayTraPhong(),
                        adults, children, infants, phiVeSinh, req.getMaKhuyenMai(), khuyenMaiService);
                total = priceResult.tongTien();
                serviceFee = priceResult.serviceFee();
                cleaningFee = priceResult.cleaningFee();
                tienGiamGia = priceResult.tienGiamGia();
                appliedKmId = req.getMaKhuyenMai();
            } else {
                int requested = req.getSoLuongKhach() != null ? req.getSoLuongKhach() : 1;
                BigDecimal subTotal = sp.getGiaMoiKhach().multiply(BigDecimal.valueOf(requested));
                BigDecimal serviceFeeRate = priceCalcService.getServiceFeeRate();
                serviceFee = subTotal.multiply(serviceFeeRate).setScale(0, java.math.RoundingMode.HALF_UP);
                total = subTotal.add(serviceFee);
            }

            DatCho dc = new DatCho();
            com.project.airbnb.entity.NguoiDung userRef = nguoiDungRepository.findById(req.getMaKhach())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách với ID: " + req.getMaKhach()));
            dc.setKhach(userRef);
            dc.setPhong(sp);
            dc.setLoaiPhong(sp.getLoaiPhong());
            dc.setSoLuongKhach(req.getSoLuongKhach() != null ? req.getSoLuongKhach() : 1);
            dc.setSoNguoiLon(req.getSoNguoiLon());
            dc.setSoTreEm(req.getSoTreEm());
            dc.setSoEmBe(req.getSoEmBe());
            dc.setGiaMoiKhach(sp.getGiaMoiKhach());
            dc.setTongTien(total);
            dc.setPhiVeSinh(cleaningFee);
            dc.setPhiDichVu(serviceFee);
            dc.setYeuCauDacBiet(req.getYeuCauDacBiet());
            // Set promotion
            if (appliedKmId != null) {
                com.project.airbnb.entity.KhuyenMai km = khuyenMaiService.findById(appliedKmId);
                dc.setKhuyenMai(km);
                dc.setTienGiamGia(tienGiamGia);
                khuyenMaiService.incrementUsage(appliedKmId);
            } else {
                dc.setTienGiamGia(BigDecimal.ZERO);
            }
            if (sp.getLoaiPhong() == Phong.LoaiPhong.noi_luu_tru) {
                dc.setNgayNhanPhong(req.getNgayNhanPhong());
                dc.setNgayTraPhong(req.getNgayTraPhong());
            } else {
                dc.setNgayDat(req.getNgayDat());
                dc.setGioBatDau(req.getGioBatDau());
                dc.setGioKetThuc(req.getGioKetThuc());
            }

            DatCho saved = datChoRepository.save(dc);
            var payments = paymentRepository.findByBooking_MaDatCho(saved.getMaDatCho());
            return ResponseEntity.ok(DatChoResponse.fromEntity(saved, payments));
        } catch (Exception e) {
            System.err.println("Booking creation error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{maDatCho}/cancel")
    @Transactional
    public ResponseEntity<?> cancelBooking(@PathVariable Long maDatCho, @RequestParam Long userId) {
        DatCho booking = datChoRepository.findById(maDatCho)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getKhach().getMaNguoiDung().equals(userId)) {
            return ResponseEntity.status(403).body(java.util.Map.of("message", "Không có quyền hủy đơn này"));
        }

        if (booking.getTrangThaiDatCho() == DatCho.TrangThaiDatCho.da_huy) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Đơn đã bị hủy trước đó"));
        }

        booking.setTrangThaiDatCho(DatCho.TrangThaiDatCho.da_huy);
        datChoRepository.save(booking);

        // Giảm lượt dùng khuyến mãi nếu có
        if (booking.getKhuyenMai() != null) {
            khuyenMaiService.decrementUsage(booking.getKhuyenMai().getMaKhuyenMai());
        }

        var payments = paymentRepository.findByBooking_MaDatCho(maDatCho);
        for (var payment : payments) {
            if (payment.getStatus() == com.project.airbnb.entity.Payment.PaymentStatus.DA_XAC_NHAN ||
                    payment.getStatus() == com.project.airbnb.entity.Payment.PaymentStatus.CHO_XAC_NHAN) {
                payment.setStatus(com.project.airbnb.entity.Payment.PaymentStatus.YEU_CAU_HOAN_TIEN);
            } else if (payment.getStatus() == com.project.airbnb.entity.Payment.PaymentStatus.CHUA_THANH_TOAN) {
                payment.setStatus(com.project.airbnb.entity.Payment.PaymentStatus.DA_HUY);
            }
        }
        if (!payments.isEmpty()) {
            paymentRepository.saveAll(payments);
        }

        return ResponseEntity.ok(java.util.Map.of("message", "Hủy đặt phòng thành công"));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateBooking(@PathVariable Long id, @RequestBody java.util.Map<String, Object> payload) {
        return datChoRepository.findById(id)
                .map(booking -> {
                    java.time.LocalDate oldCheckIn = booking.getNgayNhanPhong();
                    java.time.LocalDate oldCheckOut = booking.getNgayTraPhong();

                    if (payload.containsKey("soLuongKhach"))
                        booking.setSoLuongKhach(Integer.parseInt(payload.get("soLuongKhach").toString()));
                    if (payload.containsKey("soNguoiLon"))
                        booking.setSoNguoiLon(Integer.parseInt(payload.get("soNguoiLon").toString()));
                    if (payload.containsKey("soTreEm"))
                        booking.setSoTreEm(Integer.parseInt(payload.get("soTreEm").toString()));
                    if (payload.containsKey("soEmBe"))
                        booking.setSoEmBe(Integer.parseInt(payload.get("soEmBe").toString()));
                    if (payload.containsKey("ngayNhanPhong") && payload.get("ngayNhanPhong") != null)
                        booking.setNgayNhanPhong(java.time.LocalDate.parse(payload.get("ngayNhanPhong").toString()));
                    if (payload.containsKey("ngayTraPhong") && payload.get("ngayTraPhong") != null)
                        booking.setNgayTraPhong(java.time.LocalDate.parse(payload.get("ngayTraPhong").toString()));
                    if (payload.containsKey("yeuCauDacBiet"))
                        booking.setYeuCauDacBiet((String) payload.get("yeuCauDacBiet"));

                    // Xử lý mã khuyến mãi khi user áp dụng trên trang thanh toán
                    if (payload.containsKey("maKhuyenMai")) {
                        Object kmVal = payload.get("maKhuyenMai");
                        Long oldKmId = booking.getKhuyenMai() != null ? booking.getKhuyenMai().getMaKhuyenMai() : null;
                        Long newKmId = kmVal != null ? Long.valueOf(kmVal.toString()) : null;

                        if (newKmId != null && !newKmId.equals(oldKmId)) {
                            // Áp dụng mã mới → tăng lượt dùng
                            com.project.airbnb.entity.KhuyenMai km = khuyenMaiService.findById(newKmId);
                            booking.setKhuyenMai(km);
                            khuyenMaiService.incrementUsage(newKmId);
                        } else if (newKmId == null && oldKmId != null) {
                            // Bỏ mã → giảm lượt dùng
                            booking.setKhuyenMai(null);
                            khuyenMaiService.decrementUsage(oldKmId);
                        }
                    }
                    if (payload.containsKey("tienGiamGia"))
                        booking.setTienGiamGia(new java.math.BigDecimal(payload.get("tienGiamGia").toString()));
                    if (payload.containsKey("tongTien"))
                        booking.setTongTien(new java.math.BigDecimal(payload.get("tongTien").toString()));

                    if (booking.getNgayNhanPhong() != null && booking.getNgayTraPhong() != null
                            && booking.getPhong() != null && booking.getPhong().getLoaiPhong() == Phong.LoaiPhong.noi_luu_tru) {
                        long nights = java.time.temporal.ChronoUnit.DAYS.between(booking.getNgayNhanPhong(), booking.getNgayTraPhong());
                        if (nights > 0) {
                            int totalGuests = booking.getSoLuongKhach() != null ? booking.getSoLuongKhach() : 1;
                            int adults = payload.containsKey("soNguoiLon") ? Integer.parseInt(payload.get("soNguoiLon").toString()) : totalGuests;
                            int children = payload.containsKey("soTreEm") ? Integer.parseInt(payload.get("soTreEm").toString()) : 0;
                            int infants = booking.getSoEmBe() != null ? booking.getSoEmBe() : 0;
                            if (adults + children != totalGuests) adults = totalGuests;
                            BigDecimal phiVeSinh = booking.getPhiVeSinh() != null ? booking.getPhiVeSinh() : BigDecimal.ZERO;
                            Long kmId = booking.getKhuyenMai() != null ? booking.getKhuyenMai().getMaKhuyenMai() : null;

                            var priceResult = priceCalcService.calculateLuuTru(
                                    booking.getGiaMoiKhach(), booking.getNgayNhanPhong(), booking.getNgayTraPhong(),
                                    adults, children, infants, phiVeSinh, kmId, khuyenMaiService);
                            booking.setTongTien(priceResult.tongTien());
                            booking.setPhiDichVu(priceResult.serviceFee());
                            booking.setTienGiamGia(priceResult.tienGiamGia());
                        }
                    }

                    datChoRepository.save(booking);
                    datChoRepository.flush();

                    boolean datesChanged = (oldCheckIn != null && !oldCheckIn.equals(booking.getNgayNhanPhong()))
                            || (oldCheckOut != null && !oldCheckOut.equals(booking.getNgayTraPhong()));
                    if (datesChanged && booking.getPhong() != null && booking.getPhong().getMaPhong() != null) {
                        Long phongId = booking.getPhong().getMaPhong();

                        if (oldCheckIn != null && oldCheckOut != null) {
                            java.time.LocalDate d = oldCheckIn;
                            while (d.isBefore(oldCheckOut)) {
                                final java.time.LocalDate day = d;
                                boolean otherBooking = datChoRepository.findByPhong_MaPhong(phongId).stream()
                                        .anyMatch(b -> b.getMaDatCho() != booking.getMaDatCho()
                                                && (b.getTrangThaiDatCho() == DatCho.TrangThaiDatCho.cho_xac_nhan
                                                    || b.getTrangThaiDatCho() == DatCho.TrangThaiDatCho.da_xac_nhan)
                                                && b.getNgayNhanPhong() != null && b.getNgayTraPhong() != null
                                                && !day.isBefore(b.getNgayNhanPhong()) && day.isBefore(b.getNgayTraPhong()));
                                if (!otherBooking) {
                                    lichTrinhRepository.findByPhong_MaPhongAndNgay(phongId, day)
                                            .ifPresent(rec -> { rec.setConKhaDung(true); lichTrinhRepository.save(rec); });
                                }
                                d = d.plusDays(1);
                            }
                        }

                        if (booking.getNgayNhanPhong() != null && booking.getNgayTraPhong() != null) {
                            java.time.LocalDate d2 = booking.getNgayNhanPhong();
                            while (d2.isBefore(booking.getNgayTraPhong())) {
                                final java.time.LocalDate day2 = d2;
                                var opt = lichTrinhRepository.findByPhong_MaPhongAndNgay(phongId, day2);
                                com.project.airbnb.entity.LichTrinh rec;
                                if (opt.isPresent()) {
                                    rec = opt.get();
                                } else {
                                    rec = new com.project.airbnb.entity.LichTrinh();
                                    rec.setPhong(booking.getPhong());
                                    rec.setNgay(day2);
                                }
                                rec.setConKhaDung(false);
                                lichTrinhRepository.save(rec);
                                d2 = d2.plusDays(1);
                            }
                        }
                    }

                    var payments = paymentRepository.findByBooking_MaDatCho(booking.getMaDatCho());
                    return ResponseEntity.ok(DatChoResponse.fromEntity(booking, payments));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Data
    public static class CreateBookingRequest {
        private Long maKhach;
        private Long phongId;
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate ngayNhanPhong;
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate ngayTraPhong;
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate ngayDat;
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "HH:mm")
        private LocalTime gioBatDau;
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "HH:mm")
        private LocalTime gioKetThuc;
        private Integer soLuongKhach;
        private Integer soNguoiLon;
        private Integer soTreEm;
        private Integer soEmBe;
        private String yeuCauDacBiet;
        private Long maKhuyenMai;
    }
}
