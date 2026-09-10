package com.project.airbnb.service;

import com.project.airbnb.dto.QuyDinhGiaRequest;
import com.project.airbnb.entity.QuyDinhGia;
import com.project.airbnb.repository.QuyDinhGiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class QuyDinhGiaService {

    private final QuyDinhGiaRepository repo;

    public QuyDinhGiaService(QuyDinhGiaRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public QuyDinhGia get() {
        return repo.findFirstByOrderByMaQuyDinhAsc().orElseGet(this::createDefault);
    }

    public QuyDinhGia update(QuyDinhGiaRequest req) {
        QuyDinhGia qd = repo.findFirstByOrderByMaQuyDinhAsc().orElseGet(this::createDefault);
        if (req.getTyLeNguoiLon() != null) {
            qd.setTyLeNguoiLon(req.getTyLeNguoiLon());
        }
        if (req.getTyLeTreEm() != null) {
            qd.setTyLeTreEm(req.getTyLeTreEm());
        }
        if (req.getTyLePhiDichVu() != null) {
            qd.setTyLePhiDichVu(req.getTyLePhiDichVu());
        }
        return repo.save(qd);
    }

    private QuyDinhGia createDefault() {
        QuyDinhGia qd = new QuyDinhGia();
        qd.setTyLeNguoiLon(new BigDecimal("100.00"));
        qd.setTyLeTreEm(new BigDecimal("60.00"));
        qd.setTyLeEmBe(BigDecimal.ZERO);
        qd.setTyLePhiDichVu(new BigDecimal("10.00"));
        return repo.save(qd);
    }
}
