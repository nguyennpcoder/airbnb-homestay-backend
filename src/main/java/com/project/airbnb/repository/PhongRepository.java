package com.project.airbnb.repository;

import com.project.airbnb.entity.Phong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PhongRepository extends JpaRepository<Phong, Long>, JpaSpecificationExecutor<Phong> {
        List<Phong> findByLoaiPhong(Phong.LoaiPhong loaiPhong);

        @Query("SELECT DISTINCT s FROM Phong s JOIN FETCH s.chuNha LEFT JOIN FETCH s.hinhAnhs WHERE s.chuNha.maNguoiDung = :maChuNha AND s.trangThai = 'hoat_dong' AND (s.biKhoa IS NULL OR s.biKhoa = 0)")
        List<Phong> findByChuNha_MaNguoiDungAndNotLocked(@Param("maChuNha") Long maChuNha);

        @Query("SELECT s FROM Phong s JOIN FETCH s.chuNha WHERE s.chuNha.maNguoiDung = :maChuNha")
        List<Phong> findByChuNha_MaNguoiDung(@Param("maChuNha") Long maChuNha);

        List<Phong> findByThanhPho(String thanhPho);

        List<Phong> findByThanhPhoAndLoaiPhong(String thanhPho, Phong.LoaiPhong loaiPhong);

        @Query("SELECT s FROM Phong s WHERE " +
                        "(:minPrice IS NULL OR s.giaMoiKhach >= :minPrice) AND " +
                        "(:maxPrice IS NULL OR s.giaMoiKhach <= :maxPrice) AND " +
                        "(:soPhongNgu IS NULL OR s.soPhongNgu >= :soPhongNgu) AND " +
                        "(:soPhongTam IS NULL OR s.soPhongTam >= :soPhongTam) AND " +
                        "(s.biKhoa IS NULL OR s.biKhoa = 0)")
        List<Phong> search(@Param("minPrice") BigDecimal minPrice,
                        @Param("maxPrice") BigDecimal maxPrice,
                        @Param("soPhongNgu") Integer soPhongNgu,
                        @Param("soPhongTam") Integer soPhongTam);

        @Query("SELECT s FROM Phong s WHERE s.trangThai = com.project.airbnb.entity.Phong$TrangThaiPhong.hoat_dong AND s.loaiPhong = :loai AND (s.biKhoa IS NULL OR s.biKhoa = 0) ORDER BY s.diemTrungBinh DESC")
        List<Phong> findTopRatedByLoai(@Param("loai") Phong.LoaiPhong loai);

        @Query("SELECT s FROM Phong s WHERE s.trangThai = com.project.airbnb.entity.Phong$TrangThaiPhong.hoat_dong AND s.duocKhachYeuThich = 1 AND (s.biKhoa IS NULL OR s.biKhoa = 0) ORDER BY s.diemTrungBinh DESC")
        List<Phong> findLovedByGuests();

        @org.springframework.data.jpa.repository.Modifying
        @org.springframework.transaction.annotation.Transactional
        @Query("UPDATE Phong s SET s.soLuotXem = COALESCE(s.soLuotXem, 0) + 1 WHERE s.maPhong = :id")
        void incrementViews(@Param("id") Long id);

        @Query("SELECT SUM(s.soLuotXem) FROM Phong s WHERE s.chuNha.maNguoiDung = :hostId")
        Long sumViewsByHost(@Param("hostId") Long hostId);

        @Query("SELECT new map(s.loaiPhong as type, COUNT(s) as count) FROM Phong s GROUP BY s.loaiPhong")
        java.util.List<java.util.Map<String, Object>> getCategoryStats();

        @Query("SELECT DISTINCT s FROM Phong s LEFT JOIN FETCH s.chuNha LEFT JOIN FETCH s.hinhAnhs ORDER BY s.maPhong ASC")
        List<Phong> findAllWithImagesAndHost();

        long countByTrangThai(Phong.TrangThaiPhong trangThai);

        long countByBiKhoaTrue();

        long countByChuNha_MaNguoiDung(Long maChuNha);

    @Query("SELECT p FROM Phong p JOIN FETCH p.chuNha WHERE p.maPhong = :maPhong")
    Optional<Phong> findByIdWithChuNha(@Param("maPhong") Long maPhong);

    @Query("SELECT p FROM Phong p WHERE p.trangThai = com.project.airbnb.entity.Phong$TrangThaiPhong.cho_duyet AND (p.biKhoa IS NULL OR p.biKhoa = 0) AND p.ngayCapNhat < :threshold")
    List<Phong> findPendingOlderThan(@Param("threshold") LocalDateTime threshold);
}
