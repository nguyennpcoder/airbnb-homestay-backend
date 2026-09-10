package com.project.airbnb.service;

import com.project.airbnb.entity.NguoiDung;
import com.project.airbnb.entity.TraLoiNhanh;
import com.project.airbnb.repository.TraLoiNhanhRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TraLoiNhanhService {

    private final TraLoiNhanhRepository traLoiNhanhRepository;

    public TraLoiNhanhService(TraLoiNhanhRepository traLoiNhanhRepository) {
        this.traLoiNhanhRepository = traLoiNhanhRepository;
    }

    public List<TraLoiNhanh> getByChuNha(Long maChuNha) {
        return traLoiNhanhRepository.findByChuNha_MaNguoiDungOrderByPhimTatAsc(maChuNha);
    }

    public Optional<TraLoiNhanh> getById(Long id) {
        return traLoiNhanhRepository.findById(id);
    }

    @Transactional
    public TraLoiNhanh create(TraLoiNhanh traLoiNhanh) {
        return traLoiNhanhRepository.save(traLoiNhanh);
    }

    @Transactional
    public TraLoiNhanh update(Long id, TraLoiNhanh updated) {
        TraLoiNhanh existing = traLoiNhanhRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quick reply not found"));
        existing.setNoiDung(updated.getNoiDung());
        existing.setPhimTat(updated.getPhimTat());
        return traLoiNhanhRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        traLoiNhanhRepository.deleteById(id);
    }

    /**
     * Pick a random quick reply for the host. Returns null if host has no quick replies.
     */
    public TraLoiNhanh getRandomReply(Long maChuNha) {
        return traLoiNhanhRepository.findRandomByChuNha(maChuNha);
    }
}
