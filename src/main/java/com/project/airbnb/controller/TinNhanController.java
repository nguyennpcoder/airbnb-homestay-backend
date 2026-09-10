package com.project.airbnb.controller;

import com.project.airbnb.entity.DatCho;
import com.project.airbnb.entity.NguoiDung;
import com.project.airbnb.entity.Phong;
import com.project.airbnb.entity.TinNhan;
import com.project.airbnb.entity.TraLoiNhanh;
import com.project.airbnb.repository.DatChoRepository;
import com.project.airbnb.repository.NguoiDungRepository;
import com.project.airbnb.repository.PhongRepository;
import com.project.airbnb.repository.TinNhanRepository;
import com.project.airbnb.service.TraLoiNhanhService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tin-nhan")
public class TinNhanController {

    private final TinNhanRepository tinNhanRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final DatChoRepository datChoRepository;
    private final PhongRepository phongRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final TraLoiNhanhService traLoiNhanhService;

    @Value("${app.backend.url:http://localhost:8089}")
    private String backendUrl;

    public TinNhanController(TinNhanRepository tinNhanRepository,
            NguoiDungRepository nguoiDungRepository,
            DatChoRepository datChoRepository,
            PhongRepository phongRepository,
            SimpMessagingTemplate messagingTemplate,
            TraLoiNhanhService traLoiNhanhService) {
        this.tinNhanRepository = tinNhanRepository;
        this.nguoiDungRepository = nguoiDungRepository;
        this.datChoRepository = datChoRepository;
        this.phongRepository = phongRepository;
        this.messagingTemplate = messagingTemplate;
        this.traLoiNhanhService = traLoiNhanhService;
    }

    @GetMapping("/dat-cho/{maDatCho}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> listByBooking(@PathVariable Long maDatCho) {
        List<TinNhan> messages = tinNhanRepository.findByDatCho_MaDatChoOrderByNgayTaoAsc(maDatCho);
        List<Map<String, Object>> result = messages.stream().map(this::toMap).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/unread-count")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestParam Long userId,
            @RequestParam(required = false) String role) {
        Long count;
        if ("guest".equals(role)) {
            count = tinNhanRepository.countGuestUnreadByUser(userId);
        } else if ("host".equals(role)) {
            count = tinNhanRepository.countHostUnreadByUser(userId);
        } else {
            count = tinNhanRepository.countAllUnreadByUser(userId);
        }
        return ResponseEntity.ok(Map.of("unreadMessages", count != null ? count : 0L));
    }

    /**
     * Danh sách hội thoại liên hệ (chưa đặt chỗ) của người dùng.
     * Tin nhắn được lưu trong bảng tin_nhan (ma_phong), KHÔNG tạo bản ghi dat_cho.
     * Mỗi hội thoại = 1 phòng + 1 người đối thoại.
     */
    @GetMapping("/hoi-thoai")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> listInquiries(@RequestParam Long userId) {
        List<TinNhan> messages = tinNhanRepository.findInquiriesByUser(userId);

        // Nhóm theo (maPhong + partnerId) giữ thứ tự thời gian để lấy tin cuối làm summary
        Map<String, List<TinNhan>> groups = new java.util.LinkedHashMap<>();
        for (TinNhan t : messages) {
            Long partnerId = t.getNguoiGui().getMaNguoiDung().equals(userId)
                    ? t.getNguoiNhan().getMaNguoiDung()
                    : t.getNguoiGui().getMaNguoiDung();
            String key = t.getPhong().getMaPhong() + ":" + partnerId;
            groups.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(t);
        }

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (List<TinNhan> threadMessages : groups.values()) {
            TinNhan last = threadMessages.get(threadMessages.size() - 1);
            Phong phong = last.getPhong();
            NguoiDung partner = last.getNguoiGui().getMaNguoiDung().equals(userId)
                    ? last.getNguoiNhan()
                    : last.getNguoiGui();

            Map<String, Object> thread = new HashMap<>();
            thread.put("maPhong", phong.getMaPhong());
            thread.put("tieuDe", phong.getTieuDe());
            thread.put("urlAnhChinh", phong.getUrlAnhChinh());
            thread.put("chuNhaId", phong.getChuNha() != null ? phong.getChuNha().getMaNguoiDung() : null);
            thread.put("partner", toUserMap(partner));
            thread.put("lastMessage", last.getNoiDung());
            thread.put("lastMessageTime", last.getNgayTao());
            thread.put("hasUnread", tinNhanRepository.countUnreadByPhong(phong.getMaPhong(), userId) > 0);
            thread.put("isInquiry", true);
            result.add(thread);
        }

        result.sort((a, b) -> {
            java.time.LocalDateTime t1 = (java.time.LocalDateTime) a.get("lastMessageTime");
            java.time.LocalDateTime t2 = (java.time.LocalDateTime) b.get("lastMessageTime");
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t2.compareTo(t1);
        });
        return ResponseEntity.ok(result);
    }

