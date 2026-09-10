package com.project.airbnb.repository;

import com.project.airbnb.entity.NhomTienNghi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NhomTienNghiRepository extends JpaRepository<NhomTienNghi, Long> {
    List<NhomTienNghi> findAllByOrderByThuTuAsc();
    List<NhomTienNghi> findByChuNha_MaNguoiDungOrderByThuTuAsc(Long maChuNha);
    boolean existsByThuTuAndMaNhomTienNghiNot(Integer thuTu, Long maNhomTienNghi);
    boolean existsByThuTu(Integer thuTu);

    @Query("SELECT n.tenNhom FROM NhomTienNghi n WHERE n.hoatDong = 1")
    List<String> findActiveGroupNames();

    @Query("SELECT n.tenNhom FROM NhomTienNghi n WHERE n.hoatDong = 1 AND n.chuNha.maNguoiDung = :maChuNha")
    List<String> findActiveGroupNamesByHost(Long maChuNha);
}
