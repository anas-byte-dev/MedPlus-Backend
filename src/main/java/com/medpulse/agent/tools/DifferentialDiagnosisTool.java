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
public class DifferentialDiagnosisTool implements AgentTool {

    private final PatientTriageCaseRepository patientCaseRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "generateDifferentialDiagnosis";
    }

    @Override
    public String getDescription() {
        return "Generates top differential clinical diagnoses and targeted diagnostic workups based on patient symptoms, vitals, age, and history.";
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        try {
            String complaint = (String) parameters.get("chiefComplaint");
            String history = (String) parameters.get("medicalHistory");
            String patientId = (String) parameters.get("patientId");

            if ((complaint == null || complaint.isBlank()) && patientId != null) {
                Optional<PatientTriageCase> opt = patientCaseRepository.findByPatientId(patientId);
                if (opt.isPresent()) {
                    complaint = opt.get().getChiefComplaint();
                    history = opt.get().getMedicalHistory();
                }
            }

            if (complaint == null || complaint.isBlank()) {
                complaint = "Undifferentiated acute symptoms";
            }

            String lower = complaint.toLowerCase();
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("presentingComplaint", complaint);

            List<Map<String, Object>> differentials = new ArrayList<>();
            List<String> suggestedWorkup = new ArrayList<>();
            List<String> cantMissDiagnoses = new ArrayList<>();

            if (lower.contains("chest pain") || lower.contains("substernal") || lower.contains("arm pain")) {
                differentials.add(Map.of(
                        "condition", "Acute Coronary Syndrome (STEMI / NSTEMI)",
                        "probability", "High (Emergency Priority)",
                        "rationale", "Presentation of crushing chest pain radiating to left arm with diaphoresis."
                ));
                differentials.add(Map.of(
                        "condition", "Acute Pulmonary Embolism (PE)",
                        "probability", "Moderate",
                        "rationale", "Evaluate with Wells score and D-dimer if dyspneic."
                ));
                differentials.add(Map.of(
                        "condition", "Aortic Dissection (Stanford Type A/B)",
                        "probability", "Low-Moderate (Critical)",
                        "rationale", "Rule out if tearing pain radiating to interscapular back."
                ));

                cantMissDiagnoses.addAll(List.of("STEMI", "Aortic Dissection", "Tension Pneumothorax", "Pulmonary Embolism"));
                suggestedWorkup.addAll(List.of("Stat 12-Lead ECG (< 10 min door-to-ECG)", "High-Sensitivity Cardiac Troponin-I at 0h and 2h", "Portable Chest Radiograph (CXR)", "Basic Metabolic Panel (BMP) and CBC"));
            } else if (lower.contains("stroke") || lower.contains("facial droop") || lower.contains("hemiparesis") || lower.contains("slurred speech")) {
                differentials.add(Map.of(
                        "condition", "Acute Ischemic Stroke (LVO Suspect)",
                        "probability", "High (Code Stroke Priority)",
                        "rationale", "Focal neurological deficit with acute unilateral motor weakness."
                ));
                differentials.add(Map.of(
                        "condition", "Intracerebral Hemorrhage (ICH)",
                        "probability", "Moderate-High",
                        "rationale", "Requires urgent non-contrast head CT to distinguish from ischemic stroke."
                ));
                differentials.add(Map.of(
                        "condition", "Transient Ischemic Attack (TIA) or Hypoglycemic Mimic",
                        "probability", "Moderate",
                        "rationale", "Immediate fingerstick glucose mandatory."
                ));

                cantMissDiagnoses.addAll(List.of("Large Vessel Occlusion (LVO) Stroke", "Subarachnoid Hemorrhage"));
                suggestedWorkup.addAll(List.of("Emergency Non-contrast Head CT / CT Angiography", "Point-of-Care Blood Glucose", "NIHSS Neurological Assessment", "Coagulation Panel (PT/INR, aPTT)"));
            } else if (lower.contains("dyspnea") || lower.contains("shortness of breath") || lower.contains("wheezing") || lower.contains("asthma")) {
                differentials.add(Map.of(
                        "condition", "Acute Severe Asthma Exacerbation",
                        "probability", "High",
                        "rationale", "Bronchospasm with bilateral expiratory wheezing and tachypnea."
                ));
                differentials.add(Map.of(
                        "condition", "COPD Exacerbation / Acute Bronchitis",
                        "probability", "Moderate",
                        "rationale", "Common respiratory decompensation in patients with smoking or airway history."
                ));
                differentials.add(Map.of(
                        "condition", "Acute Cardiogenic Pulmonary Edema",
                        "probability", "Moderate",
                        "rationale", "Assess for jugular venous distention, rales, and orthopnea."
                ));

                cantMissDiagnoses.addAll(List.of("Tension Pneumothorax", "Acute Pulmonary Embolism", "Impending Respiratory Arrest"));
                suggestedWorkup.addAll(List.of("Continuous Pulse Oximetry & Peak Expiratory Flow (PEF)", "Arterial Blood Gas (ABG)", "Chest X-Ray (AP view)", "Nebulized Albuterol + Ipratropium Bromide"));
            } else {
                differentials.add(Map.of(
                        "condition", "Acute Abdominal Pathology / Inflammatory Process",
                        "probability", "Moderate",
                        "rationale", "Symptomatic presentation warranting focused surgical and medical evaluation."
                ));
                differentials.add(Map.of(
                        "condition", "Systemic Infection / Sepsis Workup",
                        "probability", "Moderate",
                        "rationale", "Monitor inflammatory biomarkers and vital trends."
                ));
                suggestedWorkup.addAll(List.of("Complete Blood Count with Differential (CBC)", "Comprehensive Metabolic Panel (CMP)", "Serum Lactate", "Abdominal Ultrasound or CT Abdomen/Pelvis"));
            }

            response.put("differentialDiagnoses", differentials);
            response.put("cantMissEmergencies", cantMissDiagnoses);
            response.put("recommendedDiagnosticWorkup", suggestedWorkup);

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        } catch (Exception e) {
            log.error("Error in DifferentialDiagnosisTool: {}", e.getMessage());
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }
}
