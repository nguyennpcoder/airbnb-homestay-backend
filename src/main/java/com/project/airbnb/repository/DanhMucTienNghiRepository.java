package com.project.airbnb.repository;

import com.project.airbnb.entity.DanhMucTienNghi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DanhMucTienNghiRepository extends JpaRepository<DanhMucTienNghi, Long> {

    @Query("SELECT d FROM DanhMucTienNghi d LEFT JOIN FETCH d.nhom")
    List<DanhMucTienNghi> findAllWithNhom();

    @Query("SELECT d FROM DanhMucTienNghi d LEFT JOIN FETCH d.nhom WHERE d.nhom.maNhomTienNghi = :groupId ORDER BY d.ten ASC")
    List<DanhMucTienNghi> findAllByGroupIdWithNhom(Long groupId);

    @Query("SELECT d FROM DanhMucTienNghi d LEFT JOIN FETCH d.nhom WHERE d.hoatDong = 1 ORDER BY d.ten ASC")
    List<DanhMucTienNghi> findAllActiveWithNhom();

    @Query("SELECT d FROM DanhMucTienNghi d LEFT JOIN FETCH d.nhom WHERE d.hoatDong = 1 AND d.nhom.maNhomTienNghi = :groupId ORDER BY d.ten ASC")
    List<DanhMucTienNghi> findAllActiveByGroupIdWithNhom(Long groupId);

    List<DanhMucTienNghi> findByNhom_MaNhomTienNghi(Long maNhomTienNghi);

    Optional<DanhMucTienNghi> findById(Long id);

    @Query("SELECT d.ten FROM DanhMucTienNghi d WHERE d.hoatDong = 0")
    List<String> findHiddenCatalogNames();

    // Per-host queries
    List<DanhMucTienNghi> findByChuNha_MaNguoiDungOrderByNhomAscTenAsc(Long maChuNha);

    @Query("SELECT d FROM DanhMucTienNghi d JOIN FETCH d.nhom WHERE d.chuNha.maNguoiDung = :maChuNha ORDER BY d.ten ASC")
    List<DanhMucTienNghi> findAllByHostWithNhom(Long maChuNha);

    @Query("SELECT d FROM DanhMucTienNghi d JOIN FETCH d.nhom WHERE d.chuNha.maNguoiDung = :maChuNha AND d.hoatDong = 1 ORDER BY d.ten ASC")
    List<DanhMucTienNghi> findAllActiveByHostWithNhom(Long maChuNha);

    @Query("SELECT d FROM DanhMucTienNghi d JOIN FETCH d.nhom WHERE d.chuNha.maNguoiDung = :maChuNha AND d.nhom.maNhomTienNghi = :groupId ORDER BY d.ten ASC")
    List<DanhMucTienNghi> findAllByHostAndGroupWithNhom(Long maChuNha, Long groupId);

    @Query("SELECT d.ten FROM DanhMucTienNghi d WHERE d.hoatDong = 0 AND d.chuNha.maNguoiDung = :maChuNha")
    List<String> findHiddenCatalogNamesByHost(Long maChuNha);
}
