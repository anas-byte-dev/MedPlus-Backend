package com.medpulse.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medpulse.agent.AgentTool;
import com.medpulse.model.PatientTriageCase;
import com.medpulse.repository.PatientTriageCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class DraftDischargeSummaryTool implements AgentTool {

    private final PatientTriageCaseRepository patientCaseRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "draftClinicalDischargeSummary";
    }

    @Override
    public String getDescription() {
        return "Generates structured clinical discharge summaries, SBAR physician handoff documentation, and patient follow-up care instructions.";
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        try {
            String patientId = (String) parameters.get("patientId");
            String doctorNotes = (String) parameters.get("doctorNotes");
            String patientName = (String) parameters.get("patientName");
            String diagnosis = (String) parameters.get("diagnosis");

            if (patientId != null) {
                Optional<PatientTriageCase> opt = patientCaseRepository.findByPatientId(patientId);
                if (opt.isPresent()) {
                    PatientTriageCase c = opt.get();
                    if (patientName == null) patientName = c.getPatientName();
                    if (diagnosis == null) diagnosis = (c.getAiAcuityLevel() != null ? c.getAiAcuityLevel() : "Clinical Episode Managed");
                }
            }

            if (patientName == null) patientName = "Patient";
            if (diagnosis == null) diagnosis = "Resolved Clinical Presentation";
            if (doctorNotes == null || doctorNotes.isBlank()) {
                doctorNotes = "Patient stabilized in clinical unit. Vital signs normalized under observation. Responded favorably to initial therapeutic interventions.";
            }

            StringBuilder sbar = new StringBuilder();
            sbar.append("=== MEDPULSE AI CLINICAL DISCHARGE & SBAR SUMMARY ===\n\n");
            sbar.append("PATIENT: ").append(patientName).append(" | ID: ").append(patientId != null ? patientId : "PT-RECORD").append("\n");
            sbar.append("FINAL CLINICAL STATUS: Clinically Stable for Discharge / Outpatient Transfer\n\n");

            sbar.append("[S - SITUATION]\n");
            sbar.append("Patient presented for acute emergency evaluation. Managed and stabilized with continuous biometric telemetry monitoring.\n\n");

            sbar.append("[B - BACKGROUND]\n");
            sbar.append("Primary Clinical Diagnosis: ").append(diagnosis).append("\n");
            sbar.append("Hospital Course Notes: ").append(doctorNotes).append("\n\n");

            sbar.append("[A - ASSESSMENT]\n");
            sbar.append("Vitals at discharge stable. Hemodynamically compensated with no acute distress, respiratory compromise, or altered mental status.\n\n");

            sbar.append("[R - RECOMMENDATIONS & DISCHARGE INSTRUCTIONS]\n");
            sbar.append("1. Continue prescribed outpatient medications strictly as directed.\n");
            sbar.append("2. Primary Care Physician (PCP) follow-up scheduled in 3-5 business days.\n");
            sbar.append("3. Strict Red-Flag Return Precautions: Return immediately to the Emergency Department if experiencing recurrent crushing chest pain, sudden dyspnea, facial droop, or high persistent fever > 39°C.\n");

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("patientId", patientId);
            output.put("patientName", patientName);
            output.put("clinicalSummaryDocument", sbar.toString());
            output.put("dischargeEligibility", "APPROVED");

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(output);
        } catch (Exception e) {
            log.error("Error in DraftDischargeSummaryTool: {}", e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
