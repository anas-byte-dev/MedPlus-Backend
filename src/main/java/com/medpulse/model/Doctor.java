package com.medpulse.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Doctor name is required")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Medical specialty is required")
    @Column(nullable = false)
    private String specialty;

    @NotBlank(message = "City is required")
    @Column(nullable = false)
    private String city; // "Muzaffarpur", "Patna", "Delhi"

    @NotBlank(message = "Hospital affiliation is required")
    @Column(nullable = false)
    private String hospital; // e.g. "AIIMS New Delhi", "AIIMS Patna", "IGIMS Patna", "Prasad Hospital", "Jay Prabha Medanta", "Khan Healthcare"

    @Column(length = 1000)
    private String address;

    private String designation;
    private String qualifications;
    private Integer experienceYears;
    private Integer consultationFee; // INR ₹
    private String availableDays; // e.g. "Mon, Wed, Fri"
    private String availableTimeSlots; // e.g. "10:00 AM - 01:00 PM, 04:00 PM - 07:00 PM"
    private Double rating;
    private Integer reviewCount;

    @Builder.Default
    private Boolean verified = true;

    @Column(length = 1000)
    private String verificationNote; // Clinical / Institutional advisory note

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
