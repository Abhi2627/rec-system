import fs from 'node:fs';
import path from 'node:path';
import { DatabaseSync } from 'node:sqlite';

export type PersonalizationMovie = {
  id: number;
  title: string;
  overview?: string;
  poster_path?: string;
  release_date?: string;
  vote_average?: number;
};

export type PersonalizationProfile = {
  userId: string;
  displayName: string;
  age: number | null;
  favoriteGenres: string[];
  favoriteKeywords: string[];
  favoriteActors: string[];
  favoriteActresses: string[];
  favoriteDirectors: string[];
  savedMovies: PersonalizationMovie[];
  recentSearches: string[];
  updatedAt: string;
};

const DATA_DIR = path.resolve(process.cwd(), 'data');
const DB_PATH = path.join(DATA_DIR, 'personalization.db');

let database: DatabaseSync | null = null;

const ensureColumn = (db: DatabaseSync, tableName: string, columnName: string, definition: string) => {
  const columns = db.prepare(`PRAGMA table_info(${tableName})`).all() as Array<{ name: string }>;
  if (!columns.some((column) => column.name === columnName)) {
    db.exec(`ALTER TABLE ${tableName} ADD COLUMN ${columnName} ${definition}`);
  }
};

/**
 * Close and release the SQLite connection.
 * Call this in tests after each test that uses a temp working directory,
 * so the module-level singleton is reset for the next test.
 */
export const closeDatabase = (): void => {
  if (database) {
    database.close();
    database = null;
  }
};

const ensureDatabase = (): DatabaseSync => {
  if (database) {
    return database;
  }

  fs.mkdirSync(DATA_DIR, { recursive: true });
  database = new DatabaseSync(DB_PATH);
  database.exec(`
    PRAGMA journal_mode = WAL;

    CREATE TABLE IF NOT EXISTS personalization_profiles (
      user_id TEXT PRIMARY KEY,
      display_name TEXT NOT NULL DEFAULT '',
      age INTEGER,
      favorite_genres TEXT NOT NULL DEFAULT '[]',
      favorite_keywords TEXT NOT NULL DEFAULT '[]',
      favorite_actors TEXT NOT NULL DEFAULT '[]',
      favorite_actresses TEXT NOT NULL DEFAULT '[]',
      favorite_directors TEXT NOT NULL DEFAULT '[]',
      updated_at TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS saved_movies (
      user_id TEXT NOT NULL,
      movie_id INTEGER NOT NULL,
      title TEXT NOT NULL,
      overview TEXT NOT NULL DEFAULT '',
      poster_path TEXT NOT NULL DEFAULT '',
      release_date TEXT NOT NULL DEFAULT '',
      vote_average REAL NOT NULL DEFAULT 0,
      saved_at TEXT NOT NULL,
      PRIMARY KEY (user_id, movie_id)
    );

    CREATE TABLE IF NOT EXISTS search_history (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id TEXT NOT NULL,
      query TEXT NOT NULL,
      searched_at TEXT NOT NULL
    );
  `);
  ensureColumn(database, 'personalization_profiles', 'display_name', "TEXT NOT NULL DEFAULT ''");
  ensureColumn(database, 'personalization_profiles', 'age', 'INTEGER');
  ensureColumn(database, 'personalization_profiles', 'favorite_actors', "TEXT NOT NULL DEFAULT '[]'");
  ensureColumn(database, 'personalization_profiles', 'favorite_actresses', "TEXT NOT NULL DEFAULT '[]'");
  ensureColumn(database, 'personalization_profiles', 'favorite_directors', "TEXT NOT NULL DEFAULT '[]'");

  return database;
};

const parseJsonArray = (value: unknown): string[] => {
  if (typeof value !== 'string') {
    return [];
  }

  try {
    const parsed = JSON.parse(value) as unknown[];
    return parsed.filter((item): item is string => typeof item === 'string');
  } catch {
    return [];
  }
};

