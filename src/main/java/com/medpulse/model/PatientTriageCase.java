package com.medpulse.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "patient_triage_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientTriageCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String patientId; // e.g. "PT-9401"

    @Column(nullable = false)
    private String patientName;

    private Integer age;

    private String gender;

    @Column(nullable = false, length = 2000)
    private String chiefComplaint;

    @Column(nullable = false, length = 1000)
    private String vitalSigns; // e.g. "HR: 118 bpm, BP: 165/105 mmHg, SpO2: 92%, Temp: 38.2°C, RR: 24/min"

    private String allergies;

    @Column(length = 2000)
    private String medicalHistory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TriageStatus triageStatus = TriageStatus.INCOMING_TRIAGE;

    @Builder.Default
    private Integer triageRiskScore = 0; // 0 to 100

    private String aiAcuityLevel; // e.g. "Level 1: Resuscitation (Critical)", "Level 2: Emergent"

    @Column(length = 4000)
    private String aiDifferentialDiagnosis;

    @Column(length = 2000)
    private String aiSuggestedWorkup;

    @Column(length = 1000)
    private String aiRiskFlags;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_doctor_id")
    private Doctor assignedDoctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private MedicalDepartment department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admitted_by_id")
    private User admittedBy;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
