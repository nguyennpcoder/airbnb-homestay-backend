package com.project.airbnb.repository;

import com.project.airbnb.entity.DanhGiaHuuIch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DanhGiaHuuIchRepository extends JpaRepository<DanhGiaHuuIch, Long> {
    Optional<DanhGiaHuuIch> findByDanhGia_MaDanhGiaAndNguoiDung_MaNguoiDung(Long maDanhGia, Long maNguoiDung);
    boolean existsByDanhGia_MaDanhGiaAndNguoiDung_MaNguoiDung(Long maDanhGia, Long maNguoiDung);
    long countByDanhGia_MaDanhGia(Long maDanhGia);
}
