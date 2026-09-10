package com.project.airbnb.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data

@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String tenDangNhap;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String matKhau;

    private String hoTen;
    private String soDienThoai;
    private Boolean laChuNha = false;
}
