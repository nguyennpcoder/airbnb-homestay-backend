package com.project.airbnb.repository;

import com.project.airbnb.entity.HinhAnh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HinhAnhRepository extends JpaRepository<HinhAnh, Long> {
    List<HinhAnh> findByPhong_MaPhongOrderByThuTuAsc(Long maPhong);

    @Query("SELECT h FROM HinhAnh h JOIN FETCH h.phong p JOIN FETCH p.chuNha WHERE h.maHinhAnh = :maHinhAnh")
    Optional<HinhAnh> findByIdWithPhongAndChuNha(@Param("maHinhAnh") Long maHinhAnh);
}
