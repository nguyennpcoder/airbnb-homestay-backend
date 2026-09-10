package com.project.airbnb.controller;

import com.project.airbnb.dto.DatChoResponse;
import com.project.airbnb.dto.PhongDTO;
import com.project.airbnb.entity.DatCho;
import com.project.airbnb.entity.Payment;
import com.project.airbnb.entity.TinNhan;
import com.project.airbnb.repository.DatChoRepository;
import com.project.airbnb.repository.NguoiDungRepository;
import com.project.airbnb.repository.PaymentRepository;
import com.project.airbnb.repository.PhongRepository;
import com.project.airbnb.repository.TinNhanRepository;
import com.project.airbnb.repository.NhomTienNghiRepository;
import com.project.airbnb.repository.DanhMucTienNghiRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/host")
@Transactional(readOnly = true)
public class HostController {

    private final DatChoRepository datChoRepository;
    private final PhongRepository phongRepository;
    private final PaymentRepository paymentRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final com.project.airbnb.repository.DanhGiaRepository danhGiaRepository;
    private final NhomTienNghiRepository nhomTienNghiRepository;
    private final DanhMucTienNghiRepository danhMucTienNghiRepository;
    private final TinNhanRepository tinNhanRepository;

    public HostController(DatChoRepository datChoRepository,
            PhongRepository phongRepository,
            PaymentRepository paymentRepository,
            NguoiDungRepository nguoiDungRepository,
            com.project.airbnb.repository.DanhGiaRepository danhGiaRepository,
            NhomTienNghiRepository nhomTienNghiRepository,
            DanhMucTienNghiRepository danhMucTienNghiRepository,
            TinNhanRepository tinNhanRepository) {
        this.datChoRepository = datChoRepository;
        this.phongRepository = phongRepository;
        this.paymentRepository = paymentRepository;
        this.nguoiDungRepository = nguoiDungRepository;
        this.danhGiaRepository = danhGiaRepository;
        this.nhomTienNghiRepository = nhomTienNghiRepository;
        this.danhMucTienNghiRepository = danhMucTienNghiRepository;
        this.tinNhanRepository = tinNhanRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@RequestParam Long hostId) {
        Double totalRevenue = paymentRepository.sumRevenueByHost(hostId);
        Long totalBookings = datChoRepository.countBookingsByHost(hostId);
        Long totalViews = phongRepository.sumViewsByHost(hostId);
        Long pendingBookingsCount = datChoRepository.countPendingRealBookingsByHost(hostId);

        java.time.LocalDate now = java.time.LocalDate.now();
        int daysInMonth = now.lengthOfMonth();
        long totalListings = phongRepository.countByChuNha_MaNguoiDung(hostId);

        double occupancyRate = 0.0;
        if (totalListings > 0) {
            List<DatCho> monthBookings = datChoRepository.findBookingsByHostAndMonth(hostId, now.getMonthValue(),
                    now.getYear());
            long bookedNights = 0;
            for (DatCho booking : monthBookings) {
                if (booking.getNgayNhanPhong() != null && booking.getNgayTraPhong() != null) {
                    long nights = java.time.temporal.ChronoUnit.DAYS.between(booking.getNgayNhanPhong(),
                            booking.getNgayTraPhong());
                    bookedNights += nights;
                }
            }
            occupancyRate = (double) bookedNights / (totalListings * daysInMonth) * 100;
        }

        Double avgRating = danhGiaRepository.getAverageRatingByHost(hostId);
        Long reviewCount = danhGiaRepository.countByPhong_ChuNha_MaNguoiDung(hostId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("revenue", totalRevenue != null ? totalRevenue : 0.0);
        stats.put("bookings", totalBookings != null ? totalBookings : 0);
        stats.put("rating", avgRating != null ? avgRating : 0.0);
        stats.put("reviews", reviewCount != null ? reviewCount : 0);
        stats.put("views", totalViews != null ? totalViews : 0);
        stats.put("occupancy", Math.min(100.0, occupancyRate));
        stats.put("pendingBookings", pendingBookingsCount != null ? pendingBookingsCount : 0);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/revenue-chart")
    public ResponseEntity<?> getRevenueChart(@RequestParam Long hostId, @RequestParam(defaultValue = "2025") int year) {
        return ResponseEntity.ok(paymentRepository.getMonthlyRevenueByHost(hostId, year));
    }

    @GetMapping("/service-fee-chart")
    public ResponseEntity<?> getServiceFeeChart(@RequestParam Long hostId, @RequestParam(defaultValue = "2025") int year) {
        return ResponseEntity.ok(paymentRepository.getMonthlyServiceFeeByHost(hostId, year));
    }

    @GetMapping("/refunds-chart")
    public ResponseEntity<?> getRefundChart(@RequestParam Long hostId, @RequestParam(defaultValue = "2025") int year) {
        return ResponseEntity.ok(paymentRepository.getMonthlyRefundsByHost(hostId, year));
    }

    @GetMapping("/cancelled-failed-chart")
    public ResponseEntity<?> getCancelledFailedChart(@RequestParam Long hostId, @RequestParam(defaultValue = "2025") int year) {
        return ResponseEntity.ok(paymentRepository.getMonthlyCancelledFailedByHost(hostId, year));
    }

    @GetMapping("/bookings")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getBookings(@RequestParam Long hostId) {
        List<DatCho> bookings = datChoRepository.findByPhong_ChuNha_MaNguoiDung(hostId);
        List<DatChoResponse> responses = bookings.stream().map(booking -> {
            List<Payment> payments = paymentRepository.findByBooking_MaDatCho(booking.getMaDatCho());
            return DatChoResponse.fromEntity(booking, payments);
        }).toList();

        // Populate lastMessage, lastMessageTime, hasUnread for each booking
        if (!responses.isEmpty()) {
            List<Long> bookingIds = responses.stream().map(DatChoResponse::getMaDatCho).toList();

            // Batch query unread counts
            List<Object[]> unreadCounts = tinNhanRepository.countUnreadByBookingIds(bookingIds, hostId);
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

    @GetMapping("/payments")
    public ResponseEntity<?> getHostPayments(@RequestParam Long hostId) {
        List<Payment> payments = paymentRepository.findAllWithDetails().stream()
                .filter(p -> p.getBooking() != null
                        && p.getBooking().getPhong() != null
                        && p.getBooking().getPhong().getChuNha() != null
                        && p.getBooking().getPhong().getChuNha().getMaNguoiDung().equals(hostId))
                .toList();
        return ResponseEntity.ok(payments.stream()
                .map(com.project.airbnb.dto.PaymentDTO::fromEntity)
                .collect(java.util.stream.Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getHostProfile(@PathVariable Long id) {
        return nguoiDungRepository.findById(id)
                .map(host -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("maNguoiDung", host.getMaNguoiDung());
                    response.put("hoTen", host.getHoTen());
                    response.put("ho", host.getHo());
                    response.put("ten", host.getTen());
                    response.put("ngaySinh", host.getNgaySinh());
                    response.put("email", host.getEmail());
                    response.put("soDienThoai", host.getSoDienThoai());
                    response.put("urlAnhDaiDien", host.getUrlAnhDaiDien());
                    response.put("laChuNha", host.getLaChuNha());
                    response.put("emailDaXacNhan", host.getEmailDaXacNhan());
                    response.put("daChapNhanCamKetCongDong", host.getDaChapNhanCamKetCongDong());
                    response.put("nhanTinNhanTiepThi", host.getNhanTinNhanTiepThi());
                    response.put("ngayTao", host.getNgayTao());
                    response.put("ngayCapNhat", host.getNgayCapNhat());
                    response.put("soDu", host.getSoDu());
                    response.put("soLuongDanhGiaHost", host.getSoLuongDanhGiaHost());
                    response.put("diemDanhGiaHost", host.getDiemDanhGiaHost());
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping({ "/{id}/phong", "/{id}/san-pham" })
    public ResponseEntity<List<PhongDTO>> getHostListings(@PathVariable Long id) {
        java.util.Set<String> activeGroups = new java.util.HashSet<>(nhomTienNghiRepository.findActiveGroupNamesByHost(id));
        java.util.Set<String> hiddenCatalogs = new java.util.HashSet<>(danhMucTienNghiRepository.findHiddenCatalogNamesByHost(id));
        return ResponseEntity.ok(phongRepository.findByChuNha_MaNguoiDungAndNotLocked(id).stream()
                .map(PhongDTO::fromEntity)
                .peek(dto -> dto.setTienNghi(PhongDTO.filterTienNghi(dto.getTienNghi(), activeGroups, hiddenCatalogs)))
                .toList());
    }

    @GetMapping({ "/{id}/phong/all", "/{id}/san-pham/all" })
    public ResponseEntity<List<PhongDTO>> getAllHostListings(@PathVariable Long id) {
        java.util.Set<String> activeGroups = new java.util.HashSet<>(nhomTienNghiRepository.findActiveGroupNamesByHost(id));
        java.util.Set<String> hiddenCatalogs = new java.util.HashSet<>(danhMucTienNghiRepository.findHiddenCatalogNamesByHost(id));
        return ResponseEntity.ok(phongRepository.findByChuNha_MaNguoiDung(id).stream()
                .map(PhongDTO::fromEntity)
                .peek(dto -> dto.setTienNghi(PhongDTO.filterTienNghi(dto.getTienNghi(), activeGroups, hiddenCatalogs)))
                .toList());
    }

    @PostMapping("/listing/{id}/view")
    @Transactional
    public ResponseEntity<?> incrementListingView(@PathVariable Long id) {
        phongRepository.incrementViews(id);
        return ResponseEntity.ok(Map.of("id", id));
    }
}
