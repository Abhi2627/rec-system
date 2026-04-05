# ai-service/main.py
from fastapi import FastAPI
from pydantic import BaseModel
from typing import List
from engine import find_recommendations

app = FastAPI()

# Define the data structure for input
class QueryRequest(BaseModel):
    query: str
    top_k: int = 5

@app.post("/recommend")
async def get_recommendations(request: QueryRequest):
    results = find_recommendations(request.query, request.top_k)
    return {
        "query": request.query,
        "recommendations": results
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)