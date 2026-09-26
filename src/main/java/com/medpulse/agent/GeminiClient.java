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
            "gemini-flash-latest",
            "gemini-3.5-flash-lite",
            "gemini-3.8-flash",
            "gemini-3.7-flash",
            "gemini-3.5-flash"
    );

    @Value("${medpulse.gemini.api-key:}")
    private String initialApiKey;

    @Value("${medpulse.gemini.model:gemini-flash-latest}")
    private String initialModelName;

    private volatile String apiKey = "";
    private volatile String modelName = "gemini-flash-latest";
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

    private String callGeminiApi(String model, String fullPrompt) throws Exception {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", fullPrompt))))
        );

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(12))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode textNode = root.at("/candidates/0/content/parts/0/text");
            if (!textNode.isMissingNode() && !textNode.asText().isBlank()) {
                return textNode.asText();
            }
        }
        throw new RuntimeException("HTTP " + resp.statusCode() + ": " + resp.body());
    }

    private void verifyConnectionSilently() {
        if (!hasApiKey()) {
            this.isLiveConnected = false;
            return;
        }

        List<String> modelsToVerify = new ArrayList<>();
        if (this.modelName != null && !this.modelName.isBlank()) modelsToVerify.add(this.modelName);
        if (!modelsToVerify.contains("gemini-flash-latest")) modelsToVerify.add("gemini-flash-latest");
        if (!modelsToVerify.contains("gemini-3.5-flash-lite")) modelsToVerify.add("gemini-3.5-flash-lite");

        for (String m : modelsToVerify) {
            try {
                String testPrompt = "Respond with 'READY' if clinical agent is online.";
                String result = callGeminiApi(m, testPrompt);
                if (result != null && !result.isBlank()) {
                    this.isLiveConnected = true;
                    this.modelName = m;
                    this.lastError = null;
                    log.info("MedPulse AI live connection verified successfully with model: {}", m);
                    return;
                }
            } catch (Exception e) {
                log.warn("Gemini verification with model '{}' failed: {}", m, e.getMessage());
                this.lastError = e.getMessage();
            }
        }
        this.isLiveConnected = false;
    }

    public String generateClinicalResponse(String prompt, String systemContext) {
        if (hasApiKey()) {
            String fullPrompt = "SYSTEM CLINICAL DIRECTIVE: You are Dr. MedPlus AI Health Consultant, an empathetic, highly knowledgeable medical triage and clinical health advisor for the MedPlus Appointments healthcare platform.\n"
                    + "GUIDELINES FOR YOUR RESPONSE:\n"
                    + "1. TONE: Warm, humanoid, respectful, and clinically reliable. Avoid robotic disclaimers or repetitive AI phrasing.\n"
                    + "2. MINOR AILMENTS (e.g., mild cold, itching/rash, tension headache, minor indigestion, dehydration, minor scrape, fatigue, sleep hygiene):\n"
                    + "   - Provide practical, safe home care, hydration, rest, and natural relief steps.\n"
                    + "   - Explain what signs to monitor.\n"
                    + "3. SERIOUS OR ACUTE SYMPTOMS (e.g., chest tightness/pain, acute breathlessness, sudden numbness, high persistent fever, severe abdominal pain, persistent vomiting):\n"
                    + "   - Emphasize the importance of urgent in-person medical evaluation.\n"
                    + "   - Clearly suggest the appropriate medical specialist (e.g. Cardiologist, Neurologist, Pulmonologist, Gastroenterologist, Dermatologist) or Emergency Department.\n"
                    + "4. RELEVANCE: Answer whatever the patient or clinician asks dynamically and thoroughly.\n\n"
                    + (systemContext != null && !systemContext.isBlank() ? "PATIENT CONTEXT:\n" + systemContext + "\n\n" : "")
                    + "USER QUERY:\n" + prompt;

            List<String> modelsToTry = new ArrayList<>();
            if (this.modelName != null && !this.modelName.isBlank()) modelsToTry.add(this.modelName);
            if (!modelsToTry.contains("gemini-flash-latest")) modelsToTry.add("gemini-flash-latest");
            if (!modelsToTry.contains("gemini-3.5-flash-lite")) modelsToTry.add("gemini-3.5-flash-lite");

            for (String targetModel : modelsToTry) {
                try {
                    String result = callGeminiApi(targetModel, fullPrompt);
                    if (result != null && !result.isBlank()) {
                        this.isLiveConnected = true;
                        this.lastError = null;
                        this.modelName = targetModel;
                        return result;
                    }
                } catch (Exception e) {
                    log.warn("Gemini call with model '{}' failed: {}", targetModel, e.getMessage());
                    this.lastError = e.getMessage();
                }
            }
        }

        return generateClinicalFallback(prompt, systemContext);
    }

    private String generateClinicalFallback(String prompt, String context) {
        String p = prompt.trim().toLowerCase();

        // 0. Friendly Humanoid Greetings
        if (p.equals("hi") || p.equals("hello") || p.equals("hey") || p.equals("hwl") || p.equals("hlw") || p.equals("helo")
                || p.contains("good morning") || p.contains("good evening") || p.contains("good afternoon") 
                || p.startsWith("hello") || p.startsWith("hi ") || p.equals("namaste") || p.contains("who are you")) {
            return "### 🩺 Welcome to Dr. MedPlus AI Clinical Assistant\n\n"
                    + "Hello! I am **Dr. MedPlus AI**, your clinical health advisor on MedPlus Appointments.\n\n"
                    + "I am here to help you:\n"
                    + "- **Evaluate Health Symptoms:** Describe any symptom you are experiencing (e.g., skin itching, fever, headache, stomach discomfort, or joint pain).\n"
                    + "- **Safe Home Care Advice:** Evidence-based first-line wellness steps, hydration tips, and monitoring guidelines.\n"
                    + "- **Specialist Referrals:** Guide you to the right specialist doctor (Cardiology, Dermatology, Neurology, Orthopedics, Pediatrics, General Medicine) across Muzaffarpur, Patna, and Delhi.\n\n"
                    + "**How are you feeling today?** Feel free to describe any symptoms, when they started, or tap one of the common concerns above.";
        }

        // 1. Critical / Red-Flag Cardiac Symptoms
        if (p.contains("chest pain") || p.contains("heart") || p.contains("angina") || p.contains("palpitation") || p.contains("left arm")) {
            return "### ⚠️ Cardiac Health Advisory — Urgent Medical Attention Recommended\n\n"
                    + "Chest discomfort, pressure, or radiating pain to the arm, neck, or jaw can be a sign of acute cardiac strain or ischemia and should **never be ignored**.\n\n"
                    + "**Immediate Steps:**\n"
                    + "1. **Stop all physical exertion immediately** and sit in a comfortable, upright position.\n"
                    + "2. If you experience shortness of breath, cold sweats, or dizziness, arrange **emergency medical transport** to the nearest cardiac center immediately.\n"
                    + "3. **Recommended Specialist:** You should consult a **Cardiologist / Cardiovascular Specialist** right away (such as *Dr. Shahabuddin Alam* or *Dr. Abhishek Mishra* in Muzaffarpur, or *Dr. Balram Bhargava* at AIIMS New Delhi, or *Dr. Anupam Bhambhani* at AIIMS Patna).\n"
                    + "4. An emergency 12-lead ECG and cardiac enzyme evaluation are critical to rule out acute events.";
        }

        // 2. Dermatology, Rashes, Itching, Allergic Reactions
        if (p.contains("itch") || p.contains("rash") || p.contains("skin") || p.contains("allergy") || p.contains("allergic") 
                || p.contains("hive") || p.contains("redness") || p.contains("eczema") || p.contains("bump") || p.contains("scabies") || p.contains("scratch")) {
            return "### 🧴 Dermatology & Skin Health Guidance\n\n"
                    + "Skin itching (*pruritus*) and rashes are commonly triggered by contact dermatitis (reactions to soap, detergent, or cosmetics), dry skin, heat rash, mild fungal infections, or allergic hives.\n\n"
                    + "**Practical Home Care & Relief Steps:**\n"
                    + "1. **Avoid Scratching:** Scratching injures the epidermis and can introduce bacterial skin infections (*impetigo*). Keep fingernails clipped and clean.\n"
                    + "2. **Cool Compresses:** Place a clean, cool, damp cloth on the itchy areas for 10–15 minutes to soothe inflamed nerve endings.\n"
                    + "3. **Gentle Moisturization:** Apply an unscented, alcohol-free ceramide moisturizer (like petroleum jelly or calamine lotion) while the skin is still slightly damp.\n"
                    + "4. **Tepid Baths:** Bathe in lukewarm water instead of hot water; use mild, fragrance-free cleanser substitutes.\n"
                    + "5. **Over-The-Counter Options:** An oral non-drowsy antihistamine (such as Cetirizine 10mg) or 1% hydrocortisone cream may alleviate allergic flare-ups.\n\n"
                    + "**When to Consult a Dermatologist:**\n"
                    + "If the rash spreads rapidly, forms blisters, oozes pus/fluid, or is accompanied by fever or swelling of the lips/face, book an appointment with a **Dermatologist / Skin Specialist** (e.g. *Dr. Abhishek Mishra* or *Dr. R.K. Jha* in Muzaffarpur or AIIMS Patna) for targeted dermatological evaluation.";
        }

        // 3. Respiratory Symptoms
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

        // 4. Neurological & Headache Symptoms
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

        // 5. Fever & Infectious Diseases
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

        // 6. Musculoskeletal, Joint, Spine & Orthopedic Pain
        if (p.contains("knee") || p.contains("joint") || p.contains("bone") || p.contains("back") || p.contains("spine") 
                || p.contains("muscle") || p.contains("neck pain") || p.contains("arthritis") || p.contains("sprain") || p.contains("leg pain")) {
            return "### 🦴 Orthopedic & Joint Care Guidance\n\n"
                    + "Musculoskeletal discomfort often stems from postural strain, repetitive physical loading, ligament sprains, or degenerative joint wear.\n\n"
                    + "**Supportive Care Steps (R.I.C.E. Protocol):**\n"
                    + "1. **Rest & Modification:** Avoid heavy lifting, sudden twisting movements, or prolonged standing on hard surfaces.\n"
                    + "2. **Ice vs. Warmth:** Apply cold packs wrapped in a towel for 15 minutes during the initial 48 hours for acute swelling; apply gentle warm compresses for chronic stiffness.\n"
                    + "3. **Posture & Ergonomics:** Maintain upright spinal alignment and use a firm, supportive mattress or lumbar support cushion.\n"
                    + "4. **Low-Impact Movement:** Gentle hamstring and calf stretches promote blood circulation without joint overload.\n\n"
                    + "**When to Consult an Orthopedic Specialist:**\n"
                    + "If you experience inability to bear weight, joint swelling with warmth, persistent numbness/tingling radiating down your legs, or severe pain lasting > 7 days, schedule a consultation with an **Orthopedic Specialist / Spine Surgeon** (e.g. *Dr. Ramakant Kumar* in Patna or *Dr. Rajesh Malhotra* at AIIMS New Delhi).";
        }

        // 7. Digestive & Gastrointestinal Health
        if (p.contains("stomach") || p.contains("acid") || p.contains("gastric") || p.contains("abdomen") || p.contains("liver") || p.contains("digest") || p.contains("constipat") || p.contains("vomit")) {
            return "### 🩺 Digestive & Gastrointestinal Health Guidance\n\n"
                    + "**For Mild Acidity or Indigestion:**\n"
                    + "- Eat smaller, frequent meals and avoid lying down for at least 2 hours after eating.\n"
                    + "- Avoid deep-fried, highly spicy foods, carbonated beverages, and excess caffeine.\n"
                    + "- Sip lukewarm water or chamomile/fennel tea to soothe gastric mucosa.\n\n"
                    + "**When to Consult a Specialist:**\n"
                    + "If you experience sharp localized abdominal pain, persistent vomiting, unexplained weight loss, or yellowing of the eyes/skin (jaundice), book an appointment with a **Gastroenterologist / Liver Specialist** (e.g. *Dr. Amitesh Kumar* in Muzaffarpur).";
        }

        // 8. Diabetes & Endocrine Health
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

        // 9. Contextual Dynamic Health Guidance
        return "### 🩺 MedPlus Health Consultant Evaluation\n\n"
                + "Thank you for describing your health concern regarding: **" + prompt + "**.\n\n"
                + "**Clinical Observations & Guidance:**\n"
                + "1. **Symptom Monitoring:** Track how long this issue has persisted, its intensity on a scale of 1–10, and whether it worsens with specific activities, foods, or positions.\n"
                + "2. **General Supportive Care:** Ensure adequate fluid intake (2–3 liters daily), restorative sleep (7–8 hours), and avoid unprescribed self-medication.\n"
                + "3. **Recommended Medical Evaluation:** For an accurate clinical diagnosis, physical examination, and appropriate diagnostic tests (blood panel, imaging, or prescriptions), we recommend booking a consultation with one of our **verified specialist doctors** using the **Find Doctors** tab above.\n\n"
                + "*If you experience severe pain, high fever, difficulty breathing, or sudden weakness, please seek immediate in-person emergency medical care.*";
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
