package com.project.airbnb.service;

import com.project.airbnb.entity.DatCho;
import com.project.airbnb.entity.LichTrinh;
import com.project.airbnb.entity.Payment;
import com.project.airbnb.entity.Phong;
import com.project.airbnb.repository.DatChoRepository;
import com.project.airbnb.repository.LichTrinhRepository;
import com.project.airbnb.repository.PaymentRepository;
import com.project.airbnb.repository.NguoiDungRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final DatChoRepository datChoRepository;
    private final LichTrinhRepository lichTrinhRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final EmailService emailService;
    private final QuyDinhGiaService quyDinhGiaService;
    private final PaymentService self;

    @Value("${vnpay.tmnCode:}")
    private String vnpTmnCode;
    @Value("${vnpay.hashSecret:}")
    private String vnpHashSecret;
    @Value("${vnpay.payUrl:}")
    private String vnpPayUrl;
    @Value("${vnpay.returnUrl:}")
    private String vnpReturnUrl;
    @Value("${vnpay.apiUrl:}")
    private String vnpApiUrl;
    @Value("${zalopay.appId:0}")
    private String zaloAppId;
    @Value("${zalopay.key1:}")
    private String zaloKey1;
    @Value("${zalopay.key2:}")
    private String zaloKey2;
    @Value("${zalopay.createUrl:}")
    private String zaloCreateUrl;
    @Value("${zalopay.returnUrl:}")
    private String zaloReturnUrl;
    @Value("${zalopay.refundUrl:}")
    private String zaloRefundUrl;
    @Value("${momo.partnerCode:}")
    private String momoPartnerCode;
    @Value("${momo.accessKey:}")
    private String momoAccessKey;
    @Value("${momo.secretKey:}")
    private String momoSecretKey;
    @Value("${momo.createUrl:}")
    private String momoCreateUrl;
    @Value("${momo.returnUrl:}")
    private String momoReturnUrl;

    public PaymentService(PaymentRepository paymentRepository,
            DatChoRepository datChoRepository,
            LichTrinhRepository lichTrinhRepository,
            NguoiDungRepository nguoiDungRepository,
            EmailService emailService,
            QuyDinhGiaService quyDinhGiaService,
            @Lazy PaymentService self) {
        this.paymentRepository = paymentRepository;
        this.datChoRepository = datChoRepository;
        this.lichTrinhRepository = lichTrinhRepository;
        this.nguoiDungRepository = nguoiDungRepository;
        this.emailService = emailService;
        this.quyDinhGiaService = quyDinhGiaService;
        this.self = self;
    }

    /**
     * Tỷ lệ phí dịch vụ (0-1) lấy từ quy_dinh_gia, mặc định 0.10.
     */
    private BigDecimal serviceFeeRate() {
        try {
            com.project.airbnb.entity.QuyDinhGia qd = quyDinhGiaService.get();
            BigDecimal tyLe = qd.getTyLePhiDichVu();
            if (tyLe == null) tyLe = new BigDecimal("10.00");
            return tyLe.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.warn("Không đọc được ty_le_phi_dich_vu, dùng mặc định 10%: {}", e.getMessage());
            return new BigDecimal("0.10");
        }
    }

    @Scheduled(fixedRate = 3000)
    public void scheduledAutoProcess() {
        try {
            self.processPendingPaymentsDirect();
            syncBookingStatuses();
            self.processRefundRequestsDirect();
            self.fixOrphanedRefunds();
        } catch (Exception e) {
            log.error("Scheduler error: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void fixOrphanedRefunds() {
        List<DatCho> cancelledBookings = datChoRepository
                .findByTrangThaiDatCho(DatCho.TrangThaiDatCho.da_huy);
        for (DatCho booking : cancelledBookings) {
            try {
                if (booking.getTienHoanLai() == null || booking.getTienHoanLai().compareTo(BigDecimal.ZERO) <= 0) continue;
                if (booking.getKhach() == null) continue;

                List<Payment> payments = paymentRepository.findByBooking_MaDatCho(booking.getMaDatCho());
                boolean allRefunded = !payments.isEmpty() && payments.stream()
                        .allMatch(p -> p.getStatus() == Payment.PaymentStatus.HOAN_TIEN);
                if (!allRefunded) continue;

                Long userId = booking.getKhach().getMaNguoiDung();
                com.project.airbnb.entity.NguoiDung user = nguoiDungRepository.findById(userId).orElse(null);
                if (user == null) continue;

                BigDecimal refundAmt = booking.getTienHoanLai();
                BigDecimal currentBalance = user.getSoDu() != null ? user.getSoDu() : BigDecimal.ZERO;
                user.setSoDu(currentBalance.add(refundAmt));
                nguoiDungRepository.saveAndFlush(user);

                booking.setTienHoanLai(BigDecimal.ZERO);
                datChoRepository.save(booking);

                log.info("Catch-up: credited {} VND to user {} for booking {}", refundAmt, userId, booking.getMaDatCho());
            } catch (Exception e) {
                log.error("Fix orphaned refund failed for booking {}: {}", booking.getMaDatCho(), e.getMessage());
            }
        }
    }

    /**
     * Catch-all: find any booking still stuck at cho_xac_nhan that already has
     * a CONFIRMED payment and upgrade it to da_xac_nhan.
     */
    @Transactional
    public void syncBookingStatuses() {
        List<DatCho> stuck = datChoRepository.findByTrangThaiDatCho(DatCho.TrangThaiDatCho.cho_xac_nhan);
        for (DatCho booking : stuck) {
            boolean hasConfirmed = paymentRepository.findByBooking_MaDatCho(booking.getMaDatCho()).stream()
                    .anyMatch(p -> p.getStatus() == Payment.PaymentStatus.DA_XAC_NHAN);
            if (hasConfirmed) {
                booking.setTrangThaiDatCho(DatCho.TrangThaiDatCho.da_xac_nhan);
                datChoRepository.save(booking);
                log.info("syncBookingStatuses: booking {} -> da_xac_nhan (has CONFIRMED payment)", booking.getMaDatCho());
            }
        }

        List<DatCho> confirmed = datChoRepository.findByTrangThaiDatCho(DatCho.TrangThaiDatCho.da_xac_nhan);
        java.time.LocalDate today = java.time.LocalDate.now();
        for (DatCho booking : confirmed) {
            java.time.LocalDate checkOut = booking.getNgayTraPhong();
            if (checkOut != null && !checkOut.isAfter(today)) {
                booking.setTrangThaiDatCho(DatCho.TrangThaiDatCho.hoan_thanh);
                datChoRepository.save(booking);
                log.info("syncBookingStatuses: booking {} -> hoan_thanh (checkout passed)", booking.getMaDatCho());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processPendingPaymentsDirect() {
        List<Payment> pendingPayments = paymentRepository.findByStatus(Payment.PaymentStatus.CHO_XAC_NHAN);
        if (pendingPayments.isEmpty()) return;
        log.info("Scheduler found {} PENDING payment(s)", pendingPayments.size());

        for (Payment payment : pendingPayments) {
            try {
                if (payment.getProvider() == Payment.PaymentProvider.SEPAY) continue;

                if (payment.getPaymentDate() != null) {
                    long secs = ChronoUnit.SECONDS.between(payment.getPaymentDate(), LocalDateTime.now());
                    if (secs < 3) continue;
                }

                payment.setStatus(Payment.PaymentStatus.DA_XAC_NHAN);
                payment.setPaymentDate(LocalDateTime.now());
                paymentRepository.save(payment);

                DatCho booking = payment.getBooking();
                if (booking == null) {
                    log.info("Payment {} confirmed (no booking linked)", payment.getId());
                    continue;
                }

                Long bookingId = booking.getMaDatCho();

                // Confirm all PENDING payments for this booking
                List<Payment> siblings = paymentRepository.findByBooking_MaDatCho(bookingId).stream()
                        .filter(p -> p.getStatus() == Payment.PaymentStatus.CHO_XAC_NHAN).toList();
                for (Payment sib : siblings) {
                    sib.setStatus(Payment.PaymentStatus.DA_XAC_NHAN);
                    sib.setPaymentDate(LocalDateTime.now());
                    paymentRepository.save(sib);
                }

                // Update booking status — availability check is best-effort
                if (booking.getTrangThaiDatCho() == DatCho.TrangThaiDatCho.cho_xac_nhan) {
                    boolean availOk = true;
                    try {
                        availOk = checkAndUpdateAvailability(booking);
                    } catch (Exception ex) {
                        log.warn("Availability check failed for booking {}: {}", bookingId, ex.getMessage());
                    }
                    // Confirm regardless of availability — payment already captured
                    booking.setTrangThaiDatCho(DatCho.TrangThaiDatCho.da_xac_nhan);
                    datChoRepository.save(booking);
                    log.info("Booking {} -> da_xac_nhan (avail={})", bookingId, availOk);
                }

                // Cong phi dich vu vao vi admin (dung query truc tiep de dam bao ghi duoi DB)
                try {
                    BigDecimal svcFee = payment.getServiceFee() != null ? payment.getServiceFee() : BigDecimal.ZERO;
                    for (Payment sib : siblings) {
                        svcFee = svcFee.add(sib.getServiceFee() != null ? sib.getServiceFee() : BigDecimal.ZERO);
                    }
                    final BigDecimal totalServiceFee = svcFee;
                    if (totalServiceFee.compareTo(BigDecimal.ZERO) > 0) {
                        int rows = nguoiDungRepository.creditAdminWallet(totalServiceFee);
                        log.info("Credited service fee {} to admin wallet. Rows updated: {}", totalServiceFee, rows);
                    }
                } catch (Exception ex) {
                    log.error("Failed to credit service fee to admin wallet: {}", ex.getMessage(), ex);
                }

                // Cong tien vao vi chu nha (tong tien - phi dich vu)
                try {
                    if (booking.getPhong() != null && booking.getPhong().getChuNha() != null) {
                        Long hostId = booking.getPhong().getChuNha().getMaNguoiDung();
                        BigDecimal totalAmount = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
                        BigDecimal svcFee = payment.getServiceFee() != null ? payment.getServiceFee() : BigDecimal.ZERO;
                        for (Payment sib : siblings) {
                            totalAmount = totalAmount.add(sib.getAmount() != null ? sib.getAmount() : BigDecimal.ZERO);
                            svcFee = svcFee.add(sib.getServiceFee() != null ? sib.getServiceFee() : BigDecimal.ZERO);
                        }
                        BigDecimal hostEarning = totalAmount.subtract(svcFee);
                        if (hostEarning.compareTo(BigDecimal.ZERO) > 0) {
                            int rows = nguoiDungRepository.creditHostWallet(hostId, hostEarning);
                            log.info("Credited host earning {} to host {}. Rows updated: {}", hostEarning, hostId, rows);
                        }
                    }
                } catch (Exception ex) {
                    log.error("Failed to credit host wallet: {}", ex.getMessage(), ex);
                }

                // Gui email xac nhan dat phong
                try {
                    if (booking.getKhach() != null && booking.getKhach().getEmail() != null) {
                        emailService.sendBookingConfirmedEmail(
                                booking.getKhach().getEmail(),
                                booking.getKhach().getHoTen() != null ? booking.getKhach().getHoTen() : "Khách",
                                booking);
                        log.info("Booking confirmed email sent for booking {}", bookingId);
                    }
                } catch (Exception ex) {
                    log.error("Failed to send confirmation email for booking {}: {}", bookingId, ex.getMessage());
                }
            } catch (Exception e) {
                log.error("Failed processing payment {}: {}", payment.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    public void processRefundRequestsDirect() {
        List<Payment> refundRequests = paymentRepository.findByStatus(Payment.PaymentStatus.YEU_CAU_HOAN_TIEN);
        if (refundRequests.isEmpty()) return;

        // Group by booking
        java.util.Map<Long, List<Payment>> byBooking = new java.util.HashMap<>();
        for (Payment p : refundRequests) {
            if (p.getBooking() != null) {
                byBooking.computeIfAbsent(p.getBooking().getMaDatCho(), k -> new java.util.ArrayList<>()).add(p);
            }
        }

        for (var entry : byBooking.entrySet()) {
            try {
                List<Payment> payments = entry.getValue();
                DatCho booking = payments.get(0).getBooking();
                if (booking == null) continue;

                // Calculate refund BEFORE changing any payment statuses
                BigDecimal refundAmount = booking.getTienHoanLai();
                if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    BigDecimal totalPaid = payments.stream()
                            .map(Payment::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal serviceFee = booking.getPhiDichVu() != null ? booking.getPhiDichVu() : BigDecimal.ZERO;
                    BigDecimal cleaningFee = booking.getPhiVeSinh() != null ? booking.getPhiVeSinh() : BigDecimal.ZERO;
                    BigDecimal nightCost = totalPaid.subtract(serviceFee).subtract(cleaningFee);
                    if (nightCost.compareTo(BigDecimal.ZERO) < 0) nightCost = BigDecimal.ZERO;

                    Phong.ChinhSachHuy policy = booking.getPhong() != null && booking.getPhong().getChinhSachHuy() != null
                            ? booking.getPhong().getChinhSachHuy() : Phong.ChinhSachHuy.LINH_HOAT;
                    LocalDate checkInDate = booking.getNgayNhanPhong();
                    long daysUntilCheckIn = checkInDate != null
                            ? ChronoUnit.DAYS.between(LocalDate.now(), checkInDate) : 30;

                    BigDecimal nightPct = BigDecimal.ZERO;
                    switch (policy) {
                        case LINH_HOAT: if (daysUntilCheckIn >= 0) nightPct = BigDecimal.ONE; break;
                        case TRUNG_BINH: if (daysUntilCheckIn >= 0) nightPct = new BigDecimal("0.5"); break;
                        case NGHIEM_NGAT: nightPct = BigDecimal.ZERO; break;
                    }
                    BigDecimal nightRefund = nightCost.multiply(nightPct).setScale(0, java.math.RoundingMode.HALF_UP);
                    refundAmount = nightRefund.add(cleaningFee);

                    booking.setTienHoanLai(refundAmount);
                    datChoRepository.save(booking);
                }

                log.info("Processing refund for booking {}: amount={}", booking.getMaDatCho(), refundAmount);

                // Step 1: Credit wallet FIRST
                if (refundAmount.compareTo(BigDecimal.ZERO) > 0 && booking.getKhach() != null) {
                    Long userId = booking.getKhach().getMaNguoiDung();
                    com.project.airbnb.entity.NguoiDung freshUser = nguoiDungRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
                    BigDecimal balance = freshUser.getSoDu() != null ? freshUser.getSoDu() : BigDecimal.ZERO;
                    freshUser.setSoDu(balance.add(refundAmount));
                    nguoiDungRepository.saveAndFlush(freshUser);
                    log.info("Wallet credited {} VND for user {}. New balance: {}", refundAmount, userId, freshUser.getSoDu());
                }

                // Step 1b: Tru tien tu vi chu nha (host da nhan tien truoc do, nay bi thu lai)
                try {
                    if (booking.getPhong() != null && booking.getPhong().getChuNha() != null) {
                        Long hostId = booking.getPhong().getChuNha().getMaNguoiDung();
                        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                            int rows = nguoiDungRepository.debitHostWallet(hostId, refundAmount);
                            log.info("Debited {} from host {} wallet. Rows updated: {}", refundAmount, hostId, rows);
                        }
                    }
                } catch (Exception ex) {
                    log.error("Failed to debit host wallet during refund: {}", ex.getMessage(), ex);
                }

                // Step 2: Mark payments as REFUNDED
                for (Payment p : payments) {
                    p.setStatus(Payment.PaymentStatus.HOAN_TIEN);
                    paymentRepository.save(p);
                }

                // Step 3: Create refund payment record (negative amount)
                if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                    Payment refundPayment = new Payment();
                    refundPayment.setBooking(booking);
                    refundPayment.setAmount(refundAmount.negate());
                    refundPayment.setCurrency("VND");
                    refundPayment.setProvider(Payment.PaymentProvider.WALLET);
                    refundPayment.setTransactionId("REFUND_" + booking.getMaDatCho() + "_" + System.currentTimeMillis());
                    refundPayment.setStatus(Payment.PaymentStatus.HOAN_TIEN);
                    refundPayment.setPaymentDate(LocalDateTime.now());
                    paymentRepository.save(refundPayment);
                }

                // Step 4: Clear tienHoanLai to prevent fixOrphanedRefunds from double-crediting
                booking.setTienHoanLai(BigDecimal.ZERO);
                datChoRepository.save(booking);

                // Step 4: Cancel booking
                if (booking.getTrangThaiDatCho() != DatCho.TrangThaiDatCho.da_huy) {
                    booking.setTrangThaiDatCho(DatCho.TrangThaiDatCho.da_huy);
                    datChoRepository.save(booking);
                    releaseDates(booking);
                }
                log.info("Booking {} fully refunded ({} VND)", booking.getMaDatCho(), refundAmount);
            } catch (Exception e) {
                log.error("Failed refund for booking {}: {}", entry.getKey(), e.getMessage(), e);
            }
        }
    }

    public int autoConfirmPendingPayments() {
        int before = paymentRepository.findByStatus(Payment.PaymentStatus.CHO_XAC_NHAN).size();
        self.processPendingPaymentsDirect();
        return Math.max(0, before - paymentRepository.findByStatus(Payment.PaymentStatus.CHO_XAC_NHAN).size());
    }

    public int autoApproveRefunds() {
        int before = paymentRepository.findByStatus(Payment.PaymentStatus.YEU_CAU_HOAN_TIEN).size();
        self.processRefundRequestsDirect();
        return Math.max(0, before - paymentRepository.findByStatus(Payment.PaymentStatus.YEU_CAU_HOAN_TIEN).size());
    }

    /**
     * Confirm a single payment: PENDING → CONFIRMED, booking → da_xac_nhan.
     * Uses REQUIRES_NEW so each payment confirmation is an independent transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void confirmPayment(Long paymentId) {
        log.info("Confirming payment {}", paymentId);
        Payment payment = paymentRepository.findByIdWithLock(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (payment.getStatus() != Payment.PaymentStatus.CHO_XAC_NHAN) {
            log.info("Payment {} status is {} (not PENDING), skipping", paymentId, payment.getStatus());
            return;
        }

        DatCho booking = payment.getBooking();
        if (booking == null) {
            throw new RuntimeException("Payment " + paymentId + " has no associated booking");
        }

        Long bookingId = booking.getMaDatCho();

        // Find all PENDING payments for the same booking
        List<Payment> allPendingPayments = paymentRepository.findByBooking_MaDatCho(bookingId).stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.CHO_XAC_NHAN)
                .toList();

        log.info("Found {} pending payment(s) for booking {} - confirming all", allPendingPayments.size(), bookingId);

        // Process wallet deductions
        com.project.airbnb.entity.NguoiDung user = booking.getKhach();
        BigDecimal totalWalletAmount = BigDecimal.ZERO;
        for (Payment p : allPendingPayments) {
            if (p.getProvider() == Payment.PaymentProvider.WALLET) {
                String tid = p.getTransactionId();
                if (tid == null || !tid.startsWith("WALLET_PAID_")) {
                    totalWalletAmount = totalWalletAmount.add(p.getAmount());
                }
            }
        }

        if (totalWalletAmount.compareTo(BigDecimal.ZERO) > 0 && user != null) {
            BigDecimal currentBalance = user.getSoDu() != null ? user.getSoDu() : BigDecimal.ZERO;
            if (currentBalance.compareTo(totalWalletAmount) < 0) {
                throw new RuntimeException("Insufficient wallet balance to confirm payment");
            }
            user.setSoDu(currentBalance.subtract(totalWalletAmount));
            nguoiDungRepository.save(user);
            log.info("Deducted {} from user {} wallet. New balance: {}",
                    totalWalletAmount, user.getMaNguoiDung(), user.getSoDu());
        }

        // Confirm all pending payments
        for (Payment p : allPendingPayments) {
            p.setStatus(Payment.PaymentStatus.DA_XAC_NHAN);
            p.setPaymentDate(LocalDateTime.now());
            paymentRepository.save(p);
            log.info("Payment {} confirmed", p.getId());
        }

        // Update booking status
        if (booking.getTrangThaiDatCho() == DatCho.TrangThaiDatCho.cho_xac_nhan) {
            boolean availabilityOk = checkAndUpdateAvailability(booking);
            if (!availabilityOk) {
                log.error("Dates unavailable for booking {}", bookingId);
                throw new RuntimeException("Dates are no longer available");
            }
            booking.setTrangThaiDatCho(DatCho.TrangThaiDatCho.da_xac_nhan);
        }
        BigDecimal totalPaid = allPendingPayments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.DA_XAC_NHAN)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            booking.setTongTien(totalPaid);
        }
        datChoRepository.save(booking);

        // Cong phi dich vu vao vi admin (dung query truc tiep)
        BigDecimal totalServiceFee = allPendingPayments.stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.DA_XAC_NHAN)
                .map(p -> p.getServiceFee() != null ? p.getServiceFee() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalServiceFee.compareTo(BigDecimal.ZERO) > 0) {
            int rows = nguoiDungRepository.creditAdminWallet(totalServiceFee);
            log.info("Credited service fee {} to admin wallet. Rows updated: {}", totalServiceFee, rows);
        }

        // Cong tien vao vi chu nha (tong tien - phi dich vu)
        try {
            if (booking.getPhong() != null && booking.getPhong().getChuNha() != null) {
                Long hostId = booking.getPhong().getChuNha().getMaNguoiDung();
                BigDecimal hostEarning = totalPaid.subtract(totalServiceFee);
                if (hostEarning.compareTo(BigDecimal.ZERO) > 0) {
                    int rows = nguoiDungRepository.creditHostWallet(hostId, hostEarning);
                    log.info("Credited host earning {} to host {}. Rows updated: {}", hostEarning, hostId, rows);
                }
            }
        } catch (Exception ex) {
            log.error("Failed to credit host wallet: {}", ex.getMessage(), ex);
        }

        // Send confirmation email
        if (user != null && user.getEmail() != null) {
            try {
                emailService.sendBookingConfirmedEmail(
                        user.getEmail(),
                        user.getHoTen() != null ? user.getHoTen() : "Khách",
                        booking);
            } catch (Exception e) {
                log.error("Failed to send confirmation email for booking {}: {}", bookingId, e.getMessage());
            }
        }
    }

    /**
     * Approve refund: REFUND_REQUESTED → REFUNDED + credit wallet balance.
     * Uses REQUIRES_NEW for independent transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void approveRefundToBalance(Long paymentId) {
        log.info("Approving refund to balance for payment {}", paymentId);
        Payment payment = paymentRepository.findByIdWithLock(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        if (payment.getStatus() != Payment.PaymentStatus.YEU_CAU_HOAN_TIEN) {
            log.warn("Payment {} status is {} (not REFUND_REQUESTED), skipping", paymentId, payment.getStatus());
            return;
        }

        DatCho booking = payment.getBooking();
        if (booking == null) {
            throw new RuntimeException("Payment " + paymentId + " has no associated booking");
        }

        // Use refund amount from booking or fallback to full payment amount
        BigDecimal refundAmount = booking.getTienHoanLai();
        if (refundAmount == null) {
            refundAmount = payment.getAmount();
        }

        com.project.airbnb.entity.NguoiDung user = booking.getKhach();
        if (user == null) {
            throw new RuntimeException("Booking has no associated user");
        }

        // Credit user's wallet (re-fetch to avoid stale entity)
        Long userId = user.getMaNguoiDung();
        com.project.airbnb.entity.NguoiDung freshUser = nguoiDungRepository.findById(userId).orElse(user);
        BigDecimal currentBalance = freshUser.getSoDu() != null ? freshUser.getSoDu() : BigDecimal.ZERO;
        freshUser.setSoDu(currentBalance.add(refundAmount));
        nguoiDungRepository.saveAndFlush(freshUser);

        // Tru tien tu vi chu nha
        try {
            if (booking.getPhong() != null && booking.getPhong().getChuNha() != null) {
                Long hostId = booking.getPhong().getChuNha().getMaNguoiDung();
                if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                    int rows = nguoiDungRepository.debitHostWallet(hostId, refundAmount);
                    log.info("Debited {} from host {} wallet (admin-approved refund). Rows updated: {}", refundAmount, hostId, rows);
                }
            }
        } catch (Exception ex) {
            log.error("Failed to debit host wallet during admin-approved refund: {}", ex.getMessage(), ex);
        }

        // Update payment status
        payment.setStatus(Payment.PaymentStatus.HOAN_TIEN);
        paymentRepository.save(payment);

        // Create refund payment record (negative amount)
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            Payment refundPayment = new Payment();
            refundPayment.setBooking(booking);
            refundPayment.setAmount(refundAmount.negate());
            refundPayment.setCurrency("VND");
            refundPayment.setProvider(Payment.PaymentProvider.WALLET);
            refundPayment.setTransactionId("REFUND_" + booking.getMaDatCho() + "_" + System.currentTimeMillis());
            refundPayment.setStatus(Payment.PaymentStatus.HOAN_TIEN);
            refundPayment.setPaymentDate(LocalDateTime.now());
            paymentRepository.save(refundPayment);
        }

        log.info("Approved refund of {} to user {}. New balance: {}",
                refundAmount, user.getMaNguoiDung(), freshUser.getSoDu());

        // Clear tienHoanLai to prevent double-credit
        booking.setTienHoanLai(BigDecimal.ZERO);
        datChoRepository.save(booking);

        // Cancel booking if not already cancelled
        if (booking.getTrangThaiDatCho() != DatCho.TrangThaiDatCho.da_huy) {
            booking.setTrangThaiDatCho(DatCho.TrangThaiDatCho.da_huy);
            datChoRepository.save(booking);
            releaseDates(booking);
            log.info("Booking {} cancelled as part of refund approval", booking.getMaDatCho());
        }
    }

    /**
     * Check and update availability dates for a lodging booking.
     */
    public boolean checkAndUpdateAvailability(DatCho booking) {
        Phong phong = booking.getPhong();
        if (phong == null || phong.getLoaiPhong() != Phong.LoaiPhong.noi_luu_tru) {
            return true;
        }

        LocalDate date = booking.getNgayNhanPhong();
        if (date == null || booking.getNgayTraPhong() == null) {
            return true;
        }

        // First pass: check availability
        LocalDate checkDate = date;
        while (checkDate.isBefore(booking.getNgayTraPhong())) {
            var opt = lichTrinhRepository.findByPhong_MaPhongAndNgay(phong.getMaPhong(), checkDate);
            if (opt.isPresent() && Boolean.FALSE.equals(opt.get().getConKhaDung())) {
                return false;
            }
            checkDate = checkDate.plusDays(1);
        }

        // Second pass: update availability
        LocalDate updateDate = date;
        while (updateDate.isBefore(booking.getNgayTraPhong())) {
            var opt = lichTrinhRepository.findByPhong_MaPhongAndNgay(phong.getMaPhong(), updateDate);
            LichTrinh rec;
            if (opt.isPresent()) {
                rec = opt.get();
            } else {
                rec = new LichTrinh();
                rec.setPhong(phong);
                rec.setNgay(updateDate);
                rec.setConKhaDung(false);
                rec = lichTrinhRepository.save(rec);
            }
            rec.setConKhaDung(false);
            lichTrinhRepository.save(rec);
            updateDate = updateDate.plusDays(1);
        }
        return true;
    }

    /**
     * Release dates when a booking is cancelled.
     */
    public void releaseDates(DatCho booking) {
        Phong phong = booking.getPhong();
        if (phong == null || phong.getLoaiPhong() != Phong.LoaiPhong.noi_luu_tru) {
            return;
        }
        if (booking.getNgayNhanPhong() == null || booking.getNgayTraPhong() == null) {
            return;
        }

        LocalDate date = booking.getNgayNhanPhong();
        while (date.isBefore(booking.getNgayTraPhong())) {
            var opt = lichTrinhRepository.findByPhong_MaPhongAndNgay(phong.getMaPhong(), date);
            if (opt.isPresent()) {
                LichTrinh rec = opt.get();
                rec.setConKhaDung(true);
                lichTrinhRepository.save(rec);
            }
            date = date.plusDays(1);
        }
    }

    @Transactional
    public String createVnPayUrl(Long bookingId, String ipAddress, BigDecimal customAmount) throws Exception {
        DatCho booking = datChoRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        BigDecimal paymentAmount = (customAmount != null && customAmount.compareTo(BigDecimal.ZERO) > 0)
                ? customAmount : booking.getTongTien();

        if (paymentAmount.longValue() < 1000) {
            throw new RuntimeException("Số tiền tối thiểu cho VNPay là 1.000 VND");
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(paymentAmount);
        payment.setCurrency("VND");
        payment.setProvider(Payment.PaymentProvider.VNPAY);
        payment.setStatus(Payment.PaymentStatus.CHUA_THANH_TOAN);
        payment.setServiceFee(booking.getPhiDichVu() != null ? booking.getPhiDichVu() : paymentAmount.multiply(serviceFeeRate()).setScale(0, java.math.RoundingMode.HALF_UP));
        paymentRepository.save(payment);

        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = String.valueOf(payment.getId());

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnpTmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(paymentAmount.longValue() * 100));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang:" + vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnpReturnUrl);
        vnp_Params.put("vnp_IpAddr", ipAddress);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));
        cld.add(Calendar.MINUTE, 15);
        vnp_Params.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII)).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) { query.append('&'); hashData.append('&'); }
            }
        }
        String vnp_SecureHash = hmacSHA512(vnpHashSecret, hashData.toString());
        String fullUrl = vnpPayUrl + "?" + query + "&vnp_SecureHash=" + vnp_SecureHash;
        log.info("VNPay URL created for booking {}: amount={}, createDate={}, expireDate={}",
                bookingId, paymentAmount, vnp_Params.get("vnp_CreateDate"), vnp_Params.get("vnp_ExpireDate"));
        return fullUrl;
    }

    @Transactional
    public boolean handleVnPayReturn(Map<String, String> requestParams) throws Exception {
        Map<String, String> params = new HashMap<>(requestParams);
        String vnp_SecureHash = params.get("vnp_SecureHash");
        params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (fieldNames.indexOf(fieldName) < fieldNames.size() - 1) hashData.append('&');
            }
        }

        String signValue = hmacSHA512(vnpHashSecret, hashData.toString());
        if (signValue.equals(vnp_SecureHash)) {
            String vnp_TxnRef = params.get("vnp_TxnRef");
            String vnp_ResponseCode = params.get("vnp_ResponseCode");
            Long paymentId = Long.parseLong(vnp_TxnRef);
            Payment payment = paymentRepository.findByIdWithLock(paymentId).orElseThrow();

            if (payment.getStatus() == Payment.PaymentStatus.DA_XAC_NHAN || payment.getStatus() == Payment.PaymentStatus.CHO_XAC_NHAN) {
                return true;
            }

            if ("00".equals(vnp_ResponseCode)) {
                payment.setStatus(Payment.PaymentStatus.CHO_XAC_NHAN);
                payment.setTransactionId(params.get("vnp_TransactionNo"));
                payment.setPaymentDate(LocalDateTime.now());
                paymentRepository.save(payment);
                return true;
            } else {
                payment.setStatus("24".equals(vnp_ResponseCode) ? Payment.PaymentStatus.DA_HUY : Payment.PaymentStatus.THAT_BAI);
                payment.setPaymentDate(LocalDateTime.now());
                paymentRepository.save(payment);
                cancelBookingIfNeeded(payment);
                return false;
            }
        }
        return false;
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public String createZaloPayUrl(Long bookingId, BigDecimal customAmount) throws Exception {
        DatCho booking = datChoRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        BigDecimal paymentAmount = (customAmount != null && customAmount.compareTo(BigDecimal.ZERO) > 0)
                ? customAmount : booking.getTongTien();

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(paymentAmount);
        payment.setCurrency("VND");
        payment.setProvider(Payment.PaymentProvider.ZALOPAY);
        payment.setStatus(Payment.PaymentStatus.CHUA_THANH_TOAN);
        payment.setServiceFee(booking.getPhiDichVu() != null ? booking.getPhiDichVu() : paymentAmount.multiply(serviceFeeRate()).setScale(0, java.math.RoundingMode.HALF_UP));
        paymentRepository.save(payment);

        long currentTimeMillis = System.currentTimeMillis();
        String appTransId = String.format("%s%d_%d", new SimpleDateFormat("yyMMdd").format(new Date()), currentTimeMillis, payment.getId());

        Map<String, Object> order = new HashMap<>();
        order.put("app_id", Integer.parseInt(zaloAppId));
        order.put("app_trans_id", appTransId);
        order.put("app_user", "user_" + booking.getKhach().getMaNguoiDung());
        order.put("app_time", currentTimeMillis);
        order.put("amount", paymentAmount.longValue());
        order.put("item", "[{\"itemid\":\"booking_" + bookingId + "\",\"itemname\":\"Booking\",\"itemprice\":" + paymentAmount.longValue() + ",\"itemquantity\":1}]");
        order.put("embed_data", "{}");
        order.put("callback_url", zaloReturnUrl);
        order.put("description", "Thanh toan dat phong #" + bookingId);
        order.put("bank_code", "");

        String data = zaloAppId + "|" + appTransId + "|" + order.get("app_user") + "|" + order.get("amount") + "|" + currentTimeMillis + "|" + order.get("embed_data") + "|" + order.get("item");
        order.put("mac", hmacSHA256(zaloKey1, data));

        payment.setTransactionId(appTransId);
        paymentRepository.save(payment);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        org.springframework.http.HttpEntity<Map<String, Object>> request = new org.springframework.http.HttpEntity<>(order, headers);
        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
        Map<String, Object> response = restTemplate.postForObject(zaloCreateUrl, request, Map.class);

        if (response != null && Integer.valueOf(1).equals(response.get("return_code"))) {
            return (String) response.get("order_url");
        }
        throw new RuntimeException("ZaloPay order creation failed");
    }

    @Transactional
    public String createMoMoUrl(Long bookingId, BigDecimal customAmount) throws Exception {
        DatCho booking = datChoRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        BigDecimal paymentAmount = (customAmount != null && customAmount.compareTo(BigDecimal.ZERO) > 0)
                ? customAmount : booking.getTongTien();

        // MoMo sandbox limit: 1,000 – 50,000,000 VND
        long amountLong = paymentAmount.longValue();
        if (amountLong < 1000) {
            throw new RuntimeException("Số tiền tối thiểu cho MoMo là 1.000 VND");
        }
        if (amountLong > 50_000_000) {
            throw new RuntimeException("Số tiền tối đa cho MoMo là 50.000.000 VND. Vui lòng sử dụng ví hoặc VNPay/ZaloPay.");
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(paymentAmount);
        payment.setCurrency("VND");
        payment.setProvider(Payment.PaymentProvider.MOMO);
        payment.setStatus(Payment.PaymentStatus.CHUA_THANH_TOAN);
        payment.setServiceFee(booking.getPhiDichVu() != null ? booking.getPhiDichVu() : paymentAmount.multiply(serviceFeeRate()).setScale(0, java.math.RoundingMode.HALF_UP));
        paymentRepository.save(payment);

        String requestId = String.valueOf(System.currentTimeMillis());
        String orderId = requestId + "_" + payment.getId();
        String orderInfo = "Thanh toan dat phong #" + bookingId;

        String rawHash = "accessKey=" + momoAccessKey + "&amount=" + paymentAmount.longValue() + "&extraData=&ipnUrl=" + momoReturnUrl + "&orderId=" + orderId + "&orderInfo=" + orderInfo + "&partnerCode=" + momoPartnerCode + "&redirectUrl=" + momoReturnUrl + "&requestId=" + requestId + "&requestType=captureWallet";
        String signature = hmacSHA256(momoSecretKey, rawHash);

        Map<String, Object> momoRequest = new HashMap<>();
        momoRequest.put("partnerCode", momoPartnerCode);
        momoRequest.put("accessKey", momoAccessKey);
        momoRequest.put("requestId", requestId);
        momoRequest.put("amount", paymentAmount.longValue());
        momoRequest.put("orderId", orderId);
        momoRequest.put("orderInfo", orderInfo);
        momoRequest.put("redirectUrl", momoReturnUrl);
        momoRequest.put("ipnUrl", momoReturnUrl);
        momoRequest.put("requestType", "captureWallet");
        momoRequest.put("extraData", "");
        momoRequest.put("signature", signature);
        momoRequest.put("lang", "vi");

        payment.setTransactionId(orderId);
        paymentRepository.save(payment);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        org.springframework.http.HttpEntity<Map<String, Object>> request = new org.springframework.http.HttpEntity<>(momoRequest, headers);
        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
        @SuppressWarnings("rawtypes")
        Map response = restTemplate.postForObject(momoCreateUrl, request, Map.class);

        if (response != null && response.get("resultCode").equals(0)) {
            return (String) response.get("payUrl");
        }
        throw new RuntimeException("MoMo order creation failed");
    }

    @Transactional
    public Payment.PaymentStatus handleMoMoReturn(Map<String, String> params) {
        String orderId = params.get("orderId");
        String resultCode = params.getOrDefault("resultCode", "9999");
        if (orderId == null) throw new RuntimeException("Missing orderId");

        Long paymentId;
        try {
            paymentId = orderId.contains("_") ? Long.parseLong(orderId.substring(orderId.lastIndexOf('_') + 1)) : Long.parseLong(orderId);
        } catch (NumberFormatException e) { throw new RuntimeException("Invalid orderId format"); }

        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> new RuntimeException("Payment not found"));

        Payment.PaymentStatus newStatus;
        if ("0".equals(resultCode)) {
            newStatus = Payment.PaymentStatus.CHO_XAC_NHAN;
            payment.setTransactionId(params.getOrDefault("transId", orderId));
            payment.setPaymentDate(LocalDateTime.now());
        } else if ("1000".equals(resultCode) || "1006".equals(resultCode)) {
            newStatus = Payment.PaymentStatus.DA_HUY;
            payment.setPaymentDate(LocalDateTime.now());
        } else {
            newStatus = Payment.PaymentStatus.THAT_BAI;
            payment.setPaymentDate(LocalDateTime.now());
        }
        payment.setStatus(newStatus);
        paymentRepository.save(payment);
        if (newStatus != Payment.PaymentStatus.CHO_XAC_NHAN) {
            cancelBookingIfNeeded(payment);
        }
        return newStatus;
    }

    @Transactional
    public Payment.PaymentStatus handleZaloPayReturn(Map<String, String> params) {
        String appTransId = Optional.ofNullable(params.get("apptransid")).orElse(params.get("app_trans_id"));
        if (appTransId == null || appTransId.isBlank()) throw new RuntimeException("Missing apptransid");

        Long paymentId;
        try { paymentId = Long.parseLong(appTransId.substring(appTransId.lastIndexOf('_') + 1)); }
        catch (Exception ex) { throw new RuntimeException("Invalid apptransid"); }

        Payment payment = paymentRepository.findByIdWithLock(paymentId).orElseThrow(() -> new RuntimeException("Payment not found"));
        if (payment.getStatus() == Payment.PaymentStatus.DA_XAC_NHAN || payment.getStatus() == Payment.PaymentStatus.CHO_XAC_NHAN) {
            return payment.getStatus();
        }

        String statusParam = Optional.ofNullable(params.get("status")).orElse(Optional.ofNullable(params.get("return_code")).orElse(params.getOrDefault("returncode", "")));
        Payment.PaymentStatus newStatus;
        if ("1".equals(statusParam) || "SUCCESS".equalsIgnoreCase(statusParam)) {
            newStatus = Payment.PaymentStatus.CHO_XAC_NHAN;
            payment.setPaymentDate(LocalDateTime.now());
        } else if ("2".equals(statusParam) || "CANCEL".equalsIgnoreCase(statusParam) || "USER_CANCEL".equalsIgnoreCase(statusParam)) {
            newStatus = Payment.PaymentStatus.DA_HUY;
            payment.setPaymentDate(LocalDateTime.now());
        } else {
            newStatus = Payment.PaymentStatus.THAT_BAI;
            payment.setPaymentDate(LocalDateTime.now());
        }
        payment.setStatus(newStatus);
        if (params.containsKey("zp_trans_id")) payment.setTransactionId(params.get("zp_trans_id"));
        paymentRepository.save(payment);
        if (newStatus != Payment.PaymentStatus.CHO_XAC_NHAN) {
            cancelBookingIfNeeded(payment);
        }
        return newStatus;
    }

    private void cancelBookingIfNeeded(Payment payment) {
        if (payment == null || payment.getBooking() == null) return;
        if (payment.getStatus() == Payment.PaymentStatus.DA_HUY || payment.getStatus() == Payment.PaymentStatus.THAT_BAI
                || payment.getStatus() == Payment.PaymentStatus.HOAN_TIEN || payment.getStatus() == Payment.PaymentStatus.YEU_CAU_HOAN_TIEN) {
            DatCho booking = payment.getBooking();
            if (booking.getTrangThaiDatCho() != DatCho.TrangThaiDatCho.da_huy) {
                booking.setTrangThaiDatCho(DatCho.TrangThaiDatCho.da_huy);
                datChoRepository.save(booking);
                releaseDates(booking);
                List<Payment> otherPayments = paymentRepository.findByBooking_MaDatCho(booking.getMaDatCho());
                for (Payment p : otherPayments) {
                    if (p.getStatus() == Payment.PaymentStatus.CHO_XAC_NHAN || p.getStatus() == Payment.PaymentStatus.DA_XAC_NHAN) {
                        if (p.getProvider() == Payment.PaymentProvider.WALLET) {
                            com.project.airbnb.entity.NguoiDung user = booking.getKhach();
                            if (user != null) {
                                BigDecimal balance = user.getSoDu() != null ? user.getSoDu() : BigDecimal.ZERO;
                                user.setSoDu(balance.add(p.getAmount()));
                                nguoiDungRepository.save(user);
                                log.info("Wallet refunded {} VND for cancelled booking {} (wallet payment)", p.getAmount(), booking.getMaDatCho());
                            }
                        }
                        p.setStatus(Payment.PaymentStatus.DA_HUY);
                        p.setPaymentDate(LocalDateTime.now());
                        paymentRepository.save(p);
                    }
                }
            }
        }
    }

    @Transactional
    public void refundPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        if (payment.getStatus() != Payment.PaymentStatus.YEU_CAU_HOAN_TIEN
                && payment.getStatus() != Payment.PaymentStatus.DA_XAC_NHAN
                && payment.getStatus() != Payment.PaymentStatus.CHO_XAC_NHAN) {
            throw new RuntimeException("Invalid status for refund");
        }
        payment.setStatus(Payment.PaymentStatus.HOAN_TIEN);
        paymentRepository.save(payment);
        cancelBookingIfNeeded(payment);
    }

    @Transactional
    public BigDecimal requestRefundToBalance(Long bookingId, String reason) {
        DatCho booking = datChoRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        if (booking.getTrangThaiDatCho() != DatCho.TrangThaiDatCho.da_xac_nhan) {
            throw new RuntimeException("Only confirmed bookings can request refund");
        }
        BigDecimal refundAmount = calculateRefundAmount(booking);
        booking.setTienHoanLai(refundAmount);
        if (reason != null && !reason.isBlank()) booking.setLyDoHuy(reason);
        datChoRepository.save(booking);

        List<Payment> payments = paymentRepository.findByBooking_MaDatCho(bookingId);
        for (Payment p : payments) {
            if (p.getStatus() == Payment.PaymentStatus.DA_XAC_NHAN || p.getStatus() == Payment.PaymentStatus.CHO_XAC_NHAN) {
                p.setStatus(Payment.PaymentStatus.YEU_CAU_HOAN_TIEN);
                paymentRepository.save(p);
            }
        }
        // Flush so the refund query below sees the just-updated payment statuses
        paymentRepository.flush();

        // Refund immediately — do NOT wait for the 5s scheduler.
        // processRefundRequestsDirect sets the booking to da_huy AND calls
        // releaseDates() to free the calendar, so we must NOT set da_huy here.
        self.processRefundRequestsDirect();
        return refundAmount;
    }

    @Transactional
    public void payWithBalance(Long bookingId, BigDecimal amountFromBalance) {
        DatCho booking = datChoRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        com.project.airbnb.entity.NguoiDung user = booking.getKhach();
        BigDecimal currentBalance = user.getSoDu() != null ? user.getSoDu() : BigDecimal.ZERO;
        if (currentBalance.compareTo(amountFromBalance) < 0) throw new RuntimeException("Insufficient balance");

        user.setSoDu(currentBalance.subtract(amountFromBalance));
        nguoiDungRepository.save(user);

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(amountFromBalance);
        payment.setCurrency("VND");
        payment.setProvider(Payment.PaymentProvider.WALLET);
        payment.setTransactionId("WALLET_PAID_" + System.currentTimeMillis());
        payment.setPaymentDate(LocalDateTime.now());
        payment.setStatus(Payment.PaymentStatus.CHO_XAC_NHAN);
        payment.setServiceFee(booking.getPhiDichVu() != null ? booking.getPhiDichVu() : amountFromBalance.multiply(serviceFeeRate()).setScale(0, java.math.RoundingMode.HALF_UP));
        paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public List<com.project.airbnb.dto.PaymentDTO> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(com.project.airbnb.dto.PaymentDTO::fromEntity)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Calculate refund amount following realistic Airbnb-style policy:
     * - Service fee (phiDichVu) is NEVER refunded — platform keeps it
     * - Cleaning fee (phiVeSinh) IS refunded if cancelled before check-in
     * - Night cost refund depends on cancellation policy and timing
     */
    public BigDecimal calculateRefundAmount(DatCho booking) {
        if (booking == null || booking.getPhong() == null) return BigDecimal.ZERO;
        Phong phong = booking.getPhong();

        BigDecimal totalPaid = paymentRepository.findByBooking_MaDatCho(booking.getMaDatCho()).stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.DA_XAC_NHAN || p.getStatus() == Payment.PaymentStatus.CHO_XAC_NHAN)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalPaid.compareTo(BigDecimal.ZERO) <= 0) {
            totalPaid = booking.getTongTien() != null ? booking.getTongTien() : BigDecimal.ZERO;
        }
        if (totalPaid.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        // Service fee is NEVER refunded
        BigDecimal serviceFee = booking.getPhiDichVu() != null ? booking.getPhiDichVu() : BigDecimal.ZERO;
        BigDecimal cleaningFee = booking.getPhiVeSinh() != null ? booking.getPhiVeSinh() : BigDecimal.ZERO;

        // Night cost = totalPaid - serviceFee - cleaningFee
        BigDecimal nightCost = totalPaid.subtract(serviceFee).subtract(cleaningFee);
        if (nightCost.compareTo(BigDecimal.ZERO) <= 0) return cleaningFee;

        Phong.ChinhSachHuy policy = phong.getChinhSachHuy() != null ? phong.getChinhSachHuy() : Phong.ChinhSachHuy.LINH_HOAT;
        LocalDate checkInDate = (phong.getLoaiPhong() == Phong.LoaiPhong.noi_luu_tru) ? booking.getNgayNhanPhong() : booking.getNgayDat();
        if (checkInDate == null) return totalPaid;

        long daysUntilCheckIn = ChronoUnit.DAYS.between(LocalDate.now(), checkInDate);

        BigDecimal nightRefundPercentage = BigDecimal.ZERO;
        switch (policy) {
            case LINH_HOAT:
                // Flexible: full night refund if cancelled before check-in, no refund after
                if (daysUntilCheckIn >= 0) nightRefundPercentage = BigDecimal.ONE;
                break;
            case TRUNG_BINH:
                // Moderate: 50% night refund if cancelled before check-in (anytime)
                if (daysUntilCheckIn >= 0) nightRefundPercentage = new BigDecimal("0.5");
                break;
            case NGHIEM_NGAT:
                // Strict: no night refund at all, only cleaning fee back
                nightRefundPercentage = BigDecimal.ZERO;
                break;
        }

        BigDecimal nightRefund = nightCost.multiply(nightRefundPercentage).setScale(0, java.math.RoundingMode.HALF_UP);

        // Total refund = night refund + cleaning fee (service fee always retained)
        return nightRefund.add(cleaningFee);
    }

    private String hmacSHA512(String key, String data) {
        try {
            final javax.crypto.Mac hmac512 = javax.crypto.Mac.getInstance("HmacSHA512");
            hmac512.init(new javax.crypto.spec.SecretKeySpec(key.getBytes(), "HmacSHA512"));
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception ex) { return ""; }
    }

    private String hmacSHA256(String key, String data) {
        try {
            javax.crypto.Mac hmac256 = javax.crypto.Mac.getInstance("HmacSHA256");
            hmac256.init(new javax.crypto.spec.SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] result = hmac256.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception ex) { return ""; }
    }
}
