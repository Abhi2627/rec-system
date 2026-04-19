# Project Map: Recommendation System

This document provides an overview of the services, their responsibilities, ports, and available API endpoints within the `rec-system` project.

## Services Overview

| Service | Technology | Port | Primary Responsibility |
|---------|------------|------|------------------------|
| `backend-node` | Node.js (TypeScript) | 8000 | Orchestration, user-facing API, auth, personalization storage, movie metadata management, and integration with AI service. |
| `ai-service` | Python (FastAPI) | 8001 | Machine learning-based movie recommendation engine. |

---

## API Endpoints

### 1. `backend-node` (Port 8000)

The Node.js backend serves as the main entry point for the application.

- **`GET /health`**
  - **Description**: Health check endpoint to verify service status.
  - **Response**: `200 OK` with status message.

- **`GET /api/discovery/trending`**
  - **Description**: Fetches trending movies.
  - **Response**: `200 OK` with a list of trending movies.

- **`GET /api/discovery/smart-search?q={query}`**
  - **Description**: Multi-page smart search. Fetches 3 pages from TMDB and reranks them using the AI service.
  - **Query Parameters**: `q` (string, required, max 200 chars)
  - **Response**: `200 OK` with top semantically relevant movies.

- **`GET /api/discovery/ai-recommend?q={query}`**
  - **Description**: Pure AI recommendation from local CSV embeddings. Returns semantically similar movies without a live TMDB search.
  - **Query Parameters**: `q` (string, required, max 200 chars)
  - **Response**: `200 OK` with AI-generated recommendations.

- **`POST /api/auth/register`**
  - **Description**: Registers a user account and returns a signed bearer token.
  - **Body**:
    ```json
    {
      "name": "string",
      "email": "string",
      "password": "string"
    }
    ```

- **`POST /api/auth/login`**
  - **Description**: Logs in an existing user and returns a signed bearer token.

- **`GET /api/auth/me`**
  - **Description**: Returns the current authenticated user from the bearer token.

- **`GET /api/personalization/profile`**
  - **Description**: Loads the user personalization profile from SQLite, including preferences, saved movies, and recent searches.

- **`PUT /api/personalization/profile`**
  - **Description**: Updates favorite genres and keywords for the authenticated user.

- **`POST /api/personalization/saved-movies`**
  - **Description**: Saves or updates a movie in the user personalization profile.

- **`DELETE /api/personalization/saved-movies/{movieId}`**
  - **Description**: Removes a saved movie for the authenticated user.

- **`POST /api/personalization/search-history`**
  - **Description**: Stores a recent search query for personalization.

- **`POST /api/personalization/recommendations`**
  - **Description**: Builds a personalization-aware prompt from the user profile and recent history, then requests AI recommendations.

### 2. `ai-service` (Port 8001)

The Python service handles core recommendation logic.

- **`POST /recommend`**
  - **Description**: Generates movie recommendations for a given natural language query.
  - **Body**:
    ```json
    {
      "query": "string",
      "top_k": 5
    }
    ```
  - **Response**: `200 OK` with query and a list of recommendations.

- **`POST /rerank`**
  - **Description**: Reranks a provided list of movies based on semantic similarity to a query.
  - **Body**:
    ```json
    {
      "query": "string",
      "movies": [ ... ],
      "top_k": 10
    }
    ```
  - **Response**: `200 OK` with query and top 10 reranked results.

---

## Environment Configuration (Local Development)

### `backend-node/.env`
- `PORT`: 8000 (Default)
- `TMDB_TOKEN`: Required for movie metadata (from TMDB API).
- `AI_SERVICE_URL`: `http://localhost:8001` (Points to the `ai-service`).
- `AUTH_SECRET`: Signs auth bearer tokens.

### `ai-service/`
- Currently, this service does not require a `.env` file as it uses default configurations within `main.py` (Port 8001).
