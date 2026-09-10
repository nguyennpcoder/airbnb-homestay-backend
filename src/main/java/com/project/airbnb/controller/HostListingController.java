package com.project.airbnb.controller;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.airbnb.dto.PhongDTO;
import com.project.airbnb.entity.NguoiDung;
import com.project.airbnb.entity.Phong;
import com.project.airbnb.repository.HinhAnhRepository;
import com.project.airbnb.repository.NguoiDungRepository;
import com.project.airbnb.repository.PhongRepository;
import com.project.airbnb.repository.TienNghiPhongRepository;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/host/listings")
@Transactional
public class HostListingController {

    private final PhongRepository phongRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final HinhAnhRepository hinhAnhRepository;
    private final TienNghiPhongRepository tienNghiPhongRepository;

    public HostListingController(PhongRepository phongRepository, NguoiDungRepository nguoiDungRepository,
            HinhAnhRepository hinhAnhRepository, TienNghiPhongRepository tienNghiPhongRepository) {
        this.phongRepository = phongRepository;
        this.nguoiDungRepository = nguoiDungRepository;
        this.hinhAnhRepository = hinhAnhRepository;
        this.tienNghiPhongRepository = tienNghiPhongRepository;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateListingRequest req) {
        if (req.getMaChuNha() == null) {
            return ResponseEntity.status(400).body(java.util.Map.of("message", "Thiếu mã chủ nhà"));
        }

        NguoiDung host = nguoiDungRepository.findById(req.getMaChuNha())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ nhà"));

        if (!host.getLaChuNha()) {
            return ResponseEntity.status(403).body(java.util.Map.of("message", "Người dùng không có quyền tạo listing"));
        }

        Phong.LoaiPhong loai = req.getLoaiPhong() != null ? req.getLoaiPhong() : req.getLoaiSanPham();
        if (loai == null) {
            return ResponseEntity.status(400).body(java.util.Map.of("message", "Loại phòng không được để trống"));
        }
        if (req.getTieuDe() == null || req.getTieuDe().trim().isEmpty()) {
            return ResponseEntity.status(400).body(java.util.Map.of("message", "Tiêu đề không được để trống"));
        }
        if (req.getGiaMoiKhach() == null) {
            return ResponseEntity.status(400).body(java.util.Map.of("message", "Giá không được để trống"));
        }

        Phong phong = new Phong();
        phong.setChuNha(host);
        phong.setLoaiPhong(loai);
        phong.setTieuDe(req.getTieuDe());
        phong.setMoTa(req.getMoTa());
        phong.setUrlAnhChinh(req.getUrlAnhChinh());
        phong.setQuocGia(req.getQuocGia());
        phong.setThanhPho(req.getThanhPho());
        phong.setQuanHuyen(req.getQuanHuyen());
        phong.setPhuongXa(req.getPhuongXa());
        phong.setDiaChiDayDu(req.getDiaChiDayDu());
        phong.setGiaMoiKhach(req.getGiaMoiKhach());
        phong.setGiaDatToiThieu(req.getGiaDatToiThieu());
        phong.setLoaiBatDongSan(req.getLoaiBatDongSan());
        phong.setSoPhongNgu(req.getSoPhongNgu());
        phong.setSoGiuong(req.getSoGiuong());
        phong.setSoPhongTam(req.getSoPhongTam());
        phong.setSoKhachToiDa(req.getSoKhachToiDa());
        phong.setSoDemToiThieu(req.getSoDemToiThieu());
        phong.setPhiVeSinh(req.getPhiVeSinh());
        if (req.getTienNghi() != null) {
            phong.setTienNghi(req.getTienNghi());
        }
        if (req.getViDo() != null) {
            phong.setViDo(req.getViDo());
        }
        if (req.getKinhDo() != null) {
            phong.setKinhDo(req.getKinhDo());
        }
        if (req.getChinhSachHuy() != null) {
            phong.setChinhSachHuy(req.getChinhSachHuy());
        }
        if (req.getPassPhong() != null) {
            phong.setPassPhong(req.getPassPhong());
        }
        phong.setTrangThai(Phong.TrangThaiPhong.cho_duyet);
        return ResponseEntity.ok(PhongDTO.fromEntity(phongRepository.save(phong)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody UpdateListingRequest req) {
        return phongRepository.findById(id)
                .map(phong -> {
                    if (phong.getChuNha() != null && !phong.getChuNha().getLaChuNha()) {
                        return ResponseEntity.status(403).body(java.util.Map.of("message", "Chủ nhà không còn quyền chỉnh sửa listing"));
                    }

                    if (req.getTieuDe() != null) {
                        phong.setTieuDe(req.getTieuDe());
                    }
                    if (req.getMoTa() != null) {
                        phong.setMoTa(req.getMoTa());
                    }
                    if (req.getUrlAnhChinh() != null) {
                        phong.setUrlAnhChinh(req.getUrlAnhChinh());
                    }
                    if (req.getGiaMoiKhach() != null) {
                        phong.setGiaMoiKhach(req.getGiaMoiKhach());
                    }
                    if (req.getGiaDatToiThieu() != null) {
                        phong.setGiaDatToiThieu(req.getGiaDatToiThieu());
                    }
                    if (req.getSoKhachToiDa() != null) {
                        phong.setSoKhachToiDa(req.getSoKhachToiDa());
                    }
                    if (req.getSoDemToiThieu() != null) {
                        phong.setSoDemToiThieu(req.getSoDemToiThieu());
                    }
                    if (req.getPhiVeSinh() != null) {
                        phong.setPhiVeSinh(req.getPhiVeSinh());
                    }
                    if (req.getLoaiBatDongSan() != null) {
                        phong.setLoaiBatDongSan(req.getLoaiBatDongSan());
                    }
                    if (req.getSoPhongNgu() != null) {
                        phong.setSoPhongNgu(req.getSoPhongNgu());
                    }
                    if (req.getSoGiuong() != null) {
                        phong.setSoGiuong(req.getSoGiuong());
                    }
                    if (req.getSoPhongTam() != null) {
                        phong.setSoPhongTam(req.getSoPhongTam());
                    }
                    if (req.getTienNghi() != null) {
                        phong.setTienNghi(req.getTienNghi());
                    }
                    if (req.getViDo() != null) {
                        phong.setViDo(req.getViDo());
                    }
                    if (req.getKinhDo() != null) {
                        phong.setKinhDo(req.getKinhDo());
                    }
                    // Extra fields not in original DTO
                    if (req.getThanhPho() != null) {
                        phong.setThanhPho(req.getThanhPho());
                    }
                    if (req.getQuocGia() != null) {
                        phong.setQuocGia(req.getQuocGia());
                    }
                    if (req.getDiaChiDayDu() != null) {
                        phong.setDiaChiDayDu(req.getDiaChiDayDu());
                    }
                    if (req.getQuanHuyen() != null) {
                        phong.setQuanHuyen(req.getQuanHuyen());
                    }
                    if (req.getPhuongXa() != null) {
                        phong.setPhuongXa(req.getPhuongXa());
                    }
                    if (req.getLoaiPhong() != null) {
                        phong.setLoaiPhong(req.getLoaiPhong());
                    }
                    if (req.getPassPhong() != null) {
                        phong.setPassPhong(req.getPassPhong());
                    }
                    if (req.getChinhSachHuy() != null) {
                        phong.setChinhSachHuy(req.getChinhSachHuy());
                    }
                    phongRepository.save(phong);
                    return ResponseEntity.ok(PhongDTO.fromEntity(phong));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOwnedListing(@PathVariable long id, @RequestParam Long maChuNha) {
        return phongRepository.findById(id)
                .map(phong -> {
                    if (phong.getChuNha() == null || !phong.getChuNha().getMaNguoiDung().equals(maChuNha)) {
                        return ResponseEntity.status(403).body(java.util.Map.of("message", "Bạn không có quyền xem listing này"));
                    }
                    var hinhAnhs = hinhAnhRepository.findByPhong_MaPhongOrderByThuTuAsc(id);
                    var amenities = tienNghiPhongRepository.findByPhong_MaPhongOrderByNhomAscTenAsc(id);
                    return ResponseEntity.ok(PhongDTO.fromEntity(phong, hinhAnhs, amenities));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/hide")
    public ResponseEntity<?> hide(@PathVariable long id) {
        return phongRepository.findById(id)
                .map(phong -> {
                    if (phong.getChuNha() != null && !phong.getChuNha().getLaChuNha()) {
                        return ResponseEntity.status(403).body(java.util.Map.of("message", "Chủ nhà không còn quyền quản lý listing"));
                    }
                    phong.setTrangThai(Phong.TrangThaiPhong.khong_hoat_dong);
                    phongRepository.save(phong);
                    return ResponseEntity.ok(java.util.Map.of("hidden", true));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/unhide")
    public ResponseEntity<?> unhide(@PathVariable long id) {
        return phongRepository.findById(id)
                .map(phong -> {
                    if (phong.getChuNha() != null && !phong.getChuNha().getLaChuNha()) {
                        return ResponseEntity.status(403).body(java.util.Map.of("message", "Chủ nhà không còn quyền quản lý listing"));
                    }
                    if (Boolean.TRUE.equals(phong.getBiKhoa())) {
                        return ResponseEntity.status(403).body(java.util.Map.of(
                                "message", "Phòng đã bị quản trị viên khóa do vi phạm. Chủ nhà không thể tự mở lại, vui lòng liên hệ quản trị viên để yêu cầu mở khóa."));
                    }
                    // Mở lại phòng -> về chờ duyệt, hệ thống tự động duyệt sau 5 giây
                    phong.setTrangThai(Phong.TrangThaiPhong.cho_duyet);
                    phongRepository.save(phong);
                    return ResponseEntity.ok(java.util.Map.of("unhidden", true, "trangThai", Phong.TrangThaiPhong.cho_duyet.name()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CreateListingRequest {
        private Long maChuNha;
        @JsonAlias("loaiSanPham")
        private Phong.LoaiPhong loaiPhong;
        private Phong.LoaiPhong loaiSanPham;
        private String tieuDe;
        private String moTa;
        private String urlAnhChinh;
        private String quocGia;
        private String thanhPho;
        private String quanHuyen;
        private String phuongXa;
        private String diaChiDayDu;
        private BigDecimal giaMoiKhach;
        private BigDecimal giaDatToiThieu;
        private String loaiBatDongSan;
        private Integer soPhongNgu;
        private Integer soGiuong;
        private BigDecimal soPhongTam;
        private Integer soKhachToiDa;
        private Integer soDemToiThieu;
        private BigDecimal phiVeSinh;
        private String tienNghi;
        private BigDecimal viDo;
        private BigDecimal kinhDo;
        private Phong.ChinhSachHuy chinhSachHuy;
        private String passPhong;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UpdateListingRequest {
        private String tieuDe;
        private String moTa;
        private String urlAnhChinh;
        private BigDecimal giaMoiKhach;
        private BigDecimal giaDatToiThieu;
        private Integer soKhachToiDa;
        private Integer soDemToiThieu;
        private BigDecimal phiVeSinh;
        private String loaiBatDongSan;
        private Integer soPhongNgu;
        private Integer soGiuong;
        private BigDecimal soPhongTam;
        private String tienNghi;
        private BigDecimal viDo;
        private BigDecimal kinhDo;
        // Missing fields that are needed for full update
        private String thanhPho;
        private String quocGia;
        private String diaChiDayDu;
        private String quanHuyen;
        private String phuongXa;
        @JsonAlias("loaiSanPham")
        private Phong.LoaiPhong loaiPhong;
        private Phong.ChinhSachHuy chinhSachHuy;
        private String passPhong;
    }
}
