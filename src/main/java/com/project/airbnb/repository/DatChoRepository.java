package com.project.airbnb.repository;

import com.project.airbnb.entity.DatCho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatChoRepository extends JpaRepository<DatCho, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "phong", "khach" })
    List<DatCho> findByKhach_MaNguoiDung(Long maKhach);

    List<DatCho> findByPhong_MaPhong(Long maPhong);

    // Find all bookings for listings owned by a specific host
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "phong", "khach", "phong.chuNha" })
    List<DatCho> findByPhong_ChuNha_MaNguoiDung(Long maNguoiDung);

    List<DatCho> findByKhach_MaNguoiDungAndTrangThaiDatCho(Long maKhach, DatCho.TrangThaiDatCho trangThai);

    List<DatCho> findByKhuyenMai_MaKhuyenMaiAndTrangThaiDatCho(Long maKhuyenMai, DatCho.TrangThaiDatCho trangThai);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(d.tongTien) FROM DatCho d WHERE d.trangThaiDatCho = 'hoan_thanh' OR d.trangThaiDatCho = 'da_xac_nhan'")
    Double sumTotalRevenue();

    @org.springframework.data.jpa.repository.Query("SELECT new map(extract(month from d.ngayTao) as month, SUM(d.tongTien) as revenue) FROM DatCho d WHERE extract(year from d.ngayTao) = :year AND (d.trangThaiDatCho = 'hoan_thanh' OR d.trangThaiDatCho = 'da_xac_nhan') GROUP BY extract(month from d.ngayTao)")
    List<java.util.Map<String, Object>> getMonthlyRevenue(int year);

    @org.springframework.data.jpa.repository.Query("SELECT new map(d.trangThaiDatCho as status, COUNT(d) as count) FROM DatCho d GROUP BY d.trangThaiDatCho")
    List<java.util.Map<String, Object>> countBookingsByStatus();

    @org.springframework.data.jpa.repository.Query("SELECT SUM(d.tongTien) FROM DatCho d WHERE d.phong.chuNha.maNguoiDung = :hostId AND (d.trangThaiDatCho = 'hoan_thanh' OR d.trangThaiDatCho = 'da_xac_nhan')")
    Double sumRevenueByHost(Long hostId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(d) FROM DatCho d WHERE d.phong.chuNha.maNguoiDung = :hostId")
    Long countBookingsByHost(Long hostId);

    @org.springframework.data.jpa.repository.Query("SELECT new map(extract(month from d.ngayTao) as month, SUM(d.tongTien) as revenue) FROM DatCho d WHERE d.phong.chuNha.maNguoiDung = :hostId AND extract(year from d.ngayTao) = :year AND (d.trangThaiDatCho = 'hoan_thanh' OR d.trangThaiDatCho = 'da_xac_nhan') GROUP BY extract(month from d.ngayTao)")
    List<java.util.Map<String, Object>> getMonthlyRevenueByHost(Long hostId, int year);

    Long countByPhong_ChuNha_MaNguoiDungAndTrangThaiDatCho(Long hostId, DatCho.TrangThaiDatCho status);

    /** Pending real bookings only — excludes pre-booking contact inquiries. */
    @org.springframework.data.jpa.repository.Query("""
            SELECT COUNT(d) FROM DatCho d
            WHERE d.phong.chuNha.maNguoiDung = :hostId
              AND d.trangThaiDatCho = 'cho_xac_nhan'
              AND (d.yeuCauDacBiet IS NULL OR d.yeuCauDacBiet <> 'LIEN_HE_PHONG')
              AND (d.tongTien IS NULL OR d.tongTien > 0 OR d.ngayNhanPhong IS NOT NULL)
            """)
    Long countPendingRealBookingsByHost(@org.springframework.data.repository.query.Param("hostId") Long hostId);

    @org.springframework.data.jpa.repository.Query("SELECT d FROM DatCho d WHERE d.phong.chuNha.maNguoiDung = :hostId AND extract(month from d.ngayNhanPhong) = :month AND extract(year from d.ngayNhanPhong) = :year")
    List<DatCho> findBookingsByHostAndMonth(@org.springframework.data.repository.query.Param("hostId") Long hostId,
            @org.springframework.data.repository.query.Param("month") int month,
            @org.springframework.data.repository.query.Param("year") int year);

    @org.springframework.data.jpa.repository.Query("SELECT new map(extract(month from d.ngayTao) as month, COUNT(d) as count) FROM DatCho d WHERE extract(year from d.ngayTao) = :year AND d.trangThaiDatCho = 'da_huy' GROUP BY extract(month from d.ngayTao)")
    List<java.util.Map<String, Object>> getMonthlyCancellations(int year);

    @org.springframework.data.jpa.repository.Query("SELECT new map(extract(month from d.ngayNhanPhong) as month, SUM(function('date_part', 'day', d.ngayTraPhong - d.ngayNhanPhong)) as bookedNights) FROM DatCho d WHERE extract(year from d.ngayNhanPhong) = :year AND (d.trangThaiDatCho = 'hoan_thanh' OR d.trangThaiDatCho = 'da_xac_nhan') GROUP BY extract(month from d.ngayNhanPhong)")
    List<java.util.Map<String, Object>> getMonthlyBookedNights(int year);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"phong", "khach"})
    java.util.List<DatCho> findTop5ByOrderByNgayTaoDesc();

    @org.springframework.data.jpa.repository.Query("SELECT new map(extract(month from d.ngayTao) as month, COUNT(d) as count) FROM DatCho d WHERE extract(year from d.ngayTao) = :year GROUP BY extract(month from d.ngayTao)")
    List<java.util.Map<String, Object>> getMonthlyBookingCounts(int year);

    long countByKhach_MaNguoiDung(Long maKhach);

    List<DatCho> findByTrangThaiDatCho(DatCho.TrangThaiDatCho trangThaiDatCho);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "khach", "phong", "phong.chuNha" })
    @Override
    java.util.List<DatCho> findAll();

    @org.springframework.data.jpa.repository.Query("""
            SELECT DISTINCT d.phong.maPhong FROM DatCho d
            WHERE d.phong.maPhong IN :phongIds
              AND d.ngayNhanPhong IS NOT NULL AND d.ngayTraPhong IS NOT NULL
              AND d.ngayNhanPhong < :ngayTra AND d.ngayTraPhong > :ngayNhan
              AND d.trangThaiDatCho IN ('cho_xac_nhan', 'da_xac_nhan')
            """)
    List<Long> findBookedPhongIdsInDateRange(
            @org.springframework.data.repository.query.Param("phongIds") List<Long> phongIds,
            @org.springframework.data.repository.query.Param("ngayNhan") java.time.LocalDate ngayNhan,
            @org.springframework.data.repository.query.Param("ngayTra") java.time.LocalDate ngayTra);
}
