package com.medpulse.controller;

import com.medpulse.dto.DepartmentDtos;
import com.medpulse.service.MedicalDepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Tag(name = "Departments", description = "Medical Units & Clinical Specialty Services")
public class MedicalDepartmentController {

    private final MedicalDepartmentService departmentService;

    @GetMapping
    @Operation(summary = "List all medical departments and ICU units")
    public ResponseEntity<List<DepartmentDtos.DepartmentResponse>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get department details by ID")
    public ResponseEntity<DepartmentDtos.DepartmentResponse> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Create a new medical department (Doctors & Admin)")
    public ResponseEntity<DepartmentDtos.DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentDtos.CreateDepartmentRequest request) {
        return new ResponseEntity<>(departmentService.createDepartment(request), HttpStatus.CREATED);
    }
}
