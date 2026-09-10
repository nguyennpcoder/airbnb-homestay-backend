package com.project.airbnb.service;

import com.project.airbnb.dto.ThongBaoDTO;
import com.project.airbnb.entity.Phong;
import com.project.airbnb.entity.ThongBao;
import com.project.airbnb.repository.PhongRepository;
import com.project.airbnb.repository.ThongBaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AutoApproveService {

    private static final Logger log = LoggerFactory.getLogger(AutoApproveService.class);

    private static final long APPROVE_DELAY_SECONDS = 3;

    private final PhongRepository phongRepository;
    private final ThongBaoRepository thongBaoRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public AutoApproveService(PhongRepository phongRepository,
                              ThongBaoRepository thongBaoRepository,
                              SimpMessagingTemplate messagingTemplate) {
        this.phongRepository = phongRepository;
        this.thongBaoRepository = thongBaoRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void autoApprovePendingListings() {
        List<Phong> pending = phongRepository.findPendingOlderThan(LocalDateTime.now().minusSeconds(APPROVE_DELAY_SECONDS));
        if (pending.isEmpty()) {
            return;
        }
        pending.forEach(phong -> {
            phong.setTrangThai(Phong.TrangThaiPhong.hoat_dong);
            phong.setDaDuyet(true);
            phong.setLyDoTuChoi(null);
            notifyHost(phong);
        });
        phongRepository.saveAll(pending);
        log.info("Tự động duyệt {} phòng sau {} giây", pending.size(), APPROVE_DELAY_SECONDS);
    }

    private void notifyHost(Phong phong) {
        try {
            if (phong.getChuNha() == null) {
                return;
            }
            ThongBao thongBao = new ThongBao();
            thongBao.setNguoiGui(phong.getChuNha());
            thongBao.setNguoiNhan(phong.getChuNha());
            thongBao.setLoaiThongBao("LISTING_APPROVED");
            thongBao.setNoiDung("Phòng \"" + phong.getTieuDe() + "\" đã được tự động duyệt và đang hoạt động.");
            thongBao.setTrangThai("ACCEPTED");
            ThongBao saved = thongBaoRepository.save(thongBao);
            thongBaoRepository.flush();
            messagingTemplate.convertAndSend("/queue/notifications/" + phong.getChuNha().getMaNguoiDung(),
                    ThongBaoDTO.fromEntity(thongBaoRepository.findById(saved.getId()).orElse(saved)));
        } catch (Exception e) {
            log.error("Không thể gửi thông báo duyệt phòng {}: {}", phong.getMaPhong(), e.getMessage());
        }
    }
}