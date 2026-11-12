package com.project.airbnb.repository;

import com.project.airbnb.entity.HinhAnh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface HinhAnhRepository extends JpaRepository<HinhAnh, Long> {
    List<HinhAnh> findByPhong_MaPhongOrderByThuTuAsc(Long maPhong);

    /**
     * Load images for many rooms in a single query.
     * The search endpoint used to run one image query per room (N+1), which
     * made the homepage take ~10s when the database was remote.
     */
    @Query("SELECT h FROM HinhAnh h JOIN FETCH h.phong p WHERE p.maPhong IN :roomIds " +
           "ORDER BY p.maPhong ASC, h.thuTu ASC")
    List<HinhAnh> findByPhongIds(@Param("roomIds") Collection<Long> roomIds);

    @Query("SELECT h FROM HinhAnh h JOIN FETCH h.phong p JOIN FETCH p.chuNha WHERE h.maHinhAnh = :maHinhAnh")
    Optional<HinhAnh> findByIdWithPhongAndChuNha(@Param("maHinhAnh") Long maHinhAnh);
}
