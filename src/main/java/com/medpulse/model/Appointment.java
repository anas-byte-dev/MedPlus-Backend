package com.medpulse.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String bookingReference; // e.g. "MED-APT-8491"

    @Column(nullable = false)
    private Long doctorId;

    @Column(nullable = false)
    private String doctorName;

    @Column(nullable = false)
    private String doctorSpecialty;

    private String doctorQualifications; // e.g. "MBBS, MD, DM (Neurology)"
    private String doctorDesignation;    // e.g. "Senior Consultant Neurologist"

    @Column(nullable = false)
    private String hospital;

    @Column(nullable = false)
    private String city;

    @Column(length = 1000)
    private String address;

    @Column(nullable = false)
    private String patientName;

    @Column(nullable = false)
    private String patientPhone;

    private String patientEmail;
    private Integer patientAge;
    private String patientGender;

    @Column(nullable = false)
    private LocalDate appointmentDate;

    @Column(nullable = false)
    private String timeSlot; // e.g. "10:30 AM"

    @Column(length = 2000)
    private String symptoms;

    @Builder.Default
    private String status = "CONFIRMED"; // "CONFIRMED", "COMPLETED", "CANCELLED"

    private Integer consultationFee;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
