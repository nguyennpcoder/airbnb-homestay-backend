package com.project.airbnb.controller;

import com.project.airbnb.entity.DatCho;
import com.project.airbnb.entity.Payment;
import com.project.airbnb.repository.DatChoRepository;
import com.project.airbnb.repository.PaymentRepository;
import com.project.airbnb.repository.NguoiDungRepository;
import com.project.airbnb.service.PaymentService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/payment/sepay")
public class SePayController {
    private static final Logger log = LoggerFactory.getLogger(SePayController.class);

    private final PaymentRepository paymentRepository;
    private final DatChoRepository datChoRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final PaymentService paymentService;

    @Value("${sepay.api.key:}")
    private String sepayApiKey;

    @Value("${sepay.account.number:}")
    private String sepayAccountNumber;

    @Value("${sepay.bank.code:Vietcombank}")
    private String sepayBankCode;

    @Value("${sepay.account.holder:}")
    private String sepayAccountHolder;

    public SePayController(PaymentRepository paymentRepository,
                           DatChoRepository datChoRepository,
                           NguoiDungRepository nguoiDungRepository,
                           PaymentService paymentService) {
        this.paymentRepository = paymentRepository;
        this.datChoRepository = datChoRepository;
        this.nguoiDungRepository = nguoiDungRepository;
        this.paymentService = paymentService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createSePayPayment(@RequestBody Map<String, Object> body) {
        try {
            Long bookingId = Long.valueOf(body.get("bookingId").toString());
            BigDecimal amount = new BigDecimal(body.get("amount").toString());

            DatCho booking = datChoRepository.findById(bookingId)
                    .orElseThrow(() -> new RuntimeException("Booking not found"));

            String code = "AIRBNB" + bookingId + "_" + System.currentTimeMillis();
            Payment payment = new Payment();
            payment.setBooking(booking);
            payment.setAmount(amount);
            payment.setCurrency("VND");
            payment.setProvider(Payment.PaymentProvider.SEPAY);
            payment.setTransactionId(code);
            payment.setStatus(Payment.PaymentStatus.CHO_XAC_NHAN);
            payment.setPaymentDate(LocalDateTime.now());
            payment.setServiceFee(booking.getPhiDichVu() != null ? booking.getPhiDichVu() : amount.multiply(new java.math.BigDecimal("0.10")).setScale(0, java.math.RoundingMode.HALF_UP));
            paymentRepository.save(payment);

            String qrUrl = "https://vietqr.app/img"
                    + "?acc=" + sepayAccountNumber
                    + "&bank=" + sepayBankCode
                    + "&amount=" + amount.intValue()
                    + "&des=" + code
                    + "&showinfo=true"
                    + "&fullacc=true"
                    + "&holder=" + sepayAccountHolder.replace(" ", "%20");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "paymentId", payment.getId(),
                    "qrUrl", qrUrl,
                    "code", code,
                    "amount", amount,
                    "accountNumber", sepayAccountNumber,
                    "bankCode", sepayBankCode,
                    "accountHolder", sepayAccountHolder
            ));
        } catch (Exception e) {
            log.error("SePay create error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(@RequestBody Map<String, Object> body,
                                     @RequestHeader Map<String, String> headers) {
        log.info("SePay webhook received: {}", body);
        try {
            String apiKey = headers.get("authorization");
            if (sepayApiKey != null && !sepayApiKey.isEmpty()
                    && apiKey != null && !apiKey.contains(sepayApiKey)) {
                log.warn("Invalid SePay API key");
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
            }

            String transferType = (String) body.get("transferType");
            if (!"in".equals(transferType)) {
                return ResponseEntity.ok(Map.of("success", true));
            }

            String code = (String) body.get("code");
            Number transferAmount = (Number) body.get("transferAmount");

            if (code == null || code.isEmpty()) {
                return ResponseEntity.ok(Map.of("success", true, "message", "No code"));
            }

            Payment payment = paymentRepository.findByTransactionId(code);
            if (payment == null) {
                log.warn("No payment found for code: {}", code);
                return ResponseEntity.ok(Map.of("success", true, "message", "Payment not found"));
            }

            if (payment.getStatus() == Payment.PaymentStatus.DA_XAC_NHAN) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Already confirmed"));
            }

            BigDecimal paidAmount = new BigDecimal(transferAmount.toString());
            if (paidAmount.compareTo(payment.getAmount()) < 0) {
                log.warn("Paid {} < required {} for code {}", paidAmount, payment.getAmount(), code);
                return ResponseEntity.ok(Map.of("success", true, "message", "Insufficient amount"));
            }

            payment.setStatus(Payment.PaymentStatus.DA_XAC_NHAN);
            payment.setPaymentDate(LocalDateTime.now());
            paymentRepository.save(payment);

            // Cong phi dich vu vao vi admin
            if (payment.getServiceFee() != null && payment.getServiceFee().compareTo(BigDecimal.ZERO) > 0) {
                nguoiDungRepository.creditAdminWallet(payment.getServiceFee());
                log.info("Credited service fee {} to admin wallet from SePay payment {}", payment.getServiceFee(), payment.getId());
            }

            // Cong tien vao vi chu nha (tong tien - phi dich vu)
            if (payment.getBooking() != null && payment.getBooking().getPhong() != null
                    && payment.getBooking().getPhong().getChuNha() != null) {
                Long hostId = payment.getBooking().getPhong().getChuNha().getMaNguoiDung();
                BigDecimal hostEarning = payment.getAmount().subtract(
                        payment.getServiceFee() != null ? payment.getServiceFee() : BigDecimal.ZERO);
                if (hostEarning.compareTo(BigDecimal.ZERO) > 0) {
                    nguoiDungRepository.creditHostWallet(hostId, hostEarning);
                    log.info("Credited host earning {} to host {} from SePay payment {}", hostEarning, hostId, payment.getId());
                }
            }

            DatCho booking = payment.getBooking();
            if (booking != null) {
                if (booking.getTrangThaiDatCho() == DatCho.TrangThaiDatCho.cho_xac_nhan) {
                    booking.setTrangThaiDatCho(DatCho.TrangThaiDatCho.da_xac_nhan);
                }
                booking.setTongTien(payment.getAmount());
                booking.setSoLuongKhach(booking.getSoLuongKhach() != null ? booking.getSoLuongKhach() : 1);
                datChoRepository.save(booking);
                paymentService.checkAndUpdateAvailability(booking);
            }

            log.info("SePay payment {} confirmed for booking {}", payment.getId(), booking != null ? booking.getMaDatCho() : "?");
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("SePay webhook error: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/status/{paymentId}")
    public ResponseEntity<?> checkStatus(@PathVariable Long paymentId) {
        return paymentRepository.findById(paymentId)
                .map(p -> ResponseEntity.ok(Map.of(
                        "status", p.getStatus().name(),
                        "paymentId", p.getId())))
                .orElse(ResponseEntity.notFound().build());
    }
}
