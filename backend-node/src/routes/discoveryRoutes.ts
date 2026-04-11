// src/routes/discoveryRoutes.ts
import { Router, type Request, type Response } from 'express';
import { getTrendingMovies, getDeepDiscovery } from '../services/movieService.js';
import { getAIRecommendations, rerankMovies } from '../services/aiService.js';

type DiscoveryDependencies = {
    getTrendingMovies: typeof getTrendingMovies;
    getDeepDiscovery: typeof getDeepDiscovery;
    getAIRecommendations: typeof getAIRecommendations;
    rerankMovies: typeof rerankMovies;
};

const defaultDependencies: DiscoveryDependencies = {
    getTrendingMovies,
    getDeepDiscovery,
    getAIRecommendations,
    rerankMovies,
};

export const createDiscoveryRouter = (
    dependencies: DiscoveryDependencies = defaultDependencies,
) => {
    const router = Router();

    router.get('/trending', async (req: Request, res: Response) => {
        try {
            const movies = await dependencies.getTrendingMovies();
            res.status(200).json(movies);
        } catch (error) {
            res.status(500).json({ error: "Failed to fetch trending movies" });
        }
    });

    router.get('/search', async (req: Request, res: Response) => {
        const query = req.query.q as string;
        
        if (!query) {
            res.status(400).json({ error: "Search query is required" });
            return;
        }

        try {
            const aiData = await dependencies.getAIRecommendations(query);
            res.status(200).json(aiData);
        } catch (error) {
            res.status(500).json({ error: "AI search failed" });
        }
    });

    router.get('/smart-search', async (req: Request, res: Response) => {
        const query = req.query.q as string;

        if (!query) {
            res.status(400).json({ error: "Search query is required" });
            return;
        }

        try {
            const rawMovies = await dependencies.getDeepDiscovery(query);
            const rerankedData = await dependencies.rerankMovies(query, rawMovies);

            res.status(200).json(rerankedData);
        } catch (error: any) {
            console.error("Smart Search Error:", error.message);
            if (error.stack) console.error(error.stack);
            res.status(500).json({ error: `Smart search failed: ${error.message}` });
        }
    });

    return router;
};

export default createDiscoveryRouter();
