package com.project.airbnb.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "nguoi_dung")
public class NguoiDung {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ma_nguoi_dung")
    private Long maNguoiDung;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "mat_khau_bam", nullable = false)
    private String matKhauBam;

    @Column(name = "ho")
    private String ho;

    @Column(name = "ten")
    private String ten;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    @Column(name = "ho_ten")
    private String hoTen;

    @Column(name = "so_dien_thoai", length = 20)
    private String soDienThoai;

    @Column(name = "email_da_xac_nhan")
    private Boolean emailDaXacNhan = false;

    @Column(name = "da_chap_nhan_cam_ket_cong_dong")
    private Boolean daChapNhanCamKetCongDong = false;

    @Column(name = "nhan_tin_nhan_tiep_thi")
    private Boolean nhanTinNhanTiepThi = true;

    @Column(name = "la_chu_nha", nullable = false)
    private Boolean laChuNha = false;

    @Column(name = "la_admin", nullable = false)
    private Boolean laAdmin = false;

    @Column(name = "bi_khoa", nullable = false)
    private Boolean biKhoa = false;

    @Column(name = "url_anh_dai_dien", length = 500)
    private String urlAnhDaiDien;

    @Column(name = "xac_minh_danh_tinh")
    private Boolean xacMinhDanhTinh = false;

    @Column(name = "url_bang_chung_danh_tinh", length = 500)
    private String urlBangChungDanhTinh;

    @Column(name = "ngay_xac_minh")
    private LocalDateTime ngayXacMinh;

    @Column(name = "ly_do_khoa", columnDefinition = "TEXT")
    private String lyDoKhoa;

    @Column(name = "cong_viec", length = 100)
    private String congViec;

    @Column(name = "thanh_pho_song", length = 100)
    private String thanhPhoSong;

    @Column(name = "so_luong_danh_gia_host")
    private Integer soLuongDanhGiaHost = 0;

    @Column(name = "diem_danh_gia_host", precision = 3, scale = 2)
    private BigDecimal diemDanhGiaHost;

    @Column(name = "so_du", precision = 15, scale = 2)
    private BigDecimal soDu = BigDecimal.ZERO;

    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    public NguoiDung() {}

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
        ngayCapNhat = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        ngayCapNhat = LocalDateTime.now();
    }

    // --- Getters ---
    public Long getMaNguoiDung() { return maNguoiDung; }
    public String getEmail() { return email; }
    public String getMatKhauBam() { return matKhauBam; }
    public String getHo() { return ho; }
    public String getTen() { return ten; }
    public LocalDate getNgaySinh() { return ngaySinh; }
    public String getHoTen() { return hoTen; }
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

    // --- Setters ---
    public void setMaNguoiDung(Long maNguoiDung) { this.maNguoiDung = maNguoiDung; }
    public void setEmail(String email) { this.email = email; }
    public void setMatKhauBam(String matKhauBam) { this.matKhauBam = matKhauBam; }
    public void setHo(String ho) { this.ho = ho; }
    public void setTen(String ten) { this.ten = ten; }
    public void setNgaySinh(LocalDate ngaySinh) { this.ngaySinh = ngaySinh; }
    public void setHoTen(String hoTen) { this.hoTen = hoTen; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
    public void setEmailDaXacNhan(Boolean emailDaXacNhan) { this.emailDaXacNhan = emailDaXacNhan; }
    public void setDaChapNhanCamKetCongDong(Boolean daChapNhanCamKetCongDong) { this.daChapNhanCamKetCongDong = daChapNhanCamKetCongDong; }
    public void setNhanTinNhanTiepThi(Boolean nhanTinNhanTiepThi) { this.nhanTinNhanTiepThi = nhanTinNhanTiepThi; }
    public void setLaChuNha(Boolean laChuNha) { this.laChuNha = laChuNha; }
    public void setLaAdmin(Boolean laAdmin) { this.laAdmin = laAdmin; }
    public void setBiKhoa(Boolean biKhoa) { this.biKhoa = biKhoa; }
    public void setUrlAnhDaiDien(String urlAnhDaiDien) { this.urlAnhDaiDien = urlAnhDaiDien; }
    public void setXacMinhDanhTinh(Boolean xacMinhDanhTinh) { this.xacMinhDanhTinh = xacMinhDanhTinh; }
    public void setUrlBangChungDanhTinh(String urlBangChungDanhTinh) { this.urlBangChungDanhTinh = urlBangChungDanhTinh; }
    public void setNgayXacMinh(LocalDateTime ngayXacMinh) { this.ngayXacMinh = ngayXacMinh; }
    public void setLyDoKhoa(String lyDoKhoa) { this.lyDoKhoa = lyDoKhoa; }
    public void setCongViec(String congViec) { this.congViec = congViec; }
    public void setThanhPhoSong(String thanhPhoSong) { this.thanhPhoSong = thanhPhoSong; }
    public void setSoLuongDanhGiaHost(Integer soLuongDanhGiaHost) { this.soLuongDanhGiaHost = soLuongDanhGiaHost; }
    public void setDiemDanhGiaHost(BigDecimal diemDanhGiaHost) { this.diemDanhGiaHost = diemDanhGiaHost; }
    public void setSoDu(BigDecimal soDu) { this.soDu = soDu; }
    public void setNgayTao(LocalDateTime ngayTao) { this.ngayTao = ngayTao; }
    public void setNgayCapNhat(LocalDateTime ngayCapNhat) { this.ngayCapNhat = ngayCapNhat; }
}
