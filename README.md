# TalkNotes – Meeting Recorder with Transcription & Summary

##  Overview
TalkNotes is an Android application built as part of a take-home assignment to demonstrate a **robust meeting recording system with transcription and summary generation**.

The app records audio in the background, splits it into chunks, processes each chunk into transcripts, and generates structured summaries using LLM APIs.

---

##  Demo Video
👉 https://drive.google.com/file/d/1fM-qOtuBPlissF4yu-1rtnbuFQVFfana/view?usp=sharing

🎬 This demo shows:
- Recording start/stop
- Chunk-based audio processing
- Transcript generation from audio
- Summary generation from transcript
- Dashboard with meeting data

---

##  Core Features Implemented

### 🎙️ Background Audio Recording
- Foreground service for continuous recording
- Works in background
- Persistent notification with stop action
- Handles **audio focus interruption (pause/resume)**

---

###  Chunk-Based Recording
- Audio split into **30-second chunks**
- Stored locally as: meeting_<meetingId>chunk<index>.m4a
- Enables reliable processing and recovery

---

###  Transcription (Real API)
- Each chunk is processed automatically
- Uses **OpenAI Whisper API**
- Transcript stored in Room DB
- Maintains correct order using chunk index

---

###  Summary Generation (Real API)
- Full transcript sent to LLM
- Uses **OpenRouter API**
- Generates structured output:
- Title
- Summary
- Action Items
- Key Points
- Stored in Room and displayed in UI

---

###  Room Database (Single Source of Truth)
Entities:
- `Meeting`
- `AudioChunk`
- `Transcript`
- `Summary`

---

###  End-to-End Pipeline
Recording → Chunk → Database → Transcription → Summary


---

###  Dashboard UI
- List of meetings
- Recording status
- Chunk count
- Transcript preview
- Summary display

---

##  Architecture

Follows **MVVM + Clean Architecture**

UI (Jetpack Compose)
↓
ViewModel
↓
Repository
↓
Room DB + API Layer


---

##  Tech Stack
- Kotlin
- Jetpack Compose
- MVVM Architecture
- Coroutines + Flow
- Room Database
- Retrofit
- Hilt
- WorkManager
- MediaRecorder API

---

##  Assignment Requirement Coverage

| Requirement | Status |
|------------|--------|
| Foreground recording service | ✅ |
| 30-sec chunk recording | ✅ |
| Background recording | ✅ |
| Audio focus interruption handling | ✅ |
| Transcript generation (real API) | ✅ |
| Summary generation (real API) | ✅ |
| Room as source of truth | ✅ |
| MVVM architecture | ✅ |

---

##  Partial / Simplified Implementations

Due to time constraints, some advanced edge cases are simplified:

- Phone call interruption handling (basic level)
- Summary streaming (not real-time streaming)
- Retry mechanism (basic)
- Process death recovery (partial)
- Notification actions (basic)

---

##  Not Implemented (Planned Improvements)

- Low storage detection
- Silence detection (no audio input)
- 2-second chunk overlap
- Full process recovery after app kill
- Microphone source change handling
- True streaming summary UI

---

##  Notes

- Initial submission (within 48-hour deadline) used **mock pipelines**
- This version includes **full real API integration**
- Architecture was designed to support real APIs from the beginning

---

##  Conclusion

TalkNotes demonstrates a scalable architecture for:
- Reliable background recording
- Incremental audio processing
- Structured transcript and summary generation

---

## 👨‍💻 Author
Surya N S