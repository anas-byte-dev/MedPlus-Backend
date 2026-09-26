package com.medpulse.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medpulse.dto.AgentDtos;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Component
@Slf4j
public class GeminiClient {

    private static final String CONFIG_FILE_NAME = "medpulse-ai.properties";
    private static final List<String> AVAILABLE_MODELS = List.of(
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-3.5-flash-lite",
            "gemini-3.8-flash"
    );

    @Value("${medpulse.gemini.api-key:}")
    private String initialApiKey;

    @Value("${medpulse.gemini.model:gemini-2.5-flash}")
    private String initialModelName;

    private volatile String apiKey = "";
    private volatile String modelName = "gemini-2.5-flash";
    private volatile String lastError = null;
    private volatile boolean isLiveConnected = false;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        loadFromPropertiesFile();

        if (!hasApiKey() && initialApiKey != null && !initialApiKey.trim().isEmpty()) {
            this.apiKey = initialApiKey.trim();
            log.info("Loaded Gemini API key from application.yml for MedPulse AI");
        }

        if (!hasApiKey()) {
            String envKey = System.getenv("GEMINI_API_KEY");
            if (envKey != null && !envKey.trim().isEmpty()) {
                this.apiKey = envKey.trim();
                log.info("Loaded Gemini API key from GEMINI_API_KEY environment variable");
            }
        }

        if (initialModelName != null && !initialModelName.trim().isEmpty()) {
            this.modelName = initialModelName.trim();
        }

        if (hasApiKey()) {
            log.info("MedPulse AI initialized with Gemini API key. Active model: {}", modelName);
            new Thread(this::verifyConnectionSilently).start();
        } else {
            log.info("MedPulse AI running in Intelligent Clinical Fallback mode. Gemini key can be updated in UI.");
        }
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    public boolean isLiveConnected() {
        return isLiveConnected && hasApiKey();
    }

    public String getActiveModel() {
        return modelName;
    }

    public String getMaskedApiKey() {
        if (!hasApiKey()) return "NOT_CONFIGURED";
        if (apiKey.length() <= 8) return "••••••••";
        return apiKey.substring(0, 4) + "••••••••" + apiKey.substring(apiKey.length() - 4);
    }

    public AgentDtos.GeminiConfigResponse getConfigStatus() {
        return AgentDtos.GeminiConfigResponse.builder()
                .configured(hasApiKey())
                .liveConnected(isLiveConnected())
                .activeModel(modelName)
                .maskedKey(getMaskedApiKey())
                .availableModels(AVAILABLE_MODELS)
                .lastError(lastError)
                .simulatedModeReason(isLiveConnected() ? null :
                        (!hasApiKey() ? "No Google Gemini API key configured. Operating in high-fidelity simulated clinical agent mode."
                                : "Gemini API key could not reach endpoint. Falling back to local clinical intelligence engine."))
                .build();
    }

    public synchronized void updateConfig(String newApiKey, String newModelName) {
        if (newApiKey != null && !newApiKey.trim().isEmpty()) {
            this.apiKey = newApiKey.trim();
        }
        if (newModelName != null && !newModelName.trim().isEmpty()) {
            this.modelName = newModelName.trim();
        }
        this.lastError = null;
        saveToPropertiesFile();
        verifyConnectionSilently();
    }