    /** Tin nhắn của một hội thoại liên hệ (theo phòng + cặp người dùng). */
    @GetMapping("/phong/{maPhong}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> listByPhong(
            @PathVariable Long maPhong,
            @RequestParam Long userId,
            @RequestParam Long partnerId) {
        List<TinNhan> messages = tinNhanRepository.findInquiryThreadMessages(maPhong, userId, partnerId);
        return ResponseEntity.ok(messages.stream().map(this::toMap).toList());
    }

    @PostMapping("/phong/{maPhong}/read-all")
    @Transactional
    public ResponseEntity<?> markAllReadByPhong(@PathVariable Long maPhong, @RequestParam Long userId) {
        List<Long> unreadIds = tinNhanRepository.findUnreadIdsByPhong(maPhong, userId);
        List<Long> senders = tinNhanRepository.findUnreadSendersByPhong(maPhong, userId);
        tinNhanRepository.markAllAsReadByPhong(maPhong, userId);
        for (Long sender : senders) {
            pushReadReceipt(sender, unreadIds);
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> send(@RequestBody Map<String, Object> payload) {
        try {
            Long maNguoiGui = Long.parseLong(payload.get("maNguoiGui").toString());
            Long maNguoiNhan = Long.parseLong(payload.get("maNguoiNhan").toString());
            Long maDatCho = payload.get("maDatCho") != null ? Long.parseLong(payload.get("maDatCho").toString()) : null;
            Long maPhong = payload.get("maPhong") != null ? Long.parseLong(payload.get("maPhong").toString()) : null;
            String noiDung = (String) payload.get("noiDung");

            NguoiDung gui = nguoiDungRepository.findById(maNguoiGui)
                    .orElseThrow(() -> new RuntimeException("Sender not found"));
            NguoiDung nhan = nguoiDungRepository.findById(maNguoiNhan)
                    .orElseThrow(() -> new RuntimeException("Receiver not found"));

            TinNhan tinNhan = new TinNhan();
            tinNhan.setNguoiGui(gui);
            tinNhan.setNguoiNhan(nhan);
            tinNhan.setNoiDung(noiDung);

            // Liên hệ trước đặt chỗ: chỉ lưu tin_nhan gắn với phòng, KHÔNG tạo dat_cho
            if (maDatCho == null && maPhong != null) {
                Phong phong = phongRepository.findById(maPhong)
                        .orElseThrow(() -> new RuntimeException("Room not found"));
                tinNhan.setPhong(phong);
            } else if (maDatCho != null) {
                DatCho datCho = datChoRepository.findById(maDatCho).orElse(null);
                tinNhan.setDatCho(datCho);
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Cần maDatCho hoặc maPhong để gửi tin nhắn"));
            }

            if (payload.containsKey("fileUrl")) tinNhan.setFileUrl((String) payload.get("fileUrl"));
            if (payload.containsKey("fileType")) tinNhan.setFileType((String) payload.get("fileType"));

            TinNhan saved = tinNhanRepository.save(tinNhan);
            Map<String, Object> messageMap = toMap(saved);

            // Push message to receiver via WebSocket
            messagingTemplate.convertAndSend("/queue/messages/" + maNguoiNhan, messageMap);

            Map<String, Object> autoReplyMap = null;

            // Auto-reply: nếu người nhận là chủ nhà có câu trả lời nhanh và chưa từng trả lời thủ công
            if (Boolean.TRUE.equals(nhan.getLaChuNha())) {
                TinNhan autoMsg = new TinNhan();
                boolean hasManualReply;
                if (maDatCho != null) {
                    hasManualReply = tinNhanRepository.countHostManualReplies(maNguoiNhan, maNguoiGui, maDatCho) > 0;
                    autoMsg.setDatCho(datChoRepository.findById(maDatCho).orElse(null));
                } else if (maPhong != null) {
                    hasManualReply = tinNhanRepository.countHostManualRepliesByPhong(maNguoiNhan, maNguoiGui, maPhong) > 0;
                    autoMsg.setPhong(phongRepository.findById(maPhong).orElse(null));
                } else {
                    hasManualReply = true;
                }

                if (!hasManualReply) {
                    TraLoiNhanh autoReply = traLoiNhanhService.getRandomReply(maNguoiNhan);
                    if (autoReply != null) {
                        autoMsg.setNguoiGui(nhan);
                        autoMsg.setNguoiNhan(gui);
                        autoMsg.setNoiDung(autoReply.getNoiDung());
                        autoMsg.setAutoReply(true);

                        TinNhan savedAutoMsg = tinNhanRepository.save(autoMsg);
                        autoReplyMap = toMap(savedAutoMsg);
                        // Realtime to guest (same path as booking chat replies)
                        messagingTemplate.convertAndSend("/queue/messages/" + maNguoiGui, autoReplyMap);
                    }
                }
            }

            // Return both messages so frontend can add them in order immediately
            if (autoReplyMap != null) {
                return ResponseEntity.ok(List.of(messageMap, autoReplyMap));
            }
            return ResponseEntity.ok(messageMap);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{maTinNhan}/read")
    @Transactional
    public ResponseEntity<?> markRead(@PathVariable Long maTinNhan) {
        Long senderId = tinNhanRepository.findSenderOf(maTinNhan);
        tinNhanRepository.markAsRead(maTinNhan);
        pushReadReceipt(senderId, List.of(maTinNhan));
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ==================== Hội thoại chủ nhà ↔ admin (lưu trong tin_nhan, phong/datCho = null) ====================

    /** Thông tin tài khoản admin để frontend hiển thị + gửi tin. */
    @GetMapping("/admin-info")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAdminInfo() {
        NguoiDung admin = nguoiDungRepository.findFirstByLaAdminTrue()
                .orElse(null);
        if (admin == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy tài khoản admin"));
        }
        return ResponseEntity.ok(toUserMap(admin));
    }

    /** Danh sách hội thoại admin của người dùng (admin xem các chủ nhà, chủ nhà xem admin). */
    @GetMapping("/admin/hoi-thoai")
    @Transactional(readOnly = true)
    public ResponseEntity<?> listAdminThreads(@RequestParam Long userId) {
        List<TinNhan> messages = tinNhanRepository.findAdminThreadsByUser(userId);

        Map<Long, List<TinNhan>> groups = new java.util.LinkedHashMap<>();
        for (TinNhan t : messages) {
            Long partnerId = t.getNguoiGui().getMaNguoiDung().equals(userId)
                    ? t.getNguoiNhan().getMaNguoiDung()
                    : t.getNguoiGui().getMaNguoiDung();
            groups.computeIfAbsent(partnerId, k -> new java.util.ArrayList<>()).add(t);
        }

        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (List<TinNhan> threadMessages : groups.values()) {
            TinNhan last = threadMessages.get(threadMessages.size() - 1);
            NguoiDung partner = last.getNguoiGui().getMaNguoiDung().equals(userId)
                    ? last.getNguoiNhan()
                    : last.getNguoiGui();

            Map<String, Object> thread = new HashMap<>();
            thread.put("partner", toUserMap(partner));
            thread.put("isAdmin", Boolean.TRUE.equals(partner.getLaAdmin()));
            thread.put("lastMessage", last.getNoiDung());
            thread.put("lastMessageTime", last.getNgayTao());
            thread.put("hasUnread", tinNhanRepository.countUnreadAdminThread(userId, partner.getMaNguoiDung()) > 0);
            thread.put("isAdminThread", true);
            result.add(thread);
        }

        result.sort((a, b) -> {
            java.time.LocalDateTime t1 = (java.time.LocalDateTime) a.get("lastMessageTime");
            java.time.LocalDateTime t2 = (java.time.LocalDateTime) b.get("lastMessageTime");
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t2.compareTo(t1);
        });
        return ResponseEntity.ok(result);
    }

    /** Tin nhắn của một hội thoại admin (giữa userId và partnerId). */
    @GetMapping("/admin/thread/{partnerId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> listAdminThreadMessages(
            @PathVariable Long partnerId,
            @RequestParam Long userId) {
        List<TinNhan> messages = tinNhanRepository.findAdminThreadMessages(userId, partnerId);
        return ResponseEntity.ok(messages.stream().map(this::toMap).toList());
    }

    /** Gửi tin nhắn trong hội thoại admin (1 trong 2 bên phải là admin). */
    @PostMapping("/admin/send")
    @Transactional
    public ResponseEntity<?> sendAdminThread(@RequestBody Map<String, Object> payload) {
        try {
            Long maNguoiGui = Long.parseLong(payload.get("maNguoiGui").toString());
            Long maNguoiNhan = Long.parseLong(payload.get("maNguoiNhan").toString());
            String noiDung = (String) payload.get("noiDung");

            if (maNguoiGui.equals(maNguoiNhan)) {
                return ResponseEntity.badRequest().body(Map.of("message", "Không thể tự nhắn cho chính mình"));
            }

            NguoiDung gui = nguoiDungRepository.findById(maNguoiGui)
                    .orElseThrow(() -> new RuntimeException("Sender not found"));
            NguoiDung nhan = nguoiDungRepository.findById(maNguoiNhan)
                    .orElseThrow(() -> new RuntimeException("Receiver not found"));

            boolean guiAdmin = Boolean.TRUE.equals(gui.getLaAdmin());
            boolean nhanAdmin = Boolean.TRUE.equals(nhan.getLaAdmin());
            if (!guiAdmin && !nhanAdmin) {
                return ResponseEntity.badRequest().body(Map.of("message", "Hội thoại admin chỉ dành cho chủ nhà và admin"));
            }

            TinNhan tinNhan = new TinNhan();
            tinNhan.setNguoiGui(gui);
            tinNhan.setNguoiNhan(nhan);
            tinNhan.setNoiDung(noiDung);

            if (payload.containsKey("fileUrl")) tinNhan.setFileUrl((String) payload.get("fileUrl"));
            if (payload.containsKey("fileType")) tinNhan.setFileType((String) payload.get("fileType"));

            TinNhan saved = tinNhanRepository.save(tinNhan);
            Map<String, Object> messageMap = toMap(saved);

            // Realtime tới người nhận
            messagingTemplate.convertAndSend("/queue/messages/" + maNguoiNhan, messageMap);

            return ResponseEntity.ok(messageMap);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Lỗi không xác định"));
        }
    }

    /** Đánh dấu đã đọc toàn bộ tin trong hội thoại admin. */
    @PostMapping("/admin/read-all")
    @Transactional
    public ResponseEntity<?> markAllReadAdminThread(
            @RequestParam Long userId,
            @RequestParam Long partnerId) {
        List<Long> unreadIds = tinNhanRepository.findUnreadIdsAdminThread(userId, partnerId);
        tinNhanRepository.markAllAsReadAdminThread(userId, partnerId);
        pushReadReceipt(partnerId, unreadIds);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/read-all")
    @Transactional
    public ResponseEntity<?> markAllRead(
            @RequestParam Long bookingId,
            @RequestParam Long userId) {
        List<Long> unreadIds = tinNhanRepository.findUnreadIdsByDatCho(bookingId, userId);
        List<Long> senders = tinNhanRepository.findUnreadSendersByDatCho(bookingId, userId);
        tinNhanRepository.markAllAsRead(bookingId, userId);
        for (Long sender : senders) {
            pushReadReceipt(sender, unreadIds);
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/upload")
    @Transactional
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "No file"));
            }
            java.nio.file.Path dir = java.nio.file.Paths.get("uploads", "messages");
            java.nio.file.Files.createDirectories(dir);

            String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
            String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
            String filename = "msg-" + System.currentTimeMillis() + ext.toLowerCase();
            java.nio.file.Path dest = dir.resolve(filename);

            try (java.io.InputStream in = file.getInputStream()) {
                java.nio.file.Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }

            String url = backendUrl + "/uploads/messages/" + filename;
            String contentType = file.getContentType();

            return ResponseEntity.ok(Map.of(
                    "url", url,
                    "fileType", contentType != null ? contentType : "application/octet-stream"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Upload failed", "error", e.getMessage()));
        }
    }

    /** Push thông báo "đã đọc" qua WS cho người gửi → frontend chuyển 1 tích thành 2 tích realtime. */
    private void pushReadReceipt(Long partnerId, List<Long> messageIds) {
        if (partnerId == null || messageIds == null || messageIds.isEmpty()) return;
        Map<String, Object> event = new HashMap<>();
        event.put("isReadReceipt", true);
        event.put("messageIds", messageIds);
        messagingTemplate.convertAndSend("/queue/messages/" + partnerId, event);
    }

    private Map<String, Object> toMap(TinNhan t) {
        Map<String, Object> map = new HashMap<>();
        map.put("maTinNhan", t.getMaTinNhan());
        map.put("noiDung", t.getNoiDung());
        map.put("daDoc", t.getDaDoc());
        map.put("ngayTao", t.getNgayTao());
        map.put("fileUrl", t.getFileUrl());
        map.put("fileType", t.getFileType());
        map.put("reactions", t.getReactions());
        map.put("isAutoReply", t.getAutoReply());
        if (t.getNguoiGui() != null) {
            map.put("nguoiGui", toUserMap(t.getNguoiGui()));
        }
        if (t.getNguoiNhan() != null) {
            Map<String, Object> nhan = new HashMap<>();
            nhan.put("maNguoiDung", t.getNguoiNhan().getMaNguoiDung());
            nhan.put("hoTen", t.getNguoiNhan().getHoTen());
            map.put("nguoiNhan", nhan);
        }
        if (t.getDatCho() != null) {
            DatCho dc = t.getDatCho();
            map.put("maDatCho", dc.getMaDatCho());
            map.put("yeuCauDacBiet", dc.getYeuCauDacBiet());
            map.put("isInquiry", "LIEN_HE_PHONG".equals(dc.getYeuCauDacBiet()));
            if (dc.getPhong() != null) {
                map.put("maPhong", dc.getPhong().getMaPhong());
                map.put("tieuDePhong", dc.getPhong().getTieuDe());
                map.put("urlAnhChinh", dc.getPhong().getUrlAnhChinh());
            }
        }
        if (t.getDatCho() == null && t.getPhong() != null) {
            Phong p = t.getPhong();
            map.put("maPhong", p.getMaPhong());
            map.put("tieuDePhong", p.getTieuDe());
            map.put("urlAnhChinh", p.getUrlAnhChinh());
            map.put("yeuCauDacBiet", "LIEN_HE_PHONG");
            map.put("isInquiry", true);
        }
        if (t.getDatCho() == null && t.getPhong() == null) {
            map.put("isAdminThread", true);
        }
        return map;
    }

    private Map<String, Object> toUserMap(NguoiDung u) {
        Map<String, Object> map = new HashMap<>();
        map.put("maNguoiDung", u.getMaNguoiDung());
        map.put("hoTen", u.getHoTen());
        map.put("urlAnhDaiDien", u.getUrlAnhDaiDien());
        return map;
    }
}
