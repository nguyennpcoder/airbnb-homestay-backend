package com.project.airbnb.repository;

import com.project.airbnb.entity.TraLoiNhanh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TraLoiNhanhRepository extends JpaRepository<TraLoiNhanh, Long> {

    List<TraLoiNhanh> findByChuNha_MaNguoiDungOrderByPhimTatAsc(Long maChuNha);

    @Query("SELECT t FROM TraLoiNhanh t WHERE t.chuNha.maNguoiDung = :maChuNha ORDER BY FUNCTION('RAND') LIMIT 1")
    TraLoiNhanh findRandomByChuNha(@Param("maChuNha") Long maChuNha);
}
