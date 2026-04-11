import ast
import os

import numpy as np
import pandas as pd
from sentence_transformers import SentenceTransformer

model = SentenceTransformer('all-MiniLM-L6-v2')


def _extract_names(value):
    if pd.isna(value) or value == '':
        return ''
    if isinstance(value, list):
        return " ".join(item.get('name', '') for item in value if isinstance(item, dict))
    if isinstance(value, str):
        try:
            parsed = ast.literal_eval(value)
            if isinstance(parsed, list):
                return " ".join(item.get('name', '') for item in parsed if isinstance(item, dict))
        except (ValueError, SyntaxError):
            return value
        return value
    return str(value)

def load_and_preprocess():
    # Load the 5000 movie dataset
    csv_path = os.path.join(os.path.dirname(__file__), 'movies.csv')
    if not os.path.exists(csv_path):
        # Create a dummy dataframe if movies.csv is missing for initial run
        print("⚠️ movies.csv not found! Creating dummy data for testing.")
        df = pd.DataFrame({
            'id': [1, 2],
            'title': ['Dummy Movie 1', 'Dummy Movie 2'],
            'overview': ['Overview 1', 'Overview 2'],
            'genres': ['Action', 'Comedy'],
            'keywords_tags': ['tag1', 'tag2']
        })
        df['combined_features'] = df['title'] + " " + df['overview'] + " " + df['genres']
        return df
    df = pd.read_csv(csv_path)

    # Keep only the fields needed by the recommendation pipeline.
    keep_columns = [col for col in ['id', 'title', 'overview', 'genres', 'keywords', 'keywords_tags'] if col in df.columns]
    df = df[keep_columns].copy()

    # Normalize metadata from the TMDB dataset shape.
    df['overview'] = df['overview'].fillna('')
    df['genres'] = df['genres'].apply(_extract_names) if 'genres' in df.columns else ''

    if 'keywords_tags' in df.columns:
        df['keywords_text'] = df['keywords_tags'].fillna('')
    elif 'keywords' in df.columns:
        df['keywords_text'] = df['keywords'].apply(_extract_names)
    else:
        df['keywords_text'] = ''

    # Combine the textual features used for semantic matching.
    df['combined_features'] = (
        df['title'].fillna('') + " " +
        df['overview'] + " " +
        df['genres'].fillna('') + " " +
        df['keywords_text'].fillna('')
    ).str.strip()
    
    return df

# Load data
df = load_and_preprocess()

# Generate Embeddings (Warning: This might take 1-2 minutes the first time)
print("⏳ Vectorizing 5,000 movies... (M.Tech Preprocessing in progress)")
encoded_data = model.encode(df['combined_features'].tolist(), show_progress_bar=True)

def find_recommendations(query, top_k=5):
    query_vector = model.encode([query])
    similarities = np.dot(encoded_data, query_vector.T).flatten()
    top_indices = np.argsort(similarities)[::-1][:top_k]
    
    results = []
    for idx in top_indices:
        results.append({
            "id": int(df.iloc[idx]['id']),
            "title": df.iloc[idx]['title'],
            "score": float(similarities[idx]),
            "overview": df.iloc[idx]['overview']
        })
    return results

def rerank_movies(query, movies, top_k=10):
    if not movies:
        return []
    
    # 1. Prepare descriptions for reranking
    # Combine title and overview for semantic search
    descriptions = [f"{m.get('title', '')} {m.get('overview', '')}" for m in movies]
    
    # 2. Encode query and candidate movies
    query_vector = model.encode([query])
    candidate_vectors = model.encode(descriptions)
    
    # 3. Calculate cosine similarity
    similarities = np.dot(candidate_vectors, query_vector.T).flatten()
    
    # 4. Attach scores and sort
    for i, m in enumerate(movies):
        m['score'] = float(similarities[i])
    
    # Sort by score descending
    sorted_movies = sorted(movies, key=lambda x: x['score'], reverse=True)
    
    return sorted_movies[:top_k]
