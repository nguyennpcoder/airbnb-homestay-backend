package com.project.airbnb.repository;

import com.project.airbnb.entity.TinNhan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TinNhanRepository extends JpaRepository<TinNhan, Long> {
    @Query("SELECT t FROM TinNhan t JOIN FETCH t.nguoiGui JOIN FETCH t.nguoiNhan WHERE t.datCho.maDatCho = :maDatCho ORDER BY t.ngayTao ASC")
    List<TinNhan> findByDatCho_MaDatChoOrderByNgayTaoAsc(@Param("maDatCho") Long maDatCho);

    @Modifying
    @Transactional
    @Query("UPDATE TinNhan t SET t.daDoc = true WHERE t.datCho.maDatCho = :maDatCho AND t.nguoiNhan.maNguoiDung = :userId AND t.daDoc = false")
    void markAllAsRead(@Param("maDatCho") Long maDatCho, @Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE TinNhan t SET t.daDoc = true WHERE t.maTinNhan = :maTinNhan")
    void markAsRead(@Param("maTinNhan") Long maTinNhan);

    @Query("SELECT COUNT(t) FROM TinNhan t WHERE t.datCho.maDatCho = :maDatCho AND t.nguoiNhan.maNguoiDung = :userId AND t.daDoc = false")
    Long countUnread(@Param("maDatCho") Long maDatCho, @Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM TinNhan t WHERE t.nguoiNhan.maNguoiDung = :userId AND t.daDoc = false")
    Long countAllUnreadByUser(@Param("userId") Long userId);

    /** Tin chưa đọc phía khách: đặt chỗ của khách hoặc hội thoại liên hệ phòng KHÔNG thuộc quyền sở hữu của user. */
    @Query("""
            SELECT COUNT(t) FROM TinNhan t
            LEFT JOIN t.datCho d
            LEFT JOIN d.khach k
            LEFT JOIN t.phong p
            LEFT JOIN p.chuNha c
            WHERE t.nguoiNhan.maNguoiDung = :userId AND t.daDoc = false
              AND ((d IS NOT NULL AND k.maNguoiDung = :userId)
                OR (d IS NULL AND p IS NOT NULL AND c.maNguoiDung <> :userId))
            """)
    Long countGuestUnreadByUser(@Param("userId") Long userId);

    /** Tin chưa đọc phía chủ nhà: đặt chỗ của phòng mình, hội thoại liên hệ phòng mình, hoặc hội thoại admin (không phòng/đặt chỗ). */
    @Query("""
            SELECT COUNT(t) FROM TinNhan t
            LEFT JOIN t.datCho d
            LEFT JOIN d.phong dp
            LEFT JOIN dp.chuNha dc
            LEFT JOIN t.phong p
            LEFT JOIN p.chuNha c
            WHERE t.nguoiNhan.maNguoiDung = :userId AND t.daDoc = false
              AND ((d IS NOT NULL AND dc.maNguoiDung = :userId)
                OR (d IS NULL AND p IS NOT NULL AND c.maNguoiDung = :userId)
                OR (d IS NULL AND p IS NULL))
            """)
    Long countHostUnreadByUser(@Param("userId") Long userId);

    @Query("SELECT t FROM TinNhan t WHERE t.datCho.maDatCho = :maDatCho ORDER BY t.ngayTao DESC")
    List<TinNhan> findLastMessageByBooking(@Param("maDatCho") Long maDatCho, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT t.datCho.maDatCho, COUNT(t) FROM TinNhan t WHERE t.datCho.maDatCho IN :bookingIds AND t.nguoiNhan.maNguoiDung = :userId AND t.daDoc = false GROUP BY t.datCho.maDatCho")
    List<Object[]> countUnreadByBookingIds(@Param("bookingIds") List<Long> bookingIds, @Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM TinNhan t WHERE t.nguoiGui.maNguoiDung = :hostId AND t.nguoiNhan.maNguoiDung = :guestId AND t.datCho.maDatCho = :maDatCho AND (t.autoReply IS NULL OR t.autoReply = false)")
    long countHostManualReplies(@Param("hostId") Long hostId, @Param("guestId") Long guestId, @Param("maDatCho") Long maDatCho);

    // ==================== Hội thoại liên hệ trước đặt chỗ (tin_nhan.ma_phong, không tạo dat_cho) ====================

    /** Tất cả tin nhắn liên hệ (chưa đặt chỗ) mà người dùng tham gia (gửi hoặc nhận). */
    @Query("""
            SELECT t FROM TinNhan t
            JOIN FETCH t.nguoiGui
            JOIN FETCH t.nguoiNhan
            LEFT JOIN FETCH t.phong
            LEFT JOIN FETCH t.phong.chuNha
            WHERE t.datCho IS NULL AND t.phong IS NOT NULL
              AND (t.nguoiGui.maNguoiDung = :userId OR t.nguoiNhan.maNguoiDung = :userId)
            ORDER BY t.ngayTao ASC
            """)
    List<TinNhan> findInquiriesByUser(@Param("userId") Long userId);

    /** Tin nhắn của một hội thoại liên hệ (phòng + cặp người dùng), chưa đặt chỗ. */
    @Query("""
            SELECT t FROM TinNhan t
            JOIN FETCH t.nguoiGui
            JOIN FETCH t.nguoiNhan
            LEFT JOIN FETCH t.phong
            WHERE t.datCho IS NULL AND t.phong.maPhong = :maPhong
              AND ((t.nguoiGui.maNguoiDung = :userId AND t.nguoiNhan.maNguoiDung = :partnerId)
                OR (t.nguoiGui.maNguoiDung = :partnerId AND t.nguoiNhan.maNguoiDung = :userId))
            ORDER BY t.ngayTao ASC
            """)
    List<TinNhan> findInquiryThreadMessages(
            @Param("maPhong") Long maPhong,
            @Param("userId") Long userId,
            @Param("partnerId") Long partnerId);

    @Modifying
    @Transactional
    @Query("UPDATE TinNhan t SET t.daDoc = true WHERE t.datCho IS NULL AND t.phong.maPhong = :maPhong AND t.nguoiNhan.maNguoiDung = :userId AND t.daDoc = false")
    void markAllAsReadByPhong(@Param("maPhong") Long maPhong, @Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM TinNhan t WHERE t.datCho IS NULL AND t.phong.maPhong = :maPhong AND t.nguoiNhan.maNguoiDung = :userId AND t.daDoc = false")
    Long countUnreadByPhong(@Param("maPhong") Long maPhong, @Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM TinNhan t WHERE t.nguoiGui.maNguoiDung = :hostId AND t.nguoiNhan.maNguoiDung = :guestId AND t.datCho IS NULL AND t.phong.maPhong = :maPhong AND (t.autoReply IS NULL OR t.autoReply = false)")
    long countHostManualRepliesByPhong(@Param("hostId") Long hostId, @Param("guestId") Long guestId, @Param("maPhong") Long maPhong);

    // ==================== Hội thoại chủ nhà ↔ admin (tin_nhan không gắn phòng/đặt chỗ) ====================

    /** Tất cả tin nhắn hội thoại admin (phòng = null, đặt chỗ = null) mà người dùng tham gia. */
    @Query("""
            SELECT t FROM TinNhan t
            JOIN FETCH t.nguoiGui
            JOIN FETCH t.nguoiNhan
            WHERE t.datCho IS NULL AND t.phong IS NULL
              AND (t.nguoiGui.maNguoiDung = :userId OR t.nguoiNhan.maNguoiDung = :userId)
            ORDER BY t.ngayTao ASC
            """)
    List<TinNhan> findAdminThreadsByUser(@Param("userId") Long userId);

    /** Tin nhắn của một hội thoại admin giữa 2 người dùng. */
    @Query("""
            SELECT t FROM TinNhan t
            JOIN FETCH t.nguoiGui
            JOIN FETCH t.nguoiNhan
            WHERE t.datCho IS NULL AND t.phong IS NULL
              AND ((t.nguoiGui.maNguoiDung = :userId AND t.nguoiNhan.maNguoiDung = :partnerId)
                OR (t.nguoiGui.maNguoiDung = :partnerId AND t.nguoiNhan.maNguoiDung = :userId))
            ORDER BY t.ngayTao ASC
            """)
    List<TinNhan> findAdminThreadMessages(
            @Param("userId") Long userId,
            @Param("partnerId") Long partnerId);

    @Modifying
    @Transactional
    @Query("UPDATE TinNhan t SET t.daDoc = true WHERE t.datCho IS NULL AND t.phong IS NULL AND t.nguoiNhan.maNguoiDung = :userId AND t.nguoiGui.maNguoiDung = :partnerId AND t.daDoc = false")
    void markAllAsReadAdminThread(@Param("userId") Long userId, @Param("partnerId") Long partnerId);

    @Query("SELECT COUNT(t) FROM TinNhan t WHERE t.datCho IS NULL AND t.phong IS NULL AND t.nguoiNhan.maNguoiDung = :userId AND t.nguoiGui.maNguoiDung = :partnerId AND t.daDoc = false")
    Long countUnreadAdminThread(@Param("userId") Long userId, @Param("partnerId") Long partnerId);

    // ==================== Read receipt realtime (người nhận đọc → push WS cho người gửi) ====================

    @Query("SELECT t.maTinNhan FROM TinNhan t WHERE t.datCho.maDatCho = :maDatCho AND t.nguoiNhan.maNguoiDung = :userId AND t.daDoc = false")
    List<Long> findUnreadIdsByDatCho(@Param("maDatCho") Long maDatCho, @Param("userId") Long userId);

    @Query("SELECT DISTINCT t.nguoiGui.maNguoiDung FROM TinNhan t WHERE t.datCho.maDatCho = :maDatCho AND t.nguoiNhan.maNguoiDung = :userId AND t.daDoc = false")
    List<Long> findUnreadSendersByDatCho(@Param("maDatCho") Long maDatCho, @Param("userId") Long userId);

    @Query("SELECT t.maTinNhan FROM TinNhan t WHERE t.datCho IS NULL AND t.phong.maPhong = :maPhong AND t.nguoiNhan.maNguoiDung = :userId AND t.daDoc = false")
    List<Long> findUnreadIdsByPhong(@Param("maPhong") Long maPhong, @Param("userId") Long userId);

    @Query("SELECT DISTINCT t.nguoiGui.maNguoiDung FROM TinNhan t WHERE t.datCho IS NULL AND t.phong.maPhong = :maPhong AND t.nguoiNhan.maNguoiDung = :userId AND t.daDoc = false")
    List<Long> findUnreadSendersByPhong(@Param("maPhong") Long maPhong, @Param("userId") Long userId);

    @Query("SELECT t.maTinNhan FROM TinNhan t WHERE t.datCho IS NULL AND t.phong IS NULL AND t.nguoiNhan.maNguoiDung = :userId AND t.nguoiGui.maNguoiDung = :partnerId AND t.daDoc = false")
    List<Long> findUnreadIdsAdminThread(@Param("userId") Long userId, @Param("partnerId") Long partnerId);

    @Query("SELECT t.nguoiGui.maNguoiDung FROM TinNhan t WHERE t.maTinNhan = :maTinNhan")
    Long findSenderOf(@Param("maTinNhan") Long maTinNhan);
}
