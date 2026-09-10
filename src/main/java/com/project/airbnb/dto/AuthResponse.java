package com.project.airbnb.dto;

import lombok.*;

@Data

@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String email;
    private Long maNguoiDung;
    private String hoTen;
    private Boolean laChuNha;
    private Boolean laAdmin;
}
