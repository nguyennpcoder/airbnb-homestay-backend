package com.project.airbnb.repository;

import com.project.airbnb.entity.LichTrinh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LichTrinhRepository extends JpaRepository<LichTrinh, Long> {
    List<LichTrinh> findByPhong_MaPhong(Long maPhong);
    Optional<LichTrinh> findByPhong_MaPhongAndNgay(Long maPhong, LocalDate ngay);
    List<LichTrinh> findByPhong_MaPhongAndNgayBetween(Long maPhong, LocalDate ngayBatDau, LocalDate ngayKetThuc);

    @Query("""
            SELECT DISTINCT l.phong.maPhong
            FROM LichTrinh l
WHERE l.phong.maPhong IN :phongIds
              AND l.ngay >= :startDate
              AND l.ngay <= :endDate
              AND (l.conKhaDung IS NULL OR l.conKhaDung = 0)
            """)
    List<Long> findUnavailablePhongIds(
            @Param("phongIds") List<Long> phongIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
