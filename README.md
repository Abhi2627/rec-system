# Rec System

Movie recommendation system with:
- `backend-node`: Node.js API on port `8000`
- `ai-service`: FastAPI recommendation engine on port `8001`
- `mobile-flutter`: Flutter client for discovery and saved movies

## What It Does

- Fetches trending movies from TMDB
- Generates semantic recommendations from a local TMDB-derived dataset
- Reranks TMDB search results with the AI service
- Exposes a Flutter client for discovery, saved movies, and smart search

## Project Structure

- `backend-node/`: main API layer
- `ai-service/`: recommendation engine and dataset
- `mobile-flutter/`: mobile client
- `docker-compose.yml`: local multi-service orchestration
- `PROJECT_MAP.md`: service and endpoint reference

## Requirements

- Node.js 20+
- Python 3.11 recommended
- Flutter SDK
- Docker Desktop optional
- TMDB bearer token

## Environment Setup

Create your local backend env file from the example:

```bash
cp backend-node/.env.example backend-node/.env
```

Then set:

- `TMDB_TOKEN`: your TMDB bearer token
- `AI_SERVICE_URL`: use `http://localhost:8001` for local native runs

## Local Run: Native

### 1. Start the AI service

```bash
cd ai-service
python3 -m pip install --user -r requirements.txt
python3 main.py
```

The first startup may take time because embeddings are generated from `movies.csv`.

### 2. Start the Node backend

```bash
cd backend-node
npm install
npm run dev
```

### 3. Start the Flutter app

```bash
cd mobile-flutter
flutter pub get
flutter run
```

For Android emulator, the client already defaults to `http://10.0.2.2:8000`.

For iOS simulator or macOS-hosted runs, it defaults to `http://127.0.0.1:8000`.

For a physical device, pass your machine IP:

```bash
flutter run --dart-define=API_BASE_URL=http://YOUR_MAC_IP:8000
```

## Local Run: Docker

Make sure `backend-node/.env` exists first, then run:

```bash
docker compose up --build
```

Services:
- Backend: `http://localhost:8000`
- AI service: `http://localhost:8001`

## Health Checks

- Backend: `GET /health`
- AI service: `GET /health`

## Key Endpoints

- `GET /api/discovery/trending`
- `GET /api/discovery/search?q=space%20adventure`
- `GET /api/discovery/smart-search?q=interstellar`
- `POST /recommend`
- `POST /rerank`

## Notes

- `backend-node/.env` is intentionally not tracked in git.
- `ai-service/movies.csv` is small and currently around `5.4 MB`.
- The system currently uses TMDB in two ways:
  - live TMDB API for trending and search
  - local TMDB-derived CSV for semantic recommendation

## Current Status

Working now:
- recommendation API flow
- TMDB-backed discovery routes
- Flutter discovery client
- saved movies
- Docker setup

Still to do:
- automated backend and AI tests
- better recommendation persistence/indexing
- auth and user accounts
- database-backed personalization
- deployment and CI/CD
