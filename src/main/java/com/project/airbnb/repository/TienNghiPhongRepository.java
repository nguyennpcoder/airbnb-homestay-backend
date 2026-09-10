package com.project.airbnb.repository;

import com.project.airbnb.entity.TienNghiPhong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TienNghiPhongRepository extends JpaRepository<TienNghiPhong, Long> {
    List<TienNghiPhong> findByPhong_MaPhongOrderByNhomAscTenAsc(Long maPhong);
}
