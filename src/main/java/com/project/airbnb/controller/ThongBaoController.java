package com.project.airbnb.controller;

import com.project.airbnb.dto.ThongBaoDTO;
import com.project.airbnb.entity.ThongBao;
import com.project.airbnb.service.ThongBaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/thong-bao")
public class ThongBaoController {

    @Autowired
    private ThongBaoService thongBaoService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ThongBaoDTO>> getNotifications(@PathVariable Long userId) {
        List<ThongBao> notifications = thongBaoService.getNotifications(userId);
        List<ThongBaoDTO> dtos = notifications.stream()
                .map(ThongBaoDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<Long> getUnreadCount(@PathVariable Long userId) {
        return ResponseEntity.ok(thongBaoService.countUnread(userId));
    }

    @GetMapping("/user/{userId}/counts")
    public ResponseEntity<Map<String, Long>> getInboxCounts(@PathVariable Long userId) {
        return ResponseEntity.ok(thongBaoService.getInboxCounts(userId));
    }

    @PostMapping("/request-host")
    public ResponseEntity<?> requestHost(@RequestBody Map<String, Object> payload) {
        try {
            Long requesterId = Long.valueOf(payload.get("userId").toString());
            String lyDo = payload.containsKey("reason") ? (String) payload.get("reason") : null;
            ThongBao thongBao = thongBaoService.createHostRequest(requesterId, lyDo);
            return ResponseEntity.ok(Map.of(
                    "message", "Yêu cầu trở thành host đã được gửi đến admin.",
                    "notification", ThongBaoDTO.fromEntity(thongBao)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/request-chat")
    public ResponseEntity<ThongBaoDTO> requestChat(@RequestBody Map<String, Long> payload) {
        Long senderId = payload.get("senderId");
        Long bookingId = payload.get("bookingId");
        ThongBao thongBao = thongBaoService.createChatRequest(senderId, bookingId);
        return ResponseEntity.ok(ThongBaoDTO.fromEntity(thongBao));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<ThongBaoDTO> acceptRequest(@PathVariable Long id) {
        ThongBao thongBao = thongBaoService.acceptRequest(id);
        return ResponseEntity.ok(ThongBaoDTO.fromEntity(thongBao));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ThongBaoDTO> rejectRequest(@PathVariable Long id) {
        ThongBao thongBao = thongBaoService.rejectRequest(id);
        return ResponseEntity.ok(ThongBaoDTO.fromEntity(thongBao));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        thongBaoService.markAsRead(id);
        return ResponseEntity.ok().build();
    }
}
