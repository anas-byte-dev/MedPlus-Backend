package com.medpulse.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

public class AnalyticsDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HospitalAnalyticsResponse {
        private long totalActivePatients;
        private long criticalEmergencyCases;
        private long inTreatmentCount;
        private long dischargedCount;
        private long totalDepartments;
        private double overallBedOccupancyRate;
        private double averageTriageTimeMinutes;
        private int aiEvaluatedCasesCount;
        private Map<String, Long> statusDistribution;
        private Map<String, Long> acuityDistribution;
        private List<BedOccupancyDto> departmentBedOccupancy;
        private List<RecentAlertDto> recentCriticalAlerts;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BedOccupancyDto {
        private Long departmentId;
        private String departmentName;
        private String departmentCode;
        private Integer capacity;
        private Integer occupied;
        private Double occupancyPercent;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentAlertDto {
        private String patientId;
        private String patientName;
        private String chiefComplaint;
        private String acuityLevel;
        private Integer riskScore;
        private String departmentCode;
        private String timestamp;
    }
}
