package com.medpulse.agent;

import java.util.Map;

/**
 * Interface for Autonomous MedPulse Agent Tools.
 */
public interface AgentTool {
    String getName();
    String getDescription();
    String execute(Map<String, Object> parameters);
}
