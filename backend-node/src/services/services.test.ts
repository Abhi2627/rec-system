import test from 'node:test';
import assert from 'node:assert/strict';
import axios from 'axios';

import {
  getAIRecommendations,
  rerankMovies,
} from './aiService.js';
import {
  clearCache,
  getDeepDiscovery,
  getTrendingMovies,
} from './movieService.js';

// ── AI service tests ────────────────────────────────────────────────────────

test('getAIRecommendations returns AI response payload', async () => {
  const originalPost = axios.post;

  (axios as any).post = async (url: string, body: Record<string, unknown>) => {
    assert.equal(url, 'http://localhost:8001/recommend');
    assert.deepEqual(body, { query: 'space adventure', top_k: 5 });

    return {
      data: {
        query: 'space adventure',
        recommendations: [{ id: 1, title: 'Interstellar' }],
      },
    };
  };

  try {
    const response = await getAIRecommendations('space adventure');

    assert.equal(response.query, 'space adventure');
    assert.equal(response.recommendations[0].title, 'Interstellar');
  } finally {
    (axios as any).post = originalPost;
  }
});

test('rerankMovies surfaces unreachable AI rerank engine', async () => {
  const originalPost = axios.post;

  (axios as any).post = async () => {
    throw new Error('connect ECONNREFUSED');
  };

  try {
    await assert.rejects(
      () => rerankMovies('space', [{ id: 1, title: 'Interstellar' }]),
      /AI Rerank Engine unreachable/,
    );
  } finally {
    (axios as any).post = originalPost;
  }
});

// ── Movie service tests ─────────────────────────────────────────────────────

test('getTrendingMovies maps TMDB payload for the client', async () => {
  clearCache();
  const originalToken = process.env['TMDB_TOKEN'];
  const originalGet = axios.get;
  process.env['TMDB_TOKEN'] = 'test-token';

  (axios as any).get = async (url: string, options: Record<string, unknown>) => {
    assert.equal(url, 'https://api.themoviedb.org/3/trending/movie/week');
    assert.equal((options as any).timeout, 10_000);
    assert.deepEqual((options as any).headers, {
      Authorization: 'Bearer test-token',
      Accept: 'application/json',
    });

    return {
      data: {
        results: [
          {
            id: 42,
            title: 'Arrival',
            overview: 'First contact story',
            poster_path: '/arrival.jpg',
            release_date: '2016-11-11',
            vote_average: 7.9,
          },
          {
            id: 99,
            title: 'No Poster Movie',
            overview: '',
            poster_path: null,
            release_date: '',
            vote_average: 0,
          },
        ],
      },
    };
  };

  try {
    const movies = await getTrendingMovies();

    assert.equal(movies.length, 2);
    assert.equal(movies[0].poster_path, 'https://image.tmdb.org/t/p/w500/arrival.jpg');
    assert.equal(movies[1].poster_path, '');
  } finally {
    clearCache();
    if (originalToken === undefined) {
      delete process.env['TMDB_TOKEN'];
    } else {
      process.env['TMDB_TOKEN'] = originalToken;
    }
    (axios as any).get = originalGet;
  }
});

test('getDeepDiscovery fetches three TMDB pages and flattens results', async () => {
  clearCache();
  const originalToken = process.env['TMDB_TOKEN'];
  const originalGet = axios.get;
  process.env['TMDB_TOKEN'] = 'test-token';
  const requestedPages: number[] = [];

  (axios as any).get = async (
    url: string,
    options: { params: { query: string; page: number } },
  ) => {
    assert.equal(url, 'https://api.themoviedb.org/3/search/movie');
    assert.equal(options.params.query, 'space');
    requestedPages.push(options.params.page);

    return {
      data: {
        results: [
          {
            id: options.params.page,
            title: `Movie ${options.params.page}`,
            overview: `Overview ${options.params.page}`,
            poster_path: null,
            release_date: '2024-01-01',
            vote_average: 7 + options.params.page / 10,
          },
        ],
      },
    };
  };

  try {
    const movies = await getDeepDiscovery('space');

    assert.deepEqual(requestedPages, [1, 2, 3]);
    assert.equal(movies.length, 3);
    assert.deepEqual(
      movies.map((m) => m.title),
      ['Movie 1', 'Movie 2', 'Movie 3'],
    );
  } finally {
    clearCache();
    if (originalToken === undefined) {
      delete process.env['TMDB_TOKEN'];
    } else {
      process.env['TMDB_TOKEN'] = originalToken;
    }
    (axios as any).get = originalGet;
  }
});

test('getTrendingMovies fails fast when TMDB token is missing', async () => {
  clearCache();
  const originalToken = process.env['TMDB_TOKEN'];
  delete process.env['TMDB_TOKEN'];

  try {
    await assert.rejects(
      () => getTrendingMovies(),
      /TMDB_TOKEN is missing in \.env file/,
    );
  } finally {
    clearCache();
    if (originalToken !== undefined) {
      process.env['TMDB_TOKEN'] = originalToken;
    }
  }
});

test('getTrendingMovies retries transient TMDB resets before succeeding', async () => {
  clearCache();
  const originalToken = process.env['TMDB_TOKEN'];
  const originalGet = axios.get;
  process.env['TMDB_TOKEN'] = 'test-token';
  let attempts = 0;

  (axios as any).get = async () => {
    attempts += 1;

    if (attempts < 3) {
      const error = Object.assign(new Error('socket hang up'), { code: 'ECONNRESET' });
      throw error;
    }

    return {
      data: {
        results: [
          {
            id: 7,
            title: 'Recovered Movie',
            overview: 'Succeeded after retry',
            poster_path: null,
            release_date: '2025-01-01',
            vote_average: 7.1,
          },
        ],
      },
    };
  };

  try {
    const movies = await getTrendingMovies();

    assert.equal(attempts, 3);
    assert.equal(movies[0].title, 'Recovered Movie');
  } finally {
    clearCache();
    if (originalToken === undefined) {
      delete process.env['TMDB_TOKEN'];
    } else {
      process.env['TMDB_TOKEN'] = originalToken;
    }
    (axios as any).get = originalGet;
  }
});

test('getTrendingMovies returns clear error after repeated TMDB resets', async () => {
  clearCache();
  const originalToken = process.env['TMDB_TOKEN'];
  const originalGet = axios.get;
  process.env['TMDB_TOKEN'] = 'test-token';

  (axios as any).get = async () => {
    throw Object.assign(new Error('socket hang up'), { code: 'ECONNRESET' });
  };

  try {
    await assert.rejects(
      () => getTrendingMovies(),
      /TMDB is temporarily unreachable after multiple retries/,
    );
  } finally {
    clearCache();
    if (originalToken === undefined) {
      delete process.env['TMDB_TOKEN'];
    } else {
      process.env['TMDB_TOKEN'] = originalToken;
    }
    (axios as any).get = originalGet;
  }
});
