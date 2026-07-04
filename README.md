# 🌌 AdaptiveMath

> An intelligent, adaptive mathematics learning platform that generates dynamic questions and personalizes the learning journey.

## 🚀 Core Features

* **Dynamic Question Generation:** Eliminates static question banks. Math problems and their narratives are generated on the fly, scaling seamlessly in difficulty.
* **🧠 Adaptive Learning Algorithm:** Tracks empirical data and error patterns. The system automatically advances students who excel, or drops them into targeted "practice sub-levels" if they struggle with specific operations.
* **🤖 AI Assistant & Generator:** A dedicated AI microservice utilizing LLMs to dynamically generate themed word problems and step-by-step guided solutions.
* **💬 Socratic AI Tutor:** An interactive AI chat microservice providing progressive hints and guidance without prematurely revealing answers.
* **📊 Smart Dashboards:** 
  * **Student View:** Real-time progress, topic maps, and interactive leaderboards.
  * **Admin View:** System-wide analytics tracking common error patterns and curriculum gaps using empirical data and K-Means clustering.
* **⭐ Gamified Experience:** A beautifully designed space-themed UI featuring streak tracking, level-up celebrations, educational games (e.g., "Build the Target"), and dynamic animations to keep learners engaged.

## 🛠️ Quick Setup

### Prerequisites
* **Java 17+** & **Maven** (for Spring Boot Backend)
* **Node.js 18+** & **npm** (for React Frontend)
* **Python 3.10+** & **uv** package manager (for AI/ML Microservices)
* **MySQL Server**
* **OpenAI API Key** (for AI generation and chat microservices)

---

### 1. Database (MySQL)
Create a schema named `adaptive_db` in your MySQL server. The Spring Boot backend will automatically generate all required tables via Hibernate/JPA on the first run.

```sql
CREATE DATABASE adaptive_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

cd server
mvn spring-boot:run

cd client
npm install
npm run dev

OPENAI_API_KEY=your_openai_api_key_here
OPENAI_MODEL=gpt-4o-mini

cd microservices/AI_Questions_Generator
uv sync
uv run uvicorn main:app --reload --port 8000

cd microservices/AI_Tutor_Chat
uv sync
uv run uvicorn main:app --reload --port 8001

cd microservices/student-clustering-service
uv sync
uv run uvicorn main:app --reload --port 8002
