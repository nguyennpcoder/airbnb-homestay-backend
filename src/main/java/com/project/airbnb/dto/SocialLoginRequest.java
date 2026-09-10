package com.project.airbnb.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data

public class SocialLoginRequest {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    private String displayName;

    private String avatarUrl;

    private String provider;

    private String providerUid;

    private String phoneNumber;
}
