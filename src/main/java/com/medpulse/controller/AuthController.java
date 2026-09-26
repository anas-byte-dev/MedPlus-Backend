package com.medpulse.controller;

import com.medpulse.dto.AuthDtos;
import com.medpulse.model.User;
import com.medpulse.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Clinical Portal Authentication & Staff Registration")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login to MedPulse Clinical Portal")
    public ResponseEntity<AuthDtos.JwtResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    @Operation(summary = "Register new clinical staff or patient")
    public ResponseEntity<AuthDtos.UserDto> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated clinical user")
    public ResponseEntity<AuthDtos.UserDto> getCurrentUser() {
        User user = authService.getCurrentAuthenticatedUser();
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(AuthDtos.UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .phone(user.getPhone())
                .medicalLicense(user.getMedicalLicense())
                .departmentName(user.getDepartmentName())
                .hospital(user.getHospital())
                .doctorId(user.getDoctorId())
                .build());
    }

    @GetMapping("/users")
    @Operation(summary = "List all registered accounts in DBMS (Admin / Staff)")
    public ResponseEntity<java.util.List<AuthDtos.UserDto>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }
}
