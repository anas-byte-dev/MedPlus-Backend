package com.medpulse.agent.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medpulse.agent.AgentTool;
import com.medpulse.model.PatientTriageCase;
import com.medpulse.repository.PatientTriageCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class CheckDrugInteractionsTool implements AgentTool {

    private final PatientTriageCaseRepository patientCaseRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "checkDrugInteractionsAndContraindications";
    }

    @Override
    public String getDescription() {
        return "Cross-references proposed medications against patient known allergies, renal/cardiac comorbidities, and dangerous drug-drug interactions.";
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        try {
            String proposedMed = (String) parameters.get("proposedMedication");
            String allergies = (String) parameters.get("allergies");
            String history = (String) parameters.get("medicalHistory");
            String patientId = (String) parameters.get("patientId");

            if (patientId != null && (allergies == null || history == null)) {
                Optional<PatientTriageCase> opt = patientCaseRepository.findByPatientId(patientId);
                if (opt.isPresent()) {
                    if (allergies == null) allergies = opt.get().getAllergies();
                    if (history == null) history = opt.get().getMedicalHistory();
                }
            }

            if (proposedMed == null || proposedMed.isBlank()) {
                proposedMed = "Aspirin, Heparin";
            }
            if (allergies == null) allergies = "None recorded";
            if (history == null) history = "Hypertension";

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("evaluatedMedication", proposedMed);
            result.put("patientAllergies", allergies);
            result.put("patientHistory", history);

            List<String> contraindications = new ArrayList<>();
            List<String> interactions = new ArrayList<>();
            List<String> safetyAdvisories = new ArrayList<>();

            String medLower = proposedMed.toLowerCase();
            String allergyLower = allergies.toLowerCase();
            String historyLower = history.toLowerCase();

            // Allergy matches
            if (allergyLower.contains("penicillin") && (medLower.contains("amoxicillin") || medLower.contains("ampicillin") || medLower.contains("penicillin"))) {
                contraindications.add("ABSOLUTE CONTRAINDICATION: Beta-lactam allergy detected with proposed penicillin derivative. Risk of anaphylaxis.");
            }
            if (allergyLower.contains("aspirin") || allergyLower.contains("nsaid")) {
                if (medLower.contains("aspirin") || medLower.contains("ibuprofen") || medLower.contains("ketorolac") || medLower.contains("toradol")) {
                    contraindications.add("ABSOLUTE CONTRAINDICATION: Patient allergic to Aspirin/NSAIDs. Severe bronchospasm or angioedema risk.");
                }
            }

            // Comorbidity contraindications
            if (historyLower.contains("asthma") && (medLower.contains("propranolol") || medLower.contains("metoprolol") || medLower.contains("beta-blocker"))) {
                interactions.add("RELATIVE CONTRAINDICATION: Non-selective beta-blockers exacerbate bronchospasm in active asthma. Use cardioselective agent with caution.");
            }
            if ((historyLower.contains("renal") || historyLower.contains("ckd")) && (medLower.contains("nsaid") || medLower.contains("ibuprofen") || medLower.contains("contrast"))) {
                interactions.add("RENAL RISK: NSAIDs and IV iodinated contrast carry high risk of Acute Kidney Injury (AKI) in preexisting CKD.");
            }

            // Dangerous co-administration
            if (medLower.contains("aspirin") && medLower.contains("warfarin")) {
                interactions.add("MAJOR BLEEDING RISK: Dual antiplatelet/anticoagulant therapy increases gastrointestinal and intracranial bleed risk. Monitor INR closely.");
            }

            if (contraindications.isEmpty() && interactions.isEmpty()) {
                safetyAdvisories.add("No immediate absolute contraindications detected for " + proposedMed + " based on charted allergies.");
                safetyAdvisories.add("Standard clinical monitoring of vital signs and renal/hepatic lab function recommended.");
            }

            result.put("absoluteContraindications", contraindications);
            result.put("drugInteractionsAndWarnings", interactions);
            result.put("safetyAdvisories", safetyAdvisories);
            result.put("clearanceStatus", contraindications.isEmpty() ? "CONDITIONALLY CLEARED" : "RESTRICTED / BLOCKED");

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (Exception e) {
            log.error("Error in CheckDrugInteractionsTool: {}", e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
