import { Router, type Request, type Response } from 'express';
import * as defaultMovieService from '../services/movieService.js';
import {
  getAIRecommendations as defaultGetAIRecommendations,
  rerankMovies as defaultRerankMovies,
} from '../services/aiService.js';
import type { Movie } from '../services/movieService.js';
import type { AIRerankResponse } from '../services/aiService.js';

type DiscoveryDependencies = {
  getTrendingMovies: () => Promise<Movie[]>;
  getDeepDiscovery: (query: string) => Promise<Movie[]>;
  getAIRecommendations: (query: string) => Promise<{ query?: string; recommendations: unknown[] }>;
  rerankMovies: (query: string, movies: Movie[]) => Promise<AIRerankResponse>;
};

const MAX_QUERY_LENGTH = 200;

const sanitiseQuery = (raw: unknown): string | null => {
  if (typeof raw !== 'string') return null;
  const trimmed = raw.trim();
  if (trimmed.length === 0 || trimmed.length > MAX_QUERY_LENGTH) return null;
  return trimmed;
};

export const createDiscoveryRouter = (deps?: Partial<DiscoveryDependencies>) => {
  const movieService = {
    getTrendingMovies: deps?.getTrendingMovies ?? defaultMovieService.getTrendingMovies,
    getTrendingTV: defaultMovieService.getTrendingTV,
    getContentByGenre: defaultMovieService.getContentByGenre,
    getDeepDiscovery: deps?.getDeepDiscovery ?? defaultMovieService.getDeepDiscovery,
    getMovieDetails: defaultMovieService.getMovieDetails,
  };
  const getAIRecommendations = deps?.getAIRecommendations ?? defaultGetAIRecommendations;
  const rerankMovies = deps?.rerankMovies ?? defaultRerankMovies;

  const router = Router();

  // ── Trending Movies ──────────────────────────────────────────────────────
  router.get('/trending', async (_req: Request, res: Response) => {
    try {
      const movies = await movieService.getTrendingMovies();
      res.status(200).json(movies);
    } catch (error: unknown) {
      res.status(500).json({ error: (error as Error).message });
    }
  });

  // ── Trending TV ──────────────────────────────────────────────────────────
  router.get('/trending/tv', async (_req: Request, res: Response) => {
    try {
      const tv = await movieService.getTrendingTV();
      res.status(200).json(tv);
    } catch (error: unknown) {
      res.status(500).json({ error: (error as Error).message });
    }
  });

  // ── By Genre/Category ────────────────────────────────────────────────────
  router.get('/category/:type/:genreId', async (req: Request, res: Response) => {
    const { type, genreId } = req.params;
    try {
      const content = await movieService.getContentByGenre(type as 'movie' | 'tv', genreId ?? '');
      res.status(200).json(content);
    } catch (error: unknown) {
      res.status(500).json({ error: (error as Error).message });
    }
  });

  // ── Smart Search: TMDB results → AI re-ranked ────────────────────────────
  // 1. Fetches candidate movies from TMDB multi-search (3 pages)
  // 2. Sends them to the AI service for semantic re-ranking
  // 3. Returns results sorted by semantic similarity to the query
  // Falls back to raw TMDB results if the AI service is unavailable.
  router.get('/smart-search', async (req: Request, res: Response) => {
    const query = sanitiseQuery(req.query.q);
    if (!query) {
      res.status(400).json({ error: 'Search query is required' });
      return;
    }

    let rawMovies: Movie[];
    try {
      rawMovies = await movieService.getDeepDiscovery(query);
    } catch (fetchErr: unknown) {
      res.status(500).json({ error: (fetchErr as Error).message });
      return;
    }

    try {
      const reranked = await rerankMovies(query, rawMovies);
      res.status(200).json({
        query,
        source: 'tmdb+ai-rerank',
        results: reranked.results ?? rawMovies,
      });
    } catch (rankErr: unknown) {
      // AI service unavailable — return the already-fetched TMDB results as-is
      console.warn('AI re-rank failed, returning TMDB results:', (rankErr as Error).message);
      res.status(200).json({ query, source: 'tmdb-fallback', results: rawMovies });
    }
  });

  // ── Pure AI Recommendation (from CSV embeddings) ─────────────────────────
  // Uses the sentence-transformer model trained on movies.csv to find
  // semantically similar movies without a live TMDB search.
  router.get('/ai-recommend', async (req: Request, res: Response) => {
    const query = sanitiseQuery(req.query.q);
    if (!query) {
      res.status(400).json({ error: 'Search query is required' });
      return;
    }
    try {
      const result = await getAIRecommendations(query);
      res.status(200).json({ query, source: 'ai-embeddings', ...result });
    } catch (error: unknown) {
      res.status(500).json({ error: (error as Error).message });
    }
  });

  // ── Movie / TV Detail (cast, trailer, credits) ───────────────────────────
  router.get('/details/:type/:id', async (req: Request, res: Response) => {
    const { type, id } = req.params;
    try {
      const result = await movieService.getMovieDetails(type as 'movie' | 'tv', id ?? '');
      res.status(200).json(result);
    } catch (error: unknown) {
      res.status(500).json({ error: (error as Error).message });
    }
  });

  return router;
};

export default createDiscoveryRouter();
