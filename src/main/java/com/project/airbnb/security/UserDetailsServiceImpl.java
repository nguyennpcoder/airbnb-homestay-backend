package com.project.airbnb.security;

import com.project.airbnb.entity.NguoiDung;
import com.project.airbnb.repository.NguoiDungRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service

public class UserDetailsServiceImpl implements UserDetailsService {

    private final NguoiDungRepository nguoiDungRepository;

    public UserDetailsServiceImpl(NguoiDungRepository nguoiDungRepository) {
        this.nguoiDungRepository = nguoiDungRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email == null ? null : email.trim().toLowerCase();
        NguoiDung nguoiDung = nguoiDungRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + email));

        // Assign admin role to the dedicated admin account
        boolean isAdmin = "admin@airbnb.com.vn".equalsIgnoreCase(nguoiDung.getEmail());
        String[] roles = isAdmin ? new String[] { "ROLE_USER", "ROLE_ADMIN" } : new String[] { "ROLE_USER" };

        return org.springframework.security.core.userdetails.User
                .withUsername(nguoiDung.getEmail())
                .password(nguoiDung.getMatKhauBam())
                .authorities(roles)
                .build();
    }
}
