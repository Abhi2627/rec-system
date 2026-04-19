import axios from 'axios';

export interface Movie {
  id: number;
  title: string;
  name?: string; // TV shows use 'name' instead of 'title'
  overview: string;
  poster_path: string;
  release_date?: string;
  first_air_date?: string;
  vote_average: number;
  media_type?: string;
}

const TMDB_BASE_URL = 'https://api.themoviedb.org/3';
const TMDB_TIMEOUT_MS = 15_000;
const TMDB_MAX_RETRIES = 2;

// ── In-memory cache ────────────────────────────────────────────────────────
// Reduces redundant TMDB API calls and avoids rate-limit exhaustion.
// TTL: 15 min for trending/genre, 5 min for search results, 30 min for details.
// The cache is a module-level singleton so tests that stub process.env TMDB_TOKEN
// must clear stale entries — use clearCacheForTesting() in test helpers if needed.

type CacheEntry<T> = { value: T; expiresAt: number };
const _cache = new Map<string, CacheEntry<unknown>>();

const cacheGet = <T>(key: string): T | null => {
  const entry = _cache.get(key) as CacheEntry<T> | undefined;
  if (!entry || Date.now() > entry.expiresAt) {
    _cache.delete(key);
    return null;
  }
  return entry.value;
};

const cacheSet = <T>(key: string, value: T, ttlMs: number): void => {
  _cache.set(key, { value, expiresAt: Date.now() + ttlMs });
};

/** Exported for use in tests to prevent stale cache hits between test cases. */
export const clearCache = (): void => _cache.clear();

const TTL_TRENDING_MS = 15 * 60 * 1_000;
const TTL_SEARCH_MS   =  5 * 60 * 1_000;
const TTL_DETAILS_MS  = 30 * 60 * 1_000;

// ── Helpers ────────────────────────────────────────────────────────────────

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

const mapMovie = (item: Record<string, unknown>): Movie => ({
  id: item['id'] as number,
  title:
    (item['title'] as string | undefined) ||
    (item['name'] as string | undefined) ||
    '',
  overview: (item['overview'] as string | undefined) ?? '',
  poster_path: item['poster_path']
    ? `https://image.tmdb.org/t/p/w500${item['poster_path'] as string}`
    : '',
  release_date:
    (item['release_date'] as string | undefined) ||
    (item['first_air_date'] as string | undefined) ||
    '',
  vote_average: (item['vote_average'] as number | undefined) ?? 0,
  media_type:
    (item['media_type'] as string | undefined) ||
    ((item['title'] as string | undefined) ? 'movie' : 'tv'),
});

const createTmdbHeaders = (token: string) => ({
  Authorization: `Bearer ${token}`,
  Accept: 'application/json',
});

const requestTmdb = async <T>(
  path: string,
  token: string,
  options: {
    params?: Record<string, string | number>;
    requestLabel: string;
  },
): Promise<T> => {
  let lastError: unknown;

  for (let attempt = 1; attempt <= TMDB_MAX_RETRIES; attempt += 1) {
    try {
      const response = await axios.get<T>(`${TMDB_BASE_URL}${path}`, {
        headers: createTmdbHeaders(token),
        params: options.params,
        timeout: TMDB_TIMEOUT_MS,
      });
      return response.data;
    } catch (error: unknown) {
      lastError = error;
      // HTTP-level error (4xx/5xx): don't retry, surface immediately
      if ((error as { response?: unknown }).response) break;
      await sleep(attempt * 500);
    }
  }

  const msg = (lastError as Error)?.message ?? 'unknown error';
  // Check whether all retries were network failures (no HTTP response at all)
  const isNetworkFailure = !(lastError as { response?: unknown })?.response;
  if (isNetworkFailure) {
    throw new Error(`TMDB is temporarily unreachable after multiple retries: ${msg}`);
  }
  throw new Error(`TMDB request failed [${options.requestLabel}]: ${msg}`);
};

const getTmdbToken = (): string => {
  const token = process.env['TMDB_TOKEN'];
  if (!token) throw new Error('TMDB_TOKEN is missing in .env file');
  return token;
};

// ── Exported service functions ─────────────────────────────────────────────

export const getTrendingMovies = async (): Promise<Movie[]> => {
  const cacheKey = 'trending:movies';
  const cached = cacheGet<Movie[]>(cacheKey);
  if (cached) return cached;

  const token = getTmdbToken();
  const data = await requestTmdb<{ results: Record<string, unknown>[] }>(
    '/trending/movie/week',
    token,
    { requestLabel: 'TrendingMovies' },
  );
  const movies = data.results.map(mapMovie);
  cacheSet(cacheKey, movies, TTL_TRENDING_MS);
  return movies;
};

