package com.medpulse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AgentDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AgentChatRequest {
        @NotBlank(message = "Message prompt cannot be empty")
        private String message;
        private Long patientCaseId;
        private Long departmentId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AgentChatResponse {
        private String reply;
        private List<ToolExecutionLogDto> executedTools;
        private String activeModel;
        private boolean liveModelUsed;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DirectToolRequest {
        @NotBlank(message = "Tool name is required")
        private String toolName;
        private Map<String, Object> parameters;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ToolExecutionLogDto {
        private String toolName;
        private String inputSummary;
        private String outputSummary;
        private Long latencyMs;
        private LocalDateTime timestamp;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GeminiConfigResponse {
        private boolean configured;
        private boolean liveConnected;
        private String activeModel;
        private String maskedKey;
        private List<String> availableModels;
        private String lastError;
        private String simulatedModeReason;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateGeminiConfigRequest {
        private String apiKey;
        private String modelName;
    }
}
