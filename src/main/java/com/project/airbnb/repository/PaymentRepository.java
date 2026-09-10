package com.project.airbnb.repository;

import com.project.airbnb.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
       List<Payment> findByBooking_MaDatCho(Long bookingId);

       Payment findByTransactionId(String transactionId);

       // Tính tổng doanh thu từ các payment đã thành công (toàn hệ thống)
       @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'DA_XAC_NHAN'")
       Double sumTotalRevenue();

       // Tính doanh thu theo tháng từ các payment đã thành công (toàn hệ thống)
       // Chỉ tính các payment có paymentDate (thời điểm thanh toán thành công)
       @Query("SELECT new map(extract(month from p.paymentDate) as month, SUM(p.amount) as revenue) " +
                      "FROM Payment p " +
                      "WHERE p.status = 'DA_XAC_NHAN' " +
                      "  AND p.paymentDate IS NOT NULL " +
                      "  AND extract(year from p.paymentDate) = :year " +
                      "GROUP BY extract(month from p.paymentDate)")
       List<Map<String, Object>> getMonthlyRevenue(int year);

       // Tìm các payment SUCCESS chưa có paymentDate để update
       @Query("SELECT p FROM Payment p WHERE p.status = 'DA_XAC_NHAN' AND p.paymentDate IS NULL")
       List<Payment> findSuccessfulPaymentsWithoutDate();

       // Đếm số lượng payment đã thành công
       @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'DA_XAC_NHAN'")
       Long countSuccessfulPayments();

       // ====== Doanh thu theo host (chủ nhà) ======

       // Tổng doanh thu của một host dựa trên các payment CONFIRMED
       @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
                     "WHERE p.status = 'DA_XAC_NHAN' " +
                     "  AND p.booking.phong.chuNha.maNguoiDung = :hostId")
       Double sumRevenueByHost(Long hostId);

       // Doanh thu theo tháng của một host dựa trên payment CONFIRMED
       @Query("SELECT new map(extract(month from p.paymentDate) as month, SUM(p.amount) as revenue) " +
                      "FROM Payment p " +
                      "WHERE p.status = 'DA_XAC_NHAN' " +
                      "  AND p.paymentDate IS NOT NULL " +
                      "  AND extract(year from p.paymentDate) = :year " +
                      "  AND p.booking.phong.chuNha.maNguoiDung = :hostId " +
                      "GROUP BY extract(month from p.paymentDate)")
       List<Map<String, Object>> getMonthlyRevenueByHost(Long hostId, int year);

       @Query("SELECT new map(p.status as status, COUNT(p) as count) FROM Payment p GROUP BY p.status")
       List<Map<String, Object>> countPaymentsByStatus();

       @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
       @Query("SELECT p FROM Payment p WHERE p.id = :id")
       java.util.Optional<Payment> findByIdWithLock(@org.springframework.data.repository.query.Param("id") Long id);

       @Query("SELECT DISTINCT p FROM Payment p " +
                     "LEFT JOIN FETCH p.booking b " +
                     "LEFT JOIN FETCH b.khach " +
                     "LEFT JOIN FETCH b.phong ph " +
                     "LEFT JOIN FETCH ph.chuNha")
       List<Payment> findAllWithDetails();

        @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"booking", "booking.khach", "booking.phong"})
    List<Payment> findByStatus(Payment.PaymentStatus status);

    // ====== Phi dich vu theo thang (toan he thong) — dung field serviceFee da luu ======
    @Query("SELECT new map(extract(month from p.paymentDate) as month, SUM(p.serviceFee) as serviceFee) " +
                  "FROM Payment p " +
                  "WHERE p.status = 'DA_XAC_NHAN' " +
                  "  AND p.paymentDate IS NOT NULL " +
                  "  AND p.serviceFee IS NOT NULL " +
                  "  AND extract(year from p.paymentDate) = :year " +
                  "GROUP BY extract(month from p.paymentDate)")
    List<Map<String, Object>> getMonthlyServiceFee(int year);

    // ====== Phi dich vu theo thang (theo host) ======
    @Query("SELECT new map(extract(month from p.paymentDate) as month, SUM(p.serviceFee) as serviceFee) " +
                  "FROM Payment p " +
                  "WHERE p.status = 'DA_XAC_NHAN' " +
                  "  AND p.paymentDate IS NOT NULL " +
                  "  AND p.serviceFee IS NOT NULL " +
                  "  AND extract(year from p.paymentDate) = :year " +
                  "  AND p.booking.phong.chuNha.maNguoiDung = :hostId " +
                  "GROUP BY extract(month from p.paymentDate)")
    List<Map<String, Object>> getMonthlyServiceFeeByHost(Long hostId, int year);

    // ====== Hoan tien theo thang (toan he thong) — chi tinh refund record (so tien am) ======
    @Query("SELECT new map(extract(month from p.paymentDate) as month, SUM(ABS(p.amount)) as refundAmount) " +
                  "FROM Payment p " +
                  "WHERE p.status = 'HOAN_TIEN' " +
                  "  AND p.amount < 0 " +
                  "  AND p.paymentDate IS NOT NULL " +
                  "  AND extract(year from p.paymentDate) = :year " +
                  "GROUP BY extract(month from p.paymentDate)")
    List<Map<String, Object>> getMonthlyRefunds(int year);

    // ====== Hoan tien theo thang (theo host) ======
    @Query("SELECT new map(extract(month from p.paymentDate) as month, SUM(ABS(p.amount)) as refundAmount) " +
                  "FROM Payment p " +
                  "WHERE p.status = 'HOAN_TIEN' " +
                  "  AND p.amount < 0 " +
                  "  AND p.paymentDate IS NOT NULL " +
                  "  AND extract(year from p.paymentDate) = :year " +
                  "  AND p.booking.phong.chuNha.maNguoiDung = :hostId " +
                  "GROUP BY extract(month from p.paymentDate)")
    List<Map<String, Object>> getMonthlyRefundsByHost(Long hostId, int year);

    // ====== Huy / That bai theo thang (toan he thong) ======
    @Query("SELECT new map(extract(month from p.paymentDate) as month, SUM(p.amount) as failedAmount) " +
                  "FROM Payment p " +
                  "WHERE p.status IN ('DA_HUY', 'THAT_BAI') " +
                  "  AND p.amount > 0 " +
                  "  AND p.paymentDate IS NOT NULL " +
                  "  AND extract(year from p.paymentDate) = :year " +
                  "GROUP BY extract(month from p.paymentDate)")
    List<Map<String, Object>> getMonthlyCancelledFailed(int year);

    // ====== Huy / That bai theo thang (theo host) ======
    @Query("SELECT new map(extract(month from p.paymentDate) as month, SUM(p.amount) as failedAmount) " +
                  "FROM Payment p " +
                  "WHERE p.status IN ('DA_HUY', 'THAT_BAI') " +
                  "  AND p.amount > 0 " +
                  "  AND p.paymentDate IS NOT NULL " +
                  "  AND extract(year from p.paymentDate) = :year " +
                  "  AND p.booking.phong.chuNha.maNguoiDung = :hostId " +
                  "GROUP BY extract(month from p.paymentDate)")
    List<Map<String, Object>> getMonthlyCancelledFailedByHost(Long hostId, int year);

    // ====== Tong phi dich vu da thu (bao gome ca da xac nhan va hoan tien - admin giu phi DV) ======
    @Query("SELECT COALESCE(SUM(p.serviceFee), 0) FROM Payment p WHERE p.status IN ('DA_XAC_NHAN', 'HOAN_TIEN') AND p.serviceFee IS NOT NULL AND p.amount > 0")
    java.math.BigDecimal sumServiceFeeByConfirmedPayments();

    // ====== Lay payment theo user (qua booking.khach) ======
    @Query("SELECT p FROM Payment p " +
           "LEFT JOIN FETCH p.booking b " +
           "LEFT JOIN FETCH b.phong ph " +
           "WHERE b.khach.maNguoiDung = :userId " +
           "ORDER BY p.paymentDate DESC")
    List<Payment> findByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
}
