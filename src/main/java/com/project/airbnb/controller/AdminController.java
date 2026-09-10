package com.project.airbnb.controller;

import com.project.airbnb.entity.Phong;
import com.project.airbnb.entity.DatCho;
import com.project.airbnb.entity.Payment;
import com.project.airbnb.dto.DatChoResponse;
import com.project.airbnb.repository.PhongRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@Transactional(readOnly = true)
public class AdminController {
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final PhongRepository phongRepository;
    private final com.project.airbnb.repository.DatChoRepository datChoRepository;
    private final com.project.airbnb.repository.NguoiDungRepository nguoiDungRepository;
    private final com.project.airbnb.repository.PaymentRepository paymentRepository;
    private final com.project.airbnb.repository.HinhAnhRepository hinhAnhRepository;
    private final com.project.airbnb.repository.TienNghiPhongRepository tienNghiPhongRepository;
    private final com.project.airbnb.repository.LichTrinhRepository lichTrinhRepository;
    private final com.project.airbnb.service.EmailService emailService;
    private final com.project.airbnb.service.PaymentService paymentService;
    private final com.project.airbnb.repository.ThongBaoRepository thongBaoRepository;
    private final com.project.airbnb.repository.DanhGiaRepository danhGiaRepository;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public AdminController(PhongRepository phongRepository,
            com.project.airbnb.repository.DatChoRepository datChoRepository,
            com.project.airbnb.repository.NguoiDungRepository nguoiDungRepository,
            com.project.airbnb.repository.PaymentRepository paymentRepository,
            com.project.airbnb.repository.HinhAnhRepository hinhAnhRepository,
            com.project.airbnb.repository.TienNghiPhongRepository tienNghiPhongRepository,
            com.project.airbnb.repository.LichTrinhRepository lichTrinhRepository,
            com.project.airbnb.service.EmailService emailService,
            com.project.airbnb.service.PaymentService paymentService,
            com.project.airbnb.repository.ThongBaoRepository thongBaoRepository,
            com.project.airbnb.repository.DanhGiaRepository danhGiaRepository) {
        this.phongRepository = phongRepository;
        this.datChoRepository = datChoRepository;
        this.nguoiDungRepository = nguoiDungRepository;
        this.paymentRepository = paymentRepository;
        this.hinhAnhRepository = hinhAnhRepository;
        this.tienNghiPhongRepository = tienNghiPhongRepository;
        this.lichTrinhRepository = lichTrinhRepository;
        this.emailService = emailService;
        this.paymentService = paymentService;
        this.thongBaoRepository = thongBaoRepository;
        this.danhGiaRepository = danhGiaRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        long totalUsers = nguoiDungRepository.count();
        long activeUsers = nguoiDungRepository.countByBiKhoaFalse();
        long lockedUsers = nguoiDungRepository.countByBiKhoaTrue();
        long hostCount = nguoiDungRepository.countByLaChuNhaTrue();
        long guestCount = totalUsers - hostCount;

        long totalListings = phongRepository.count();
        long activeListings = phongRepository.countByTrangThai(Phong.TrangThaiPhong.hoat_dong);
        long pendingListings = phongRepository.countByTrangThai(Phong.TrangThaiPhong.cho_duyet);
        long lockedListings = phongRepository.countByBiKhoaTrue();

        Double totalRevenue = paymentRepository.sumTotalRevenue();
        long totalBookings = datChoRepository.count();
        Long successfulPayments = paymentRepository.countSuccessfulPayments();

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("totalUsers", totalUsers);
        result.put("activeUsers", activeUsers);
        result.put("lockedUsers", lockedUsers);
        result.put("hostCount", hostCount);
        result.put("guestCount", guestCount);
        result.put("totalListings", totalListings);
        result.put("activeListings", activeListings);
        result.put("pendingListings", pendingListings);
        result.put("lockedListings", lockedListings);
        result.put("totalRevenue", totalRevenue != null ? totalRevenue : 0);
        result.put("totalBookings", totalBookings);
        result.put("successfulPayments", successfulPayments != null ? successfulPayments : 0);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/analytics/revenue")
    public ResponseEntity<?> getRevenueChart(@RequestParam(defaultValue = "2025") int year) {
        // Tính doanh thu theo tháng từ payment đã thành công
        return ResponseEntity.ok(paymentRepository.getMonthlyRevenue(year));
    }

    @GetMapping("/analytics/service-fee")
    public ResponseEntity<?> getServiceFeeChart(@RequestParam(defaultValue = "2025") int year) {
        return ResponseEntity.ok(paymentRepository.getMonthlyServiceFee(year));
    }

    @GetMapping("/wallet")
    public ResponseEntity<?> getAdminWallet() {
        com.project.airbnb.entity.NguoiDung admin = nguoiDungRepository.findFirstByLaAdminTrue()
                .orElse(null);
        if (admin == null) {
            return ResponseEntity.ok(java.util.Map.of("soDu", 0, "email", "", "hoTen", ""));
        }
        java.math.BigDecimal soDu = admin.getSoDu() != null ? admin.getSoDu() : java.math.BigDecimal.ZERO;

        // Tính tổng phí dịch vụ đã thu từ các payment đã xác nhận
        java.math.BigDecimal totalServiceFee = paymentRepository.sumServiceFeeByConfirmedPayments();
        if (totalServiceFee == null) totalServiceFee = java.math.BigDecimal.ZERO;

        return ResponseEntity.ok(java.util.Map.of(
                "soDu", soDu,
                "totalServiceFee", totalServiceFee,
                "email", admin.getEmail() != null ? admin.getEmail() : "",
                "hoTen", admin.getHoTen() != null ? admin.getHoTen() : "Admin"));
    }

    @GetMapping("/analytics/refunds")
    public ResponseEntity<?> getRefundChart(@RequestParam(defaultValue = "2025") int year) {
        return ResponseEntity.ok(paymentRepository.getMonthlyRefunds(year));
    }

    @GetMapping("/analytics/cancelled-failed")
    public ResponseEntity<?> getCancelledFailedChart(@RequestParam(defaultValue = "2025") int year) {
        return ResponseEntity.ok(paymentRepository.getMonthlyCancelledFailed(year));
    }

    @GetMapping("/analytics/user-growth")
    public ResponseEntity<?> getUserGrowth(@RequestParam(defaultValue = "2025") int year) {
        return ResponseEntity.ok(nguoiDungRepository.getMonthlyRegistrations(year));
    }

    @GetMapping("/analytics/category-distribution")
    public ResponseEntity<?> getCategoryDistribution() {
        return ResponseEntity.ok(phongRepository.getCategoryStats());
    }

    @GetMapping("/analytics/reviews")
    public ResponseEntity<?> getAllReviews() {
        List<com.project.airbnb.entity.DanhGia> reviews = danhGiaRepository.findAllWithDetails();
        List<Map<String, Object>> result = reviews.stream().map(d -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("maDanhGia", d.getMaDanhGia());
            map.put("diemSo", d.getDiemSo() != null ? d.getDiemSo().doubleValue() : 0.0);
            map.put("maPhong", d.getPhong() != null ? d.getPhong().getMaPhong() : null);
            map.put("ngayTao", d.getNgayTao());
            return map;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/analytics/occupancy")
    public ResponseEntity<?> getOccupancyRate(@RequestParam(defaultValue = "2025") int year) {
        long totalListings = phongRepository.count();
        if (totalListings == 0)
            return ResponseEntity.ok(java.util.List.of());

        java.util.List<java.util.Map<String, Object>> bookedNights = datChoRepository.getMonthlyBookedNights(year);

        return ResponseEntity.ok(bookedNights.stream().map(m -> {
            int month = (int) m.get("month");
            long nights = ((Number) m.get("bookedNights")).longValue();
            int daysInMonth = java.time.YearMonth.of(year, month).lengthOfMonth();
            double rate = (double) nights / (totalListings * daysInMonth) * 100;
            return java.util.Map.of("month", month, "rate", rate);
        }).collect(java.util.stream.Collectors.toList()));
    }

    @GetMapping("/analytics/cancellation-rates")
    public ResponseEntity<?> getCancellationRates(@RequestParam(defaultValue = "2025") int year) {
        java.util.List<java.util.Map<String, Object>> cancellations = datChoRepository.getMonthlyCancellations(year);
        return ResponseEntity.ok(cancellations);
    }

    @GetMapping("/analytics/bookings-by-status")
    public ResponseEntity<?> getBookingsByStatus() {
        return ResponseEntity.ok(datChoRepository.countBookingsByStatus());
    }

    @GetMapping("/analytics/bookings-monthly")
    public ResponseEntity<?> getMonthlyBookings(@RequestParam(defaultValue = "2025") int year) {
        java.util.List<java.util.Map<String, Object>> totalByMonth = datChoRepository.getMonthlyBookingCounts(year);
        java.util.List<java.util.Map<String, Object>> cancellations = datChoRepository.getMonthlyCancellations(year);

        // Merge into single response with total and cancelled counts per month
        java.util.Map<Integer, java.util.Map<String, Object>> merged = new java.util.HashMap<>();
        for (int i = 1; i <= 12; i++) {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("month", i);
            m.put("total", 0);
            m.put("cancelled", 0);
            merged.put(i, m);
        }
        for (java.util.Map<String, Object> entry : totalByMonth) {
            int m = (int) entry.get("month");
            merged.get(m).put("total", entry.get("count"));
        }
        for (java.util.Map<String, Object> entry : cancellations) {
            int m = (int) entry.get("month");
            merged.get(m).put("cancelled", entry.get("count"));
        }
        return ResponseEntity.ok(merged.values());
    }

    @GetMapping("/analytics/payment-status-distribution")
    public ResponseEntity<?> getPaymentStatusDistribution() {
        return ResponseEntity.ok(paymentRepository.countPaymentsByStatus());
    }

    @GetMapping("/analytics/recent-activities")
    public ResponseEntity<?> getRecentActivities() {
        java.util.List<java.util.Map<String, Object>> activities = new java.util.ArrayList<>();

        // Sử dụng repository queries với sorting ở database để tránh NPE
        java.util.List<DatCho> recentBookings = datChoRepository.findTop5ByOrderByNgayTaoDesc();
        for (DatCho dc : recentBookings) {
            String guestName = "N/A";
            if (dc.getKhach() != null) {
                try {
                    guestName = dc.getKhach().getHoTen() != null ? dc.getKhach().getHoTen() : "N/A";
                } catch (Exception e) {
                    guestName = "N/A";
                }
            }
            String listingTitle = "";
            if (dc.getPhong() != null) {
                try {
                    listingTitle = dc.getPhong().getTieuDe() != null ? dc.getPhong().getTieuDe() : "";
                } catch (Exception e) {
                    listingTitle = "";
                }
            }
            if (dc.getNgayTao() != null) {
                activities.add(java.util.Map.of(
                        "type", "BOOKING",
                        "id", dc.getMaDatCho(),
                        "title", "Đặt chỗ mới",
                        "description", "Khách " + guestName + " đặt " + listingTitle,
                        "timestamp", dc.getNgayTao()));
            }
        }

        // Lấy 5 user mới nhất
        java.util.List<com.project.airbnb.entity.NguoiDung> recentUsers = nguoiDungRepository.findTop5ByOrderByNgayTaoDesc();
        for (com.project.airbnb.entity.NguoiDung u : recentUsers) {
            if (u.getNgayTao() != null) {
                String hoTen = u.getHoTen() != null ? u.getHoTen() : "";
                String email = u.getEmail() != null ? u.getEmail() : "";
                activities.add(java.util.Map.of(
                        "type", "USER_REGISTER",
                        "id", u.getMaNguoiDung(),
                        "title", "Người dùng mới",
                        "description", hoTen + " (" + email + ") vừa tham gia",
                        "timestamp", u.getNgayTao()));
            }
        }

        // Sort safely using Comparator.nullsLast
        activities.sort(java.util.Comparator.comparing(
                m -> (java.time.LocalDateTime) m.get("timestamp"),
                java.util.Comparator.nullsLast(java.time.LocalDateTime::compareTo).reversed()));

        return ResponseEntity.ok(activities.stream().limit(10).collect(java.util.stream.Collectors.toList()));
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers() {
        List<com.project.airbnb.dto.AdminUserDTO> users = nguoiDungRepository.findAll().stream()
                .map(u -> com.project.airbnb.dto.AdminUserDTO.from(
                        u,
                        datChoRepository.countByKhach_MaNguoiDung(u.getMaNguoiDung()),
                        phongRepository.countByChuNha_MaNguoiDung(u.getMaNguoiDung()),
                        thongBaoRepository.existsByNguoiGui_MaNguoiDungAndLoaiThongBaoAndTrangThai(
                                u.getMaNguoiDung(), "HOST_REQUEST", "PENDING")))
                .sorted(java.util.Comparator.comparing(
                        com.project.airbnb.dto.AdminUserDTO::getNgayTao,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PostMapping("/users/{id}/lock")
    @Transactional
    public ResponseEntity<?> lockUser(@PathVariable Long id) {
        if (id == null)
            return ResponseEntity.badRequest().build();
        return nguoiDungRepository.findById(id)
                .map(u -> {
                    boolean wasHost = Boolean.TRUE.equals(u.getLaChuNha());
                    u.setBiKhoa(true);
                    nguoiDungRepository.save(u);

                    // Khi khóa host: tạm dừng listing đang hoạt động, giữ nguyên chờ duyệt
                    if (wasHost) {
                        java.util.List<Phong> hostListings = phongRepository.findByChuNha_MaNguoiDung(id);
                        for (Phong listing : hostListings) {
                            if (listing.getTrangThai() == Phong.TrangThaiPhong.hoat_dong) {
                                listing.setTrangThai(Phong.TrangThaiPhong.khong_hoat_dong);
                            }
                        }
                        phongRepository.saveAll(hostListings);
                    }

                    return ResponseEntity.ok(java.util.Map.of("message", "Đã khóa người dùng " + u.getHoTen()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/users/{id}/unlock")
    @Transactional
    public ResponseEntity<?> unlockUser(@PathVariable Long id) {
        if (id == null)
            return ResponseEntity.badRequest().build();
        return nguoiDungRepository.findById(id)
                .map(u -> {
                    boolean wasHost = Boolean.TRUE.equals(u.getLaChuNha());
                    u.setBiKhoa(false);
                    u.setLyDoKhoa(null);
                    nguoiDungRepository.save(u);

                    // Khi mở khóa host: khôi phục listing ngừng hoạt động về hoạt động, giữ nguyên chờ duyệt
                    if (wasHost) {
                        java.util.List<Phong> hostListings = phongRepository.findByChuNha_MaNguoiDung(id);
                        for (Phong listing : hostListings) {
                            if (listing.getTrangThai() == Phong.TrangThaiPhong.khong_hoat_dong) {
                                listing.setTrangThai(Phong.TrangThaiPhong.hoat_dong);
                            }
                        }
                        phongRepository.saveAll(hostListings);
                    }

                    // Send email notification
                    try {
                        emailService.sendAccountUnlockedEmail(u.getEmail(), u.getHoTen());
                    } catch (Exception e) {
                        log.error("Failed to send unlock email to {}: {}", u.getEmail(), e.getMessage());
                    }

                    return ResponseEntity.ok(java.util.Map.of("message", "Đã mở khóa người dùng " + u.getHoTen()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/users/{id}/verify-identity")
    @Transactional
    public ResponseEntity<?> verifyIdentity(@PathVariable Long id, @RequestParam Boolean verify) {
        return nguoiDungRepository.findById(id)
                .map(u -> {
                    u.setXacMinhDanhTinh(verify);
                    if (verify) {
                        u.setNgayXacMinh(java.time.LocalDateTime.now());
                    }
                    nguoiDungRepository.save(u);

                    // Send email notification
                    try {
                        if (verify) {
                            emailService.sendKycVerifiedEmail(u.getEmail(), u.getHoTen());
                        } else {
                            emailService.sendKycRevokedEmail(u.getEmail(), u.getHoTen());
                        }
                    } catch (Exception e) {
                        log.error("Failed to send KYC email to {}: {}", u.getEmail(), e.getMessage());
                    }

                    // Send WebSocket notification to guest for real-time profile update
                    if (messagingTemplate != null) {
                        try {
                            com.project.airbnb.entity.ThongBao notif = new com.project.airbnb.entity.ThongBao();
                            notif.setNguoiGui(u); // self-notification
                            notif.setNguoiNhan(u);
                            notif.setLoaiThongBao(verify ? "KYC_VERIFIED" : "KYC_REVOKED");
                            notif.setNoiDung(verify ? "Danh tính đã được xác minh." : "Xác minh danh tính đã bị hủy.");
                            notif.setTrangThai("ACCEPTED");
                            thongBaoRepository.save(notif);
                            thongBaoRepository.flush();

                            com.project.airbnb.dto.ThongBaoDTO dto = com.project.airbnb.dto.ThongBaoDTO.fromEntity(notif);
                            messagingTemplate.convertAndSend("/queue/notifications/" + u.getMaNguoiDung(), dto);
                        } catch (Exception e) {
                            log.error("Failed to send WebSocket KYC notification to {}: {}", u.getMaNguoiDung(), e.getMessage());
                        }
                    }

                    return ResponseEntity
                            .ok(java.util.Map.of("message", "Đã cập nhật trạng thái xác minh cho " + u.getHoTen()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/users/{id}/lock-with-reason")
    @Transactional
    public ResponseEntity<?> lockUserWithReason(@PathVariable Long id,
            @RequestBody java.util.Map<String, String> payload) {
        String reason = payload.get("reason");
        return nguoiDungRepository.findById(id)
                .map(u -> {
                    boolean wasHost = Boolean.TRUE.equals(u.getLaChuNha());
                    u.setBiKhoa(true);
                    u.setLyDoKhoa(reason);
                    nguoiDungRepository.save(u);

                    // Khi khóa host: tạm dừng listing đang hoạt động, giữ nguyên chờ duyệt
                    if (wasHost) {
                        java.util.List<Phong> hostListings = phongRepository.findByChuNha_MaNguoiDung(id);
                        for (Phong listing : hostListings) {
                            if (listing.getTrangThai() == Phong.TrangThaiPhong.hoat_dong) {
                                listing.setTrangThai(Phong.TrangThaiPhong.khong_hoat_dong);
                            }
                        }
                        phongRepository.saveAll(hostListings);
                    }

                    // Send email notification
                    try {
                        emailService.sendAccountLockedEmail(u.getEmail(), u.getHoTen(), reason);
                    } catch (Exception e) {
                        log.error("Failed to send lock email to {}: {}", u.getEmail(), e.getMessage());
                    }

                    return ResponseEntity.ok(java.util.Map.of("message", "Đã khóa người dùng với lý do: " + reason));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/users/{id}/toggle-host")
    @Transactional
    public ResponseEntity<?> toggleHostRole(@PathVariable Long id) {
        return nguoiDungRepository.findById(id)
                .map(u -> {
                    boolean wasHost = u.getLaChuNha();
                    u.setLaChuNha(!wasHost);
                    nguoiDungRepository.save(u);

                    // Khi chuyển Host -> Guest: tạm停止 listing đang hoạt động, giữ nguyên chờ duyệt
                    if (wasHost) {
                        java.util.List<Phong> hostListings = phongRepository.findByChuNha_MaNguoiDung(id);
                        for (Phong listing : hostListings) {
                            if (listing.getTrangThai() == Phong.TrangThaiPhong.hoat_dong) {
                                listing.setTrangThai(Phong.TrangThaiPhong.khong_hoat_dong);
                            }
                        }
                        phongRepository.saveAll(hostListings);
                    }

                    // Khi chuyển Guest -> Host: khôi phục listing ngừng hoạt động về hoạt động, giữ nguyên chờ duyệt
                    if (!wasHost) {
                        java.util.List<Phong> hostListings = phongRepository.findByChuNha_MaNguoiDung(id);
                        for (Phong listing : hostListings) {
                            if (listing.getTrangThai() == Phong.TrangThaiPhong.khong_hoat_dong && !Boolean.TRUE.equals(listing.getBiKhoa())) {
                                listing.setTrangThai(Phong.TrangThaiPhong.hoat_dong);
                            }
                        }
                        phongRepository.saveAll(hostListings);
                    }

                    // Send email notification based on new role
                    try {
                        if (!wasHost) {
                            // User was guest, now becomes host
                            emailService.sendHostApprovedEmail(u.getEmail(), u.getHoTen());
                        } else {
                            // User was host, now becomes guest
                            emailService.sendHostRevokedEmail(u.getEmail(), u.getHoTen());
                        }
                    } catch (Exception e) {
                        log.error("Failed to send role changed email to {}: {}", u.getEmail(), e.getMessage());
                    }

                    return ResponseEntity.ok(java.util.Map.of(
                            "message", "Đã cập nhật vai trò cho " + u.getHoTen(),
                            "laChuNha", u.getLaChuNha()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/phong/{id}/status")
    @Transactional
    public ResponseEntity<?> setStatus(@PathVariable Long id, @RequestParam Phong.TrangThaiPhong trangThai,
            @RequestParam(required = false) String reason) {
        return phongRepository.findById(id)
                .map(sp -> {
                    sp.setTrangThai(trangThai);
                    if (trangThai == Phong.TrangThaiPhong.hoat_dong) {
                        sp.setDaDuyet(true);
                        sp.setLyDoTuChoi(null);
                    }
                    if (trangThai == Phong.TrangThaiPhong.khong_hoat_dong && reason != null) {
                        sp.setLyDoTuChoi(reason);
                    }
                    phongRepository.save(sp);
                    return ResponseEntity.ok(java.util.Map.of("updated", true));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/phong/batch-status")
    @Transactional
    public ResponseEntity<?> batchStatus(@RequestBody java.util.Map<String, Object> payload) {
        java.util.List<Long> ids = (java.util.List<Long>) payload.get("ids");
        Phong.TrangThaiPhong trangThai = Phong.TrangThaiPhong.valueOf((String) payload.get("trangThai"));

        java.util.List<Phong> listings = phongRepository.findAllById(ids);
        listings.forEach(sp -> {
            sp.setTrangThai(trangThai);
            if (trangThai == Phong.TrangThaiPhong.hoat_dong) {
                sp.setDaDuyet(true);
                sp.setLyDoTuChoi(null);
            }
        });
        phongRepository.saveAll(listings);

        return ResponseEntity.ok(java.util.Map.of("updated", listings.size()));
    }

    @PostMapping("/phong/batch-lock")
    @Transactional
    public ResponseEntity<?> batchLock(@RequestBody java.util.Map<String, Object> payload) {
        java.util.List<Long> ids = (java.util.List<Long>) payload.get("ids");
        Boolean lock = (Boolean) payload.get("lock");

        java.util.List<Phong> listings = phongRepository.findAllById(ids);
        listings.forEach(sp -> sp.setBiKhoa(lock));
        phongRepository.saveAll(listings);

        return ResponseEntity.ok(java.util.Map.of("updated", listings.size()));
    }

    @PostMapping("/phong/{id}/flags")
    @Transactional
    public ResponseEntity<?> setFlags(@PathVariable Long id,
            @RequestParam(required = false) Boolean duocKhachYeuThich,
            @RequestParam(required = false) Boolean laPhoBien) {
        return phongRepository.findById(id)
                .map(sp -> {
                    if (duocKhachYeuThich != null)
                        sp.setDuocKhachYeuThich(duocKhachYeuThich);
                    if (laPhoBien != null)
                        sp.setLaPhoBien(laPhoBien);
                    phongRepository.save(sp);
                    return ResponseEntity.ok(java.util.Map.of("updated", true));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/phong/pending")
    public ResponseEntity<?> pending() {
        return ResponseEntity.ok(
                phongRepository.findAll().stream()
                        .filter(sp -> sp.getTrangThai() == Phong.TrangThaiPhong.cho_duyet)
                        .toList());
    }

    @GetMapping("/phong")
    public ResponseEntity<?> getListings() {
        return ResponseEntity.ok(
                phongRepository.findAllWithImagesAndHost().stream()
                        .map(com.project.airbnb.dto.PhongDTO::fromEntity)
                        .collect(java.util.stream.Collectors.toList()));
    }

    @PostMapping("/phong/{id}/lock")
    @Transactional
    public ResponseEntity<?> lockProduct(@PathVariable Long id) {
        return phongRepository.findById(id)
                .map(sp -> {
                    sp.setBiKhoa(true);
                    phongRepository.save(sp);
                    return ResponseEntity.ok(java.util.Map.of("locked", true));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/phong/{id}/unlock")
    @Transactional
    public ResponseEntity<?> unlockProduct(@PathVariable Long id) {
        return phongRepository.findById(id)
                .map(sp -> {
                    sp.setBiKhoa(false);
                    phongRepository.save(sp);
                    return ResponseEntity.ok(java.util.Map.of("unlocked", true));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/phong/{id}")
    @Transactional
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        if (!phongRepository.existsById(id))
            return ResponseEntity.notFound().build();
        phongRepository.deleteById(id);
        return ResponseEntity.ok(java.util.Map.of("deleted", true));
    }

    @GetMapping("/phong/{id}")
    public ResponseEntity<?> getListingDetail(@PathVariable Long id) {
        return phongRepository.findById(id)
                .map(sp -> {
                    java.util.List<com.project.airbnb.entity.HinhAnh> hinhAnhs = hinhAnhRepository
                            .findByPhong_MaPhongOrderByThuTuAsc(id);
                    java.util.List<com.project.airbnb.entity.TienNghiPhong> tienNghis = tienNghiPhongRepository
                            .findByPhong_MaPhongOrderByNhomAscTenAsc(id);
                    return com.project.airbnb.dto.PhongDTO.fromEntity(sp, hinhAnhs, tienNghis);
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/phong")
    @Transactional
    public ResponseEntity<?> createListing(@RequestBody CreateListingRequest req) {
        com.project.airbnb.entity.NguoiDung host = nguoiDungRepository.findById(req.getMaChuNha())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ nhà"));

        Phong sp = new Phong();
        sp.setChuNha(host);
        sp.setLoaiPhong(req.getLoaiPhong());
        sp.setTieuDe(req.getTieuDe());
        sp.setMoTa(req.getMoTa());
        sp.setUrlAnhChinh(req.getUrlAnhChinh());
        sp.setQuocGia(req.getQuocGia());
        sp.setThanhPho(req.getThanhPho());
        sp.setQuanHuyen(req.getQuanHuyen());
        sp.setPhuongXa(req.getPhuongXa());
        sp.setDiaChiDayDu(req.getDiaChiDayDu());
        sp.setGiaMoiKhach(req.getGiaMoiKhach());
        sp.setGiaDatToiThieu(req.getGiaDatToiThieu());
        sp.setLoaiBatDongSan(req.getLoaiBatDongSan());
        sp.setSoPhongNgu(req.getSoPhongNgu());
        sp.setSoGiuong(req.getSoGiuong());
        sp.setSoPhongTam(req.getSoPhongTam());
        sp.setSoKhachToiDa(req.getSoKhachToiDa());
        sp.setSoDemToiThieu(req.getSoDemToiThieu());
        sp.setPhiVeSinh(req.getPhiVeSinh());

        // Admin tạo thì mặc định duyệt luôn hoặc tuỳ logic business, ở đây để mặc định
        // như Host tạo (chờ duyệt) hoặc set luôn Hoạt động
        // User yêu cầu "fix admin", thường admin tạo thì nên active luôn
        sp.setTrangThai(Phong.TrangThaiPhong.hoat_dong);
        sp.setDaDuyet(true);

        return ResponseEntity.ok(com.project.airbnb.dto.PhongDTO.fromEntity(phongRepository.save(sp)));
    }

    @PutMapping("/phong/{id}")
    @Transactional
    public ResponseEntity<?> updateListing(@PathVariable Long id, @RequestBody UpdateListingRequest req) {
        return phongRepository.findById(id)
                .map(sp -> {
                    if (req.getTieuDe() != null)
                        sp.setTieuDe(req.getTieuDe());
                    if (req.getMoTa() != null)
                        sp.setMoTa(req.getMoTa());
                    if (req.getUrlAnhChinh() != null)
                        sp.setUrlAnhChinh(req.getUrlAnhChinh());
                    if (req.getGiaMoiKhach() != null)
                        sp.setGiaMoiKhach(req.getGiaMoiKhach());
                    if (req.getGiaDatToiThieu() != null)
                        sp.setGiaDatToiThieu(req.getGiaDatToiThieu());
                    if (req.getSoKhachToiDa() != null)
                        sp.setSoKhachToiDa(req.getSoKhachToiDa());
                    if (req.getSoDemToiThieu() != null)
                        sp.setSoDemToiThieu(req.getSoDemToiThieu());
                    if (req.getPhiVeSinh() != null)
                        sp.setPhiVeSinh(req.getPhiVeSinh());
                    if (req.getPassPhong() != null)
                        sp.setPassPhong(req.getPassPhong());

                    // Admin update address info if needed
                    if (req.getQuocGia() != null)
                        sp.setQuocGia(req.getQuocGia());
                    if (req.getThanhPho() != null)
                        sp.setThanhPho(req.getThanhPho());
                    if (req.getQuanHuyen() != null)
                        sp.setQuanHuyen(req.getQuanHuyen());
                    if (req.getPhuongXa() != null)
                        sp.setPhuongXa(req.getPhuongXa());
                    if (req.getDiaChiDayDu() != null)
                        sp.setDiaChiDayDu(req.getDiaChiDayDu());

                    if (req.getLoaiPhong() != null)
                        sp.setLoaiPhong(req.getLoaiPhong());

                    phongRepository.save(sp);
                    return ResponseEntity.ok(com.project.airbnb.dto.PhongDTO.fromEntity(sp));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/payments")
    public ResponseEntity<?> getAllPayments() {
        // Auto-process pending payments & refunds before returning
        try {
            paymentService.autoConfirmPendingPayments();
            paymentService.autoApproveRefunds();
        } catch (Exception e) {
            log.warn("Auto-processing payments failed: {}", e.getMessage());
        }

        List<com.project.airbnb.entity.Payment> payments = paymentRepository.findAllWithDetails();
        return ResponseEntity.ok(
                payments.stream()
                        .map(com.project.airbnb.dto.PaymentDTO::fromEntity)
                        .collect(Collectors.toList()));
    }

    @GetMapping("/bookings")
    public ResponseEntity<?> getAllBookings() {
        // Auto-process pending payments & refunds before returning (like payment page)
        try {
            paymentService.autoConfirmPendingPayments();
            paymentService.autoApproveRefunds();
        } catch (Exception e) {
            log.warn("Auto-processing before bookings list failed: {}", e.getMessage());
        }

        List<DatCho> bookings = datChoRepository.findAll();
        return ResponseEntity.ok(bookings.stream().map(booking -> {
            List<Payment> payments = paymentRepository.findByBooking_MaDatCho(booking.getMaDatCho());
            return DatChoResponse.fromEntity(booking, payments);
        }).collect(Collectors.toList()));
    }

    @PostMapping("/bookings/{id}/cancel")
    @Transactional
    public ResponseEntity<?> adminCancelBooking(@PathVariable Long id) {
        return datChoRepository.findById(id)
                .map(dc -> {
                    dc.setTrangThaiDatCho(DatCho.TrangThaiDatCho.da_huy);

                    List<Payment> payments = paymentRepository.findByBooking_MaDatCho(id);
                    boolean hasPaid = payments.stream().anyMatch(p ->
                            p.getStatus() == Payment.PaymentStatus.DA_XAC_NHAN
                            || p.getStatus() == Payment.PaymentStatus.CHO_XAC_NHAN);
                    if (hasPaid) {
                        BigDecimal refundAmt = paymentService.calculateRefundAmount(dc);
                        dc.setTienHoanLai(refundAmt);
                    }

                    datChoRepository.save(dc);
                    payments.forEach(p -> {
                        if (p.getStatus() == Payment.PaymentStatus.DA_XAC_NHAN
                                || p.getStatus() == Payment.PaymentStatus.CHO_XAC_NHAN) {
                            p.setStatus(Payment.PaymentStatus.YEU_CAU_HOAN_TIEN);
                        } else if (p.getStatus() == Payment.PaymentStatus.CHUA_THANH_TOAN) {
                            p.setStatus(Payment.PaymentStatus.DA_HUY);
                        }
                    });
                    paymentRepository.saveAll(payments);
                    releaseDates(dc);
                    return ResponseEntity.ok(Map.of("cancelled", true));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @lombok.Data
    public static class CreateListingRequest {
        private Long maChuNha;
        private Phong.LoaiPhong loaiPhong;
        private String tieuDe;
        private String moTa;
        private String urlAnhChinh;
        private String quocGia;
        private String thanhPho;
        private String quanHuyen;
        private String phuongXa;
        private String diaChiDayDu;
        private java.math.BigDecimal giaMoiKhach;
        private java.math.BigDecimal giaDatToiThieu;
        private String loaiBatDongSan;
        private Integer soPhongNgu;
        private Integer soGiuong;
        private java.math.BigDecimal soPhongTam;
        private Integer soKhachToiDa;
        private Integer soDemToiThieu;
        private java.math.BigDecimal phiVeSinh;
        private String passPhong;
    }

    @lombok.Data
    public static class UpdateListingRequest {
        private String tieuDe;
        private String moTa;
        private String urlAnhChinh;
        private java.math.BigDecimal giaMoiKhach;
        private java.math.BigDecimal giaDatToiThieu;
        private Integer soKhachToiDa;
        private Integer soDemToiThieu;
        private java.math.BigDecimal phiVeSinh;

        // Additional fields for Admin
        private Phong.LoaiPhong loaiPhong;
        private String quocGia;
        private String thanhPho;
        private String quanHuyen;
        private String phuongXa;
        private String diaChiDayDu;
        private String passPhong;
    }

    @lombok.Data
    public static class SetStatusRequest {
        private String reason;
    }

    private void releaseDates(DatCho booking) {
        Phong sp = booking.getPhong();
        if (sp != null && sp.getLoaiPhong() == Phong.LoaiPhong.noi_luu_tru) {
            java.time.LocalDate date = booking.getNgayNhanPhong();
            while (date.isBefore(booking.getNgayTraPhong())) {
                var opt = lichTrinhRepository.findByPhong_MaPhongAndNgay(sp.getMaPhong(), date);
                if (opt.isPresent()) {
                    com.project.airbnb.entity.LichTrinh rec = opt.get();
                    rec.setConKhaDung(true);
                    lichTrinhRepository.save(rec);
                }
                date = date.plusDays(1);
            }
        }
    }
}
