# CineRec — AI-Powered Movie Recommendation Platform

> A full-stack production-grade movie discovery application with an AI microservice, REST API, and native Android client.

---

## Architecture

```
┌─────────────────────────────────┐
│   Android App (Kotlin/Compose)  │
│   • Discovery  • Search         │
│   • Watchlist  • Profile        │
└────────────┬────────────────────┘
             │ HTTP/REST
             ▼
┌─────────────────────────────────┐
│   Node.js / TypeScript API      │  :8000
│   • Auth (HMAC tokens, scrypt)  │
│   • TMDB integration + cache    │
│   • Personalization endpoints   │
└────────────┬────────────────────┘
             │ HTTP
             ▼
┌─────────────────────────────────┐
│   Python / FastAPI AI Service   │  :8001
│   • Sentence-transformer (MiniLM│
│   • Cosine similarity re-ranking│
│   • /recommend  /rerank         │
└─────────────────────────────────┘
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Android | Kotlin, Jetpack Compose, Retrofit, Coil, Navigation Compose |
| Backend API | Node.js, TypeScript, Express, SQLite, Axios |
| AI Service | Python, FastAPI, sentence-transformers, NumPy |
| Auth | Custom HMAC tokens, scrypt password hashing |
| External API | TMDB (The Movie Database) |

---

## Key Features

**Android App**
- Guest-first flow — Discovery loads immediately, no login wall
- Trending carousel with portrait poster alignment + auto-scroll
- Genre rows (Action, Comedy, Thriller, Sci-Fi, Trending TV)
- AI-reranked search with 400ms debounce and Instagram-style explore grid
- Movie detail screen with YouTube trailer (deep-links to YouTube app)
- Watchlist — save/remove movies, persisted per user on backend
- Onboarding genre picker after registration (tap to select)
- Profile screen with editable preferences (genres, actors, directors)
- Compose splash screen + custom adaptive icon

**Backend API**
- JWT-style HMAC token auth with 7-day TTL
- In-memory LRU cache (15min trending, 5min search, 30min details)
- Parallel TMDB fetches with timeout and retry
- Personalization endpoints: profile, saved movies, search history
- 30 passing unit tests

**AI Microservice**
- `all-MiniLM-L6-v2` sentence-transformer embeddings
- L2-normalised cosine similarity on TMDB 5K dataset
- SHA-256 fingerprinted `.npy` embedding cache (no re-computation)
- `/recommend` — pure AI recommendation from natural language query
- `/rerank` — re-ranks TMDB search results by semantic similarity

---

## Running Locally

### Prerequisites
- Node.js 18+, Python 3.11+, Android Studio Hedgehog+
- TMDB API key → add to `backend-node/.env` as `TMDB_TOKEN=your_key`

### Backend

```bash
# Node API
cd backend-node
npm install
npm run dev          # http://localhost:8000

# AI Service
cd ai-service
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python main.py       # http://localhost:8001
```

### Android

1. Open `mobile-kotlin/` in Android Studio
2. In `local.properties` add: `API_BASE_URL=http://YOUR_LOCAL_IP:8000/`
3. Run on device or emulator

---

## Project Structure

```
rec-system/
├── backend-node/          # TypeScript REST API
│   ├── src/
│   │   ├── routes/        # auth, discovery, personalization
│   │   ├── services/      # movieService, authService, personalizationService
│   │   └── index.ts
│   └── tests/             # 30 unit tests
├── ai-service/            # Python FastAPI AI microservice
│   ├── engine.py          # embeddings + similarity
│   └── main.py
└── mobile-kotlin/         # Android Kotlin/Compose app
    └── app/src/main/java/com/example/recsystem/
        ├── data/          # API, models, repositories
        └── ui/            # screens, viewmodels, theme
```

---

## Resume Summary

> Architected and shipped a full-stack AI-powered movie recommendation system comprising a **Kotlin/Jetpack Compose Android application**, a **Node.js/TypeScript REST API** with TMDB integration and SQLite-backed authentication, and a **Python/FastAPI AI microservice** using sentence-transformer embeddings for semantic search and personalised content re-ranking.
