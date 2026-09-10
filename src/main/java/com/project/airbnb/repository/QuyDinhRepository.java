package com.project.airbnb.repository;

import com.project.airbnb.entity.QuyDinh;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuyDinhRepository extends JpaRepository<QuyDinh, Long> {

    List<QuyDinh> findAllByOrderByThuTuAsc();

    List<QuyDinh> findByActiveTrueOrderByThuTuAsc();

    List<QuyDinh> findByActiveTrueAndNhomOrderByThuTuAsc(String nhom);

    List<QuyDinh> findByActiveTrueAndDoiTuongInOrderByThuTuAsc(List<String> doiTuong);

    List<QuyDinh> findByActiveTrueAndNhomAndDoiTuongInOrderByThuTuAsc(String nhom, List<String> doiTuong);

    Optional<QuyDinh> findByMa(String ma);
}
