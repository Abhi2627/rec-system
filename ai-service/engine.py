import ast
import hashlib
import json
import os

import numpy as np
import pandas as pd
from sentence_transformers import SentenceTransformer

BASE_DIR = os.path.dirname(__file__)
CSV_PATH = os.path.join(BASE_DIR, 'movies.csv')
MODELS_DIR = os.path.join(BASE_DIR, 'models')
EMBEDDINGS_CACHE_PATH = os.path.join(MODELS_DIR, 'movie_embeddings.npy')
EMBEDDINGS_META_PATH = os.path.join(MODELS_DIR, 'movie_embeddings.meta.json')

model = SentenceTransformer('all-MiniLM-L6-v2')


def _extract_names(value):
    if isinstance(value, list):
        return " ".join(item.get('name', '') for item in value if isinstance(item, dict))
    if pd.isna(value) or value == '':
        return ''
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
    if not os.path.exists(CSV_PATH):
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
    df = pd.read_csv(CSV_PATH)

    keep_columns = [col for col in ['id', 'title', 'overview', 'genres', 'keywords', 'keywords_tags'] if col in df.columns]
    df = df[keep_columns].copy()

    df['overview'] = df['overview'].fillna('')
    df['genres'] = df['genres'].apply(_extract_names) if 'genres' in df.columns else ''

    if 'keywords_tags' in df.columns:
        df['keywords_text'] = df['keywords_tags'].fillna('')
    elif 'keywords' in df.columns:
        df['keywords_text'] = df['keywords'].apply(_extract_names)
    else:
        df['keywords_text'] = ''

    df['combined_features'] = (
        df['title'].fillna('') + " " +
        df['overview'] + " " +
        df['genres'].fillna('') + " " +
        df['keywords_text'].fillna('')
    ).str.strip()
    
    return df

def build_dataset_fingerprint(dataframe):
    payload = (
        dataframe[['id', 'combined_features']]
        .fillna('')
        .astype({'id': 'string', 'combined_features': 'string'})
        .to_json(orient='records')
    )
    return hashlib.sha256(payload.encode('utf-8')).hexdigest()

def load_cached_embeddings(expected_fingerprint):
    if not os.path.exists(EMBEDDINGS_CACHE_PATH) or not os.path.exists(EMBEDDINGS_META_PATH):
        return None

    try:
        with open(EMBEDDINGS_META_PATH, 'r', encoding='utf-8') as meta_file:
            metadata = json.load(meta_file)

        if metadata.get('fingerprint') != expected_fingerprint:
            return None

        return np.load(EMBEDDINGS_CACHE_PATH)
    except (OSError, json.JSONDecodeError, ValueError):
        return None

def save_cached_embeddings(embeddings, fingerprint):
    os.makedirs(MODELS_DIR, exist_ok=True)
    np.save(EMBEDDINGS_CACHE_PATH, embeddings)
    with open(EMBEDDINGS_META_PATH, 'w', encoding='utf-8') as meta_file:
        json.dump(
            {
                'fingerprint': fingerprint,
                'rows': int(len(embeddings)),
            },
            meta_file,
        )

def load_or_create_embeddings(dataframe):
    fingerprint = build_dataset_fingerprint(dataframe)
    cached_embeddings = load_cached_embeddings(fingerprint)

    if cached_embeddings is not None:
        print("✅ Loaded cached movie embeddings")
        return cached_embeddings

    print("⏳ Vectorizing movie dataset and refreshing cache...")
    embeddings = model.encode(
        dataframe['combined_features'].tolist(),
        show_progress_bar=True,
    )
    save_cached_embeddings(embeddings, fingerprint)
    return embeddings

# Load data at module import time — cached embeddings are reused across calls.
df = load_and_preprocess()
_raw_embeddings = load_or_create_embeddings(df)

# L2-normalise so dot product == cosine similarity (scores in [-1, 1])
_norms = np.linalg.norm(_raw_embeddings, axis=1, keepdims=True)
_norms = np.where(_norms == 0, 1, _norms)  # avoid divide-by-zero on zero vectors
encoded_data = _raw_embeddings / _norms


def _normalise(vectors: np.ndarray) -> np.ndarray:
    """L2-normalise a 2D array of row vectors."""
    norms = np.linalg.norm(vectors, axis=1, keepdims=True)
    norms = np.where(norms == 0, 1, norms)
    return vectors / norms

def find_recommendations(query, top_k=5):
    query_vector = _normalise(model.encode([query]))
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
    
    # Encode and normalise query and candidate movies
    query_vector = _normalise(model.encode([query]))
    candidate_vectors = _normalise(model.encode(descriptions))
    
    # 3. Calculate cosine similarity
    similarities = np.dot(candidate_vectors, query_vector.T).flatten()
    
    # 4. Attach scores and sort
    for i, m in enumerate(movies):
        m['score'] = float(similarities[i])
    
    # Sort by score descending
    sorted_movies = sorted(movies, key=lambda x: x['score'], reverse=True)
    
    return sorted_movies[:top_k]
