package com.project.airbnb.controller;

import com.project.airbnb.entity.Payment;
import com.project.airbnb.repository.PaymentRepository;
import com.project.airbnb.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    public PaymentController(PaymentService paymentService, PaymentRepository paymentRepository) {
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/user/{userId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getByUser(@PathVariable Long userId) {
        try {
            List<Payment> payments = paymentRepository.findByUserId(userId);
            List<Map<String, Object>> result = payments.stream().map(p -> {
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("id", p.getId());
                m.put("amount", p.getAmount());
                m.put("currency", p.getCurrency());
                m.put("provider", p.getProvider() != null ? p.getProvider().name() : null);
                m.put("status", p.getStatus() != null ? p.getStatus().name() : null);
                m.put("paymentDate", p.getPaymentDate());
                m.put("transactionId", p.getTransactionId());
                m.put("serviceFee", p.getServiceFee());
                if (p.getBooking() != null) {
                    Map<String, Object> b = new java.util.HashMap<>();
                    b.put("maDatCho", p.getBooking().getMaDatCho());
                    if (p.getBooking().getPhong() != null) {
                        Map<String, Object> ph = new java.util.HashMap<>();
                        ph.put("tieuDe", p.getBooking().getPhong().getTieuDe());
                        b.put("phong", ph);
                    }
                    m.put("booking", b);
                }
                return m;
            }).collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Lỗi"));
        }
    }

    @PostMapping("/vnpay/create-url")
    public ResponseEntity<?> createVnPayUrl(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            Long bookingId = ((Number) payload.get("bookingId")).longValue();
            java.math.BigDecimal amount = payload.containsKey("amount")
                    ? new java.math.BigDecimal(payload.get("amount").toString())
                    : null;
            String vnpayUrl = paymentService.createVnPayUrl(bookingId, getClientIp(request), amount);
            return ResponseEntity.ok(Map.of("url", vnpayUrl));
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Không thể tạo link thanh toán VNPay";
            return ResponseEntity.badRequest()
                    .body(Map.of("message", errorMessage, "error", e.getClass().getSimpleName()));
        }
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<?> handleVnPayReturn(@RequestParam Map<String, String> params) {
        try {
            boolean success = paymentService.handleVnPayReturn(params);
            if (success) {
                return ResponseEntity.ok(Map.of("status", "success", "message", "Payment successful"));
            } else {
                return ResponseEntity.badRequest().body(Map.of("status", "failed", "message", "Payment failed"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("status", "failed", "message", e.getMessage()));
        }
    }

    @PostMapping("/zalopay/create-url")
    public ResponseEntity<?> createZaloPayUrl(@RequestBody Map<String, Object> payload) {
        try {
            Long bookingId = ((Number) payload.get("bookingId")).longValue();
            java.math.BigDecimal amount = payload.containsKey("amount")
                    ? new java.math.BigDecimal(payload.get("amount").toString())
                    : null;
            String zaloPayUrl = paymentService.createZaloPayUrl(bookingId, amount);
            return ResponseEntity.ok(Map.of("url", zaloPayUrl));
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Không thể tạo link thanh toán ZaloPay";
            return ResponseEntity.badRequest()
                    .body(Map.of("message", errorMessage, "error", e.getClass().getSimpleName()));
        }
    }

    @GetMapping("/zalopay/return")
    public ResponseEntity<?> handleZaloPayReturn(@RequestParam Map<String, String> params) {
        try {
            var status = paymentService.handleZaloPayReturn(params);
            return ResponseEntity.ok(Map.of("status", status.name()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/zalopay/return")
    public ResponseEntity<?> handleZaloPayReturnPost(@RequestBody Map<String, Object> payload) {
        try {
            Map<String, String> params = normalizeParams(payload);
            var status = paymentService.handleZaloPayReturn(params);
            return ResponseEntity.ok(Map.of("status", status.name()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/momo/create-url")
    public ResponseEntity<?> createMoMoUrl(@RequestBody Map<String, Object> payload) {
        try {
            Long bookingId = ((Number) payload.get("bookingId")).longValue();
            java.math.BigDecimal amount = payload.containsKey("amount")
                    ? new java.math.BigDecimal(payload.get("amount").toString())
                    : null;
            String momoUrl = paymentService.createMoMoUrl(bookingId, amount);
            return ResponseEntity.ok(Map.of("url", momoUrl));
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "Không thể tạo link thanh toán MoMo";
            return ResponseEntity.badRequest()
                    .body(Map.of("message", errorMessage, "error", e.getClass().getSimpleName()));
        }
    }

    @GetMapping("/momo/return")
    public ResponseEntity<?> handleMoMoReturn(@RequestParam Map<String, String> params) {
        try {
            var status = paymentService.handleMoMoReturn(params);
            return ResponseEntity.ok(Map.of("status", status.name()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/momo/return")
    public ResponseEntity<?> handleMoMoReturnPost(@RequestBody Map<String, Object> payload) {
        try {
            Map<String, String> params = normalizeParams(payload);
            var status = paymentService.handleMoMoReturn(params);
            return ResponseEntity.ok(Map.of("status", status.name()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/pay-with-balance")
    public ResponseEntity<?> payWithBalance(@RequestBody Map<String, Object> payload) {
        try {
            Long bookingId = Long.parseLong(payload.get("bookingId").toString());
            java.math.BigDecimal amount = new java.math.BigDecimal(payload.get("amount").toString());
            paymentService.payWithBalance(bookingId, amount);
            return ResponseEntity.ok(Map.of("message", "Paid with balance successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/request-refund-to-balance/{bookingId}")
    public ResponseEntity<?> requestRefundToBalance(@PathVariable Long bookingId,
            @RequestBody(required = false) Map<String, String> payload) {
        try {
            String reason = payload != null ? payload.get("reason") : null;
            java.math.BigDecimal refundAmount = paymentService.requestRefundToBalance(bookingId, reason);
            return ResponseEntity.ok(Map.of(
                    "message", "Hoàn tiền thành công",
                    "refundAmount", refundAmount));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/admin/{id}/confirm")
    public ResponseEntity<?> confirmPayment(@PathVariable Long id,
            @RequestBody(required = false) Map<String, String> payload) {
        try {
            paymentService.confirmPayment(id);
            return ResponseEntity.ok(Map.of("message", "Payment confirmed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/admin/{id}/refund")
    public ResponseEntity<?> refundPayment(@PathVariable Long id) {
        try {
            paymentService.refundPayment(id);
            return ResponseEntity.ok(Map.of("message", "Payment refunded successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/admin/{paymentId}/approve-refund-to-balance")
    public ResponseEntity<?> approveRefundToBalance(@PathVariable Long paymentId) {
        try {
            paymentService.approveRefundToBalance(paymentId);
            return ResponseEntity.ok(Map.of("message", "Refund to balance approved successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/admin/auto-process")
    public ResponseEntity<?> autoProcess() {
        try {
            int confirmed = paymentService.autoConfirmPendingPayments();
            int refunded = paymentService.autoApproveRefunds();
            return ResponseEntity.ok(Map.of(
                    "confirmed", confirmed,
                    "refunded", refunded,
                    "message", "Auto-processed: " + confirmed + " confirmed, " + refunded + " refunded"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return ipAddress.split(",")[0].trim();
        }
        ipAddress = request.getHeader("X-Real-IP");
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            return ipAddress;
        }
        ipAddress = request.getRemoteAddr();
        // Normalize localhost IPv6 to IPv4
        if ("0:0:0:0:0:0:0:1".equals(ipAddress) || "::1".equals(ipAddress)) {
            return "127.0.0.1";
        }
        return ipAddress != null ? ipAddress : "127.0.0.1";
    }

    private Map<String, String> normalizeParams(Map<String, ?> raw) {
        return raw.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> Objects.toString(entry.getValue(), "")));
    }
}
