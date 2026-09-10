package com.project.airbnb.controller;

import com.project.airbnb.repository.NguoiDungRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/users")

public class UserController {

    private final NguoiDungRepository nguoiDungRepository;

    public UserController(NguoiDungRepository nguoiDungRepository) {
        this.nguoiDungRepository = nguoiDungRepository;
    }

    @Value("${app.backend.url:http://localhost:8089}")
    private String backendUrl;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUserProfile(@PathVariable Long id) {
        return nguoiDungRepository.findById(id)
                .map(nguoiDung -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("maNguoiDung", nguoiDung.getMaNguoiDung());
                    response.put("hoTen", nguoiDung.getHoTen());
                    response.put("ho", nguoiDung.getHo());
                    response.put("ten", nguoiDung.getTen());
                    response.put("ngaySinh", nguoiDung.getNgaySinh());
                    response.put("email", nguoiDung.getEmail());
                    response.put("soDienThoai", nguoiDung.getSoDienThoai());
                    response.put("urlAnhDaiDien", nguoiDung.getUrlAnhDaiDien());
                    response.put("laChuNha", nguoiDung.getLaChuNha());
                    response.put("laAdmin", nguoiDung.getLaAdmin());
                    response.put("emailDaXacNhan", nguoiDung.getEmailDaXacNhan());
                    response.put("daChapNhanCamKetCongDong", nguoiDung.getDaChapNhanCamKetCongDong());
                    response.put("nhanTinNhanTiepThi", nguoiDung.getNhanTinNhanTiepThi());
                    response.put("ngayTao", nguoiDung.getNgayTao());
                    response.put("ngayCapNhat", nguoiDung.getNgayCapNhat());
                    response.put("soDu", nguoiDung.getSoDu());
                    response.put("xacMinhDanhTinh", nguoiDung.getXacMinhDanhTinh());
                    response.put("urlBangChungDanhTinh", nguoiDung.getUrlBangChungDanhTinh());
                    response.put("congViec", nguoiDung.getCongViec());
                    response.put("thanhPhoSong", nguoiDung.getThanhPhoSong());
                    response.put("biKhoa", nguoiDung.getBiKhoa());
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/{id}/avatar", consumes = { "multipart/form-data" })
    public ResponseEntity<Map<String, Object>> uploadAvatar(@PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return nguoiDungRepository.findById(id)
                .map(user -> {
                    try {
                        if (file == null || file.isEmpty()) {
                            Map<String, Object> err = new HashMap<>();
                            err.put("message", "No file uploaded");
                            return ResponseEntity.badRequest().body(err);
                        }

                        // Ensure directory exists
                        java.nio.file.Path avatarDir = java.nio.file.Paths.get("uploads", "@avatar");
                        java.nio.file.Files.createDirectories(avatarDir);

                        // Use original extension if present
                        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "avatar";
                        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : ".jpg";
                        String filename = id + "-" + System.currentTimeMillis() + ext.toLowerCase();
                        java.nio.file.Path dest = avatarDir.resolve(filename);

                        // Save the uploaded file (robust NIO copy)
                        try (java.io.InputStream in = file.getInputStream()) {
                            java.nio.file.Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                        }

                        // Build public URL and persist
                        String url = backendUrl + "/uploads/@avatar/" + filename;

                        // Optionally: remove old avatar if it was previously saved in our uploads
                        // folder
                        String old = user.getUrlAnhDaiDien();
                        if (old != null && old.contains("/uploads/@avatar/")) {
                            try {
                                String oldName = old.substring(old.lastIndexOf('/') + 1);
                                java.nio.file.Path oldPath = avatarDir.resolve(oldName);
                                java.nio.file.Files.deleteIfExists(oldPath);
                            } catch (Exception ignore) {
                            }
                        }

                        user.setUrlAnhDaiDien(url);
                        nguoiDungRepository.saveAndFlush(user);

                        Map<String, Object> resp = new HashMap<>();
                        resp.put("url", url);
                        resp.put("maNguoiDung", user.getMaNguoiDung());
                        return ResponseEntity.ok(resp);
                    } catch (Exception e) {
                        log.error("Avatar upload failed for user {}: {}", id, e.getMessage(), e);
                        Map<String, Object> err = new HashMap<>();
                        err.put("message", "Upload failed");
                        err.put("error", e.getMessage());
                        return ResponseEntity.status(500).body(err);
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/{id}/kyc", consumes = { "multipart/form-data" })
    public ResponseEntity<Map<String, Object>> uploadKyc(@PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return nguoiDungRepository.findById(id)
                .map(user -> {
                    try {
                        if (file == null || file.isEmpty()) {
                            Map<String, Object> err = new HashMap<>();
                            err.put("message", "No file uploaded");
                            return ResponseEntity.badRequest().body(err);
                        }

                        java.nio.file.Path kycDir = java.nio.file.Paths.get("uploads", "kyc");
                        java.nio.file.Files.createDirectories(kycDir);

                        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "kyc";
                        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : ".jpg";
                        String filename = "kyc-" + id + "-" + System.currentTimeMillis() + ext.toLowerCase();
                        java.nio.file.Path dest = kycDir.resolve(filename);

                        try (java.io.InputStream in = file.getInputStream()) {
                            java.nio.file.Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                        }

                        String url = backendUrl + "/uploads/kyc/" + filename;

                        String old = user.getUrlBangChungDanhTinh();
                        if (old != null && old.contains("/uploads/kyc/")) {
                            try {
                                String oldName = old.substring(old.lastIndexOf('/') + 1);
                                java.nio.file.Path oldPath = kycDir.resolve(oldName);
                                java.nio.file.Files.deleteIfExists(oldPath);
                            } catch (Exception ignore) {
                            }
                        }

                        user.setUrlBangChungDanhTinh(url);
                        user.setXacMinhDanhTinh(true);
                        nguoiDungRepository.saveAndFlush(user);

                        Map<String, Object> resp = new HashMap<>();
                        resp.put("url", url);
                        resp.put("xacMinhDanhTinh", true);
                        return ResponseEntity.ok(resp);
                    } catch (Exception e) {
                        log.error("KYC upload failed for user {}: {}", id, e.getMessage(), e);
                        Map<String, Object> err = new HashMap<>();
                        err.put("message", "Upload failed");
                        err.put("error", e.getMessage());
                        return ResponseEntity.status(500).body(err);
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProfile(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        return nguoiDungRepository.findById(id)
                .map(u -> {
                    if (payload.containsKey("ho"))
                        u.setHo(String.valueOf(payload.get("ho")));
                    if (payload.containsKey("ten"))
                        u.setTen(String.valueOf(payload.get("ten")));
                    if (payload.containsKey("hoTen"))
                        u.setHoTen(String.valueOf(payload.get("hoTen")));
                    if (payload.containsKey("ngaySinh")) {
                        try {
                            String d = String.valueOf(payload.get("ngaySinh"));
                            if (d != null && !d.isEmpty()) {
                                u.setNgaySinh(java.time.LocalDate.parse(d));
                            }
                        } catch (Exception ignore) {
                        }
                    }
                    if (payload.containsKey("soDienThoai"))
                        u.setSoDienThoai(String.valueOf(payload.get("soDienThoai")));
                    if (payload.containsKey("laChuNha"))
                        u.setLaChuNha(Boolean.parseBoolean(String.valueOf(payload.get("laChuNha"))));
                    if (payload.containsKey("nhanTinNhanTiepThi"))
                        u.setNhanTinNhanTiepThi(
                                Boolean.parseBoolean(String.valueOf(payload.get("nhanTinNhanTiepThi"))));
                    if (payload.containsKey("daChapNhanCamKetCongDong"))
                        u.setDaChapNhanCamKetCongDong(
                                Boolean.parseBoolean(String.valueOf(payload.get("daChapNhanCamKetCongDong"))));
                    if (payload.containsKey("xacMinhDanhTinh"))
                        u.setXacMinhDanhTinh(
                                Boolean.parseBoolean(String.valueOf(payload.get("xacMinhDanhTinh"))));
                    if (payload.containsKey("urlBangChungDanhTinh"))
                        u.setUrlBangChungDanhTinh(String.valueOf(payload.get("urlBangChungDanhTinh")));
                    if (payload.containsKey("congViec"))
                        u.setCongViec(String.valueOf(payload.get("congViec")));
                    if (payload.containsKey("thanhPhoSong"))
                        u.setThanhPhoSong(String.valueOf(payload.get("thanhPhoSong")));
                    // Recompute hoTen if missing
                    if ((u.getHoTen() == null || u.getHoTen().isEmpty()) && (u.getHo() != null || u.getTen() != null)) {
                        String ho = u.getHo() != null ? u.getHo() : "";
                        String ten = u.getTen() != null ? u.getTen() : "";
                        u.setHoTen((ho + " " + ten).trim());
                    }
                    nguoiDungRepository.save(u);
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("maNguoiDung", u.getMaNguoiDung());
                    resp.put("ho", u.getHo());
                    resp.put("ten", u.getTen());
                    resp.put("hoTen", u.getHoTen());
                    resp.put("ngaySinh", u.getNgaySinh());
                    resp.put("email", u.getEmail());
                    resp.put("soDienThoai", u.getSoDienThoai());
                    resp.put("laChuNha", u.getLaChuNha());
                    resp.put("laAdmin", u.getLaAdmin());
                    resp.put("nhanTinNhanTiepThi", u.getNhanTinNhanTiepThi());
                    resp.put("daChapNhanCamKetCongDong", u.getDaChapNhanCamKetCongDong());
                    resp.put("xacMinhDanhTinh", u.getXacMinhDanhTinh());
                    resp.put("urlBangChungDanhTinh", u.getUrlBangChungDanhTinh());
                    resp.put("congViec", u.getCongViec());
                    resp.put("thanhPhoSong", u.getThanhPhoSong());
                    return ResponseEntity.ok(resp);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
