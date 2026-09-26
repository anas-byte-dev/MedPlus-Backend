package com.medpulse.dto;

import com.medpulse.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

public class AuthDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {
        @NotBlank(message = "Full name is required")
        private String fullName;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        private Role role; // DOCTOR, TRIAGE_NURSE, PATIENT, HOSPITAL, ADMIN

        private String phone;

        private String medicalLicense;

        private String departmentName;

        private String hospital;

        private Long doctorId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JwtResponse {
        private String token;
        @Builder.Default
        private String type = "Bearer";
        private java.util.UUID id;
        private String email;
        private String fullName;
        private String role;
        private String phone;
        private String medicalLicense;
        private String departmentName;
        private String hospital;
        private Long doctorId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserDto {
        private java.util.UUID id;
        private String email;
        private String fullName;
        private String role;
        private String phone;
        private String medicalLicense;
        private String departmentName;
        private String hospital;
        private Long doctorId;
    }
}
