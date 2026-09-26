package com.medpulse.controller;

import com.medpulse.model.Doctor;
import com.medpulse.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctor Specialists", description = "Specialist physician directory and medical appointments")
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping
    @Operation(summary = "Search specialist doctors by city, hospital, specialty, or keywords")
    public ResponseEntity<List<Doctor>> getDoctors(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String hospital,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String query
    ) {
        return ResponseEntity.ok(doctorService.getDoctors(city, hospital, specialty, query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get specialist doctor profile by ID")
    public ResponseEntity<Doctor> getDoctorById(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.getDoctorById(id));
    }

    @GetMapping("/cities")
    @Operation(summary = "Get list of available cities with top specialists")
    public ResponseEntity<List<String>> getCities() {
        return ResponseEntity.ok(doctorService.getCities());
    }

    @GetMapping("/hospitals")
    @Operation(summary = "Get list of hospitals")
    public ResponseEntity<List<String>> getHospitals() {
        return ResponseEntity.ok(doctorService.getHospitals());
    }

    @GetMapping("/specialties")
    @Operation(summary = "Get list of medical specialties")
    public ResponseEntity<List<String>> getSpecialties() {
        return ResponseEntity.ok(doctorService.getSpecialties());
    }

    @PostMapping
    @Operation(summary = "Register or onboard a new specialist doctor")
    public ResponseEntity<Doctor> createDoctor(@Valid @RequestBody Doctor doctor) {
        return ResponseEntity.ok(doctorService.createDoctor(doctor));
    }
}
