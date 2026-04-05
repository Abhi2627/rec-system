# ai-service/engine.py
from sentence_transformers import SentenceTransformer
import numpy as np
import pandas as pd

# 1. Load a lightweight, high-performance model
# MiniLM is perfect for 2026—it's fast and runs easily on a MacBook Air
model = SentenceTransformer('all-MiniLM-L6-v2')

# 2. Mock Dataset (We will replace this with a CSV later)
# This mimics 'Fused Data' from TMDB + IMDb
movies_db = pd.DataFrame([
    {"id": 101, "title": "Alien", "description": "A crew on a spacecraft encounters a deadly extraterrestrial life form."},
    {"id": 102, "title": "Event Horizon", "description": "A rescue crew investigates a spaceship that disappeared into a black hole."},
    {"id": 103, "title": "The Lion King", "description": "A young lion prince flees his kingdom only to learn the true meaning of responsibility."},
    {"id": 104, "title": "Interstellar", "description": "A team of explorers travel through a wormhole in space to ensure humanity's survival."}
])

# 3. Pre-calculate Embeddings (This is 'Preprocessing')
# In a real app, you do this once and save it to a Vector DB.
print("⏳ Generating Vector Embeddings for the movie database...")
movie_descriptions = movies_db['description'].tolist()
encoded_data = model.encode(movie_descriptions)

def find_recommendations(query, top_k=2):
    # Convert user search into a vector
    query_vector = model.encode([query])
    
    # Calculate 'Cosine Similarity' (Mathematical closeness)
    # This is standard M.Tech level linear algebra
    similarities = np.dot(encoded_data, query_vector.T).flatten()
    
    # Get top indices
    top_indices = np.argsort(similarities)[::-1][:top_k]
    
    results = []
    for idx in top_indices:
        results.append({
            "id": int(movies_db.iloc[idx]['id']),
            "title": movies_db.iloc[idx]['title'],
            "score": float(similarities[idx])
        })
    return results