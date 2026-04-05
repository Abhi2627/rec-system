# ai-service/preprocess.py
import pandas as pd
from sklearn.preprocessing import MultiLabelBinarizer

def clean_movie_data(df):
    """
    Standard MLOps Preprocessing:
    1. Handling Missing Values
    2. Feature Engineering (Combining Title + Overview)
    3. Normalization
    """
    # Fill missing overviews so the NLP model doesn't crash
    df['overview'] = df['overview'].fillna('')
    
    # Create a 'Content' column for Vectorization
    # This is the "Brain" of your search
    df['content_tags'] = df['title'] + " " + df['overview'] + " " + df['genres']
    
    # Basic Cleaning: Lowercase and remove special characters
    df['content_tags'] = df['content_tags'].str.lower().replace(r'[^a-zA-Z0-9\s]', '', regex=True)
    
    return df

if __name__ == "__main__":
    print("🚀 Starting Data Preprocessing...")
    # Later we will load a real CSV here, for now, we're defining the logic.
    print("✅ Preprocessing logic initialized.")