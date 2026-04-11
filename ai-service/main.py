# ai-service/main.py
from fastapi import FastAPI
from pydantic import BaseModel
from typing import List, Optional

app = FastAPI()

# Define the data structure for input
class QueryRequest(BaseModel):
    query: str
    top_k: int = 5

class RerankRequest(BaseModel):
    query: str
    movies: List[dict]
    top_k: Optional[int] = 10

def _find_recommendations(query: str, top_k: int):
    from engine import find_recommendations

    return find_recommendations(query, top_k)

def _rerank_movies(query: str, movies: List[dict], top_k: int):
    from engine import rerank_movies

    return rerank_movies(query, movies, top_k)

@app.get("/health")
async def health_check():
    return {"status": "OK", "service": "ai-service"}

@app.post("/recommend")
async def get_recommendations(request: QueryRequest):
    results = _find_recommendations(request.query, request.top_k)
    return {
        "query": request.query,
        "recommendations": results
    }

@app.post("/rerank")
async def rerank(request: RerankRequest):
    results = _rerank_movies(request.query, request.movies, request.top_k)
    return {
        "query": request.query,
        "results": results
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
