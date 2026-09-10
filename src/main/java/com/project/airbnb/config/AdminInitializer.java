package com.project.airbnb.config;

import com.project.airbnb.entity.NguoiDung;
import com.project.airbnb.repository.NguoiDungRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration

public class AdminInitializer {

    private final NguoiDungRepository nguoiDungRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(NguoiDungRepository nguoiDungRepository, PasswordEncoder passwordEncoder) {
        this.nguoiDungRepository = nguoiDungRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public CommandLineRunner ensureAdminUser() {
        return args -> {
            final String adminEmail = "admin@airbnb.com.vn";
            NguoiDung admin = nguoiDungRepository.findByEmail(adminEmail).orElse(null);

            if (admin == null) {
                admin = new NguoiDung();
                admin.setEmail(adminEmail);
                admin.setMatKhauBam(passwordEncoder.encode("Admin@123"));
                admin.setHoTen("Administrator");
                admin.setSoDienThoai("+84000000000");
                // Quan trong: Admin khong phai la chu nha, nhung la admin
                admin.setLaChuNha(false);
                admin.setLaAdmin(true);
                admin.setEmailDaXacNhan(true);
                nguoiDungRepository.save(admin);
            } else {
                // Check if corrections are needed
                boolean modified = false;
                if (Boolean.TRUE.equals(admin.getLaChuNha())) {
                    admin.setLaChuNha(false);
                    modified = true;
                }
                if (!Boolean.TRUE.equals(admin.getLaAdmin())) {
                    admin.setLaAdmin(true);
                    modified = true;
                }
                if (modified) {
                    nguoiDungRepository.save(admin);
                }
            }
        };
    }
}
