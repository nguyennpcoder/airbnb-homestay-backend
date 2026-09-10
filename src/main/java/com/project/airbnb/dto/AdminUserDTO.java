package com.project.airbnb.dto;

import com.project.airbnb.entity.NguoiDung;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminUserDTO {
    private Long maNguoiDung;
    private String email;
    private String ho;
    private String ten;
    private String hoTen;
    private LocalDate ngaySinh;
    private String soDienThoai;
    private Boolean emailDaXacNhan;
    private Boolean daChapNhanCamKetCongDong;
    private Boolean nhanTinNhanTiepThi;
    private Boolean laChuNha;
    private Boolean laAdmin;
    private Boolean biKhoa;
    private String urlAnhDaiDien;
    private Boolean xacMinhDanhTinh;
    private String urlBangChungDanhTinh;
    private LocalDateTime ngayXacMinh;
    private String lyDoKhoa;
    private String congViec;
    private String thanhPhoSong;
    private Integer soLuongDanhGiaHost;
    private BigDecimal diemDanhGiaHost;
    private BigDecimal soDu;
    private LocalDateTime ngayTao;
    private LocalDateTime ngayCapNhat;
    private long soLuongDatCho;
    private long soLuongListing;
    private boolean coYeuCauHost; // Whether this user has a pending HOST_REQUEST

    public static AdminUserDTO from(NguoiDung u, long soLuongDatCho, long soLuongListing, boolean coYeuCauHost) {
        return AdminUserDTO.builder()
                .maNguoiDung(u.getMaNguoiDung())
                .email(u.getEmail())
                .ho(u.getHo())
                .ten(u.getTen())
                .hoTen(u.getHoTen())
                .ngaySinh(u.getNgaySinh())
                .soDienThoai(u.getSoDienThoai())
                .emailDaXacNhan(u.getEmailDaXacNhan())
                .daChapNhanCamKetCongDong(u.getDaChapNhanCamKetCongDong())
                .nhanTinNhanTiepThi(u.getNhanTinNhanTiepThi())
                .laChuNha(u.getLaChuNha())
                .laAdmin(u.getLaAdmin())
                .biKhoa(u.getBiKhoa())
                .urlAnhDaiDien(u.getUrlAnhDaiDien())
                .xacMinhDanhTinh(u.getXacMinhDanhTinh())
                .urlBangChungDanhTinh(u.getUrlBangChungDanhTinh())
                .ngayXacMinh(u.getNgayXacMinh())
                .lyDoKhoa(u.getLyDoKhoa())
                .congViec(u.getCongViec())
                .thanhPhoSong(u.getThanhPhoSong())
                .soLuongDanhGiaHost(u.getSoLuongDanhGiaHost())
                .diemDanhGiaHost(u.getDiemDanhGiaHost())
                .soDu(u.getSoDu())
                .ngayTao(u.getNgayTao())
                .ngayCapNhat(u.getNgayCapNhat())
                .soLuongDatCho(soLuongDatCho)
                .soLuongListing(soLuongListing)
                .coYeuCauHost(coYeuCauHost)
                .build();
    }
}
