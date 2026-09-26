package com.medpulse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

public class DepartmentDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateDepartmentRequest {
        @NotBlank(message = "Department name is required")
        private String name;

        @NotBlank(message = "Department code is required")
        private String code; // e.g. "ED-TRAUMA"

        @NotBlank(message = "Medical specialty is required")
        private String specialty;

        private String headPhysician;

        @NotNull(message = "Bed capacity is required")
        private Integer bedCapacity;

        private String acuityLevel;

        private String description;

        private Set<String> clinicalProtocols;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DepartmentResponse {
        private Long id;
        private String name;
        private String code;
        private String specialty;
        private String headPhysician;
        private Integer bedCapacity;
        private Integer currentOccupancy;
        private String acuityLevel;
        private String description;
        private Set<String> clinicalProtocols;
        private Double occupancyRate;
        private LocalDateTime createdAt;
    }
}
