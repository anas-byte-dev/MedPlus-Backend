# 🏥 MedPlus Backend — Enterprise Spring Boot Healthcare Engine

- **Frontend Repository:** [https://github.com/anas-byte-dev/MedPlus-Frontend](https://github.com/anas-byte-dev/MedPlus-Frontend)
- **Backend Repository:** [https://github.com/anas-byte-dev/MedPlus-Backend](https://github.com/anas-byte-dev/MedPlus-Backend)

Welcome to the backend service of **MedPlus Appointments** — a high-performance clinical management API built with **Java 21/25**, **Spring Boot 3**, **Spring Security 6**, and **Spring Data JPA**. It provides secure multi-role access for patients, doctors, hospital admins, along with clinical triage workflows and Gemini AI health assistance.

---

## 🏛️ System Architecture

Key architectural highlights:
1. **Stateless JWT Authentication (Spring Security 6)**:
   - Every protected route is guarded by my custom `JwtAuthFilter`.
   - Passwords are encrypted using strong BCrypt hashing.
   - Roles (`ROLE_PATIENT`, `ROLE_DOCTOR`, `ROLE_HOSPITAL`, `ROLE_ADMIN`) strictly gate sensitive endpoints.
2. **Dual Database Flexibility (PostgreSQL via Supabase + H2)**:
   - Configured with HikariCP connection pooling to connect to PostgreSQL (Supabase cloud database with SSL enabled).
   - Can easily run locally or in memory with H2 for rapid development and testing without spinning up external cloud databases.
3. **Clinical Decision Support & Health Advisor (Gemini Integration)**:
   - Integrated Google Gemini via `GeminiClient` with automated clinical tool execution:
   - Evaluates symptoms and executes specialized clinical tools when needed:
     - 🩺 **AnalyzeVitalsTool**: Evaluates blood pressure, pulse, SpO2, and temperature against clinical thresholds.
     - 💊 **CheckDrugInteractionsTool**: Scans proposed prescriptions against patient allergies and contraindications.
     - 📋 **DifferentialDiagnosisTool**: Synthesizes chief complaints into prioritized differential workups.
     - 📝 **DraftDischargeSummaryTool**: Formats structured SBAR (Situation, Background, Assessment, Recommendation) discharge notes.
   - All tool executions are recorded in `AgentAuditLog` for medical audit compliance.
4. **Interactive OpenAPI / Swagger Documentation**:
   - Fully documented using `springdoc-openapi-starter-webmvc-ui` (v2.8.5). Anyone can open the browser and test all endpoints interactively.

---

## 🛠️ Technology Stack

| Layer | Technologies Used |
| :--- | :--- |
| **Language & Runtime** | Java 21 / 25 |
| **Framework** | Spring Boot 3.4+ / 4.x |
| **Security** | Spring Security 6, JJWT (`io.jsonwebtoken` 0.12.6), BCrypt |
| **Data & Persistence** | Spring Data JPA, Hibernate, HikariCP |
| **Databases Supported** | Cloud PostgreSQL (Supabase) & In-memory H2 |
| **AI & LLM Engine** | Google Gemini Live API (`gemini-2.5-flash`, `gemini-3.8-flash`) |
| **API Documentation** | Swagger UI / OpenAPI 3.0 |
| **Build Tool** | Apache Maven with Maven Wrapper (`mvnw`) |

---

## 📡 API Architecture & Endpoints

### 🔐 1. Authentication & Users (`/api/auth`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Public | Register a new user (Patient, Doctor, Hospital staff) |
| `POST` | `/api/auth/login` | Public | Authenticate with email/password; returns JWT token + user details |
| `GET` | `/api/auth/me` | Authenticated | Fetch current user session profile |
| `GET` | `/api/auth/users` | Admin Only | Inspect all registered database accounts |

### 👨‍⚕️ 2. Doctor Directory (`/api/doctors`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/doctors` | Public | List all 41+ doctors with optional city & specialty filters |
| `GET` | `/api/doctors/{id}` | Public | Get single doctor details, fee, OPD schedule |
| `POST` | `/api/doctors` | Hospital / Admin | Onboard a new specialist doctor into the DBMS |
| `PUT` | `/api/doctors/{id}` | Hospital / Admin | Update doctor profile or OPD hours |

### 📅 3. Appointments Management (`/api/appointments`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/appointments` | Patient / Public | Book a new consultation slot; generates unique reference |
| `GET` | `/api/appointments/patient` | Patient | List appointments booked by the logged-in patient |
| `GET` | `/api/appointments/doctor/{doctorId}` | Doctor / Staff | List upcoming OPD queue for a specific doctor |
| `GET` | `/api/appointments/hospital/{hospitalName}` | Hospital / Admin | List facility-wide consultations |
| `PATCH`| `/api/appointments/{id}/status` | Doctor / Admin | Change status (`CONFIRMED`, `COMPLETED`, `CANCELLED`) |

### 🤖 4. Autonomous Clinical Copilot (`/api/agent`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/agent/chat` | Authenticated | Clinical chat with autonomous ReAct tool orchestration |
| `GET` | `/api/agent/config` | Admin | Retrieve current Gemini model and runtime settings |
| `POST` | `/api/agent/config` | Admin | Update Gemini API key or active model on the fly |
| `POST` | `/api/agent/tools/execute` | Staff / Admin | Directly execute a specific clinical tool |
| `GET` | `/api/agent/audit` | Admin | View audit trail of all AI decisions and tool runs |

### 🚑 5. Emergency Triage & Analytics (`/api/triage`, `/api/analytics`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/triage/assess` | Staff / Triage Nurse | Compute acuity level (RED/ORANGE/YELLOW/GREEN) & NEWS2 score |
| `GET` | `/api/analytics/summary` | Admin / Hospital | Get hospital occupancy, total appointments, and doctor counts |

---

## ⚙️ Configuration & Environment Variables

The backend reads configuration from `application.yml` with sensible defaults. You can override values via environment variables or a local `.env` file:

```properties
# Server
PORT=8080

# Database Configuration (PostgreSQL / Supabase)
DATABASE_URL=jdbc:postgresql://your-host:5432/postgres?sslmode=require
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your_secure_password
DDL_AUTO=none

# Security & JWT
JWT_SECRET=your_super_secret_base64_or_long_random_key_min_256_bits
JWT_EXPIRATION_MS=86400000

# Google Gemini AI Integration
GEMINI_API_KEY=your_gemini_api_key_here
GEMINI_MODEL=gemini-2.5-flash
```

---

## 🚀 How to Run Locally

### 1. Prerequisites
- **Java Development Kit (JDK 21 or 25)** installed and on your `PATH`.
- **Git**.
*(Maven is already included via `mvnw.cmd` on Windows and `./mvnw` on Linux/macOS).*

### 2. Open Terminal in `backend/` directory
```bash
cd backend
```

### 3. Build & Run
On Windows (PowerShell or Command Prompt):
```powershell
.\mvnw.cmd spring-boot:run
```
On Linux or macOS:
```bash
./mvnw spring-boot:run
```

Once the application boots:
- **Backend Base URL**: `http://localhost:8080`
- **Interactive Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON Spec**: `http://localhost:8080/v3/api-docs`
- **H2 In-Memory Console** (when enabled): `http://localhost:8080/h2-console`

---

## 🧪 Testing the APIs

You can test endpoints easily either through **Swagger UI** or via standard `curl`:

#### Health Check:
```bash
curl http://localhost:8080/api/health
```

#### User Login:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"patient@medplus.com","password":"password123"}'
```

#### Fetch Doctors:
```bash
curl http://localhost:8080/api/doctors
```

---

## 📁 Backend Directory Architecture

```
backend/
├── pom.xml                     # Maven dependencies (Spring Boot, Security, JJWT, JPA)
├── src/main/
│   ├── resources/
│   │   └── application.yml     # Spring datasource, JWT, Gemini, and Hikari settings
│   └── java/com/medpulse/
│       ├── MedpulseBackendApplication.java # Spring Boot entry point
│       ├── config/             # CORS config, RestTemplate & Jackson beans
│       ├── security/           # JwtAuthFilter, JwtUtil, SecurityConfig, UserDetailsServiceImpl
│       ├── controller/         # REST Controllers (Auth, Doctor, Appointment, Agent, etc.)
│       ├── service/            # Core business logic services
│       ├── model/              # JPA Entities (User, Doctor, Appointment, TriageCase, etc.)
│       ├── repository/         # Spring Data JPA Repositories
│       ├── dto/                # Request & Response DTOs
│       ├── exception/          # Global exception handlers & custom errors
│       └── agent/              # Autonomous AI Clinical Copilot
│           ├── AutonomousAgentService.java  # ReAct reasoning loop
│           ├── GeminiClient.java            # Google Gemini HTTP client
│           └── tools/                       # AnalyzeVitals, DrugInteractions, DifferentialDiagnosis
```

---

## 👨‍💻 Author

**Anas Siddiqui**  
*Java Full Stack & AI Healthcare Developer*
