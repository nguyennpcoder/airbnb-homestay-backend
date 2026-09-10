package com.project.airbnb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.airbnb.entity.Phong;
import com.project.airbnb.entity.TienNghiPhong;
import lombok.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Data
public class PhongDTO {
    @JsonProperty("maPhong")
    private Long maPhong;
    private String tieuDe;
    private String moTa;
    private String urlAnhChinh;
    private String quocGia;
    private String thanhPho;
    private String quanHuyen;
    private String phuongXa;
    private String diaChiDayDu;
    private BigDecimal giaMoiKhach;
    private String tienTe;
    private BigDecimal giaDatToiThieu;
    private BigDecimal phiVeSinh;
    private BigDecimal kinhDo;
    private BigDecimal viDo;
    private String loaiBatDongSan;
    private Integer soPhongNgu;
    private Integer soGiuong;
    private BigDecimal soPhongTam;
    private Integer soKhachToiDa;
    private Integer soDemToiThieu;
    private BigDecimal thoiGianTinhBangGio;
    private String phanLoai;
    private Boolean laOriginal;
    private Boolean laPhoBien;
    private BigDecimal diemTrungBinh;
    private Integer soLuongDanhGia;
    private Boolean duocKhachYeuThich;
    @JsonProperty("loaiPhong")
    private Phong.LoaiPhong loaiPhong;
    private Phong.TrangThaiPhong trangThai;
    private Boolean biKhoa;
    private Boolean daDuyet;
    private String tienNghi; // JSON string từ database (legacy support)
    private List<HinhAnhDTO> hinhAnhs;
    private List<TienNghiDTO> tienNghiItems;
    private Phong.ChinhSachHuy chinhSachHuy;
    private String passPhong;
    private HostInfo hostInfo;

    /** Alias for clients expecting `chuNha` (same payload as hostInfo). */
    @JsonProperty("chuNha")
    public HostInfo getChuNha() {
        return hostInfo;
    }

    @Data
    @Getter
    @Setter
    public static class HostInfo {
        private Long maNguoiDung;
        private String hoTen;
        private String avatarUrl;
        private String congViec;
        private String thanhPho;
        private Integer soLuongDanhGia;
        private java.math.BigDecimal diemDanhGia;
        private Integer soNamKinhNghiem;
        private Boolean xacMinhDanhTinh;

        /** Alias used by messaging / profile UIs. */
        @JsonProperty("urlAnhDaiDien")
        public String getUrlAnhDaiDien() {
            return avatarUrl;
        }
    }

    @JsonProperty("maSanPham")
    public Long getMaSanPham() {
        return maPhong;
    }

    @JsonProperty("loaiSanPham")
    public String getLoaiSanPham() {
        return loaiPhong != null ? loaiPhong.name() : null;
    }

    public static PhongDTO fromEntity(Phong phong) {
        return fromEntity(phong, phong.getHinhAnhs(), null);
    }

    public static PhongDTO fromEntity(Phong phong, List<com.project.airbnb.entity.HinhAnh> hinhAnhList) {
        return fromEntity(phong, hinhAnhList, null);
    }

