package com.medpulse.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String toolName;

    @Column(length = 2000)
    private String inputSummary;

    @Column(length = 4000)
    private String outputSummary;

    private Long latencyMs;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
