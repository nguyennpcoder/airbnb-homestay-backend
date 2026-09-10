package com.project.airbnb.service;

import com.project.airbnb.dto.KhuyenMaiRequest;
import com.project.airbnb.dto.ThongBaoDTO;
import com.project.airbnb.entity.DatCho;
import com.project.airbnb.entity.KhuyenMai;
import com.project.airbnb.entity.ThongBao;
import com.project.airbnb.repository.DatChoRepository;
import com.project.airbnb.repository.KhuyenMaiRepository;
import com.project.airbnb.repository.ThongBaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class KhuyenMaiService {

    private static final Logger log = LoggerFactory.getLogger(KhuyenMaiService.class);

    private final KhuyenMaiRepository repo;
    private final DatChoRepository datChoRepository;
    private final ThongBaoRepository thongBaoRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public KhuyenMaiService(KhuyenMaiRepository repo,
                            DatChoRepository datChoRepository,
                            ThongBaoRepository thongBaoRepository,
                            SimpMessagingTemplate messagingTemplate) {
        this.repo = repo;
        this.datChoRepository = datChoRepository;
        this.thongBaoRepository = thongBaoRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public List<KhuyenMai> findAll() {
        List<KhuyenMai> all = repo.findAllByOrderByNgayTaoDesc();
        LocalDate today = LocalDate.now();
        boolean changed = false;
        for (KhuyenMai km : all) {
            boolean expired = false;
            if (km.getDieuKienNgayKetThuc() != null && today.isAfter(km.getDieuKienNgayKetThuc())) {
                expired = true;
            }
            if (km.getSoLuongToiDa() > 0 && km.getSoLuongDaDung() >= km.getSoLuongToiDa()) {
                expired = true;
            }
            if (expired && Boolean.TRUE.equals(km.getHoatDong())) {
                km.setHoatDong(false);
                changed = true;
            }
        }
        if (changed) {
            repo.saveAll(all);
        }
        return all;
    }

    @Transactional(readOnly = true)
    public List<KhuyenMai> findAvailable() {
        return repo.findAvailable(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public KhuyenMai findById(Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy khuyến mãi"));
    }

    public KhuyenMai create(KhuyenMaiRequest req) {
        KhuyenMai km = new KhuyenMai();
        applyFields(km, req);
        km.setSoLuongDaDung(0);
        km.setHoatDong(true);
        return repo.save(km);
    }

    public KhuyenMai update(Long id, KhuyenMaiRequest req) {
        KhuyenMai km = findById(id);
        applyFields(km, req);
        KhuyenMai saved = repo.save(km);
        if (req.getHoatDong() != null && !req.getHoatDong()) {
            notifyPromoInvalidated(saved);
        }
        return saved;
    }

    public KhuyenMai toggle(Long id) {
        KhuyenMai km = findById(id);
        km.setHoatDong(!Boolean.TRUE.equals(km.getHoatDong()));
        KhuyenMai saved = repo.save(km);
        if (!Boolean.TRUE.equals(saved.getHoatDong())) {
            notifyPromoInvalidated(saved);
        }
        return saved;
    }

    /**
     * Khi admin ẩn/vô hiệu khuyến mãi: gỡ mã khỏi mọi đặt chỗ đang chờ xác nhận
     * và realtime thông báo cho khách đang ở form thanh toán.
     */
    private void notifyPromoInvalidated(KhuyenMai km) {
        try {
            List<DatCho> bookings = datChoRepository
                    .findByKhuyenMai_MaKhuyenMaiAndTrangThaiDatCho(km.getMaKhuyenMai(), DatCho.TrangThaiDatCho.cho_xac_nhan);
            if (bookings.isEmpty()) {
                return;
            }
            for (DatCho booking : bookings) {
                Long oldKmId = booking.getKhuyenMai() != null ? booking.getKhuyenMai().getMaKhuyenMai() : null;
                BigDecimal oldDiscount = booking.getTienGiamGia() != null ? booking.getTienGiamGia() : BigDecimal.ZERO;
                if (booking.getTongTien() != null) {
                    booking.setTongTien(booking.getTongTien().add(oldDiscount));
                }
                booking.setKhuyenMai(null);
                booking.setTienGiamGia(BigDecimal.ZERO);
                datChoRepository.save(booking);
                if (oldKmId != null) {
                    decrementUsage(oldKmId);
                }
                if (booking.getKhach() == null) {
                    continue;
                }
                ThongBao thongBao = new ThongBao();
                thongBao.setNguoiNhan(booking.getKhach());
                thongBao.setDatCho(booking);
                thongBao.setLoaiThongBao("PROMO_INVALIDATED");
                thongBao.setNoiDung("Khuyến mãi \"" + km.getTenKhuyenMai() + "\" không còn hiệu lực và đã được gỡ khỏi đặt chỗ #" + booking.getMaDatCho() + " của bạn.");
                thongBao.setTrangThai("ACCEPTED");
                ThongBao saved = thongBaoRepository.save(thongBao);
                thongBaoRepository.flush();
                messagingTemplate.convertAndSend("/queue/notifications/" + booking.getKhach().getMaNguoiDung(),
                        ThongBaoDTO.fromEntity(thongBaoRepository.findById(saved.getId()).orElse(saved)));
            }
            log.info("Đã gỡ khuyến mãi {} khỏi {} đặt chỗ đang chờ xác nhận", km.getTenKhuyenMai(), bookings.size());
        } catch (Exception e) {
            log.error("Không thể thông báo khuyến mãi hết hiệu lực {}: {}", km.getMaKhuyenMai(), e.getMessage());
        }
    }

    /**
     * Tính số tiền giảm giá dựa trên khuyến mãi và thông tin đặt chỗ.
     * @param nights số đêm (dùng cho per-guest promotions để tính discount per night)
     * @param adultsCost tổng tiền người lớn (để tính chính xác giá mỗi loại khách)
     * @param childrenCost tổng tiền trẻ em
     */
    public BigDecimal calculateDiscount(KhuyenMai km, int soNguoiLon, int soTreEm, int soEmBe, BigDecimal roomCost, long nights, BigDecimal adultsCost, BigDecimal childrenCost) {
        if (km == null || !Boolean.TRUE.equals(km.getHoatDong())) {
            return BigDecimal.ZERO;
        }

        // Kiểm tra điều kiện ngày kết thúc (ngày bắt đầu chỉ là metadata hiển thị, không chặn áp dụng)
        LocalDate today = LocalDate.now();
        if (km.getDieuKienNgayKetThuc() != null && today.isAfter(km.getDieuKienNgayKetThuc())) {
            return BigDecimal.ZERO;
        }

        // Kiểm tra số lượng
        if (km.getSoLuongToiDa() > 0 && km.getSoLuongDaDung() >= km.getSoLuongToiDa()) {
            return BigDecimal.ZERO;
        }

        // Kiểm tra điều kiện áp dụng (ví dụ: mã chỉ dành cho đơn có trẻ em)
        if (km.getDieuKienApDung() == KhuyenMai.DieuKienApDung.CO_TRE_EM && soTreEm <= 0) {
            return BigDecimal.ZERO;
        }
        if (km.getDieuKienApDung() == KhuyenMai.DieuKienApDung.CO_NGUOI_LON && soNguoiLon <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;
        long n = Math.max(1, nights);

        switch (km.getApCho()) {
            case DON_HANG:
                discount = applyToOrder(km, roomCost);
                break;
            case NGUOI_LON:
                discount = applyPerGuest(km, soNguoiLon, adultsCost, n);
                break;
            case TRE_EM:
                discount = applyPerGuest(km, soTreEm, childrenCost, n);
                break;
        }

        return discount;
    }

    /**
     * Overload cho backward compatibility (khong truyen adultsCost/childrenCost)
     */
    public BigDecimal calculateDiscount(KhuyenMai km, int soNguoiLon, int soTreEm, int soEmBe, BigDecimal roomCost, long nights) {
        return calculateDiscount(km, soNguoiLon, soTreEm, soEmBe, roomCost, nights, roomCost, roomCost);
    }

    private BigDecimal applyToOrder(KhuyenMai km, BigDecimal roomCost) {
        if (km.getLoaiGiamGia() == KhuyenMai.LoaiGiamGia.PHAN_TRAM) {
            return roomCost.multiply(km.getGiaTri()).divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
        } else {
            return km.getGiaTri().min(roomCost);
        }
    }

    private BigDecimal applyPerGuest(KhuyenMai km, int guestCount, BigDecimal guestTypeCost, long nights) {
        if (guestCount <= 0 || guestTypeCost == null || guestTypeCost.compareTo(BigDecimal.ZERO) <= 0 || nights <= 0) return BigDecimal.ZERO;

        // guestTypeCost = giá_mới_khách * tyLe * guestCount * nights (tổng tiền loại khách)
        if (km.getLoaiGiamGia() == KhuyenMai.LoaiGiamGia.PHAN_TRAM) {
            // Giảm X% trên tổng tiền loại khách, nhưng tối đa = 1 đêm của loại khách đó
            // (mã -100% = miễn phí 1 đêm, không miễn toàn bộ số đêm)
            BigDecimal percentDiscount = guestTypeCost.multiply(km.getGiaTri()).divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
            BigDecimal oneNightCost = guestTypeCost.divide(BigDecimal.valueOf(nights), 0, java.math.RoundingMode.HALF_UP);
            return percentDiscount.min(oneNightCost);
        } else {
            // Giảm cố định X₩ cho mỗi guest trong đơn (per-stay, không nhân nights)
            BigDecimal fixedDiscount = km.getGiaTri().multiply(BigDecimal.valueOf(guestCount));
            return fixedDiscount.min(guestTypeCost);
        }
    }

    public void incrementUsage(Long id) {
        KhuyenMai km = findById(id);
        if (km.getSoLuongToiDa() > 0 && km.getSoLuongDaDung() >= km.getSoLuongToiDa()) {
            throw new RuntimeException("Mã khuyến mãi đã hết lượt sử dụng");
        }
        km.setSoLuongDaDung(km.getSoLuongDaDung() + 1);
        if (km.getSoLuongToiDa() > 0 && km.getSoLuongDaDung() >= km.getSoLuongToiDa()) {
            km.setHoatDong(false);
            repo.save(km);
            notifyPromoInvalidated(km);
            return;
        }
        repo.save(km);
    }

    public void decrementUsage(Long id) {
        KhuyenMai km = findById(id);
        if (km.getSoLuongDaDung() > 0) {
            km.setSoLuongDaDung(km.getSoLuongDaDung() - 1);
            repo.save(km);
        }
    }

    /**
     * Kiểm tra mã khuyến mãi còn khả dụng không (real-time).
     */
    @Transactional(readOnly = true)
    public boolean isAvailable(Long id) {
        KhuyenMai km = findById(id);
        if (!Boolean.TRUE.equals(km.getHoatDong())) return false;
        LocalDate today = LocalDate.now();
        if (km.getDieuKienNgayKetThuc() != null && today.isAfter(km.getDieuKienNgayKetThuc())) return false;
        if (km.getSoLuongToiDa() > 0 && km.getSoLuongDaDung() >= km.getSoLuongToiDa()) return false;
        return true;
    }

    private void applyFields(KhuyenMai km, KhuyenMaiRequest req) {
        if (req.getTenKhuyenMai() != null) km.setTenKhuyenMai(req.getTenKhuyenMai());
        if (req.getMoTa() != null) km.setMoTa(req.getMoTa());
        if (req.getLoaiGiamGia() != null) km.setLoaiGiamGia(KhuyenMai.LoaiGiamGia.valueOf(req.getLoaiGiamGia()));
        if (req.getGiaTri() != null) km.setGiaTri(req.getGiaTri());
        if (req.getApCho() != null) km.setApCho(KhuyenMai.ApCho.valueOf(req.getApCho()));
        if (req.getDieuKienApDung() != null) km.setDieuKienApDung(KhuyenMai.DieuKienApDung.valueOf(req.getDieuKienApDung()));
        if (req.getDieuKienNgayBatDau() != null) km.setDieuKienNgayBatDau(req.getDieuKienNgayBatDau());
        if (req.getDieuKienNgayKetThuc() != null) km.setDieuKienNgayKetThuc(req.getDieuKienNgayKetThuc());
        if (req.getSoLuongToiDa() != null) km.setSoLuongToiDa(req.getSoLuongToiDa());
        if (req.getHoatDong() != null) km.setHoatDong(req.getHoatDong());
    }
}
