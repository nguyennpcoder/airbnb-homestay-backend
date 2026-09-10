package com.project.airbnb.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

@Data

@NoArgsConstructor
@AllArgsConstructor
public class CompleteRegistrationRequest {
    @NotBlank(message = "Họ không được để trống")
    private String ho;

    @NotBlank(message = "Tên không được để trống")
    private String ten;

    private LocalDate ngaySinh;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String matKhau;

    private Boolean nhanTinNhanTiepThi = true;
}
