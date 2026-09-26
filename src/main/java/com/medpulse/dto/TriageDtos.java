package com.medpulse.dto;

import com.medpulse.model.TriageStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class TriageDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreatePatientCaseRequest {
        private String patientId; // optional, generated if null

        @NotBlank(message = "Patient name is required")
        private String patientName;

        private Integer age;
        private String gender;

        @NotBlank(message = "Chief complaint is required")
        private String chiefComplaint;

        @NotBlank(message = "Vital signs are required")
        private String vitalSigns; // e.g. "HR: 118 bpm, BP: 165/105 mmHg, SpO2: 92%, Temp: 38.2°C, RR: 24/min"

        private String allergies;
        private String medicalHistory;

        private Long departmentId;
        private Long assignedDoctorId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateTriageStatusRequest {
        private TriageStatus status;
        private String clinicalNotes;
        private Long assignedDoctorId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PatientCaseResponse {
        private Long id;
        private String patientId;
        private String patientName;
        private Integer age;
        private String gender;
        private String chiefComplaint;
        private String vitalSigns;
        private String allergies;
        private String medicalHistory;
        private TriageStatus triageStatus;
        private Integer triageRiskScore;
        private String aiAcuityLevel;
        private String aiDifferentialDiagnosis;
        private String aiSuggestedWorkup;
        private String aiRiskFlags;
        private Long departmentId;
        private String departmentName;
        private String departmentCode;
        private Long assignedDoctorId;
        private String assignedDoctorName;
        private java.util.UUID admittedById;
        private String admittedByName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BatchTriageResult {
        private int totalProcessed;
        private int criticalHighRiskCount;
        private long executionTimeMs;
        private List<PatientCaseResponse> cases;
    }
}
