import importlib
import json
import os
import sys
import tempfile
import types
import unittest
from unittest.mock import patch

import numpy as np
import pandas as pd


class FakeSentenceTransformer:
    def __init__(self, _model_name):
        pass

    def encode(self, texts, show_progress_bar=False):
        if isinstance(texts, str):
            texts = [texts]

        vectors = []
        for text in texts:
            normalized = str(text).lower()
            if "space" in normalized or "galaxy" in normalized:
                vectors.append([1.0, 0.0])
            elif "romance" in normalized or "love" in normalized:
                vectors.append([0.0, 1.0])
            else:
                vectors.append([0.5, 0.5])
        return np.array(vectors, dtype=float)


def load_engine_module(mock_df):
    sys.modules.pop("engine", None)
    fake_sentence_transformers = types.ModuleType("sentence_transformers")
    fake_sentence_transformers.SentenceTransformer = FakeSentenceTransformer

    with patch.dict(sys.modules, {"sentence_transformers": fake_sentence_transformers}):
        with patch("pandas.read_csv", return_value=mock_df.copy()):
            return importlib.import_module("engine")


class EngineTest(unittest.TestCase):
    def test_extract_names_supports_lists_and_strings(self):
        engine = load_engine_module(
            pd.DataFrame(
                [
                    {
                        "id": 1,
                        "title": "Space Story",
                        "overview": "Space mission",
                        "genres": "[{'name': 'Sci-Fi'}]",
                        "keywords": "[{'name': 'galaxy'}]",
                    }
                ]
            )
        )

        self.assertEqual(
            engine._extract_names([{"name": "Action"}, {"name": "Drama"}]),
            "Action Drama",
        )
        self.assertEqual(
            engine._extract_names("[{'name': 'Romance'}, {'name': 'Comedy'}]"),
            "Romance Comedy",
        )
        self.assertEqual(engine._extract_names("plain value"), "plain value")

    def test_load_and_preprocess_builds_combined_features(self):
        mock_df = pd.DataFrame(
            [
                {
                    "id": 1,
                    "title": "Space Story",
                    "overview": None,
                    "genres": "[{'name': 'Sci-Fi'}]",
                    "keywords": "[{'name': 'galaxy'}]",
                }
            ]
        )
        engine = load_engine_module(mock_df)

        with patch("pandas.read_csv", return_value=mock_df.copy()):
            df = engine.load_and_preprocess()

        self.assertEqual(df.loc[0, "genres"], "Sci-Fi")
        self.assertEqual(df.loc[0, "keywords_text"], "galaxy")
        self.assertEqual(df.loc[0, "combined_features"], "Space Story  Sci-Fi galaxy")

    def test_find_recommendations_returns_best_matches(self):
        engine = load_engine_module(
            pd.DataFrame(
                [
                    {
                        "id": 1,
                        "title": "Space Story",
                        "overview": "A galaxy mission",
                        "genres": "[{'name': 'Sci-Fi'}]",
                        "keywords": "[{'name': 'space'}]",
                    },
                    {
                        "id": 2,
                        "title": "Love Story",
                        "overview": "A romance drama",
                        "genres": "[{'name': 'Romance'}]",
                        "keywords": "[{'name': 'love'}]",
                    },
                ]
            )
        )

        results = engine.find_recommendations("space adventure", top_k=1)

        self.assertEqual(len(results), 1)
        self.assertEqual(results[0]["title"], "Space Story")
        self.assertGreater(results[0]["score"], 0.0)

    def test_rerank_movies_sorts_candidates_by_similarity(self):
        engine = load_engine_module(
            pd.DataFrame(
                [
                    {
                        "id": 1,
                        "title": "Placeholder",
                        "overview": "Placeholder overview",
                        "genres": "[]",
                        "keywords": "[]",
                    }
                ]
            )
        )

        movies = [
            {"id": 10, "title": "Romance Film", "overview": "Love and heartbreak"},
            {"id": 11, "title": "Space Epic", "overview": "Galaxy rescue mission"},
        ]

        results = engine.rerank_movies("space", movies, top_k=2)

        self.assertEqual(results[0]["title"], "Space Epic")
        self.assertGreater(results[0]["score"], results[1]["score"])

    def test_load_or_create_embeddings_reuses_cached_index(self):
        engine = load_engine_module(
            pd.DataFrame(
                [
                    {
                        "id": 1,
                        "title": "Space Story",
                        "overview": "A galaxy mission",
                        "genres": "[{'name': 'Sci-Fi'}]",
                        "keywords": "[{'name': 'space'}]",
                    }
                ]
            )
        )
        dataframe = pd.DataFrame(
            [
                {
                    "id": 1,
                    "combined_features": "Space Story galaxy",
                }
            ]
        )

        with tempfile.TemporaryDirectory() as temp_dir:
            cache_path = os.path.join(temp_dir, "embeddings.npy")
            meta_path = os.path.join(temp_dir, "embeddings.meta.json")

            with patch.object(engine, "EMBEDDINGS_CACHE_PATH", cache_path), patch.object(
                engine, "EMBEDDINGS_META_PATH", meta_path
            ), patch.object(engine, "MODELS_DIR", temp_dir):
                first_embeddings = engine.load_or_create_embeddings(dataframe)

                with patch.object(
                    engine.model,
                    "encode",
                    side_effect=AssertionError("encode should not run when cache is valid"),
                ):
                    second_embeddings = engine.load_or_create_embeddings(dataframe)

                self.assertTrue(np.array_equal(first_embeddings, second_embeddings))

                with open(meta_path, "r", encoding="utf-8") as meta_file:
                    metadata = json.load(meta_file)

                self.assertEqual(
                    metadata["fingerprint"],
                    engine.build_dataset_fingerprint(dataframe),
                )

    def test_load_or_create_embeddings_rebuilds_when_dataset_changes(self):
        engine = load_engine_module(
            pd.DataFrame(
                [
                    {
                        "id": 1,
                        "title": "Space Story",
                        "overview": "A galaxy mission",
                        "genres": "[{'name': 'Sci-Fi'}]",
                        "keywords": "[{'name': 'space'}]",
                    }
                ]
            )
        )
        initial_df = pd.DataFrame(
            [{"id": 1, "combined_features": "Space Story galaxy"}]
        )
        updated_df = pd.DataFrame(
            [{"id": 1, "combined_features": "Romance Story love"}]
        )

        with tempfile.TemporaryDirectory() as temp_dir:
            cache_path = os.path.join(temp_dir, "embeddings.npy")
            meta_path = os.path.join(temp_dir, "embeddings.meta.json")

            with patch.object(engine, "EMBEDDINGS_CACHE_PATH", cache_path), patch.object(
                engine, "EMBEDDINGS_META_PATH", meta_path
            ), patch.object(engine, "MODELS_DIR", temp_dir):
                engine.load_or_create_embeddings(initial_df)

                with patch.object(
                    engine.model,
                    "encode",
                    wraps=engine.model.encode,
                ) as mock_encode:
                    refreshed_embeddings = engine.load_or_create_embeddings(updated_df)

                self.assertGreater(mock_encode.call_count, 0)
                self.assertEqual(refreshed_embeddings.shape, (1, 2))


if __name__ == "__main__":
    unittest.main()
