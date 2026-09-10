package com.project.airbnb.repository;

import com.project.airbnb.entity.ChinhSachHoanTien;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChinhSachHoanTienRepository extends JpaRepository<ChinhSachHoanTien, Long> {

    List<ChinhSachHoanTien> findAllByOrderByThuTuAsc();

    Optional<ChinhSachHoanTien> findByMa(String ma);
}