    private void verifyConnectionSilently() {
        if (!hasApiKey()) {
            this.isLiveConnected = false;
            return;
        }
        try {
            String testPrompt = "Respond with 'READY' if clinical agent is online.";
            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey;

            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", testPrompt))))
            );

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(6))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                this.isLiveConnected = true;
                this.lastError = null;
                log.info("MedPulse AI live connection verified successfully with model: {}", modelName);
            } else {
                this.isLiveConnected = false;
                this.lastError = "HTTP " + resp.statusCode() + ": " + resp.body();
                log.warn("Gemini connection test failed: {}", this.lastError);
            }
        } catch (Exception e) {
            this.isLiveConnected = false;
            this.lastError = e.getMessage();
            log.warn("Gemini verification failed silently: {}", e.getMessage());
        }
    }

    public String generateClinicalResponse(String prompt, String systemContext) {
        if (isLiveConnected()) {
            try {
                String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey;

                String fullPrompt = "SYSTEM CLINICAL DIRECTIVE: You are Dr. MedPlus AI Health Consultant, an empathetic, highly knowledgeable medical triage and clinical health advisor for the MedPlus Appointments healthcare platform.\n"
                        + "GUIDELINES FOR YOUR RESPONSE:\n"
                        + "1. TONE: Warm, humanoid, respectful, and clinically reliable. Avoid robotic disclaimers or repetitive AI phrasing.\n"
                        + "2. MINOR AILMENTS (e.g., mild cold, tension headache, minor indigestion, dehydration, minor scrape, fatigue, sleep hygiene):\n"
                        + "   - Provide practical, safe home care, hydration, rest, and natural relief steps.\n"
                        + "   - Explain what signs to monitor.\n"
                        + "3. SERIOUS OR ACUTE SYMPTOMS (e.g., chest tightness/pain, acute breathlessness, sudden numbness, high persistent fever, severe abdominal pain, persistent vomiting):\n"
                        + "   - Emphasize the importance of urgent in-person medical evaluation.\n"
                        + "   - Clearly suggest the appropriate medical specialist (e.g. Cardiologist, Neurologist, Pulmonologist, Gastroenterologist) or Emergency Department.\n"
                        + "4. RELEVANCE: Answer whatever the patient or clinician asks dynamically and thoroughly.\n\n"
                        + (systemContext != null && !systemContext.isBlank() ? "PATIENT CONTEXT:\n" + systemContext + "\n\n" : "")
                        + "USER QUERY:\n" + prompt;

                Map<String, Object> body = Map.of(
                        "contents", List.of(Map.of("parts", List.of(Map.of("text", fullPrompt))))
                );

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(15))
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                        .build();

                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(resp.body());
                    JsonNode textNode = root.at("/candidates/0/content/parts/0/text");
                    if (!textNode.isMissingNode() && !textNode.asText().isBlank()) {
                        return textNode.asText();
                    }
                } else {
                    log.warn("Gemini API returned status {}: {}", resp.statusCode(), resp.body());
                }
            } catch (Exception e) {
                log.warn("Live Gemini API call failed, using intelligent clinical fallback engine: {}", e.getMessage());
            }
        }

        return generateClinicalFallback(prompt, systemContext);
    }

    private String generateClinicalFallback(String prompt, String context) {
        String p = prompt.toLowerCase();

        // 1. Critical / Red-Flag Symptoms: High Acuity -> Strongly advise specialist / emergency
        if (p.contains("chest pain") || p.contains("heart") || p.contains("angina") || p.contains("palpitation") || p.contains("left arm")) {
            return "### ⚠️ Cardiac Health Advisory — Urgent Medical Attention Recommended\n\n"
                    + "Chest discomfort, pressure, or radiating pain to the arm, neck, or jaw can be a sign of acute cardiac strain or ischemia and should **never be ignored**.\n\n"
                    + "**Immediate Steps:**\n"
                    + "1. **Stop all physical exertion immediately** and sit in a comfortable, upright position.\n"
                    + "2. If you experience shortness of breath, cold sweats, or dizziness, arrange **emergency medical transport** to the nearest cardiac center immediately.\n"
                    + "3. **Recommended Specialist:** You should consult a **Cardiologist / Cardiovascular Specialist** right away (such as *Dr. Shahabuddin Alam* or *Dr. Abhishek Mishra* in Muzaffarpur, or *Dr. Balram Bhargava* at AIIMS New Delhi, or *Dr. Anupam Bhambhani* at AIIMS Patna).\n"
                    + "4. An emergency 12-lead ECG and cardiac enzyme evaluation are critical to rule out acute events.";
        }

        if (p.contains("breath") || p.contains("cough") || p.contains("lung") || p.contains("asthma") || p.contains("wheez")) {
            boolean severe = p.contains("severe") || p.contains("blood") || p.contains("cannot breathe") || p.contains("gasp");
            if (severe) {
                return "### ⚠️ Severe Respiratory Alert\n\n"
                        + "Acute breathing difficulty or hemoptysis (coughing blood) requires urgent oxygen saturation measurement and immediate hospital evaluation.\n\n"
                        + "**Recommendations:**\n"
                        + "- Check SpO2 with a pulse oximeter if available (normal > 95%).\n"
                        + "- Do not lie flat; sit upright to ease respiratory effort.\n"
                        + "- **Consult a Pulmonologist immediately** (e.g. *Dr. Randeep Guleria* at Institute of Internal Medicine, Delhi) or visit the nearest emergency facility.";
            } else {
                return "### 🫁 Respiratory Care & Guidance\n\n"
                        + "For persistent cough or mild respiratory congestion:\n\n"
                        + "- **Hydration & Steam:** Warm steam inhalation twice daily helps loosen airways. Sip warm water, ginger tea, or honey with warm water.\n"
                        + "- **Rest:** Ensure adequate rest and avoid exposure to cold air, smoke, or dust allergens.\n"
                        + "- **When to see a Doctor:** If symptoms persist beyond 5–7 days, or if accompanied by fever > 101°F or chest tightness, book an in-person consultation with a **Pulmonologist** or **General Physician**.";
            }
        }

        if (p.contains("headache") || p.contains("migraine") || p.contains("dizziness") || p.contains("numbness") || p.contains("seizure")) {
            boolean neuroRedFlag = p.contains("numb") || p.contains("weak") || p.contains("slurr") || p.contains("worst") || p.contains("seizure");
            if (neuroRedFlag) {
                return "### ⚠️ Neurological Warning Signs Detected\n\n"
                        + "Sudden weakness on one side of the body, numbness, facial drooping, speech difficulty, or a sudden 'thunderclap' headache are potential neurological emergencies.\n\n"
                        + "**Action Required:**\n"
                        + "- Perform the **FAST** check (Face drooping, Arm weakness, Speech difficulty, Time to call emergency).\n"
                        + "- Immediately proceed to an emergency department with a stroke unit or consult a **Neurologist / Neurosurgeon** (such as *Dr. Prakash Kumar Sinha* or *Dr. Mayank Kumar* in Muzaffarpur).\n"
                        + "- Do not administer unprescribed blood thinners without a CT/MRI scan.";
            } else {
                return "### 💆‍♂️ Headache & Tension Relief Guidance\n\n"
                        + "Most mild to moderate headaches stem from dehydration, screen eye-strain, lack of sleep, or muscle tension.\n\n"
                        + "**Practical Home Measures:**\n"
                        + "- Drink 500 mL of water and rest in a dark, quiet room for 30 minutes.\n"
                        + "- Apply a cool compress to the forehead or warm cloth to the back of the neck.\n"
                        + "- Limit screen time and take regular 20-20-20 visual breaks.\n"
                        + "- **Consultation Advice:** If headaches become frequent (more than twice weekly) or are accompanied by visual disturbances, book an evaluation with a **Neurologist** or **General Physician**.";
            }
        }

        if (p.contains("fever") || p.contains("temperature") || p.contains("cold") || p.contains("flu") || p.contains("throat")) {
            return "### 🌡️ Fever & Viral Infection Management\n\n"
                    + "Mild viral infections and low-grade fevers can often be managed with supportive care, but monitoring temperature trajectory is essential.\n\n"
                    + "**Home Recovery Care:**\n"
                    + "1. **Hydration:** Consume oral fluids (electrolyte water, coconut water, warm broths) to replace fluid loss.\n"
                    + "2. **Rest:** Full bed rest allows your immune system to fight infection effectively.\n"
                    + "3. **Temperature Control:** Use lukewarm sponge baths if temperature exceeds 101°F.\n\n"
                    + "**Doctor Consultation Criteria:**\n"
                    + "If fever persists for **more than 3 consecutive days**, exceeds 102.5°F, or is accompanied by rash, joint pain, or severe lethargy, book a consultation with a **General Physician / Internal Medicine Specialist** (e.g., *Dr. Navneet Kumar* in Muzaffarpur, or *Dr. Arvind Kumar* at AIIMS New Delhi) for complete blood counts and diagnostic workup.";
        }

        if (p.contains("stomach") || p.contains("acid") || p.contains("gastric") || p.contains("abdomen") || p.contains("liver") || p.contains("digest")) {
            return "### 🩺 Digestive & Gastrointestinal Health Guidance\n\n"
                    + "**For Mild Acidity or Indigestion:**\n"
                    + "- Eat smaller, frequent meals and avoid lying down for at least 2 hours after eating.\n"
                    + "- Avoid deep-fried, highly spicy foods, carbonated beverages, and excess caffeine.\n"
                    + "- Sip lukewarm water or chamomile/fennel tea to soothe gastric mucosa.\n\n"
                    + "**When to Consult a Specialist:**\n"
                    + "If you experience sharp localized abdominal pain, persistent vomiting, unexplained weight loss, or yellowing of the eyes/skin (jaundice), book an appointment with a **Gastroenterologist / Liver Specialist** (e.g. *Dr. Amitesh Kumar* in Muzaffarpur).";
        }

        if (p.contains("diabetes") || p.contains("sugar") || p.contains("thyroid") || p.contains("glucose")) {
            return "### 🩸 Metabolic & Diabetes Care Support\n\n"
                    + "Maintaining steady glycemic control prevents long-term vascular and nerve complications.\n\n"
                    + "**Key Self-Care Practices:**\n"
                    + "- Log fasting and post-prandial blood glucose levels regularly.\n"
                    + "- Emphasize complex carbohydrates, leafy vegetables, lean proteins, and stay well hydrated.\n"
                    + "- Check feet daily for minor cuts or numbness.\n\n"
                    + "**Specialist Follow-Up:**\n"
                    + "Schedule regular HbA1c reviews (every 3 months) with a **Diabetologist / Endocrine Specialist** (e.g. *Dr. Navneet Kumar* or *Dr. Ajit Kumar Sinha* in Muzaffarpur) to optimize therapeutic medication.";
        }

        // General humanoid medical advice
        return "### 🩺 MedPlus Health Consultant Overview\n\n"
                + "Thank you for reaching out with your health question. Here is a balanced clinical overview:\n\n"
                + "1. **Initial Assessment:** Pay attention to how long these symptoms have been present and whether they are stable or worsening.\n"
                + "2. **Everyday Wellness Steps:** Prioritize hydration (2–3 liters daily), consistent sleep (7–8 hours), and a balanced, low-inflammatory diet.\n"
                + "3. **Professional Consultation:** For specific diagnoses, prescription medications, or laboratory tests, we recommend booking an appointment with one of our **verified specialist doctors** across Muzaffarpur, Delhi, or Patna using the **Find Doctors** tab above.\n\n"
                + "*If you have specific symptoms, please share details such as duration, severity, and any existing medical history for tailored guidance.*";
    }

    private void loadFromPropertiesFile() {
        File file = new File(CONFIG_FILE_NAME);
        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                Properties props = new Properties();
                props.load(in);
                String k = props.getProperty("gemini.api.key");
                String m = props.getProperty("gemini.model");
                if (k != null && !k.trim().isEmpty()) this.apiKey = k.trim();
                if (m != null && !m.trim().isEmpty()) this.modelName = m.trim();
                log.info("Loaded MedPulse AI configuration from {}", CONFIG_FILE_NAME);
            } catch (Exception e) {
                log.error("Failed to load {}", CONFIG_FILE_NAME, e);
            }
        }
    }

    private void saveToPropertiesFile() {
        File file = new File(CONFIG_FILE_NAME);
        try (FileOutputStream out = new FileOutputStream(file)) {
            Properties props = new Properties();
            if (this.modelName != null) {
                props.setProperty("gemini.model", this.modelName);
            }
            props.store(out, "MedPulse AI Runtime Configuration (Keys managed in-memory and via environment)");
            log.info("Saved MedPulse AI runtime settings to {}", CONFIG_FILE_NAME);
        } catch (Exception e) {
            log.error("Failed to save {}", CONFIG_FILE_NAME, e);
        }
    }
}
