package com.project.airbnb.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "thanh_toan")
@Data

@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ma_dat_cho", nullable = false)
    private DatCho booking;

    @Column(name = "so_tien", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "don_vi_tien_te", nullable = false, length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "nha_cung_cap", nullable = false, length = 20)
    private PaymentProvider provider;

    @Column(name = "ma_giao_dich")
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "ngay_thanh_toan")
    private LocalDateTime paymentDate = LocalDateTime.now();

    @Column(name = "phi_dich_vu", precision = 15, scale = 2)
    private BigDecimal serviceFee = BigDecimal.ZERO;

    @PrePersist
    protected void onCreate() {
        paymentDate = LocalDateTime.now();
    }

    public enum PaymentProvider {
        STRIPE, VNPAY, PAYPAL, ZALOPAY, MOMO, WALLET, SEPAY
    }

    public enum PaymentStatus {
        CHUA_THANH_TOAN, CHO_XAC_NHAN, DA_XAC_NHAN, THAT_BAI, DA_HUY, HOAN_TIEN, YEU_CAU_HOAN_TIEN
    }
}
