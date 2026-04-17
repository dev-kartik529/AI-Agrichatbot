`# AgriBot — AI Chatbot for Agricultural Advice

My final semester major project. Built a chatbot that helps farmers get advice on crop diseases, fertilizers, weather, and market prices using Google Gemini AI.

---

## What it does

Farmers can type questions in plain language and get expert advice instantly. No need to call a KVK or wait for an agronomist. The chatbot handles:

- Crop disease identification from symptoms
- Season and weather based crop planning
- Fertilizer and pesticide recommendations with dosage
- MSP rates and where to sell crops

---

## Tech Stack

- **Backend** — Java 17, Spring Boot 3.2
- **AI** — Google Gemini 2.5 Flash API
- **Database** — PostgreSQL (running in Docker)
- **Frontend** — HTML, CSS, JavaScript (Thymeleaf templates)
- **ORM** — Spring Data JPA + Hibernate
- **Build** — Maven

---

## Project Structure

```
agri-chatbot/
├── src/main/java/com/agrichat/
│   ├── AgriChatApplication.java
│   ├── config/
│   │   ├── AppConfig.java
│   │   └── GlobalExceptionHandler.java
│   ├── controller/
│   │   ├── ChatController.java
│   │   └── WebController.java
│   ├── dto/
│   │   ├── ChatRequestDto.java
│   │   └── ChatResponseDto.java
│   ├── entity/
│   │   ├── ChatSession.java
│   │   └── ChatMessage.java
│   ├── repository/
│   │   ├── ChatSessionRepository.java
│   │   └── ChatMessageRepository.java
│   └── service/
│       ├── ChatService.java
│       └── GeminiService.java
├── src/main/resources/
│   ├── application.properties
│   ├── templates/index.html
│   └── static/
│       ├── css/style.css
│       └── js/chat.js
├── database_setup.sql
└── pom.xml
```

---

## How to Run

### Requirements
- Java 17
- Maven
- Docker (for PostgreSQL)

### 1. Start PostgreSQL in Docker

```bash
docker run --name agrichat-postgres \
  -e POSTGRES_PASSWORD=admin123 \
  -e POSTGRES_USER=postgres \
  -p 5432:5432 \
  -d postgres:latest
```

Then create the database:

```bash
docker exec -it agrichat-postgres psql -U postgres -c "CREATE DATABASE agrichat_db;"
```

### 2. Update application.properties

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/agrichat_db
spring.datasource.username=postgres
spring.datasource.password=admin123
gemini.api.key=YOUR_KEY_HERE
gemini.api.url=https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent
```

### 3. Run the project

```bash
mvn spring-boot:run
```

Open `http://localhost:8080` in your browser.

---

## API Endpoints

| Method | Endpoint | What it does |
|--------|----------|--------------|
| POST | `/api/chat/message` | Send a message, get AI reply |
| GET | `/api/chat/history/{sessionId}` | Get full conversation history |
| GET | `/api/chat/health` | Check if server is running |

Example request:

```bash
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "My wheat has yellow spots, what disease is this?"}'
```

---

## Database

Two tables — `chat_sessions` and `chat_messages`.

Sessions store the conversation UUID and when it was last active. Messages store every question and answer with role (USER/ASSISTANT), timestamp, and detected category.

Spring Boot auto-creates both tables on first run using `ddl-auto=update`.

---

## How Gemini is integrated

`GeminiService.java` makes an HTTP POST to the Gemini API endpoint using Spring's `RestTemplate`. Every request includes a system prompt that defines the chatbot as an Indian agricultural expert, plus the last 10 messages from the conversation for context. The response is parsed from JSON and saved to PostgreSQL before being sent back to the frontend.

The category of each message (CROP_DISEASE, WEATHER, FERTILIZER, MARKET_PRICE) is detected using keyword matching and stored alongside the message.

---

## Things I debugged along the way

- `RestTemplateBuilder.connectTimeout()` was removed in Spring Boot 3.2 — switched to `SimpleClientHttpRequestFactory`
- Gemini model `gemini-1.5-flash` returned 404 — had to call the ListModels endpoint to find what was actually available
- First API key had zero quota (`limit: 0`) — created a new key in a fresh Google Cloud project
- `gemini-2.0-flash` also had quota issues — `gemini-2.5-flash` worked fine
- PostgreSQL dialect warning — removed the explicit dialect from properties since Hibernate detects it automatically

---

## What I would add next

- Login system so chat history persists across devices
- Hindi and Punjabi language support
- Photo upload so farmers can show a picture of the diseased crop
- Real-time market prices from the Agmarknet API
- Voice input for farmers not comfortable with typing

---

## Running Tests

```bash
mvn test
```

Tests cover category detection logic in GeminiService and the REST endpoints in ChatController.`