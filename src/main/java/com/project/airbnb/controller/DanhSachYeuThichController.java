package com.project.airbnb.controller;

import com.project.airbnb.entity.DanhSachYeuThich;
import com.project.airbnb.entity.Phong;
import com.project.airbnb.repository.DanhSachYeuThichRepository;
import com.project.airbnb.repository.PhongRepository;
import com.project.airbnb.repository.NguoiDungRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/yeu-thich")
public class DanhSachYeuThichController {

    private final DanhSachYeuThichRepository danhSachYeuThichRepository;
    private final PhongRepository phongRepository;
    private final NguoiDungRepository nguoiDungRepository;

    public DanhSachYeuThichController(DanhSachYeuThichRepository danhSachYeuThichRepository,
            PhongRepository phongRepository, NguoiDungRepository nguoiDungRepository) {
        this.danhSachYeuThichRepository = danhSachYeuThichRepository;
        this.phongRepository = phongRepository;
        this.nguoiDungRepository = nguoiDungRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getYeuThichByUser(@RequestParam("userId") Long userId) {
        try {
            List<DanhSachYeuThich> list = danhSachYeuThichRepository.findByNguoiDung_MaNguoiDung(userId);
            List<Map<String, Object>> dtoList = list.stream()
                    .map(DanhSachYeuThich::getPhong)
                    .filter(sp -> sp != null)
                    .map(this::phongToMap)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtoList);
        } catch (Exception e) {
            System.err.println("Error in getYeuThichByUser for userId=" + userId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(List.of());
        }
    }

    @PostMapping("/toggle")
    public ResponseEntity<?> toggleYeuThich(@RequestParam("userId") Long userId,
            @RequestParam("phongId") Long phongId) {
        try {
            var existing = danhSachYeuThichRepository
                    .findByNguoiDung_MaNguoiDungAndPhong_MaPhong(userId, phongId);
            if (existing.isPresent()) {
                danhSachYeuThichRepository.delete(existing.get());
                return ResponseEntity.ok(Map.of("liked", false));
            }

            Phong sp = phongRepository.findById(phongId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

            com.project.airbnb.entity.NguoiDung user = nguoiDungRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            DanhSachYeuThich ds = new DanhSachYeuThich();
            ds.setPhong(sp);
            ds.setNguoiDung(user);
            ds.setLoaiPhong(sp.getLoaiPhong());
            danhSachYeuThichRepository.save(ds);
            return ResponseEntity.ok(Map.of("liked", true));
        } catch (Exception e) {
            System.err.println("Error in toggleYeuThich: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> countYeuThich(@RequestParam("userId") Long userId) {
        long count = danhSachYeuThichRepository.countByNguoiDung_MaNguoiDung(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkLiked(@RequestParam("userId") Long userId,
            @RequestParam("phongId") Long phongId) {
        boolean liked = Boolean.TRUE.equals(
                danhSachYeuThichRepository.existsByNguoiDung_MaNguoiDungAndPhong_MaPhong(userId, phongId));
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    @DeleteMapping
    public ResponseEntity<?> removeYeuThich(@RequestParam("userId") Long userId,
            @RequestParam("phongId") Long phongId) {
        var existing = danhSachYeuThichRepository
                .findByNguoiDung_MaNguoiDungAndPhong_MaPhong(userId, phongId);
        existing.ifPresent(danhSachYeuThichRepository::delete);
        return ResponseEntity.ok(Map.of("removed", true));
    }

    private Map<String, Object> phongToMap(Phong phong) {
        java.util.HashMap<String, Object> map = new java.util.HashMap<>();
        map.put("maPhong", phong.getMaPhong());
        map.put("tieuDe", phong.getTieuDe() != null ? phong.getTieuDe() : "");
        map.put("urlAnhChinh", phong.getUrlAnhChinh() != null ? phong.getUrlAnhChinh() : "");
        map.put("giaMoiKhach", phong.getGiaMoiKhach() != null ? phong.getGiaMoiKhach().doubleValue() : 0);
        map.put("diemTrungBinh", phong.getDiemTrungBinh() != null ? phong.getDiemTrungBinh().doubleValue() : 0);
        map.put("soLuongDanhGia", phong.getSoLuongDanhGia() != null ? phong.getSoLuongDanhGia() : 0);
        map.put("thanhPho", phong.getThanhPho() != null ? phong.getThanhPho() : "");
        map.put("quocGia", phong.getQuocGia() != null ? phong.getQuocGia() : "");
        map.put("viDo", phong.getViDo() != null ? phong.getViDo().doubleValue() : null);
        map.put("kinhDo", phong.getKinhDo() != null ? phong.getKinhDo().doubleValue() : null);
        map.put("laOriginal", phong.getLaOriginal() != null ? phong.getLaOriginal() : false);
        map.put("laPhoBien", phong.getLaPhoBien() != null ? phong.getLaPhoBien() : false);
        map.put("duocKhachYeuThich", phong.getDuocKhachYeuThich() != null ? phong.getDuocKhachYeuThich() : false);
        return map;
    }
}
