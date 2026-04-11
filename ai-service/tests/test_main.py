import asyncio
import unittest
from unittest.mock import patch

from main import (
    QueryRequest,
    RerankRequest,
    get_recommendations,
    health_check,
    rerank,
)


class AiServiceRoutesTest(unittest.TestCase):
    def test_health_returns_ok(self):
        response = asyncio.run(health_check())

        self.assertEqual(response, {"status": "OK", "service": "ai-service"})

    @patch("main._find_recommendations")
    def test_recommend_returns_payload(self, mock_find_recommendations):
        mock_find_recommendations.return_value = [
            {
                "id": 1,
                "title": "Mock Movie",
                "score": 0.88,
                "overview": "Mock overview",
            }
        ]

        response = asyncio.run(
            get_recommendations(
                QueryRequest(query="space adventure", top_k=1),
            )
        )

        self.assertEqual(response["query"], "space adventure")
        self.assertEqual(response["recommendations"][0]["title"], "Mock Movie")

    @patch("main._rerank_movies")
    def test_rerank_returns_ranked_results(self, mock_rerank_movies):
        mock_rerank_movies.return_value = [
            {
                "id": 10,
                "title": "Interstellar",
                "score": 0.97,
                "overview": "Space exploration",
            }
        ]

        response = asyncio.run(
            rerank(
                RerankRequest(
                    query="space",
                    movies=[{"id": 10, "title": "Interstellar", "overview": "Space exploration"}],
                    top_k=1,
                ),
            )
        )

        self.assertEqual(response["query"], "space")
        self.assertEqual(response["results"][0]["score"], 0.97)


if __name__ == "__main__":
    unittest.main()
