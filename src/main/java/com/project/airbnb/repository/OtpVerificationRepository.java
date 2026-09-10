package com.project.airbnb.repository;

import com.project.airbnb.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    Optional<OtpVerification> findBySoDienThoaiAndOtpCodeAndLoaiAndDaSuDungFalseAndHetHanFalse(
            String soDienThoai, String otpCode, OtpVerification.Loai loai);
    
    Optional<OtpVerification> findByEmailAndOtpCodeAndLoaiAndDaSuDungFalseAndHetHanFalse(
            String email, String otpCode, OtpVerification.Loai loai);

    List<OtpVerification> findBySoDienThoaiAndLoaiOrderByThoiGianTaoDesc(
            String soDienThoai, OtpVerification.Loai loai);

    List<OtpVerification> findByEmailAndLoaiOrderByThoiGianTaoDesc(
            String email, OtpVerification.Loai loai);

    @Query("SELECT o FROM OtpVerification o WHERE o.thoiGianHetHan < :now AND o.hetHan = false")
    List<OtpVerification> findExpiredOtps(@Param("now") LocalDateTime now);
}

