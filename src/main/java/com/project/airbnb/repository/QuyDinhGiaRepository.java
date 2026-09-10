package com.project.airbnb.repository;

import com.project.airbnb.entity.QuyDinhGia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuyDinhGiaRepository extends JpaRepository<QuyDinhGia, Long> {

    Optional<QuyDinhGia> findFirstByOrderByMaQuyDinhAsc();
}
