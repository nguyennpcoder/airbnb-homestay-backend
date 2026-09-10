package com.project.airbnb.service;

import com.project.airbnb.entity.DatCho;
import com.project.airbnb.entity.NguoiDung;
import com.project.airbnb.entity.Phong;
import com.project.airbnb.entity.ThongBao;
import com.project.airbnb.repository.DatChoRepository;
import com.project.airbnb.repository.NguoiDungRepository;
import com.project.airbnb.repository.PhongRepository;
import com.project.airbnb.repository.ThongBaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ThongBaoService {

    @Autowired
    private ThongBaoRepository thongBaoRepository;

    @Autowired
    private NguoiDungRepository nguoiDungRepository;

    @Autowired
    private DatChoRepository datChoRepository;

    @Autowired(required = false)
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PhongRepository phongRepository;

    private static final Logger log = LoggerFactory.getLogger(ThongBaoService.class);

    public List<ThongBao> getNotifications(Long userId) {
        return thongBaoRepository.findByNguoiNhan_MaNguoiDungOrderByNgayTaoDesc(userId);
    }

    public long countUnread(Long userId) {
        return thongBaoRepository.countByNguoiNhan_MaNguoiDungAndDaDocFalse(userId);
    }

    public Map<String, Long> getInboxCounts(Long userId) {
        long pendingRequests = thongBaoRepository.countByNguoiNhan_MaNguoiDungAndLoaiThongBaoAndTrangThai(
                userId, "CHAT_REQUEST", "PENDING");
        long hostRequests = thongBaoRepository.countByNguoiNhan_MaNguoiDungAndLoaiThongBaoAndTrangThai(
                userId, "HOST_REQUEST", "PENDING");
        long unreadMessages = thongBaoRepository.countByNguoiNhan_MaNguoiDungAndDaDocFalse(userId);

        return Map.of(
                "pendingRequests", pendingRequests,
                "hostRequests", hostRequests,
                "unreadMessages", unreadMessages,
                "total", pendingRequests + unreadMessages);
    }

    /**
     * Find admin user (laAdmin = true) for sending admin notifications.
     */
    private NguoiDung findAdminUser() {
        return nguoiDungRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getLaAdmin()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No admin user found"));
    }

    /**
     * Khi user được cấp lại quyền chủ nhà: khôi phục các phòng đang ngừng hoạt động
     * (do admin chuyển chủ nhà về khách) trở về hoạt động, trừ phòng bị khóa bởi admin.
     */
    private void restoreHostListings(NguoiDung host) {
        try {
            List<Phong> hostListings = phongRepository.findByChuNha_MaNguoiDung(host.getMaNguoiDung());
            for (Phong listing : hostListings) {
                if (listing.getTrangThai() == Phong.TrangThaiPhong.khong_hoat_dong
                        && !Boolean.TRUE.equals(listing.getBiKhoa())) {
                    listing.setTrangThai(Phong.TrangThaiPhong.hoat_dong);
                }
            }
            phongRepository.saveAll(hostListings);
            if (!hostListings.isEmpty()) {
                log.info("Khôi phục {} phòng về hoạt động cho chủ nhà {}", hostListings.size(), host.getMaNguoiDung());
            }
        } catch (Exception e) {
            log.error("Khôi phục listing thất bại cho user {}: {}", host.getMaNguoiDung(), e.getMessage());
        }
    }

    /**
     * Create a HOST_REQUEST notification to admin when a guest wants to become a host.
     * Auto-approve if all 3 conditions met: email verified, community commitment, KYC.
     * Otherwise reject and notify guest.
     */
    @Transactional
    public ThongBao createHostRequest(Long requesterId, String lyDo) {
        NguoiDung requester = nguoiDungRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (Boolean.TRUE.equals(requester.getLaChuNha())) {
            throw new RuntimeException("Bạn đã là chủ nhà rồi");
        }

        boolean exists = thongBaoRepository.existsByNguoiGui_MaNguoiDungAndLoaiThongBaoAndTrangThai(
                requesterId, "HOST_REQUEST", "PENDING");
        if (exists) {
            throw new RuntimeException("Yêu cầu trở thành host đã được gửi trước đó, vui lòng chờ admin xét duyệt");
        }

        // Check 3 verification conditions
        boolean emailVerified = Boolean.TRUE.equals(requester.getEmailDaXacNhan());
        boolean communityAccepted = Boolean.TRUE.equals(requester.getDaChapNhanCamKetCongDong());
        boolean kycVerified = Boolean.TRUE.equals(requester.getXacMinhDanhTinh());

        NguoiDung admin = findAdminUser();

        // If all 3 conditions met → auto-approve as host
        if (emailVerified && communityAccepted && kycVerified) {
            requester.setLaChuNha(true);
            nguoiDungRepository.save(requester);
            restoreHostListings(requester);

            // Create ACCEPTED notification to admin for record
            String noiDung = (lyDo != null && !lyDo.isBlank())
                    ? requester.getHoTen() + " đã được tự động duyệt làm chủ nhà (đủ 3 điều kiện). Lý do: " + lyDo
                    : requester.getHoTen() + " đã được tự động duyệt làm chủ nhà (đủ 3 điều kiện)";
            ThongBao thongBao = new ThongBao();
            thongBao.setNguoiGui(requester);
            thongBao.setNguoiNhan(admin);
            thongBao.setLoaiThongBao("HOST_REQUEST");
            thongBao.setNoiDung(noiDung);
            thongBao.setTrangThai("ACCEPTED");
            thongBao.setDaDoc(false);
            ThongBao saved = thongBaoRepository.save(thongBao);
            thongBaoRepository.flush();

            // Send notification to admin
            if (messagingTemplate != null) {
                try {
                    com.project.airbnb.dto.ThongBaoDTO dto = com.project.airbnb.dto.ThongBaoDTO.fromEntity(thongBaoRepository.findById(saved.getId()).orElse(saved));
                    messagingTemplate.convertAndSend("/queue/notifications/" + admin.getMaNguoiDung(), dto);
                } catch (Exception e) {
                    log.error("Failed to send WebSocket notification to admin: {}", e.getMessage());
                }
            }

            // Notify guest: approved
            ThongBao notifToGuest = new ThongBao();
            notifToGuest.setNguoiGui(admin);
            notifToGuest.setNguoiNhan(requester);
            notifToGuest.setLoaiThongBao("HOST_APPROVED");
            notifToGuest.setNoiDung("Chúc mừng! Yêu cầu trở thành chủ nhà của bạn đã được chấp nhận tự động vì bạn đã đáp ứng đủ 3 điều kiện: xác minh email, cam kết cộng đồng, và xác minh danh tính.");
            notifToGuest.setTrangThai("ACCEPTED");
            thongBaoRepository.save(notifToGuest);
            thongBaoRepository.flush();

            // Send email
            try {
                emailService.sendHostApprovedEmail(requester.getEmail(), requester.getHoTen());
            } catch (Exception e) {
                log.error("Failed to send host approved email: {}", e.getMessage());
            }

            if (messagingTemplate != null) {
                try {
                    com.project.airbnb.dto.ThongBaoDTO guestDto = com.project.airbnb.dto.ThongBaoDTO.fromEntity(notifToGuest);
                    messagingTemplate.convertAndSend("/queue/notifications/" + requester.getMaNguoiDung(), guestDto);
                } catch (Exception e) {
                    log.error("Failed to send WebSocket approval to guest: {}", e.getMessage());
                }
            }

            return thongBaoRepository.findById(saved.getId()).orElse(saved);
        }

        // If conditions not met → reject and notify guest with reason
        StringBuilder missingReasons = new StringBuilder();
        if (!emailVerified) missingReasons.append("chưa xác minh email");
        if (!communityAccepted) missingReasons.append("chưa cam kết cộng đồng");
        if (!kycVerified) missingReasons.append("chưa xác minh danh tính (KYC)");
        String missing = missingReasons.toString();

        // Create REJECTED notification to admin
        String noiDungAdmin = requester.getHoTen() + " gửi yêu cầu làm chủ nhà nhưng bị từ chối tự động vì: " + missing;
        ThongBao thongBao = new ThongBao();
        thongBao.setNguoiGui(requester);
        thongBao.setNguoiNhan(admin);
        thongBao.setLoaiThongBao("HOST_REQUEST");
        thongBao.setNoiDung(noiDungAdmin);
        thongBao.setTrangThai("REJECTED");
        ThongBao saved = thongBaoRepository.save(thongBao);
        thongBaoRepository.flush();

        // Notify guest: rejected with specific reasons
        ThongBao notifToGuest = new ThongBao();
        notifToGuest.setNguoiGui(admin);
        notifToGuest.setNguoiNhan(requester);
        notifToGuest.setLoaiThongBao("HOST_REJECTED");
        notifToGuest.setNoiDung("Yêu cầu trở thành chủ nhà của bạn chưa được chấp nhận. Lý do: " + missing + ". Vui lòng hoàn thành các điều kiện trên rồi gửi lại yêu cầu.");
        notifToGuest.setTrangThai("REJECTED");
        thongBaoRepository.save(notifToGuest);
        thongBaoRepository.flush();

        // Send rejection email
        try {
            emailService.sendHostRejectedEmail(requester.getEmail(), requester.getHoTen());
        } catch (Exception e) {
            log.error("Failed to send host rejected email: {}", e.getMessage());
        }

        if (messagingTemplate != null) {
            try {
                com.project.airbnb.dto.ThongBaoDTO guestDto = com.project.airbnb.dto.ThongBaoDTO.fromEntity(notifToGuest);
                messagingTemplate.convertAndSend("/queue/notifications/" + requester.getMaNguoiDung(), guestDto);
            } catch (Exception e) {
                log.error("Failed to send WebSocket rejection to guest: {}", e.getMessage());
            }
        }

        throw new RuntimeException("Yêu cầu chưa được chấp nhận. Bạn cần: " + missing);
    }

    @Transactional
    public ThongBao createChatRequest(Long senderId, Long bookingId) {
        NguoiDung sender = nguoiDungRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        DatCho booking = datChoRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (thongBaoRepository.existsByDatCho_MaDatChoAndNguoiGui_MaNguoiDungAndLoaiThongBao(bookingId,
                senderId, "CHAT_REQUEST")) {
            throw new RuntimeException("Yêu cầu kết nối đã được gửi trước đó");
        }

        NguoiDung receiver = booking.getPhong().getChuNha();
        String bookingTitle = booking.getPhong().getTieuDe();

        ThongBao thongBao = new ThongBao();
        thongBao.setNguoiGui(sender);
        thongBao.setNguoiNhan(receiver);
        thongBao.setDatCho(booking);
        thongBao.setLoaiThongBao("CHAT_REQUEST");
        thongBao.setNoiDung("Khách " + sender.getHoTen() + " muốn kết nối trò chuyện với bạn về chuyến đi tại "
                + bookingTitle);
        thongBao.setTrangThai("PENDING");

        ThongBao saved = thongBaoRepository.save(thongBao);
        thongBaoRepository.flush();

        ThongBao finalThongBao = thongBaoRepository.findById(saved.getId()).orElse(saved);

        if (messagingTemplate != null) {
            try {
                com.project.airbnb.dto.ThongBaoDTO dto = com.project.airbnb.dto.ThongBaoDTO.fromEntity(finalThongBao);
                messagingTemplate.convertAndSend("/queue/notifications/" + receiver.getMaNguoiDung(), dto);
            } catch (Exception e) {
                System.err.println("Failed to send WebSocket notification: " + e.getMessage());
            }
        }

        return finalThongBao;
    }

    @Transactional
    public ThongBao acceptRequest(Long notificationId) {
        ThongBao thongBao = thongBaoRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // If this is a HOST_REQUEST, automatically grant host role to the requester
        if ("HOST_REQUEST".equals(thongBao.getLoaiThongBao()) && thongBao.getNguoiGui() != null) {
            NguoiDung requester = thongBao.getNguoiGui();
            requester.setLaChuNha(true);
            nguoiDungRepository.save(requester);
            restoreHostListings(requester);

            // Send email notification to the guest
            try {
                emailService.sendHostApprovedEmail(requester.getEmail(), requester.getHoTen());
                log.info("Host approved email sent to {} for user {}", requester.getEmail(), requester.getMaNguoiDung());
            } catch (Exception e) {
                log.error("Failed to send host approved email to {}: {}", requester.getEmail(), e.getMessage());
            }
        }

        thongBao.setTrangThai("ACCEPTED");
        thongBao.setDaDoc(true);
        thongBaoRepository.save(thongBao);
        thongBaoRepository.flush();

        // Send WebSocket notification to the guest so their profile updates in real-time
        if ("HOST_REQUEST".equals(thongBao.getLoaiThongBao()) && thongBao.getNguoiGui() != null) {
            NguoiDung requester = thongBao.getNguoiGui();
            if (messagingTemplate != null) {
                try {
                    ThongBao notifToGuest = new ThongBao();
                    notifToGuest.setNguoiGui(thongBao.getNguoiNhan()); // admin
                    notifToGuest.setNguoiNhan(requester);
                    notifToGuest.setLoaiThongBao("HOST_APPROVED");
                    notifToGuest.setNoiDung("Chúc mừng! Yêu cầu trở thành chủ nhà của bạn đã được chấp nhận.");
                    notifToGuest.setTrangThai("ACCEPTED");
                    thongBaoRepository.save(notifToGuest);
                    thongBaoRepository.flush();

                    com.project.airbnb.dto.ThongBaoDTO dto = com.project.airbnb.dto.ThongBaoDTO.fromEntity(notifToGuest);
                    messagingTemplate.convertAndSend("/queue/notifications/" + requester.getMaNguoiDung(), dto);
                } catch (Exception e) {
                    log.error("Failed to send WebSocket approval notification to guest {}: {}", requester.getMaNguoiDung(), e.getMessage());
                }
            }
        }

        return thongBaoRepository.findById(notificationId).orElse(thongBao);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        ThongBao thongBao = thongBaoRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        thongBao.setDaDoc(true);
        thongBaoRepository.save(thongBao);
    }

    @Transactional
    public ThongBao rejectRequest(Long notificationId) {
        ThongBao thongBao = thongBaoRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // If this is a HOST_REQUEST, send rejection email to the guest
        if ("HOST_REQUEST".equals(thongBao.getLoaiThongBao()) && thongBao.getNguoiGui() != null) {
            NguoiDung requester = thongBao.getNguoiGui();

            // Send email notification to the guest
            try {
                emailService.sendHostRejectedEmail(requester.getEmail(), requester.getHoTen());
                log.info("Host rejected email sent to {} for user {}", requester.getEmail(), requester.getMaNguoiDung());
            } catch (Exception e) {
                log.error("Failed to send host rejected email to {}: {}", requester.getEmail(), e.getMessage());
            }

            // Send WebSocket notification to the guest so their profile updates in real-time
            if (messagingTemplate != null) {
                try {
                    ThongBao notifToGuest = new ThongBao();
                    notifToGuest.setNguoiGui(thongBao.getNguoiNhan()); // admin
                    notifToGuest.setNguoiNhan(requester);
                    notifToGuest.setLoaiThongBao("HOST_REJECTED");
                    notifToGuest.setNoiDung("Yêu cầu trở thành chủ nhà của bạn đã bị từ chối.");
                    notifToGuest.setTrangThai("REJECTED");
                    thongBaoRepository.save(notifToGuest);
                    thongBaoRepository.flush();

                    com.project.airbnb.dto.ThongBaoDTO dto = com.project.airbnb.dto.ThongBaoDTO.fromEntity(notifToGuest);
                    messagingTemplate.convertAndSend("/queue/notifications/" + requester.getMaNguoiDung(), dto);
                } catch (Exception e) {
                    log.error("Failed to send WebSocket rejection notification to guest {}: {}", requester.getMaNguoiDung(), e.getMessage());
                }
            }
        }

        thongBao.setTrangThai("REJECTED");
        thongBao.setDaDoc(true);
        thongBaoRepository.save(thongBao);
        thongBaoRepository.flush();

        return thongBaoRepository.findById(notificationId).orElse(thongBao);
    }
}
