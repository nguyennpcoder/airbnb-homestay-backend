package com.project.airbnb.repository;

import com.project.airbnb.entity.KhuyenMai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface KhuyenMaiRepository extends JpaRepository<KhuyenMai, Long> {

    List<KhuyenMai> findAllByOrderByNgayTaoDesc();

    List<KhuyenMai> findByHoatDongTrueOrderByNgayTaoDesc();

    @Query("SELECT k FROM KhuyenMai k WHERE k.hoatDong = true " +
           "AND (k.dieuKienNgayKetThuc IS NULL OR k.dieuKienNgayKetThuc >= :today) " +
           "AND (k.soLuongToiDa = 0 OR k.soLuongDaDung < k.soLuongToiDa) " +
           "ORDER BY k.ngayTao DESC")
    List<KhuyenMai> findAvailable(@Param("today") LocalDate today);
}
