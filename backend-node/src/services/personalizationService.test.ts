import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';

/**
 * Each test loads personalizationService from a unique tempDir (via query-string
 * cache-busting) so the SQLite db singleton is fresh per test.
 * closeDatabase() is called in finally blocks to release the file handle so the
 * temp directory can be cleaned up by the OS.
 */
const loadPersonalizationService = async (cwd: string) => {
  const previousCwd = process.cwd();
  process.chdir(cwd);

  try {
    return await import(
      `./personalizationService.js?cwd=${encodeURIComponent(cwd)}&ts=${Date.now()}`
    );
  } finally {
    process.chdir(previousCwd);
  }
};

test('personalization profile persists preferences, movies, and searches in sqlite', async () => {
  const tempDir = await fs.mkdtemp(path.join(os.tmpdir(), 'rec-personalization-'));
  const service = await loadPersonalizationService(tempDir);

  try {
    let profile = service.getPersonalizationProfile('user-1');
    assert.equal(profile.userId, 'user-1');
    assert.equal(profile.displayName, '');
    assert.equal(profile.age, null);
    assert.deepEqual(profile.favoriteGenres, []);
    assert.deepEqual(profile.savedMovies, []);

    profile = service.updatePersonalizationProfile(
      'user-1',
      'Abhay',
      27,
      ['Sci-Fi', 'Drama', 'Sci-Fi'], // duplicate should be deduped
      ['space', 'future'],
      ['Shah Rukh Khan'],
      ['Priyanka Chopra'],
      ['Christopher Nolan'],
    );
    assert.equal(profile.displayName, 'Abhay');
    assert.equal(profile.age, 27);
    assert.deepEqual(profile.favoriteGenres, ['Sci-Fi', 'Drama']);
    assert.deepEqual(profile.favoriteKeywords, ['space', 'future']);
    assert.deepEqual(profile.favoriteActors, ['Shah Rukh Khan']);
    assert.deepEqual(profile.favoriteActresses, ['Priyanka Chopra']);
    assert.deepEqual(profile.favoriteDirectors, ['Christopher Nolan']);

    profile = service.saveMovieForUser('user-1', {
      id: 42,
      title: 'Interstellar',
      overview: 'Space exploration',
      vote_average: 8.6,
    });
    assert.equal(profile.savedMovies.length, 1);
    assert.equal(profile.savedMovies[0].title, 'Interstellar');

    service.addSearchHistoryEntry('user-1', 'space adventure');
    profile = service.addSearchHistoryEntry('user-1', 'time travel');
    assert.deepEqual(profile.recentSearches, ['time travel', 'space adventure']);

    // Confirm SQLite db was created at the correct path
    const dbPath = path.join(tempDir, 'data', 'personalization.db');
    await assert.doesNotReject(() => fs.access(dbPath), 'personalization.db should exist');
  } finally {
    service.closeDatabase();
  }
});

test('saved movie can be removed', async () => {
  const tempDir = await fs.mkdtemp(path.join(os.tmpdir(), 'rec-personalization-'));
  const service = await loadPersonalizationService(tempDir);

  try {
    service.saveMovieForUser('user-1', { id: 42, title: 'Interstellar' });
    service.saveMovieForUser('user-1', { id: 99, title: 'Arrival' });

    let profile = service.getPersonalizationProfile('user-1');
    assert.equal(profile.savedMovies.length, 2);

    profile = service.removeSavedMovieForUser('user-1', 42);
    assert.equal(profile.savedMovies.length, 1);
    assert.equal(profile.savedMovies[0].title, 'Arrival');
  } finally {
    service.closeDatabase();
  }
});

test('search history is capped at 10 entries and deduplicates', async () => {
  const tempDir = await fs.mkdtemp(path.join(os.tmpdir(), 'rec-personalization-'));
  const service = await loadPersonalizationService(tempDir);

  try {
    // Add 12 unique queries — only the 10 most recent should be kept
    for (let i = 1; i <= 12; i++) {
      service.addSearchHistoryEntry('user-1', `query ${i}`);
    }
    const profile = service.getPersonalizationProfile('user-1');
    assert.equal(profile.recentSearches.length, 10);
    assert.equal(profile.recentSearches[0], 'query 12'); // most recent first

    // Adding the same query again should promote it to top, not duplicate
    service.addSearchHistoryEntry('user-1', 'query 12');
    const profile2 = service.getPersonalizationProfile('user-1');
    assert.equal(profile2.recentSearches.length, 10);
    assert.equal(profile2.recentSearches[0], 'query 12');
    assert.equal(
      profile2.recentSearches.filter((q: string) => q === 'query 12').length,
      1,
      'query 12 should not be duplicated',
    );
  } finally {
    service.closeDatabase();
  }
});

test('buildPersonalizedPrompt blends query with stored personalization context', async () => {
  const tempDir = await fs.mkdtemp(path.join(os.tmpdir(), 'rec-personalization-'));
  const service = await loadPersonalizationService(tempDir);

  try {
    service.updatePersonalizationProfile(
      'user-1',
      'Abhay',
      27,
      ['Sci-Fi'],
      ['space'],
      ['Shah Rukh Khan'],
      ['Priyanka Chopra'],
      ['Christopher Nolan'],
    );
    service.saveMovieForUser('user-1', { id: 1, title: 'Interstellar' });
    const profile = service.addSearchHistoryEntry('user-1', 'wormhole movie');

    const prompt = service.buildPersonalizedPrompt(profile, 'mind-bending sci-fi');

    assert.match(prompt, /mind-bending sci-fi/);
    assert.match(prompt, /Preferred genres: Sci-Fi/);
    assert.match(prompt, /Preferred themes: space/);
    assert.match(prompt, /Favorite actors: Shah Rukh Khan/);
    assert.match(prompt, /Favorite actresses: Priyanka Chopra/);
    assert.match(prompt, /Favorite directors: Christopher Nolan/);
    assert.match(prompt, /Recent searches: wormhole movie/);
    assert.match(prompt, /Saved movies: Interstellar/);
  } finally {
    service.closeDatabase();
  }
});
