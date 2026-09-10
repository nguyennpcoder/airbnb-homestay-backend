package com.project.airbnb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.airbnb.entity.DatCho;
import com.project.airbnb.entity.Phong;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
public class DatChoResponse {
    private Long maDatCho;
    private Long maKhach;
    private PhongSummary phong;
    private String loaiPhong;
    private LocalDate ngayNhanPhong;
    private LocalDate ngayTraPhong;
    private LocalDate ngayDat;
    private LocalTime gioBatDau;
    private LocalTime gioKetThuc;
    private Integer soLuongKhach;
    private Integer soNguoiLon;
    private Integer soTreEm;
    private Integer soEmBe;
    private BigDecimal giaMoiKhach;
    private BigDecimal tongTien;
    private BigDecimal phiVeSinh;
    private BigDecimal phiDichVu;
    private String tienTe;
    private String trangThaiDatCho;
    private String yeuCauDacBiet;
    private BigDecimal tienHoanLai;
    private String lyDoHuy;
    private String chinhSachHuy;
    private LocalDateTime ngayTao;
    private LocalDateTime ngayCapNhat;
    private List<PaymentSummary> payments;
    private KhachSummary nguoiDat;

    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Boolean hasUnread;

    @Data
    public static class PhongSummary {
        @JsonProperty("maPhong")
        private Long maPhong;
        private String tieuDe;
        private String urlAnhChinh;
        private String thanhPho;
        private String quocGia;
        private BigDecimal giaMoiKhach;
        private BigDecimal diemTrungBinh;
        private Integer soLuongDanhGia;
        private Boolean duocKhachYeuThich;
        private Integer soKhachToiDa;
        private ChuNhaSummary chuNha;

        @JsonProperty("maSanPham")
        public Long getMaSanPham() {
            return maPhong;
        }
    }

    @Data
    public static class ChuNhaSummary {
        private Long maNguoiDung;
        private String ho;
        private String ten;
        private String hoTen;
        private String email;
        private String soDienThoai;
        private String urlAnhDaiDien;
        private Boolean xacMinhDanhTinh;
        private BigDecimal diemDanhGia;
        private Integer soLuongDanhGia;
        private Boolean biKhoa;
        private String lyDoKhoa;
    }

    @Data
    public static class KhachSummary {
        private Long maNguoiDung;
        private String ho;
        private String ten;
        private String hoTen;
        private String email;
        private String soDienThoai;
        private String urlAnhDaiDien;
        private Boolean xacMinhDanhTinh;
        private Boolean biKhoa;
        private String lyDoKhoa;
    }

    @Data
    public static class PaymentSummary {
        private Long id;
        private BigDecimal amount;
        private String currency;
        private String provider;
        private String status;
        private String transactionId;
    }

    private static String resolveDisplayName(com.project.airbnb.entity.NguoiDung user, String fallback) {
        if (user == null) return fallback;
        String name = user.getHoTen();
        if (name != null && !name.isBlank()) return name.trim();
        String ho = user.getHo() != null ? user.getHo() : "";
        String ten = user.getTen() != null ? user.getTen() : "";
        name = (ho + " " + ten).trim();
        return name.isBlank() ? fallback : name;
    }