const normalizeList = (values: string[]) =>
  [...new Set(values.map((value) => value.trim()).filter(Boolean))];

const getOrCreateProfileRow = (userId: string) => {
  const db = ensureDatabase();
  const now = new Date().toISOString();
  db.prepare(`
    INSERT INTO personalization_profiles (
      user_id,
      display_name,
      age,
      favorite_genres,
      favorite_keywords,
      favorite_actors,
      favorite_actresses,
      favorite_directors,
      updated_at
    )
    VALUES (?, '', NULL, '[]', '[]', '[]', '[]', '[]', ?)
    ON CONFLICT(user_id) DO NOTHING
  `).run(userId, now);

  return db.prepare(`
    SELECT
      user_id,
      display_name,
      age,
      favorite_genres,
      favorite_keywords,
      favorite_actors,
      favorite_actresses,
      favorite_directors,
      updated_at
    FROM personalization_profiles
    WHERE user_id = ?
  `).get(userId) as {
    user_id: string;
    display_name: string;
    age: number | null;
    favorite_genres: string;
    favorite_keywords: string;
    favorite_actors: string;
    favorite_actresses: string;
    favorite_directors: string;
    updated_at: string;
  };
};

export const getPersonalizationProfile = (userId: string): PersonalizationProfile => {
  const db = ensureDatabase();
  const profileRow = getOrCreateProfileRow(userId);

  const savedMovies = db.prepare(`
    SELECT movie_id, title, overview, poster_path, release_date, vote_average
    FROM saved_movies
    WHERE user_id = ?
    ORDER BY saved_at DESC
  `).all(userId) as Array<{
    movie_id: number;
    title: string;
    overview: string;
    poster_path: string;
    release_date: string;
    vote_average: number;
  }>;

  const recentSearches = db.prepare(`
    SELECT query
    FROM search_history
    WHERE user_id = ?
    ORDER BY searched_at DESC, id DESC
    LIMIT 10
  `).all(userId) as Array<{ query: string }>;

  return {
    userId: profileRow.user_id,
    displayName: profileRow.display_name,
    age: profileRow.age,
    favoriteGenres: parseJsonArray(profileRow.favorite_genres),
    favoriteKeywords: parseJsonArray(profileRow.favorite_keywords),
    favoriteActors: parseJsonArray(profileRow.favorite_actors),
    favoriteActresses: parseJsonArray(profileRow.favorite_actresses),
    favoriteDirectors: parseJsonArray(profileRow.favorite_directors),
    savedMovies: savedMovies.map((movie) => ({
      id: movie.movie_id,
      title: movie.title,
      overview: movie.overview,
      poster_path: movie.poster_path,
      release_date: movie.release_date,
      vote_average: movie.vote_average,
    })),
    recentSearches: recentSearches.map((row) => row.query),
    updatedAt: profileRow.updated_at,
  };
};

export const updatePersonalizationProfile = (
  userId: string,
  displayName: string,
  age: number | null,
  favoriteGenres: string[],
  favoriteKeywords: string[],
  favoriteActors: string[],
  favoriteActresses: string[],
  favoriteDirectors: string[],
) => {
  const db = ensureDatabase();
  const now = new Date().toISOString();
  const normalizedGenres = normalizeList(favoriteGenres);
  const normalizedKeywords = normalizeList(favoriteKeywords);
  const normalizedActors = normalizeList(favoriteActors);
  const normalizedActresses = normalizeList(favoriteActresses);
  const normalizedDirectors = normalizeList(favoriteDirectors);

  db.prepare(`
    INSERT INTO personalization_profiles (
      user_id,
      display_name,
      age,
      favorite_genres,
      favorite_keywords,
      favorite_actors,
      favorite_actresses,
      favorite_directors,
      updated_at
    )
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT(user_id) DO UPDATE SET
      display_name = excluded.display_name,
      age = excluded.age,
      favorite_genres = excluded.favorite_genres,
      favorite_keywords = excluded.favorite_keywords,
      favorite_actors = excluded.favorite_actors,
      favorite_actresses = excluded.favorite_actresses,
      favorite_directors = excluded.favorite_directors,
      updated_at = excluded.updated_at
  `).run(
    userId,
    displayName.trim(),
    age,
    JSON.stringify(normalizedGenres),
    JSON.stringify(normalizedKeywords),
    JSON.stringify(normalizedActors),
    JSON.stringify(normalizedActresses),
    JSON.stringify(normalizedDirectors),
    now,
  );

  return getPersonalizationProfile(userId);
};