export const getTrendingTV = async (): Promise<Movie[]> => {
  const cacheKey = 'trending:tv';
  const cached = cacheGet<Movie[]>(cacheKey);
  if (cached) return cached;

  const token = getTmdbToken();
  const data = await requestTmdb<{ results: Record<string, unknown>[] }>(
    '/trending/tv/week',
    token,
    { requestLabel: 'TrendingTV' },
  );
  const tv = data.results.map(mapMovie);
  cacheSet(cacheKey, tv, TTL_TRENDING_MS);
  return tv;
};

export const getContentByGenre = async (
  type: 'movie' | 'tv',
  genreId: string,
): Promise<Movie[]> => {
  const cacheKey = `genre:${type}:${genreId}`;
  const cached = cacheGet<Movie[]>(cacheKey);
  if (cached) return cached;

  const token = getTmdbToken();
  const data = await requestTmdb<{ results: Record<string, unknown>[] }>(
    `/discover/${type}`,
    token,
    {
      requestLabel: `Genre_${genreId}`,
      params: { with_genres: genreId, sort_by: 'popularity.desc' },
    },
  );
  const content = data.results.map(mapMovie);
  cacheSet(cacheKey, content, TTL_TRENDING_MS);
  return content;
};

export const getDeepDiscovery = async (query: string): Promise<Movie[]> => {
  const cacheKey = `search:${query.toLowerCase()}`;
  const cached = cacheGet<Movie[]>(cacheKey);
  if (cached) return cached;

  const token = getTmdbToken();
  // Fetch 3 pages of movie search results for a broader candidate pool before AI re-ranking.
  // Uses /search/movie (not /search/multi) so results are movies only — consistent with
  // the embedding model trained on movies.csv.
  const responses = await Promise.all(
    [1, 2, 3].map((page) =>
      requestTmdb<{ results: Record<string, unknown>[] }>('/search/movie', token, {
        requestLabel: 'SearchMovie',
        params: { query, page },
      }),
    ),
  );
  const movies = responses.flatMap((r) => r.results).map(mapMovie);
  cacheSet(cacheKey, movies, TTL_SEARCH_MS);
  return movies;
};

export const getMovieDetails = async (
  type: 'movie' | 'tv',
  id: string,
): Promise<unknown> => {
  const cacheKey = `details:${type}:${id}`;
  const cached = cacheGet<unknown>(cacheKey);
  if (cached) return cached;

  const token = getTmdbToken();
  const data = await requestTmdb<Record<string, unknown>>(`/${type}/${id}`, token, {
    requestLabel: `Details_${type}_${id}`,
    params: { append_to_response: 'credits,videos' },
  });

  type CrewMember  = { job: string; name: string };
  type CastMember  = { name: string; character?: string; profile_path?: string };
  type Video       = { type: string; site: string; key: string };

  const credits = data['credits'] as
    | { crew?: CrewMember[]; cast?: CastMember[] }
    | undefined;
  const videos = data['videos'] as { results?: Video[] } | undefined;

  const director =
    credits?.crew?.find((c) => c.job === 'Director')?.name ?? '';

  const cast =
    credits?.cast?.slice(0, 15).map((c) => ({
      name: c.name,
      character: c.character ?? '',
      profilePath: c.profile_path
        ? `https://image.tmdb.org/t/p/w185${c.profile_path}`
        : '',
    })) ?? [];

  const trailerKey =
    videos?.results?.find((v) => v.type === 'Trailer' && v.site === 'YouTube')
      ?.key ?? '';

  const posterPath   = data['poster_path']   as string | undefined;
  const backdropPath = data['backdrop_path'] as string | undefined;
  const episodeRunTime = data['episode_run_time'] as number[] | undefined;

  const result = {
    id: data['id'] as number,
    title:
      (data['title'] as string | undefined) ||
      (data['name'] as string | undefined) ||
      '',
    overview: (data['overview'] as string | undefined) ?? '',
    poster_path: posterPath
      ? `https://image.tmdb.org/t/p/w500${posterPath}`
      : '',
    backdrop_path: backdropPath
      ? `https://image.tmdb.org/t/p/w780${backdropPath}`
      : '',
    release_date:
      (data['release_date'] as string | undefined) ||
      (data['first_air_date'] as string | undefined) ||
      '',
    vote_average: (data['vote_average'] as number | undefined) ?? 0,
    runtime:
      (data['runtime'] as number | undefined) || (episodeRunTime?.[0] ?? 0),
    director,
    cast,
    trailerKey,
    trailerUrl: trailerKey
      ? `https://www.youtube.com/watch?v=${trailerKey}`
      : '',
    genres:
      (data['genres'] as { name: string }[] | undefined)?.map((g) => g.name) ??
      [],
  };

  cacheSet(cacheKey, result, TTL_DETAILS_MS);
  return result;
};
