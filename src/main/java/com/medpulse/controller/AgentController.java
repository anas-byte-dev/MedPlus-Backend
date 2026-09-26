package com.medpulse.controller;

import com.medpulse.agent.AutonomousAgentService;
import com.medpulse.agent.GeminiClient;
import com.medpulse.dto.AgentDtos;
import com.medpulse.model.AgentAuditLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
@Tag(name = "Agentic AI", description = "Autonomous Clinical Diagnostic Copilot & Tool Execution Engine")
public class AgentController {

    private final AutonomousAgentService agentService;
    private final GeminiClient geminiClient;

    @PostMapping("/chat")
    @Operation(summary = "Interactive conversation with MedPulse Clinical Copilot")
    public ResponseEntity<AgentDtos.AgentChatResponse> chat(@Valid @RequestBody AgentDtos.AgentChatRequest request) {
        return ResponseEntity.ok(agentService.chat(request));
    }

    @PostMapping("/execute-tool")
    @PreAuthorize("hasAnyRole('DOCTOR', 'TRIAGE_NURSE', 'ADMIN')")
    @Operation(summary = "Direct invocation of an autonomous clinical agent tool")
    public ResponseEntity<String> executeTool(@Valid @RequestBody AgentDtos.DirectToolRequest request) {
        return ResponseEntity.ok(agentService.executeDirectTool(request.getToolName(), request.getParameters()));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Fetch real-time audit logs of autonomous tool executions")
    public ResponseEntity<List<AgentAuditLog>> getAuditLogs() {
        return ResponseEntity.ok(agentService.getRecentAuditLogs());
    }

    @GetMapping("/config")
    @Operation(summary = "Retrieve current Gemini AI model connection status")
    public ResponseEntity<AgentDtos.GeminiConfigResponse> getConfig() {
        return ResponseEntity.ok(geminiClient.getConfigStatus());
    }

    @PostMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update Google Gemini API key or active model (Admin)")
    public ResponseEntity<AgentDtos.GeminiConfigResponse> updateConfig(@RequestBody AgentDtos.UpdateGeminiConfigRequest request) {
        geminiClient.updateConfig(request.getApiKey(), request.getModelName());
        return ResponseEntity.ok(geminiClient.getConfigStatus());
    }
}
