# 🌌 AdaptiveMath

> An intelligent, adaptive mathematics learning platform that generates dynamic questions and personalizes the learning journey.

## 🚀 Core Features

* **Dynamic Question Generation:** Eliminates static question banks. Math problems and their narratives are generated on the fly, scaling seamlessly in difficulty.
* **🧠 Adaptive Learning Algorithm:** Tracks empirical data and error patterns. The system automatically advances students who excel, or drops them into targeted "practice sub-levels" if they struggle with specific operations.
* **🤖 AI Assistant & Generator:** A dedicated AI microservice utilizing LLMs to dynamically generate themed word problems and step-by-step guided solutions.
* **📊 Smart Dashboards:** * **Student View:** Real-time progress, topic maps, and interactive leaderboards.
  * **Admin View:** System-wide analytics tracking common error patterns and curriculum gaps using empirical data.
* **⭐ Gamified Experience:** A beautifully designed space-themed UI featuring streak tracking, level-up celebrations, and dynamic animations to keep learners engaged.

## 🛠️ Quick Setup

**1. Database (MySQL)**
Create a schema named `adaptive_db`. The backend will automatically generate the required tables on the first run.

**2. Backend (Spring Boot)**
Navigate to the server directory and run the Spring Boot application:
```bash
cd server
mvn spring-boot:run

cd client
npm install
npm run dev

cd microservices/AI_Questions_Generator
uv run uvicorn main:app --reload
