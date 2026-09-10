package com.project.airbnb.repository;

import com.project.airbnb.entity.ThongBao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThongBaoRepository extends JpaRepository<ThongBao, Long> {
    @Query("SELECT t FROM ThongBao t " +
            "LEFT JOIN FETCH t.nguoiGui " +
            "LEFT JOIN FETCH t.nguoiNhan " +
            "LEFT JOIN FETCH t.datCho d " +
            "LEFT JOIN FETCH d.phong " +
            "WHERE t.nguoiNhan.maNguoiDung = :userId " +
            "ORDER BY t.ngayTao DESC")
    List<ThongBao> findByNguoiNhan_MaNguoiDungOrderByNgayTaoDesc(@Param("userId") Long maNguoiDung);

    long countByNguoiNhan_MaNguoiDungAndDaDocFalse(Long maNguoiDung);

    long countByNguoiNhan_MaNguoiDungAndLoaiThongBaoAndTrangThai(Long maNguoiDung, String loaiThongBao, String trangThai);

    boolean existsByDatCho_MaDatChoAndNguoiGui_MaNguoiDungAndLoaiThongBao(Long maDatCho, Long maNguoiGui, String loaiThongBao);

    boolean existsByNguoiGui_MaNguoiDungAndLoaiThongBaoAndTrangThai(Long maNguoiGui, String loaiThongBao, String trangThai);

    @Query("SELECT t FROM ThongBao t " +
            "LEFT JOIN FETCH t.nguoiGui " +
            "LEFT JOIN FETCH t.nguoiNhan " +
            "LEFT JOIN FETCH t.datCho d " +
            "LEFT JOIN FETCH d.phong " +
            "WHERE t.id = :id")
    Optional<ThongBao> findById(@Param("id") Long id);
}
