# Project Map: Recommendation System

This document provides an overview of the services, their responsibilities, ports, and available API endpoints within the `rec-system` project.

## Services Overview

| Service | Technology | Port | Primary Responsibility |
|---------|------------|------|------------------------|
| `backend-node` | Node.js (TypeScript) | 8000 | Orchestration, user-facing API, movie metadata management, and integration with AI service. |
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

- **`GET /api/discovery/search?q={query}`**
  - **Description**: Performs an AI-powered search for movie recommendations based on the provided query.
  - **Query Parameters**: `q` (string, required)
  - **Response**: `200 OK` with AI-generated recommendations.

- **`GET /api/discovery/smart-search?q={query}`**
  - **Description**: Multi-page smart search. Fetches 3 pages from TMDB and reranks them using the AI service.
  - **Query Parameters**: `q` (string, required)
  - **Response**: `200 OK` with top 10 semantically relevant movies.

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

### `ai-service/`
- Currently, this service does not require a `.env` file as it uses default configurations within `main.py` (Port 8001).