export const saveMovieForUser = (
  userId: string,
  movie: PersonalizationMovie,
) => {
  const db = ensureDatabase();
  db.prepare(`
    INSERT INTO saved_movies (
      user_id,
      movie_id,
      title,
      overview,
      poster_path,
      release_date,
      vote_average,
      saved_at
    )
    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT(user_id, movie_id) DO UPDATE SET
      title = excluded.title,
      overview = excluded.overview,
      poster_path = excluded.poster_path,
      release_date = excluded.release_date,
      vote_average = excluded.vote_average,
      saved_at = excluded.saved_at
  `).run(
    userId,
    movie.id,
    movie.title,
    movie.overview ?? '',
    movie.poster_path ?? '',
    movie.release_date ?? '',
    movie.vote_average ?? 0,
    new Date().toISOString(),
  );

  return getPersonalizationProfile(userId);
};

export const removeSavedMovieForUser = (userId: string, movieId: number) => {
  const db = ensureDatabase();
  db.prepare(`
    DELETE FROM saved_movies
    WHERE user_id = ? AND movie_id = ?
  `).run(userId, movieId);

  return getPersonalizationProfile(userId);
};

export const addSearchHistoryEntry = (userId: string, query: string) => {
  const db = ensureDatabase();
  const trimmedQuery = query.trim();
  if (!trimmedQuery) {
    return getPersonalizationProfile(userId);
  }

  db.prepare(`
    DELETE FROM search_history
    WHERE user_id = ? AND query = ?
  `).run(userId, trimmedQuery);

  db.prepare(`
    INSERT INTO search_history (user_id, query, searched_at)
    VALUES (?, ?, ?)
  `).run(userId, trimmedQuery, new Date().toISOString());

  db.prepare(`
    DELETE FROM search_history
    WHERE user_id = ?
      AND id NOT IN (
        SELECT id
        FROM search_history
        WHERE user_id = ?
        ORDER BY searched_at DESC, id DESC
        LIMIT 10
      )
  `).run(userId, userId);

  return getPersonalizationProfile(userId);
};

export const buildPersonalizedPrompt = (
  profile: PersonalizationProfile,
  query: string,
) => {
  const parts = [query.trim()];

  if (profile.favoriteGenres.length > 0) {
    parts.push(`Preferred genres: ${profile.favoriteGenres.join(', ')}`);
  }

  if (profile.favoriteKeywords.length > 0) {
    parts.push(`Preferred themes: ${profile.favoriteKeywords.join(', ')}`);
  }

  if (profile.favoriteActors.length > 0) {
    parts.push(`Favorite actors: ${profile.favoriteActors.join(', ')}`);
  }

  if (profile.favoriteActresses.length > 0) {
    parts.push(`Favorite actresses: ${profile.favoriteActresses.join(', ')}`);
  }

  if (profile.favoriteDirectors.length > 0) {
    parts.push(`Favorite directors: ${profile.favoriteDirectors.join(', ')}`);
  }

  if (profile.recentSearches.length > 0) {
    parts.push(`Recent searches: ${profile.recentSearches.slice(0, 3).join(', ')}`);
  }

  if (profile.savedMovies.length > 0) {
    parts.push(`Saved movies: ${profile.savedMovies.slice(0, 3).map((movie) => movie.title).join(', ')}`);
  }

  return parts.filter(Boolean).join('. ');
};
