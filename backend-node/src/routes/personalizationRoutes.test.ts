import test from 'node:test';
import assert from 'node:assert/strict';
import request from 'supertest';

import { createApp, createPersonalizationRouter } from '../app.js';

const baseProfile = {
  userId: 'user-1',
  displayName: 'Abhay',
  age: 27,
  favoriteGenres: ['Sci-Fi'],
  favoriteKeywords: ['space'],
  favoriteActors: ['Shah Rukh Khan'],
  favoriteActresses: ['Priyanka Chopra'],
  favoriteDirectors: ['Christopher Nolan'],
  savedMovies: [],
  recentSearches: [],
  updatedAt: '2026-04-18T00:00:00.000Z',
};

test('GET /api/personalization/profile requires auth token', async () => {
  const app = createApp();

  const response = await request(app).get('/api/personalization/profile');

  assert.equal(response.status, 401);
  assert.equal(response.body.error, 'Authorization token is required');
});

test('PUT /api/personalization/profile updates preferences', async () => {
  const personalizationRouter = createPersonalizationRouter({
    getUserFromToken: async () => ({
      id: 'user-1',
      name: 'Abhay',
      email: 'abhay@example.com',
      createdAt: '2026-04-18T00:00:00.000Z',
    }),
    getPersonalizationProfile: () => baseProfile,
    updatePersonalizationProfile: (
      _userId,
      displayName,
      age,
      favoriteGenres,
      favoriteKeywords,
      favoriteActors,
      favoriteActresses,
      favoriteDirectors,
    ) => ({
      ...baseProfile,
      displayName,
      age,
      favoriteGenres,
      favoriteKeywords,
      favoriteActors,
      favoriteActresses,
      favoriteDirectors,
    }),
    saveMovieForUser: () => baseProfile,
    removeSavedMovieForUser: () => baseProfile,
    addSearchHistoryEntry: () => baseProfile,
    buildPersonalizedPrompt: () => 'prompt',
    getAIRecommendations: async () => ({ recommendations: [] }),
  });
  const app = createApp(undefined, undefined, personalizationRouter);

  const response = await request(app)
    .put('/api/personalization/profile')
    .set('Authorization', 'Bearer good-token')
    .send({
      displayName: 'Abhay',
      age: 27,
      favoriteGenres: ['Sci-Fi', 'Drama'],
      favoriteKeywords: ['space', 'future'],
      favoriteActors: ['Shah Rukh Khan'],
      favoriteActresses: ['Priyanka Chopra'],
      favoriteDirectors: ['Christopher Nolan'],
    });

  assert.equal(response.status, 200);
  assert.equal(response.body.profile.displayName, 'Abhay');
  assert.deepEqual(response.body.profile.favoriteGenres, ['Sci-Fi', 'Drama']);
  assert.deepEqual(response.body.profile.favoriteKeywords, ['space', 'future']);
});

test('POST /api/personalization/recommendations returns prompt-enriched results', async () => {
  const personalizationRouter = createPersonalizationRouter({
    getUserFromToken: async () => ({
      id: 'user-1',
      name: 'Abhay',
      email: 'abhay@example.com',
      createdAt: '2026-04-18T00:00:00.000Z',
    }),
    getPersonalizationProfile: () => baseProfile,
    updatePersonalizationProfile: () => baseProfile,
    saveMovieForUser: () => baseProfile,
    removeSavedMovieForUser: () => baseProfile,
    addSearchHistoryEntry: () => ({
      ...baseProfile,
      recentSearches: ['mind-bending sci-fi'],
    }),
    buildPersonalizedPrompt: (_profile, query) => `${query}. Preferred genres: Sci-Fi`,
    getAIRecommendations: async () => ({
      recommendations: [{ id: 1, title: 'Interstellar', score: 0.99 }],
    }),
  });
  const app = createApp(undefined, undefined, personalizationRouter);

  const response = await request(app)
    .post('/api/personalization/recommendations')
    .set('Authorization', 'Bearer good-token')
    .send({ query: 'mind-bending sci-fi' });

  assert.equal(response.status, 200);
  assert.match(response.body.prompt, /Preferred genres: Sci-Fi/);
  assert.equal(response.body.recommendations[0].title, 'Interstellar');
});
