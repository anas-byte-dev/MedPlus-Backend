package com.medpulse.service;

import com.medpulse.agent.AutonomousAgentService;
import com.medpulse.dto.TriageDtos;
import com.medpulse.exception.ResourceNotFoundException;
import com.medpulse.model.MedicalDepartment;
import com.medpulse.model.PatientTriageCase;
import com.medpulse.model.TriageStatus;
import com.medpulse.model.User;
import com.medpulse.repository.MedicalDepartmentRepository;
import com.medpulse.repository.PatientTriageCaseRepository;
import com.medpulse.repository.UserRepository;
import com.medpulse.service.concurrency.AsyncBatchTriageService;
import com.medpulse.service.strategy.ScoringStrategyContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class PatientTriageCaseService {

    private final PatientTriageCaseRepository patientCaseRepository;
    private final MedicalDepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final com.medpulse.repository.DoctorRepository doctorRepository;
    private final ScoringStrategyContext scoringContext;
    private final AsyncBatchTriageService asyncBatchService;
    private final AutonomousAgentService agentService;
    private final AuthService authService;

    @Transactional
    public TriageDtos.PatientCaseResponse submitTriageCase(TriageDtos.CreatePatientCaseRequest request) {
        User currentUser = authService.getCurrentAuthenticatedUser();

        String pId = request.getPatientId();
        if (pId == null || pId.isBlank()) {
            pId = "PT-" + (System.currentTimeMillis() % 10000000) + "-" + (1000 + new Random().nextInt(9000));
        }

        MedicalDepartment dept = null;
        if (request.getDepartmentId() != null) {
            dept = departmentRepository.findById(request.getDepartmentId()).orElse(null);
        } else {
            // Default to Emergency Department if available
            dept = departmentRepository.findByCode("ED-TRAUMA").orElse(null);
            if (dept == null && !departmentRepository.findAll().isEmpty()) {
                dept = departmentRepository.findAll().get(0);
            }
        }

        com.medpulse.model.Doctor assignedDoc = null;
        if (request.getAssignedDoctorId() != null) {
            assignedDoc = doctorRepository.findById(request.getAssignedDoctorId()).orElse(null);
        }

        PatientTriageCase patientCase = PatientTriageCase.builder()
                .patientId(pId)
                .patientName(request.getPatientName())
                .age(request.getAge())
                .gender(request.getGender())
                .chiefComplaint(request.getChiefComplaint())
                .vitalSigns(request.getVitalSigns())
                .allergies(request.getAllergies())
                .medicalHistory(request.getMedicalHistory())
                .triageStatus(TriageStatus.INCOMING_TRIAGE)
                .department(dept)
                .assignedDoctor(assignedDoc)
                .admittedBy(currentUser)
                .build();

        // Calculate initial baseline risk score
        int score = scoringContext.calculateBlendedRiskScore(patientCase, dept);
        String acuity = ScoringStrategyContext.determineAcuityLevel(score);
        patientCase.setTriageRiskScore(score);
        patientCase.setAiAcuityLevel(acuity);

        PatientTriageCase saved = patientCaseRepository.save(patientCase);

        // Update department occupancy
        if (dept != null) {
            dept.setCurrentOccupancy(Math.min(dept.getBedCapacity(), dept.getCurrentOccupancy() + 1));
            departmentRepository.save(dept);
        }

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TriageDtos.PatientCaseResponse> getAllCases(TriageStatus status, Long departmentId) {
        List<PatientTriageCase> list;
        if (status != null && departmentId != null) {
            list = patientCaseRepository.findByTriageStatus(status).stream()
                    .filter(c -> c.getDepartment() != null && c.getDepartment().getId().equals(departmentId))
                    .toList();
        } else if (status != null) {
            list = patientCaseRepository.findByTriageStatus(status);
        } else if (departmentId != null) {
            list = patientCaseRepository.findByDepartmentId(departmentId);
        } else {
            list = patientCaseRepository.findAll();
        }

        return list.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public TriageDtos.PatientCaseResponse getCaseById(Long id) {
        PatientTriageCase c = patientCaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient case not found with id: " + id));
        return mapToResponse(c);
    }

    @Transactional(readOnly = true)
    public List<TriageDtos.PatientCaseResponse> getMyCases() {
        User user = authService.getCurrentAuthenticatedUser();
        if (user == null) {
            return Collections.emptyList();
        }
        return patientCaseRepository.findByAdmittedBy_Id(user.getId()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public TriageDtos.PatientCaseResponse updateCaseStatus(Long id, TriageDtos.UpdateTriageStatusRequest request) {
        PatientTriageCase c = patientCaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient case not found with id: " + id));

        TriageStatus previousStatus = c.getTriageStatus();
        if (request.getStatus() != null) {
            c.setTriageStatus(request.getStatus());

            // If discharged, decrement department occupancy
            if (request.getStatus() == TriageStatus.DISCHARGED && previousStatus != TriageStatus.DISCHARGED) {
                if (c.getDepartment() != null && c.getDepartment().getCurrentOccupancy() > 0) {
                    c.getDepartment().setCurrentOccupancy(c.getDepartment().getCurrentOccupancy() - 1);
                    departmentRepository.save(c.getDepartment());
                }
            }
        }

        if (request.getAssignedDoctorId() != null) {
            doctorRepository.findById(request.getAssignedDoctorId()).ifPresent(c::setAssignedDoctor);
        }

        return mapToResponse(patientCaseRepository.save(c));
    }

    @Transactional
    public TriageDtos.PatientCaseResponse runAiTriageEvaluation(Long id) {
        PatientTriageCase c = patientCaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient case not found with id: " + id));

        // 1. Calculate blended risk score & acuity
        int score = scoringContext.calculateBlendedRiskScore(c, c.getDepartment());
        String acuity = ScoringStrategyContext.determineAcuityLevel(score);
        c.setTriageRiskScore(score);
        c.setAiAcuityLevel(acuity);
        c.setTriageStatus(TriageStatus.AI_EVALUATED);

        // 2. Invoke Autonomous Clinical Tools to enrich case
        try {
            String vitalsAnalysis = agentService.executeDirectTool("analyzePatientVitalsAndBiometrics",
                    Map.of("vitalSigns", c.getVitalSigns(), "patientId", c.getPatientId()));
            c.setAiRiskFlags(vitalsAnalysis);

            String diffDiag = agentService.executeDirectTool("generateDifferentialDiagnosis",
                    Map.of("chiefComplaint", c.getChiefComplaint(), "medicalHistory", c.getMedicalHistory() != null ? c.getMedicalHistory() : "", "patientId", c.getPatientId()));
            c.setAiDifferentialDiagnosis(diffDiag);

            String drugScreen = agentService.executeDirectTool("checkDrugInteractionsAndContraindications",
                    Map.of("allergies", c.getAllergies() != null ? c.getAllergies() : "None", "medicalHistory", c.getMedicalHistory() != null ? c.getMedicalHistory() : "", "proposedMedication", "Standard Protocol"));
            c.setAiSuggestedWorkup(drugScreen);
        } catch (Exception e) {
            log.warn("Non-fatal tool execution note during AI triage: {}", e.getMessage());
        }

        return mapToResponse(patientCaseRepository.save(c));
    }

    @Transactional
    public TriageDtos.BatchTriageResult runBatchTriage() {
        long startTime = System.currentTimeMillis();
        List<PatientTriageCase> incoming = patientCaseRepository.findAll();

        CompletableFuture<List<PatientTriageCase>> future = asyncBatchService.processBatchTriageAsync(incoming);
        List<PatientTriageCase> processed = future.join();

        long duration = System.currentTimeMillis() - startTime;
        int criticalCount = (int) processed.stream().filter(c -> c.getTriageRiskScore() != null && c.getTriageRiskScore() >= 80).count();

        return TriageDtos.BatchTriageResult.builder()
                .totalProcessed(processed.size())
                .criticalHighRiskCount(criticalCount)
                .executionTimeMs(duration)
                .cases(processed.stream().map(this::mapToResponse).toList())
                .build();
    }

    public TriageDtos.PatientCaseResponse mapToResponse(PatientTriageCase c) {
        return TriageDtos.PatientCaseResponse.builder()
                .id(c.getId())
                .patientId(c.getPatientId())
                .patientName(c.getPatientName())
                .age(c.getAge())
                .gender(c.getGender())
                .chiefComplaint(c.getChiefComplaint())
                .vitalSigns(c.getVitalSigns())
                .allergies(c.getAllergies())
                .medicalHistory(c.getMedicalHistory())
                .triageStatus(c.getTriageStatus())
                .triageRiskScore(c.getTriageRiskScore())
                .aiAcuityLevel(c.getAiAcuityLevel())
                .aiDifferentialDiagnosis(c.getAiDifferentialDiagnosis())
                .aiSuggestedWorkup(c.getAiSuggestedWorkup())
                .aiRiskFlags(c.getAiRiskFlags())
                .departmentId(c.getDepartment() != null ? c.getDepartment().getId() : null)
                .departmentName(c.getDepartment() != null ? c.getDepartment().getName() : null)
                .departmentCode(c.getDepartment() != null ? c.getDepartment().getCode() : null)
                .assignedDoctorId(c.getAssignedDoctor() != null ? c.getAssignedDoctor().getId() : null)
                .assignedDoctorName(c.getAssignedDoctor() != null ? c.getAssignedDoctor().getName() : null)
                .admittedById(c.getAdmittedBy() != null ? c.getAdmittedBy().getId() : null)
                .admittedByName(c.getAdmittedBy() != null ? c.getAdmittedBy().getFullName() : null)
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
