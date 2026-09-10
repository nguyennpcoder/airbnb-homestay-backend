package com.project.airbnb.repository;

import com.project.airbnb.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByTokenAndDaSuDungFalseAndHetHanFalse(String token);
    Optional<EmailVerification> findByEmailAndDaSuDungFalseAndHetHanFalseOrderByThoiGianTaoDesc(String email);
}

