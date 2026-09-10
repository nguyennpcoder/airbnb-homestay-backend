package com.project.airbnb.repository;

import com.project.airbnb.entity.DanhSachYeuThich;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DanhSachYeuThichRepository extends JpaRepository<DanhSachYeuThich, Long> {
    @Query("SELECT dsyt FROM DanhSachYeuThich dsyt JOIN FETCH dsyt.phong WHERE dsyt.nguoiDung.maNguoiDung = :maNguoiDung")
    List<DanhSachYeuThich> findByNguoiDung_MaNguoiDung(@Param("maNguoiDung") Long maNguoiDung);

    Optional<DanhSachYeuThich> findByNguoiDung_MaNguoiDungAndPhong_MaPhong(Long maNguoiDung, Long maPhong);
    Boolean existsByNguoiDung_MaNguoiDungAndPhong_MaPhong(Long maNguoiDung, Long maPhong);
    long countByNguoiDung_MaNguoiDung(Long maNguoiDung);
}
