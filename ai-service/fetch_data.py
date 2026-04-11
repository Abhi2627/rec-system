# ai-service/fetch_data.py
import os
import pandas as pd

# Link to a hosted version of the TMDB 5000 dataset for easy access
URL = "https://huggingface.co/sujoy0011/Movie-Recommendation-System/resolve/main/tmdb_5000_movies.csv"
CSV_PATH = os.path.join(os.path.dirname(__file__), "movies.csv")


def download_data():
    if not os.path.exists(CSV_PATH):
        print("Downloading 5,000 movie dataset...")
        df = pd.read_csv(URL)
        df.to_csv(CSV_PATH, index=False)
        print(f"Dataset saved as {CSV_PATH}")
    else:
        print("Dataset already exists.")


if __name__ == "__main__":
    download_data()
