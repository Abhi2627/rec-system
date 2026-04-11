# ai-service/main.py
from fastapi import FastAPI
from pydantic import BaseModel
from typing import List, Optional
from engine import find_recommendations, rerank_movies

app = FastAPI()

# Define the data structure for input
class QueryRequest(BaseModel):
    query: str
    top_k: int = 5

class RerankRequest(BaseModel):
    query: str
    movies: List[dict]
    top_k: Optional[int] = 10

@app.post("/recommend")
async def get_recommendations(request: QueryRequest):
    results = find_recommendations(request.query, request.top_k)
    return {
        "query": request.query,
        "recommendations": results
    }

@app.post("/rerank")
async def rerank(request: RerankRequest):
    results = rerank_movies(request.query, request.movies, request.top_k)
    return {
        "query": request.query,
        "results": results
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)