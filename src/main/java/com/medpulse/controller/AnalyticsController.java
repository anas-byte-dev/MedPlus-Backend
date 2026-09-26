package com.medpulse.controller;

import com.medpulse.dto.AnalyticsDtos;
import com.medpulse.model.MedicalDepartment;
import com.medpulse.model.PatientTriageCase;
import com.medpulse.model.TriageStatus;
import com.medpulse.repository.MedicalDepartmentRepository;
import com.medpulse.repository.PatientTriageCaseRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Hospital KPIs, Bed Occupancy & Triage Telemetry")
public class AnalyticsController {

    private final PatientTriageCaseRepository patientCaseRepository;
    private final MedicalDepartmentRepository departmentRepository;

    @GetMapping({"/hospital-overview", "/hospital"})
    @Operation(summary = "Get live hospital telemetry KPIs and emergency acuity metrics")
    public ResponseEntity<AnalyticsDtos.HospitalAnalyticsResponse> getHospitalAnalytics() {
        List<PatientTriageCase> allCases = patientCaseRepository.findAll();
        List<MedicalDepartment> allDepts = departmentRepository.findAll();

        long totalActive = allCases.stream().filter(c -> c.getTriageStatus() != TriageStatus.DISCHARGED).count();
        long criticalCount = allCases.stream()
                .filter(c -> c.getTriageRiskScore() != null && c.getTriageRiskScore() >= 80 && c.getTriageStatus() != TriageStatus.DISCHARGED)
                .count();
        long inTreatment = allCases.stream().filter(c -> c.getTriageStatus() == TriageStatus.IN_TREATMENT).count();
        long discharged = allCases.stream().filter(c -> c.getTriageStatus() == TriageStatus.DISCHARGED).count();
        int aiEvaluated = (int) allCases.stream().filter(c -> c.getAiAcuityLevel() != null).count();

        // Calculate Bed Occupancy
        int totalCapacity = allDepts.stream().mapToInt(d -> d.getBedCapacity() != null ? d.getBedCapacity() : 0).sum();
        int totalOccupied = allDepts.stream().mapToInt(d -> d.getCurrentOccupancy() != null ? d.getCurrentOccupancy() : 0).sum();
        double overallOccupancyRate = totalCapacity > 0 ? Math.round(((double) totalOccupied / totalCapacity) * 1000.0) / 10.0 : 0.0;

        // Status Distribution
        Map<String, Long> statusDist = allCases.stream()
                .collect(Collectors.groupingBy(c -> c.getTriageStatus().name(), Collectors.counting()));

        // Acuity Distribution
        Map<String, Long> acuityDist = allCases.stream()
                .filter(c -> c.getAiAcuityLevel() != null)
                .collect(Collectors.groupingBy(PatientTriageCase::getAiAcuityLevel, Collectors.counting()));

        // Department Bed Breakdown
        List<AnalyticsDtos.BedOccupancyDto> bedBreakdown = allDepts.stream().map(d -> {
            int cap = d.getBedCapacity() != null ? d.getBedCapacity() : 0;
            int occ = d.getCurrentOccupancy() != null ? d.getCurrentOccupancy() : 0;
            double pct = cap > 0 ? Math.round(((double) occ / cap) * 1000.0) / 10.0 : 0.0;
            return AnalyticsDtos.BedOccupancyDto.builder()
                    .departmentId(d.getId())
                    .departmentName(d.getName())
                    .departmentCode(d.getCode())
                    .capacity(cap)
                    .occupied(occ)
                    .occupancyPercent(pct)
                    .build();
        }).toList();

        // Recent Critical Alerts
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
        List<AnalyticsDtos.RecentAlertDto> recentAlerts = allCases.stream()
                .filter(c -> c.getTriageRiskScore() != null && c.getTriageRiskScore() >= 70)
                .sorted(Comparator.comparing(PatientTriageCase::getCreatedAt).reversed())
                .limit(5)
                .map(c -> AnalyticsDtos.RecentAlertDto.builder()
                        .patientId(c.getPatientId())
                        .patientName(c.getPatientName())
                        .chiefComplaint(c.getChiefComplaint())
                        .acuityLevel(c.getAiAcuityLevel())
                        .riskScore(c.getTriageRiskScore())
                        .departmentCode(c.getDepartment() != null ? c.getDepartment().getCode() : "ED")
                        .timestamp(c.getCreatedAt() != null ? c.getCreatedAt().format(dtf) : "Just now")
                        .build())
                .toList();

        double avgTriageMinutes = allCases.stream()
                .filter(c -> c.getCreatedAt() != null && c.getUpdatedAt() != null && c.getTriageStatus() != TriageStatus.INCOMING_TRIAGE)
                .mapToDouble(c -> Math.max(0.5, java.time.Duration.between(c.getCreatedAt(), c.getUpdatedAt()).toSeconds() / 60.0))
                .average()
                .orElse(4.2);
        double roundedAvgTriage = Math.round(avgTriageMinutes * 10.0) / 10.0;

        return ResponseEntity.ok(AnalyticsDtos.HospitalAnalyticsResponse.builder()
                .totalActivePatients(totalActive)
                .criticalEmergencyCases(criticalCount)
                .inTreatmentCount(inTreatment)
                .dischargedCount(discharged)
                .totalDepartments(allDepts.size())
                .overallBedOccupancyRate(overallOccupancyRate)
                .averageTriageTimeMinutes(roundedAvgTriage)
                .aiEvaluatedCasesCount(aiEvaluated)
                .statusDistribution(statusDist)
                .acuityDistribution(acuityDist)
                .departmentBedOccupancy(bedBreakdown)
                .recentCriticalAlerts(recentAlerts)
                .build());
    }
}
