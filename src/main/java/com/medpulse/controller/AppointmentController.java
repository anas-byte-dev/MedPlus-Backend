package com.medpulse.controller;

import com.medpulse.dto.AppointmentDtos.AppointmentResponse;
import com.medpulse.dto.AppointmentDtos.BookAppointmentRequest;
import com.medpulse.dto.AppointmentDtos.UpdateStatusRequest;
import com.medpulse.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Clinical consultations and patient appointment booking")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @Operation(summary = "Book a consultation appointment with a specialist doctor")
    public ResponseEntity<AppointmentResponse> bookAppointment(@Valid @RequestBody BookAppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.bookAppointment(request));
    }

    @GetMapping
    @Operation(summary = "List all consultations / appointments with optional role-based filters")
    public ResponseEntity<List<AppointmentResponse>> getAllAppointments(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) String hospital
    ) {
        return ResponseEntity.ok(appointmentService.getAppointments(phone, email, doctorId, hospital));
    }

    @GetMapping("/ref/{bookingRef}")
    @Operation(summary = "Lookup appointment details by booking reference code")
    public ResponseEntity<AppointmentResponse> getByBookingReference(@PathVariable String bookingRef) {
        return ResponseEntity.ok(appointmentService.getByBookingReference(bookingRef));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update consultation appointment status")
    public ResponseEntity<AppointmentResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        return ResponseEntity.ok(appointmentService.updateStatus(id, request.getStatus()));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a consultation appointment")
    public ResponseEntity<AppointmentResponse> cancelAppointment(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id));
    }
}