    public static DatChoResponse fromEntity(DatCho entity, List<com.project.airbnb.entity.Payment> payments) {
        DatChoResponse dto = new DatChoResponse();
        dto.setMaDatCho(entity.getMaDatCho());
        dto.setMaKhach(entity.getKhach() != null ? entity.getKhach().getMaNguoiDung() : null);

        if (entity.getKhach() != null) {
            KhachSummary k = new KhachSummary();
            k.setMaNguoiDung(entity.getKhach().getMaNguoiDung());
            k.setHo(entity.getKhach().getHo());
            k.setTen(entity.getKhach().getTen());
            k.setHoTen(resolveDisplayName(entity.getKhach(), "Khách"));
            k.setEmail(entity.getKhach().getEmail());
            k.setSoDienThoai(entity.getKhach().getSoDienThoai());
            k.setUrlAnhDaiDien(entity.getKhach().getUrlAnhDaiDien());
            k.setXacMinhDanhTinh(entity.getKhach().getXacMinhDanhTinh());
            k.setBiKhoa(entity.getKhach().getBiKhoa());
            k.setLyDoKhoa(entity.getKhach().getLyDoKhoa());
            dto.setNguoiDat(k);
        }

        dto.setLoaiPhong(entity.getLoaiPhong() != null ? entity.getLoaiPhong().name() : null);
        dto.setNgayNhanPhong(entity.getNgayNhanPhong());
        dto.setNgayTraPhong(entity.getNgayTraPhong());
        dto.setNgayDat(entity.getNgayDat());
        dto.setGioBatDau(entity.getGioBatDau());
        dto.setGioKetThuc(entity.getGioKetThuc());
        dto.setSoLuongKhach(entity.getSoLuongKhach());
        dto.setSoNguoiLon(entity.getSoNguoiLon());
        dto.setSoTreEm(entity.getSoTreEm());
        dto.setSoEmBe(entity.getSoEmBe());
        dto.setGiaMoiKhach(entity.getGiaMoiKhach());
        dto.setTongTien(entity.getTongTien());
        dto.setPhiVeSinh(entity.getPhiVeSinh());
        dto.setPhiDichVu(entity.getPhiDichVu());
        dto.setTienTe(entity.getTienTe());
        dto.setTrangThaiDatCho(entity.getTrangThaiDatCho() != null ? entity.getTrangThaiDatCho().name() : null);
        dto.setYeuCauDacBiet(entity.getYeuCauDacBiet());
        dto.setTienHoanLai(entity.getTienHoanLai());
        dto.setLyDoHuy(entity.getLyDoHuy());
        dto.setNgayTao(entity.getNgayTao());
        dto.setNgayCapNhat(entity.getNgayCapNhat());

        Phong sp = entity.getPhong();
        if (sp != null) {
            PhongSummary summary = new PhongSummary();
            summary.setMaPhong(sp.getMaPhong());
            summary.setTieuDe(sp.getTieuDe());
            summary.setUrlAnhChinh(sp.getUrlAnhChinh());
            summary.setThanhPho(sp.getThanhPho());
            summary.setQuocGia(sp.getQuocGia());
            summary.setGiaMoiKhach(sp.getGiaMoiKhach());
            summary.setDiemTrungBinh(sp.getDiemTrungBinh());
            summary.setSoLuongDanhGia(sp.getSoLuongDanhGia());
            summary.setDuocKhachYeuThich(sp.getDuocKhachYeuThich());
            summary.setSoKhachToiDa(sp.getSoKhachToiDa());

            if (sp.getChuNha() != null) {
                ChuNhaSummary host = new ChuNhaSummary();
                host.setMaNguoiDung(sp.getChuNha().getMaNguoiDung());
                host.setHo(sp.getChuNha().getHo());
                host.setTen(sp.getChuNha().getTen());
                host.setHoTen(resolveDisplayName(sp.getChuNha(), "Chủ nhà"));
                host.setEmail(sp.getChuNha().getEmail());
                host.setSoDienThoai(sp.getChuNha().getSoDienThoai());
                host.setUrlAnhDaiDien(sp.getChuNha().getUrlAnhDaiDien());
                host.setXacMinhDanhTinh(sp.getChuNha().getXacMinhDanhTinh());
                host.setDiemDanhGia(sp.getChuNha().getDiemDanhGiaHost());
                host.setSoLuongDanhGia(sp.getChuNha().getSoLuongDanhGiaHost());
                host.setBiKhoa(sp.getChuNha().getBiKhoa());
                host.setLyDoKhoa(sp.getChuNha().getLyDoKhoa());
                summary.setChuNha(host);
            }

            dto.setPhong(summary);
            dto.setChinhSachHuy(sp.getChinhSachHuy() != null ? sp.getChinhSachHuy().name() : null);
        }

        // Map payments
        if (payments != null) {
            dto.setPayments(payments.stream().map(p -> {
                PaymentSummary ps = new PaymentSummary();
                ps.setId(p.getId());
                ps.setAmount(p.getAmount());
                ps.setCurrency(p.getCurrency());
                ps.setProvider(p.getProvider() != null ? p.getProvider().name() : null);
                ps.setStatus(p.getStatus() != null ? p.getStatus().name() : null);
                ps.setTransactionId(p.getTransactionId());
                return ps;
            }).toList());
        }

        return dto;
    }

    @JsonProperty("sanPham")
    public PhongSummary getSanPham() {
        return phong;
    }

    @JsonProperty("maSanPham")
    public Long getMaSanPham() {
        return phong != null ? phong.getMaPhong() : null;
    }

    // Overload for backward compatibility
    public static DatChoResponse fromEntity(DatCho entity) {
        return fromEntity(entity, null);
    }
}
