package com.project.airbnb.dto;

import com.project.airbnb.entity.NguoiDung;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private boolean coYeuCauHost;

    public static AdminUserDTO from(NguoiDung u, long soLuongDatCho, long soLuongListing, boolean coYeuCauHost) {
        AdminUserDTO dto = new AdminUserDTO();
        dto.maNguoiDung = u.getMaNguoiDung();
        dto.email = u.getEmail();
        dto.ho = u.getHo();
        dto.ten = u.getTen();
        dto.hoTen = u.getHoTen();
        dto.ngaySinh = u.getNgaySinh();
        dto.soDienThoai = u.getSoDienThoai();
        dto.emailDaXacNhan = u.getEmailDaXacNhan();
        dto.daChapNhanCamKetCongDong = u.getDaChapNhanCamKetCongDong();
        dto.nhanTinNhanTiepThi = u.getNhanTinNhanTiepThi();
        dto.laChuNha = u.getLaChuNha();
        dto.laAdmin = u.getLaAdmin();
        dto.biKhoa = u.getBiKhoa();
        dto.urlAnhDaiDien = u.getUrlAnhDaiDien();
        dto.xacMinhDanhTinh = u.getXacMinhDanhTinh();
        dto.urlBangChungDanhTinh = u.getUrlBangChungDanhTinh();
        dto.ngayXacMinh = u.getNgayXacMinh();
        dto.lyDoKhoa = u.getLyDoKhoa();
        dto.congViec = u.getCongViec();
        dto.thanhPhoSong = u.getThanhPhoSong();
        dto.soLuongDanhGiaHost = u.getSoLuongDanhGiaHost();
        dto.diemDanhGiaHost = u.getDiemDanhGiaHost();
        dto.soDu = u.getSoDu();
        dto.ngayTao = u.getNgayTao();
        dto.ngayCapNhat = u.getNgayCapNhat();
        dto.soLuongDatCho = soLuongDatCho;
        dto.soLuongListing = soLuongListing;
        dto.coYeuCauHost = coYeuCauHost;
        return dto;
    }

    // --- Getters ---
    public Long getMaNguoiDung() { return maNguoiDung; }
    public String getEmail() { return email; }
    public String getHo() { return ho; }
    public String getTen() { return ten; }
    public String getHoTen() { return hoTen; }
    public LocalDate getNgaySinh() { return ngaySinh; }
    public String getSoDienThoai() { return soDienThoai; }
    public Boolean getEmailDaXacNhan() { return emailDaXacNhan; }
    public Boolean getDaChapNhanCamKetCongDong() { return daChapNhanCamKetCongDong; }
    public Boolean getNhanTinNhanTiepThi() { return nhanTinNhanTiepThi; }
    public Boolean getLaChuNha() { return laChuNha; }
    public Boolean getLaAdmin() { return laAdmin; }
    public Boolean getBiKhoa() { return biKhoa; }
    public String getUrlAnhDaiDien() { return urlAnhDaiDien; }
    public Boolean getXacMinhDanhTinh() { return xacMinhDanhTinh; }
    public String getUrlBangChungDanhTinh() { return urlBangChungDanhTinh; }
    public LocalDateTime getNgayXacMinh() { return ngayXacMinh; }
    public String getLyDoKhoa() { return lyDoKhoa; }
    public String getCongViec() { return congViec; }
    public String getThanhPhoSong() { return thanhPhoSong; }
    public Integer getSoLuongDanhGiaHost() { return soLuongDanhGiaHost; }
    public BigDecimal getDiemDanhGiaHost() { return diemDanhGiaHost; }
    public BigDecimal getSoDu() { return soDu; }
    public LocalDateTime getNgayTao() { return ngayTao; }
    public LocalDateTime getNgayCapNhat() { return ngayCapNhat; }
    public long getSoLuongDatCho() { return soLuongDatCho; }
    public long getSoLuongListing() { return soLuongListing; }
    public boolean isCoYeuCauHost() { return coYeuCauHost; }
}
