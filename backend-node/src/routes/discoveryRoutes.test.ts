import test from 'node:test';
import assert from 'node:assert/strict';
import request from 'supertest';

import { createApp, createDiscoveryRouter } from '../app.js';

test('GET /health returns OK', async () => {
  const app = createApp();

  const response = await request(app).get('/health');

  assert.equal(response.status, 200);
  assert.equal(response.body.status, 'OK');
});

test('GET /api/discovery/smart-search requires query parameter', async () => {
  const app = createApp();

  const response = await request(app).get('/api/discovery/smart-search');

  assert.equal(response.status, 400);
  assert.equal(response.body.error, 'Search query is required');
});

test('GET /api/discovery/smart-search rejects empty query', async () => {
  const app = createApp();

  const response = await request(app).get('/api/discovery/smart-search?q=');

  assert.equal(response.status, 400);
  assert.equal(response.body.error, 'Search query is required');
});

test('GET /api/discovery/trending returns movie payload', async () => {
  const router = createDiscoveryRouter({
    getTrendingMovies: async () => [
      {
        id: 1,
        title: 'Mock Movie',
        overview: 'Mock Overview',
        poster_path: '',
        release_date: '2025-01-01',
        vote_average: 7.5,
      },
    ],
  });
  const app = createApp(router);

  const response = await request(app).get('/api/discovery/trending');

  assert.equal(response.status, 200);
  assert.equal(response.body[0].title, 'Mock Movie');
});

test('GET /api/discovery/smart-search reranks TMDB candidates', async () => {
  const rawMovies = [
    {
      id: 10,
      title: 'Interstellar',
      overview: 'Space exploration',
      poster_path: '',
      release_date: '2014-11-07',
      vote_average: 8.6,
    },
  ];

  const router = createDiscoveryRouter({
    getTrendingMovies: async () => [],
    getDeepDiscovery: async () => rawMovies,
    rerankMovies: async () => ({
      query: 'space',
      results: [{ ...rawMovies[0], score: 0.9 }],
    }),
  });
  const app = createApp(router);

  const response = await request(app).get('/api/discovery/smart-search?q=space');

  assert.equal(response.status, 200);
  assert.equal(response.body.source, 'tmdb+ai-rerank');
  assert.equal((response.body.results as { title: string }[])[0].title, 'Interstellar');
});

test('GET /api/discovery/smart-search falls back to TMDB when AI service fails', async () => {
  const rawMovies = [
    {
      id: 10,
      title: 'Interstellar',
      overview: 'Space exploration',
      poster_path: '',
      release_date: '2014-11-07',
      vote_average: 8.6,
    },
  ];

  const router = createDiscoveryRouter({
    getDeepDiscovery: async () => rawMovies,
    rerankMovies: async () => { throw new Error('AI service unreachable'); },
  });
  const app = createApp(router);

  const response = await request(app).get('/api/discovery/smart-search?q=space');

  assert.equal(response.status, 200);
  assert.equal(response.body.source, 'tmdb-fallback');
  assert.equal((response.body.results as { title: string }[])[0].title, 'Interstellar');
});
