package com.project.airbnb.repository;

import com.project.airbnb.entity.DanhGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DanhGiaRepository extends JpaRepository<DanhGia, Long> {
    List<DanhGia> findByPhong_MaPhong(Long maPhong);

    List<DanhGia> findByPhong_MaPhongAndLoaiPhong(Long maPhong, com.project.airbnb.entity.Phong.LoaiPhong loaiPhong);

    @Query("SELECT AVG(d.diemSo) FROM DanhGia d WHERE d.phong.chuNha.maNguoiDung = :hostId")
    Double getAverageRatingByHost(Long hostId);

    List<DanhGia> findByPhong_ChuNha_MaNguoiDung(Long hostId);

    Long countByPhong_ChuNha_MaNguoiDung(Long hostId);

    List<DanhGia> findByKhach_MaNguoiDung(Long maKhach);

    @Query("SELECT d FROM DanhGia d LEFT JOIN FETCH d.phong LEFT JOIN FETCH d.khach")
    List<DanhGia> findAllWithDetails();
}
