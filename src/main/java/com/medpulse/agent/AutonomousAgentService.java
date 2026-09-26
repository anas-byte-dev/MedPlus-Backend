package com.medpulse.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medpulse.dto.AgentDtos;
import com.medpulse.model.AgentAuditLog;
import com.medpulse.model.PatientTriageCase;
import com.medpulse.repository.AgentAuditLogRepository;
import com.medpulse.repository.PatientTriageCaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class AutonomousAgentService {

    private final Map<String, AgentTool> tools = new HashMap<>();
    private final GeminiClient geminiClient;
    private final AgentAuditLogRepository auditLogRepository;
    private final PatientTriageCaseRepository patientCaseRepository;
    private final ObjectMapper objectMapper;

    public AutonomousAgentService(
            List<AgentTool> toolList,
            GeminiClient geminiClient,
            AgentAuditLogRepository auditLogRepository,
            PatientTriageCaseRepository patientCaseRepository) {
        this.geminiClient = geminiClient;
        this.auditLogRepository = auditLogRepository;
        this.patientCaseRepository = patientCaseRepository;
        this.objectMapper = new ObjectMapper();

        for (AgentTool tool : toolList) {
            tools.put(tool.getName(), tool);
            log.info("Registered MedPulse Clinical Agent Tool: {}", tool.getName());
        }
    }

    public AgentDtos.AgentChatResponse chat(AgentDtos.AgentChatRequest request) {
        String userMessage = request.getMessage();
        List<AgentDtos.ToolExecutionLogDto> executedTools = new ArrayList<>();

        StringBuilder clinicalContext = new StringBuilder();
        if (request.getPatientCaseId() != null) {
            patientCaseRepository.findById(request.getPatientCaseId()).ifPresent(c -> {
                clinicalContext.append("Active Case: ").append(c.getPatientName())
                        .append(" (ID: ").append(c.getPatientId()).append(")\n")
                        .append("Age: ").append(c.getAge()).append(", Gender: ").append(c.getGender()).append("\n")
                        .append("Chief Complaint: ").append(c.getChiefComplaint()).append("\n")
                        .append("Vital Signs: ").append(c.getVitalSigns()).append("\n")
                        .append("Allergies: ").append(c.getAllergies()).append("\n")
                        .append("Medical History: ").append(c.getMedicalHistory()).append("\n")
                        .append("Current Status: ").append(c.getTriageStatus()).append("\n")
                        .append("Current Risk Score: ").append(c.getTriageRiskScore()).append(" (")
                        .append(c.getAiAcuityLevel()).append(")\n");
            });
        }

        // Autonomous ReAct Intent Recognition: Determine if clinical tools should be invoked
        String lowerMsg = userMessage.toLowerCase();

        if (lowerMsg.contains("vital") || lowerMsg.contains("spo2") || lowerMsg.contains("heart rate") || lowerMsg.contains("blood pressure")) {
            executeToolSilently("analyzePatientVitalsAndBiometrics", Map.of("vitalSigns", clinicalContext.toString()), executedTools);
        }

        if (lowerMsg.contains("differential") || lowerMsg.contains("diagnos") || lowerMsg.contains("cause") || lowerMsg.contains("workup")) {
            executeToolSilently("generateDifferentialDiagnosis", Map.of("chiefComplaint", clinicalContext.toString()), executedTools);
        }

        if (lowerMsg.contains("drug") || lowerMsg.contains("medication") || lowerMsg.contains("prescri") || lowerMsg.contains("contraindicat") || lowerMsg.contains("allergy")) {
            executeToolSilently("checkDrugInteractionsAndContraindications", Map.of("proposedMedication", userMessage), executedTools);
        }

        if (lowerMsg.contains("discharge") || lowerMsg.contains("sbar") || lowerMsg.contains("handoff") || lowerMsg.contains("summary")) {
            executeToolSilently("draftClinicalDischargeSummary", Map.of("doctorNotes", userMessage), executedTools);
        }

        // Generate response using Gemini Client (Live or Clinical Fallback)
        String aiReply = geminiClient.generateClinicalResponse(userMessage, clinicalContext.toString());

        return AgentDtos.AgentChatResponse.builder()
                .reply(aiReply)
                .executedTools(executedTools)
                .activeModel(geminiClient.getActiveModel())
                .liveModelUsed(geminiClient.isLiveConnected())
                .build();
    }

    public String executeDirectTool(String toolName, Map<String, Object> params) {
        AgentTool tool = tools.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("Unknown clinical agent tool: " + toolName);
        }

        long start = System.currentTimeMillis();
        String result = tool.execute(params);
        long duration = System.currentTimeMillis() - start;

        // Persist audit log
        AgentAuditLog logEntry = AgentAuditLog.builder()
                .toolName(toolName)
                .inputSummary(summarize(params))
                .outputSummary(result.length() > 3900 ? result.substring(0, 3900) + "..." : result)
                .latencyMs(duration)
                .build();
        auditLogRepository.save(logEntry);

        return result;
    }

    private void executeToolSilently(String toolName, Map<String, Object> params, List<AgentDtos.ToolExecutionLogDto> executed) {
        AgentTool tool = tools.get(toolName);
        if (tool != null) {
            try {
                long start = System.currentTimeMillis();
                String output = tool.execute(params);
                long duration = System.currentTimeMillis() - start;

                AgentAuditLog logEntry = AgentAuditLog.builder()
                        .toolName(toolName)
                        .inputSummary(summarize(params))
                        .outputSummary(output.length() > 3900 ? output.substring(0, 3900) + "..." : output)
                        .latencyMs(duration)
                        .build();
                auditLogRepository.save(logEntry);

                executed.add(AgentDtos.ToolExecutionLogDto.builder()
                        .toolName(toolName)
                        .inputSummary(logEntry.getInputSummary())
                        .outputSummary(logEntry.getOutputSummary())
                        .latencyMs(duration)
                        .timestamp(logEntry.getTimestamp())
                        .build());
            } catch (Exception e) {
                log.warn("Silently handled tool execution error for {}: {}", toolName, e.getMessage());
            }
        }
    }

    public List<AgentAuditLog> getRecentAuditLogs() {
        return auditLogRepository.findTop20ByOrderByTimestampDesc();
    }

    private String summarize(Map<String, Object> params) {
        if (params == null || params.isEmpty()) return "{}";
        try {
            String str = objectMapper.writeValueAsString(params);
            return str.length() > 1900 ? str.substring(0, 1900) + "..." : str;
        } catch (Exception e) {
            return params.toString();
        }
    }
}
