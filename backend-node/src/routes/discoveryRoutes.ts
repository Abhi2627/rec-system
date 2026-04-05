// src/routes/discoveryRoutes.ts
import { Router, type Request, type Response } from 'express';
import { getTrendingMovies } from '../services/movieService.js';

const router = Router();

router.get('/trending', async (req: Request, res: Response) => {
    try {
        const movies = await getTrendingMovies();
        res.status(200).json(movies);
    } catch (error) {
        res.status(500).json({ error: "Failed to fetch trending movies" });
    }
});

export default router;