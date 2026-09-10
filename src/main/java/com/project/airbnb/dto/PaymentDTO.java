package com.project.airbnb.dto;

import com.project.airbnb.entity.NguoiDung;
import com.project.airbnb.entity.Payment;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentDTO {
    private Long id;
    private Long bookingId;
    private String bookingStatus;
    private BigDecimal amount;
    private String currency;
    private String provider;
    private String transactionId;
    private String status;
    private String refundReason;
    private LocalDateTime paymentDate;
    private BigDecimal serviceFee;

    // Listing info
    private String listingTitle;
    private String listingImageUrl;

    // Host info
    private Long hostId;
    private String hostName;
    private String hostAvatarUrl;
    private String hostEmail;
    private String hostPhone;

    // Guest info
    private Long guestId;
    private String guestName;
    private String guestAvatarUrl;
    private String guestEmail;
    private String guestPhone;
    private Boolean guestXacMinhDanhTinh;
    private Boolean hostXacMinhDanhTinh;

    // Booking guest summary (from dat_cho)
    private Integer soLuongKhach;
    private BigDecimal giaMoiKhach;
    private String ngayNhanPhong;
    private String ngayTraPhong;

    private static String resolveDisplayName(NguoiDung user, String fallback) {
        if (user == null) return fallback;
        String name = user.getHoTen();
        if (name != null && !name.isBlank()) return name.trim();
        String ho = user.getHo() != null ? user.getHo() : "";
        String ten = user.getTen() != null ? user.getTen() : "";
        name = (ho + " " + ten).trim();
        return name.isBlank() ? fallback : name;
    }

    public static PaymentDTO fromEntity(Payment payment) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        if (payment.getBooking() != null) {
            var booking = payment.getBooking();
            dto.setBookingId(booking.getMaDatCho());
            dto.setBookingStatus(booking.getTrangThaiDatCho().name());
            dto.setRefundReason(booking.getLyDoHuy());
            dto.setSoLuongKhach(booking.getSoLuongKhach());
            dto.setGiaMoiKhach(booking.getGiaMoiKhach());
            if (booking.getNgayNhanPhong() != null) {
                dto.setNgayNhanPhong(booking.getNgayNhanPhong().toString());
            }
            if (booking.getNgayTraPhong() != null) {
                dto.setNgayTraPhong(booking.getNgayTraPhong().toString());
            }

            if (booking.getPhong() != null) {
                var phong = booking.getPhong();
                dto.setListingTitle(phong.getTieuDe());
                dto.setListingImageUrl(phong.getUrlAnhChinh());

                if (phong.getChuNha() != null) {
                    NguoiDung host = phong.getChuNha();
                    dto.setHostId(host.getMaNguoiDung());
                    dto.setHostName(resolveDisplayName(host, "Chủ nhà"));
                    dto.setHostAvatarUrl(host.getUrlAnhDaiDien());
                    dto.setHostEmail(host.getEmail());
                    dto.setHostPhone(host.getSoDienThoai());
                    dto.setHostXacMinhDanhTinh(host.getXacMinhDanhTinh());
                }
            }

            if (booking.getKhach() != null) {
                NguoiDung guest = booking.getKhach();
                dto.setGuestId(guest.getMaNguoiDung());
                dto.setGuestName(resolveDisplayName(guest, "Khách"));
                dto.setGuestAvatarUrl(guest.getUrlAnhDaiDien());
                dto.setGuestEmail(guest.getEmail());
                dto.setGuestPhone(guest.getSoDienThoai());
                dto.setGuestXacMinhDanhTinh(guest.getXacMinhDanhTinh());
            }
        }
        dto.setAmount(payment.getAmount());
        dto.setCurrency(payment.getCurrency());
        dto.setProvider(payment.getProvider().name());
        dto.setTransactionId(payment.getTransactionId());
        dto.setStatus(payment.getStatus().name());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setServiceFee(payment.getServiceFee());
        return dto;
    }
}
