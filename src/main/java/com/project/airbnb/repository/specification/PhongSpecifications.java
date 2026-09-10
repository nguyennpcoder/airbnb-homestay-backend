package com.project.airbnb.repository.specification;

import com.project.airbnb.dto.SearchCriteria;
import com.project.airbnb.entity.Phong;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class PhongSpecifications {

    private PhongSpecifications() {
    }

    public static Specification<Phong> withCriteria(SearchCriteria criteria) {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("chuNha", JoinType.LEFT);
            }
            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("trangThai"), Phong.TrangThaiPhong.hoat_dong));

            if (criteria == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            if (criteria.getLoaiPhong() != null) {
                predicates.add(cb.equal(root.get("loaiPhong"), criteria.getLoaiPhong()));
            }

            if (StringUtils.hasText(criteria.getThanhPho())) {
                predicates.add(cb.like(
                        cb.lower(root.get("thanhPho")),
                        "%" + criteria.getThanhPho().toLowerCase() + "%"));
            }

            if (criteria.getSoKhach() != null && criteria.getSoKhach() > 0) {
                Predicate capacityPredicate = cb.or(
                        root.get("soKhachToiDa").isNull(),
                        cb.greaterThanOrEqualTo(root.get("soKhachToiDa"), criteria.getSoKhach()));
                predicates.add(capacityPredicate);
            }

            if (criteria.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("giaMoiKhach"), criteria.getMinPrice()));
            }

            if (criteria.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("giaMoiKhach"), criteria.getMaxPrice()));
            }

            if (criteria.getMinLat() != null && criteria.getMaxLat() != null) {
                predicates.add(cb.between(root.get("viDo"), criteria.getMinLat(), criteria.getMaxLat()));
            }

            if (criteria.getMinLng() != null && criteria.getMaxLng() != null) {
                predicates.add(cb.between(root.get("kinhDo"), criteria.getMinLng(), criteria.getMaxLng()));
            }

            if (!CollectionUtils.isEmpty(criteria.getLoaiHinh())) {
                predicates.add(root.get("loaiBatDongSan").in(criteria.getLoaiHinh()));
            }

            if (!CollectionUtils.isEmpty(criteria.getTienNghi())) {
                var tienNghiExpr = cb.lower(cb.coalesce(root.get("tienNghi"), ""));
                criteria.getTienNghi().forEach(amenity -> {
                    predicates.add(cb.like(tienNghiExpr, "%" + amenity.toLowerCase() + "%"));
                });
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
