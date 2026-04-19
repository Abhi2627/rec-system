import { Router, type Request, type Response } from 'express';
import { getUserFromToken } from '../services/authService.js';
import { getAIRecommendations } from '../services/aiService.js';
import {
  addSearchHistoryEntry,
  buildPersonalizedPrompt,
  getPersonalizationProfile,
  removeSavedMovieForUser,
  saveMovieForUser,
  updatePersonalizationProfile,
  type PersonalizationMovie,
} from '../services/personalizationService.js';

type PersonalizationDependencies = {
  getUserFromToken: typeof getUserFromToken;
  getPersonalizationProfile: typeof getPersonalizationProfile;
  updatePersonalizationProfile: typeof updatePersonalizationProfile;
  saveMovieForUser: typeof saveMovieForUser;
  removeSavedMovieForUser: typeof removeSavedMovieForUser;
  addSearchHistoryEntry: typeof addSearchHistoryEntry;
  buildPersonalizedPrompt: typeof buildPersonalizedPrompt;
  getAIRecommendations: typeof getAIRecommendations;
};

const defaultDependencies: PersonalizationDependencies = {
  getUserFromToken,
  getPersonalizationProfile,
  updatePersonalizationProfile,
  saveMovieForUser,
  removeSavedMovieForUser,
  addSearchHistoryEntry,
  buildPersonalizedPrompt,
  getAIRecommendations,
};

const getBearerToken = (request: Request) => {
  const header = request.header('authorization');
  if (!header?.startsWith('Bearer ')) {
    return null;
  }

  return header.slice('Bearer '.length).trim();
};

const getAuthenticatedUser = async (
  request: Request,
  response: Response,
  dependencies: PersonalizationDependencies,
) => {
  const token = getBearerToken(request);
  if (!token) {
    response.status(401).json({ error: 'Authorization token is required' });
    return null;
  }

  const user = await dependencies.getUserFromToken(token);
  if (!user) {
    response.status(401).json({ error: 'Invalid or expired token' });
    return null;
  }

  return user;
};

export const createPersonalizationRouter = (
  dependencies: PersonalizationDependencies = defaultDependencies,
) => {
  const router = Router();

  router.get('/profile', async (req, res) => {
    const user = await getAuthenticatedUser(req, res, dependencies);
    if (!user) {
      return;
    }

    const profile = dependencies.getPersonalizationProfile(user.id);
    res.status(200).json({ profile });
  });

  router.put('/profile', async (req, res) => {
    const user = await getAuthenticatedUser(req, res, dependencies);
    if (!user) {
      return;
    }

    const displayName = typeof req.body.displayName === 'string'
      ? req.body.displayName
      : '';
    const age = typeof req.body.age === 'number' && req.body.age > 0
      ? Math.floor(req.body.age)
      : null;
    const favoriteGenres = Array.isArray(req.body.favoriteGenres)
      ? req.body.favoriteGenres.filter((item: unknown): item is string => typeof item === 'string')
      : [];
    const favoriteKeywords = Array.isArray(req.body.favoriteKeywords)
      ? req.body.favoriteKeywords.filter((item: unknown): item is string => typeof item === 'string')
      : [];
    const favoriteActors = Array.isArray(req.body.favoriteActors)
      ? req.body.favoriteActors.filter((item: unknown): item is string => typeof item === 'string')
      : [];
    const favoriteActresses = Array.isArray(req.body.favoriteActresses)
      ? req.body.favoriteActresses.filter((item: unknown): item is string => typeof item === 'string')
      : [];
    const favoriteDirectors = Array.isArray(req.body.favoriteDirectors)
      ? req.body.favoriteDirectors.filter((item: unknown): item is string => typeof item === 'string')
      : [];

    const profile = dependencies.updatePersonalizationProfile(
      user.id,
      displayName,
      age,
      favoriteGenres,
      favoriteKeywords,
      favoriteActors,
      favoriteActresses,
      favoriteDirectors,
    );
    res.status(200).json({ profile });
  });

  router.post('/saved-movies', async (req, res) => {
    const user = await getAuthenticatedUser(req, res, dependencies);
    if (!user) {
      return;
    }

    const movie = req.body as PersonalizationMovie;
    if (!movie?.id || !movie?.title) {
      res.status(400).json({ error: 'Movie id and title are required' });
      return;
    }

    const profile = dependencies.saveMovieForUser(user.id, movie);
    res.status(200).json({ profile });
  });

  router.delete('/saved-movies/:movieId', async (req, res) => {
    const user = await getAuthenticatedUser(req, res, dependencies);
    if (!user) {
      return;
    }

    const movieId = Number(req.params.movieId);
    if (!Number.isInteger(movieId) || movieId <= 0) {
      res.status(400).json({ error: 'Valid movie id is required' });
      return;
    }

    const profile = dependencies.removeSavedMovieForUser(user.id, movieId);
    res.status(200).json({ profile });
  });

  router.post('/search-history', async (req, res) => {
    const user = await getAuthenticatedUser(req, res, dependencies);
    if (!user) {
      return;
    }

    const query = typeof req.body.query === 'string' ? req.body.query.trim() : '';
    if (!query || query.length > 200) {
      res.status(400).json({ error: 'Search query is required and must be under 200 characters' });
      return;
    }

    const profile = dependencies.addSearchHistoryEntry(user.id, query);
    res.status(200).json({ profile });
  });

  router.post('/recommendations', async (req, res) => {
    const user = await getAuthenticatedUser(req, res, dependencies);
    if (!user) {
      return;
    }

    const rawQuery = typeof req.body.query === 'string' ? req.body.query.trim() : '';
    if (!rawQuery || rawQuery.length > 200) {
      res.status(400).json({ error: 'Search query is required and must be under 200 characters' });
      return;
    }
    const query = rawQuery;

    const profile = dependencies.addSearchHistoryEntry(user.id, query);
    const prompt = dependencies.buildPersonalizedPrompt(profile, query);
    const results = await dependencies.getAIRecommendations(prompt);

    res.status(200).json({
      query,
      prompt,
      profile,
      recommendations: results.recommendations ?? [],
    });
  });

  return router;
};

export default createPersonalizationRouter();
