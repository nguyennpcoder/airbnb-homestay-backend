package com.project.airbnb.repository;

import com.project.airbnb.entity.NguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;

@Repository
public interface NguoiDungRepository extends JpaRepository<NguoiDung, Long> {
    Optional<NguoiDung> findByEmail(String email);

    Optional<NguoiDung> findBySoDienThoai(String soDienThoai);

    Boolean existsByEmail(String email);

    Boolean existsBySoDienThoai(String soDienThoai);

    @org.springframework.data.jpa.repository.Query("SELECT new map(extract(month from u.ngayTao) as month, COUNT(u) as count) " +
            "FROM NguoiDung u " +
            "WHERE u.ngayTao IS NOT NULL " +
            "  AND extract(year from u.ngayTao) = :year " +
            "GROUP BY extract(month from u.ngayTao)")
    java.util.List<java.util.Map<String, Object>> getMonthlyRegistrations(int year);

    List<NguoiDung> findTop5ByOrderByNgayTaoDesc();

    long countByBiKhoaFalse();

    long countByBiKhoaTrue();

    long countByLaChuNhaTrue();

    Optional<NguoiDung> findFirstByLaAdminTrue();

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = "UPDATE nguoi_dung SET so_du = COALESCE(so_du, 0) + :amount WHERE la_admin = 1", nativeQuery = true)
    int creditAdminWallet(@org.springframework.data.repository.query.Param("amount") java.math.BigDecimal amount);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = "UPDATE nguoi_dung SET so_du = COALESCE(so_du, 0) + :amount WHERE ma_nguoi_dung = :hostId", nativeQuery = true)
    int creditHostWallet(@org.springframework.data.repository.query.Param("hostId") Long hostId,
                         @org.springframework.data.repository.query.Param("amount") java.math.BigDecimal amount);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query(value = "UPDATE nguoi_dung SET so_du = GREATEST(COALESCE(so_du, 0) - :amount, 0) WHERE ma_nguoi_dung = :hostId", nativeQuery = true)
    int debitHostWallet(@org.springframework.data.repository.query.Param("hostId") Long hostId,
                        @org.springframework.data.repository.query.Param("amount") java.math.BigDecimal amount);
}
