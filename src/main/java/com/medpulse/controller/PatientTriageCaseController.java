package com.medpulse.controller;

import com.medpulse.dto.TriageDtos;
import com.medpulse.model.TriageStatus;
import com.medpulse.service.PatientTriageCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/triage")
@RequiredArgsConstructor
@Tag(name = "Triage Cases", description = "Patient Triage Cases & Clinical Decision Pipeline")
public class PatientTriageCaseController {

    private final PatientTriageCaseService triageCaseService;

    @PostMapping
    @Operation(summary = "Submit new patient arrival for triage")
    public ResponseEntity<TriageDtos.PatientCaseResponse> submitTriageCase(@Valid @RequestBody TriageDtos.CreatePatientCaseRequest request) {
        return new ResponseEntity<>(triageCaseService.submitTriageCase(request), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all triage cases with optional filters")
    public ResponseEntity<List<TriageDtos.PatientCaseResponse>> getAllCases(
            @RequestParam(required = false) TriageStatus status,
            @RequestParam(required = false) Long departmentId) {
        return ResponseEntity.ok(triageCaseService.getAllCases(status, departmentId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get clinical case chart by ID")
    public ResponseEntity<TriageDtos.PatientCaseResponse> getCaseById(@PathVariable Long id) {
        return ResponseEntity.ok(triageCaseService.getCaseById(id));
    }

    @GetMapping("/my-cases")
    @Operation(summary = "Get clinical cases admitted by current authenticated user")
    public ResponseEntity<List<TriageDtos.PatientCaseResponse>> getMyCases() {
        return ResponseEntity.ok(triageCaseService.getMyCases());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DOCTOR', 'TRIAGE_NURSE', 'ADMIN')")
    @Operation(summary = "Advance clinical triage stage (Doctor / Nurse)")
    public ResponseEntity<TriageDtos.PatientCaseResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody TriageDtos.UpdateTriageStatusRequest request) {
        return ResponseEntity.ok(triageCaseService.updateCaseStatus(id, request));
    }

    @PostMapping("/{id}/ai-evaluate")
    @PreAuthorize("hasAnyRole('DOCTOR', 'TRIAGE_NURSE', 'ADMIN')")
    @Operation(summary = "Trigger autonomous AI diagnostic assessment for a patient")
    public ResponseEntity<TriageDtos.PatientCaseResponse> runAiTriageEvaluation(@PathVariable Long id) {
        return ResponseEntity.ok(triageCaseService.runAiTriageEvaluation(id));
    }

    @PostMapping("/batch-evaluate")
    @PreAuthorize("hasAnyRole('DOCTOR', 'TRIAGE_NURSE', 'ADMIN')")
    @Operation(summary = "Execute concurrent multithreaded batch screening on all arrivals")
    public ResponseEntity<TriageDtos.BatchTriageResult> runBatchTriage() {
        return ResponseEntity.ok(triageCaseService.runBatchTriage());
    }
}
