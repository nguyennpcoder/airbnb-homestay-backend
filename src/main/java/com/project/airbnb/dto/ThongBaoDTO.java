package com.project.airbnb.dto;

import com.project.airbnb.entity.ThongBao;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ThongBaoDTO {
    private Long id;
    private UserInfo nguoiGui;
    private UserInfo nguoiNhan;
    private BookingInfo datCho;
    private String loaiThongBao;
    private String noiDung;
    private boolean daDoc;
    private String trangThai;
    private LocalDateTime ngayTao;

    @Data
    public static class UserInfo {
        private Long maNguoiDung;
        private String hoTen;
        private String email;
        private String urlAnhDaiDien;
        private String soDienThoai;
    }

    @Data
    public static class BookingInfo {
        private Long maDatCho;
        private String trangThaiDatCho;
        private LocalDateTime ngayTao;
        private PhongInfo phong;
    }

    @Data
    public static class PhongInfo {
        private Long maPhong;
        private String tieuDe;
        private String urlAnhChinh;
    }

    public static ThongBaoDTO fromEntity(ThongBao entity) {
        ThongBaoDTO dto = new ThongBaoDTO();
        dto.setId(entity.getId());
        dto.setLoaiThongBao(entity.getLoaiThongBao());
        dto.setNoiDung(entity.getNoiDung());
        dto.setDaDoc(entity.isDaDoc());
        dto.setTrangThai(entity.getTrangThai());
        dto.setNgayTao(entity.getNgayTao());

        if (entity.getNguoiGui() != null) {
            UserInfo sender = new UserInfo();
            sender.setMaNguoiDung(entity.getNguoiGui().getMaNguoiDung());
            sender.setHoTen(entity.getNguoiGui().getHoTen());
            sender.setEmail(entity.getNguoiGui().getEmail());
            sender.setUrlAnhDaiDien(entity.getNguoiGui().getUrlAnhDaiDien());
            sender.setSoDienThoai(entity.getNguoiGui().getSoDienThoai());
            dto.setNguoiGui(sender);
        }

        if (entity.getNguoiNhan() != null) {
            UserInfo receiver = new UserInfo();
            receiver.setMaNguoiDung(entity.getNguoiNhan().getMaNguoiDung());
            receiver.setHoTen(entity.getNguoiNhan().getHoTen());
            receiver.setEmail(entity.getNguoiNhan().getEmail());
            receiver.setUrlAnhDaiDien(entity.getNguoiNhan().getUrlAnhDaiDien());
            receiver.setSoDienThoai(entity.getNguoiNhan().getSoDienThoai());
            dto.setNguoiNhan(receiver);
        }

        if (entity.getDatCho() != null) {
            BookingInfo booking = new BookingInfo();
            booking.setMaDatCho(entity.getDatCho().getMaDatCho());
            if (entity.getDatCho().getTrangThaiDatCho() != null) {
                booking.setTrangThaiDatCho(entity.getDatCho().getTrangThaiDatCho().name());
            }
            booking.setNgayTao(entity.getDatCho().getNgayTao());

            if (entity.getDatCho().getPhong() != null) {
                PhongInfo phong = new PhongInfo();
                phong.setMaPhong(entity.getDatCho().getPhong().getMaPhong());
                phong.setTieuDe(entity.getDatCho().getPhong().getTieuDe());
                phong.setUrlAnhChinh(entity.getDatCho().getPhong().getUrlAnhChinh());
                booking.setPhong(phong);
            }

            dto.setDatCho(booking);
        }

        return dto;
    }
}
