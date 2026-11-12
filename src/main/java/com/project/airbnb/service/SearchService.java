package com.project.airbnb.service;

import com.project.airbnb.dto.PhongDTO;
import com.project.airbnb.dto.SearchCriteria;
import com.project.airbnb.entity.HinhAnh;
import com.project.airbnb.entity.Phong;
import com.project.airbnb.repository.DatChoRepository;
import com.project.airbnb.repository.HinhAnhRepository;
import com.project.airbnb.repository.LichTrinhRepository;
import com.project.airbnb.repository.PhongRepository;
import com.project.airbnb.repository.specification.PhongSpecifications;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private final PhongRepository phongRepository;
    private final LichTrinhRepository lichTrinhRepository;
    private final HinhAnhRepository hinhAnhRepository;
    private final DatChoRepository datChoRepository;

    public SearchService(PhongRepository phongRepository,
            LichTrinhRepository lichTrinhRepository,
            HinhAnhRepository hinhAnhRepository,
            DatChoRepository datChoRepository) {
        this.phongRepository = phongRepository;
        this.lichTrinhRepository = lichTrinhRepository;
        this.hinhAnhRepository = hinhAnhRepository;
        this.datChoRepository = datChoRepository;
    }

    @Transactional(readOnly = true)
    public List<PhongDTO> search(SearchCriteria criteria) {
        var specification = PhongSpecifications.withCriteria(criteria);
        List<Phong> rooms = phongRepository.findAll(specification);

        rooms = rooms.stream()
                .filter(p -> p.getBiKhoa() == null || !p.getBiKhoa())
                .collect(Collectors.toList());

        List<Phong> availableRooms = filterAvailability(rooms, criteria);

        if (availableRooms.isEmpty()) {
            return Collections.emptyList();
        }

        // One image query for all rooms instead of one query per room (N+1).
        List<Long> roomIds = availableRooms.stream()
                .map(Phong::getMaPhong)
                .toList();
        Map<Long, List<HinhAnh>> imagesByRoom = hinhAnhRepository.findByPhongIds(roomIds).stream()
                .collect(Collectors.groupingBy(h -> h.getPhong().getMaPhong()));

        return availableRooms.stream()
                .map(room -> PhongDTO.fromEntity(
                        room,
                        imagesByRoom.getOrDefault(room.getMaPhong(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    private List<Phong> filterAvailability(List<Phong> rooms, SearchCriteria criteria) {
        if (criteria == null
                || criteria.getNgayNhan() == null
                || criteria.getNgayTra() == null
                || !criteria.getNgayNhan().isBefore(criteria.getNgayTra())) {
            return rooms;
        }

        List<Long> stayIds = rooms.stream()
                .filter(p -> p.getLoaiPhong() == Phong.LoaiPhong.noi_luu_tru)
                .map(Phong::getMaPhong)
                .toList();

        if (stayIds.isEmpty()) {
            return rooms;
        }

        // Check LichTrinh for blocked dates
        List<Long> unavailableFromSchedule = lichTrinhRepository.findUnavailablePhongIds(
                stayIds,
                criteria.getNgayNhan(),
                criteria.getNgayTra());

        // Check DatCho for active bookings (cho_xac_nhan or da_xac_nhan) that overlap
        List<Long> unavailableFromBookings = datChoRepository.findBookedPhongIdsInDateRange(
                stayIds,
                criteria.getNgayNhan(),
                criteria.getNgayTra());

        // Merge both sets of unavailable IDs
        Set<Long> unavailableSet = new java.util.HashSet<>();
        unavailableSet.addAll(unavailableFromSchedule);
        unavailableSet.addAll(unavailableFromBookings);

        if (unavailableSet.isEmpty()) {
            return rooms;
        }

        return rooms.stream()
                .filter(room -> !unavailableSet.contains(room.getMaPhong()))
                .collect(Collectors.toList());
    }

    private List<Long> findUnavailablePhongIds(List<Long> stayIds, LocalDate startDate, LocalDate endDate) {
        if (stayIds.isEmpty()) {
            return Collections.emptyList();
        }
        return lichTrinhRepository.findUnavailablePhongIds(stayIds, startDate, endDate);
    }
}
