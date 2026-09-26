package com.medpulse.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

public class AppointmentDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookAppointmentRequest {
        @NotNull(message = "Doctor ID is required")
        private Long doctorId;

        @NotBlank(message = "Patient name is required")
        private String patientName;

        @NotBlank(message = "Contact phone number is required")
        private String patientPhone;

        private String patientEmail;
        private Integer patientAge;
        private String patientGender;

        @NotNull(message = "Appointment date is required")
        @FutureOrPresent(message = "Appointment date cannot be in the past")
        private LocalDate appointmentDate;

        @NotBlank(message = "Time slot is required")
        private String timeSlot;

        private String symptoms;
        private String reasonForVisit;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AppointmentResponse {
        private Long id;
        private String bookingReference;
        private Long doctorId;
        private String doctorName;
        private String doctorSpecialty;
        private String doctorQualifications;  // e.g. "MBBS, MD (Gold Medalist)"
        private String doctorDesignation;      // e.g. "Senior Consultant Neurologist"
        private String hospital;
        private String city;
        private String address;
        private String patientName;
        private String patientPhone;
        private String patientEmail;
        private Integer patientAge;
        private String patientGender;
        private LocalDate appointmentDate;
        private String timeSlot;
        private String symptoms;
        private String reasonForVisit;
        private String description;
        private String status;
        private Integer consultationFee;
        private String formattedBookingTime;
        private String confirmationMessage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStatusRequest {
        @NotBlank(message = "Status is required")
        @Pattern(regexp = "^(?i)(CONFIRMED|CANCELLED|COMPLETED|PENDING)$", message = "Status must be CONFIRMED, CANCELLED, COMPLETED, or PENDING")
        private String status;
    }
}
