package com.medpulse.service;

import com.medpulse.dto.AuthDtos;
import com.medpulse.exception.BadRequestException;
import com.medpulse.model.Role;
import com.medpulse.model.User;
import com.medpulse.repository.UserRepository;
import com.medpulse.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public AuthDtos.JwtResponse login(AuthDtos.LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User record not found"));

        return AuthDtos.JwtResponse.builder()
                .token(jwt)
                .type("Bearer")
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .phone(user.getPhone())
                .medicalLicense(user.getMedicalLicense())
                .departmentName(user.getDepartmentName())
                .hospital(user.getHospital())
                .doctorId(user.getDoctorId())
                .build();
    }

    public AuthDtos.UserDto register(AuthDtos.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered: " + request.getEmail());
        }

        Role assignedRole = request.getRole() != null ? request.getRole() : Role.ROLE_PATIENT;

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(assignedRole)
                .phone(request.getPhone() != null ? request.getPhone().trim() : null)
                .medicalLicense(request.getMedicalLicense())
                .departmentName(request.getDepartmentName())
                .hospital(request.getHospital())
                .doctorId(request.getDoctorId())
                .build();

        User saved = userRepository.save(user);

        return AuthDtos.UserDto.builder()
                .id(saved.getId())
                .email(saved.getEmail())
                .fullName(saved.getFullName())
                .role(saved.getRole().name())
                .phone(saved.getPhone())
                .medicalLicense(saved.getMedicalLicense())
                .departmentName(saved.getDepartmentName())
                .hospital(saved.getHospital())
                .doctorId(saved.getDoctorId())
                .build();
    }

    public java.util.List<AuthDtos.UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(u -> AuthDtos.UserDto.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .fullName(u.getFullName())
                        .role(u.getRole().name())
                        .phone(u.getPhone())
                        .medicalLicense(u.getMedicalLicense())
                        .departmentName(u.getDepartmentName())
                        .hospital(u.getHospital())
                        .doctorId(u.getDoctorId())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    public User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email).orElse(null);
    }
}
