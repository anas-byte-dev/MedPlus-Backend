package com.medpulse.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "medical_departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalDepartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code; // e.g. "ED-TRAUMA", "CARDIO-ICU", "NEURO-STROKE"

    @Column(nullable = false)
    private String specialty;

    private String headPhysician;

    private Integer bedCapacity;

    @Builder.Default
    private Integer currentOccupancy = 0;

    private String acuityLevel; // e.g. "Critical Level 1", "Level 2", "Step-Down"

    @Column(length = 2000)
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "department_protocols", joinColumns = @JoinColumn(name = "department_id"))
    @Column(name = "protocol_name")
    @Builder.Default
    private Set<String> clinicalProtocols = new HashSet<>();

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