    public static PhongDTO fromEntity(Phong phong, List<com.project.airbnb.entity.HinhAnh> hinhAnhList,
            List<TienNghiPhong> amenityList) {
        if (phong == null) {
            return null;
        }

        PhongDTO dto = new PhongDTO();
        dto.setMaPhong(phong.getMaPhong());
        dto.setTieuDe(phong.getTieuDe());
        dto.setMoTa(phong.getMoTa());
        dto.setUrlAnhChinh(phong.getUrlAnhChinh());
        dto.setQuocGia(phong.getQuocGia());
        dto.setThanhPho(phong.getThanhPho());
        dto.setQuanHuyen(phong.getQuanHuyen());
        dto.setPhuongXa(phong.getPhuongXa());
        dto.setDiaChiDayDu(phong.getDiaChiDayDu());
        dto.setGiaMoiKhach(phong.getGiaMoiKhach());
        dto.setTienTe(phong.getTienTe());
        dto.setGiaDatToiThieu(phong.getGiaDatToiThieu());
        dto.setPhiVeSinh(phong.getPhiVeSinh());
        dto.setKinhDo(phong.getKinhDo());
        dto.setViDo(phong.getViDo());
        dto.setLoaiBatDongSan(phong.getLoaiBatDongSan());
        dto.setSoPhongNgu(phong.getSoPhongNgu());
        dto.setSoGiuong(phong.getSoGiuong());
        dto.setSoPhongTam(phong.getSoPhongTam());
        dto.setSoKhachToiDa(phong.getSoKhachToiDa());
        dto.setSoDemToiThieu(phong.getSoDemToiThieu());
        dto.setThoiGianTinhBangGio(phong.getThoiGianTinhBangGio());
        dto.setPhanLoai(phong.getPhanLoai());
        dto.setLaOriginal(phong.getLaOriginal() != null ? phong.getLaOriginal() : false);
        dto.setLaPhoBien(phong.getLaPhoBien() != null ? phong.getLaPhoBien() : false);
        dto.setDiemTrungBinh(
                phong.getDiemTrungBinh() != null ? phong.getDiemTrungBinh() : java.math.BigDecimal.ZERO);
        dto.setSoLuongDanhGia(phong.getSoLuongDanhGia() != null ? phong.getSoLuongDanhGia() : 0);
        dto.setDuocKhachYeuThich(phong.getDuocKhachYeuThich() != null ? phong.getDuocKhachYeuThich() : false);
        dto.setLoaiPhong(phong.getLoaiPhong());
        dto.setTrangThai(phong.getTrangThai());
        dto.setBiKhoa(phong.getBiKhoa() != null ? phong.getBiKhoa() : false);
        dto.setDaDuyet(phong.getDaDuyet() != null ? phong.getDaDuyet() : false);
        dto.setChinhSachHuy(
                phong.getChinhSachHuy() != null ? phong.getChinhSachHuy() : Phong.ChinhSachHuy.LINH_HOAT);
        dto.setPassPhong(phong.getPassPhong());

        // Map tienNghi JSON string từ database (nếu có)
        dto.setTienNghi(phong.getTienNghi());

        // Map hình ảnh nếu có
        if (hinhAnhList != null && !hinhAnhList.isEmpty()) {
            dto.setHinhAnhs(hinhAnhList.stream()
                    .map(h -> {
                        HinhAnhDTO hdto = new HinhAnhDTO();
                        hdto.setMaHinhAnh(h.getMaHinhAnh());
                        hdto.setUrlHinhAnh(h.getUrlHinhAnh());
                        hdto.setThuTu(h.getThuTu());
                        hdto.setLaAnhChinh(h.getLaAnhChinh());
                        hdto.setPhanLoaiAnh(h.getPhanLoaiAnh());
                        return hdto;
                    })
                    .collect(java.util.stream.Collectors.toList()));
        }
        // Map tiện nghi nếu có
        if (amenityList != null && !amenityList.isEmpty()) {
            dto.setTienNghiItems(amenityList.stream()
                    .map(a -> {
                        TienNghiDTO adto = new TienNghiDTO();
                        adto.setNhom(a.getNhom());
                        adto.setTen(a.getTen());
                        adto.setDuocCungCap(a.getDuocCungCap() != null ? a.getDuocCungCap() : true);
                        adto.setMoTa(a.getMoTa());
                        return adto;
                    })
                    .collect(Collectors.toList()));
        }

        if (phong.getChuNha() != null) {
            HostInfo info = new HostInfo();
            info.setMaNguoiDung(phong.getChuNha().getMaNguoiDung());
            String displayName = "";
            // Prioritize ho + ten
            if (phong.getChuNha().getHo() != null && !phong.getChuNha().getHo().isEmpty()
                    && phong.getChuNha().getTen() != null && !phong.getChuNha().getTen().isEmpty()) {
                displayName = (phong.getChuNha().getHo() + " " + phong.getChuNha().getTen()).trim();
            }
            // Fallback to hoTen
            else if (phong.getChuNha().getHoTen() != null && !phong.getChuNha().getHoTen().isEmpty()) {
                displayName = phong.getChuNha().getHoTen();
            }

            // Fallback to email
            if (displayName.isEmpty()) {
                String email = phong.getChuNha().getEmail();
                if (email != null && email.contains("@")) {
                    displayName = email.substring(0, email.indexOf("@"));
                } else {
                    displayName = "Chủ nhà";
                }
            }
            info.setHoTen(displayName);
            info.setAvatarUrl(phong.getChuNha().getUrlAnhDaiDien());
            info.setCongViec(phong.getChuNha().getCongViec());
            info.setThanhPho(phong.getChuNha().getThanhPhoSong());
            info.setSoLuongDanhGia(
                    phong.getChuNha().getSoLuongDanhGiaHost() != null ? phong.getChuNha().getSoLuongDanhGiaHost()
                            : 0);
            info.setDiemDanhGia(phong.getChuNha().getDiemDanhGiaHost());
            info.setXacMinhDanhTinh(phong.getChuNha().getXacMinhDanhTinh());
            // Calculate years of experience (mock or real)
            info.setSoNamKinhNghiem(java.time.Period
                    .between(phong.getChuNha().getNgayTao().toLocalDate(), java.time.LocalDate.now()).getYears());

            dto.setHostInfo(info);
        }

        return dto;
    }

    /**
     * Filter tienNghi JSON string: remove hidden groups and hidden individual amenities.
     */
    public static String filterTienNghi(String tienNghiJson, Set<String> activeGroupNames, Set<String> hiddenCatalogNames) {
        if (tienNghiJson == null || tienNghiJson.isBlank()) return tienNghiJson;
        if (activeGroupNames == null || activeGroupNames.isEmpty()) return tienNghiJson;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = mapper.readValue(tienNghiJson, Map.class);
            Map<String, Object> filtered = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                String groupName = entry.getKey();
                if (!activeGroupNames.contains(groupName)) continue;
                if (entry.getValue() instanceof List<?> items) {
                    List<String> filteredItems = items.stream()
                            .filter(item -> item instanceof String && !hiddenCatalogNames.contains((String) item))
                            .map(item -> (String) item)
                            .collect(Collectors.toList());
                    if (!filteredItems.isEmpty()) {
                        filtered.put(groupName, filteredItems);
                    }
                }
            }
            return mapper.writeValueAsString(filtered);
        } catch (Exception e) {
            return tienNghiJson;
        }
    }

    @Data
    @Getter
    @Setter
    public static class TienNghiDTO {
        private String nhom;
        private String ten;
        private Boolean duocCungCap;
        private String moTa;
    }
}
