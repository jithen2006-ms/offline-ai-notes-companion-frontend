# 📚 Offline AI Notes Companion — Frontend

> 🎓 **Minor Academic Project**
> Client Layer: React Web Dashboard & Native Android Application

---

## 📖 Overview

This repository contains the complete frontend client applications for the **Offline AI Notes Companion** project:
1. **Web Dashboard (`web/`)**: React 19 + Vite web interface for searching, browsing, and reading lecture notes and timestamped transcripts.
2. **Native Android App (`android/`)**: Native Kotlin mobile application for classroom audio recording, local Whisper speech-to-text inference, and lecture upload.

---

## 📁 Repository Structure

```
offline-ai-notes-companion-frontend/
├── web/                       # React 19 + Vite web dashboard
│   ├── index.html             # Application HTML shell
│   ├── package.json           # Dependencies & scripts
│   ├── vite.config.js         # Vite bundler configuration
│   └── src/
│       ├── App.jsx            # Main dashboard component
│       ├── main.jsx           # React DOM root entry
│       ├── styles.css         # Styling
│       ├── components/        # UI components (MyLectures.jsx)
│       └── data/              # Data fixtures & helpers
└── android/                   # Native Android application
    ├── app/                   # Kotlin & Java Android application source
    │   ├── src/main/java/     # Recording service, STT engine, and UI activities
    │   ├── src/main/res/      # Layouts, strings, and UI drawables
    │   └── src/main/cpp/      # Whisper C++ native JNI bindings
    ├── gradle/                # Gradle wrapper binaries & properties
    ├── third_party/           # C++ whisper.cpp inference dependencies
    ├── build.gradle           # Root Gradle build script
    └── settings.gradle        # Project module definitions
```

---

## 🚀 Getting Started

### 1. Web Dashboard (React + Vite)

```bash
cd web
npm install
npm run dev
```
The web dashboard runs locally on `http://localhost:5173`.

### 2. Android Mobile Application

1. Open Android Studio.
2. Select **Open an Existing Project** and choose the `android/` directory.
3. Allow Gradle to sync dependencies.
4. Connect an Android device (or launch an emulator) and click **Run 'app'**.

---

## 🔗 Backend Connectivity

- **Web Application**: Configured in `web/src/App.jsx` to route requests to `/api` endpoints (`/api/lectures`, `/api/subjects`).
- **Android Application**: Configured in `android/app/src/main/java/com/lecturecap/network/ApiConfig.java` to connect to the backend service.
