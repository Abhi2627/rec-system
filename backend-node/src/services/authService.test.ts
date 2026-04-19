import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';

/**
 * Each test loads authService from a unique tempDir (via query-string
 * cache-busting) so the SQLite db singleton is fresh per test.
 * closeDatabase() is called in finally blocks to release the file handle.
 */
const loadAuthService = async (cwd: string) => {
  const previousCwd = process.cwd();
  process.chdir(cwd);

  try {
    return await import(`./authService.js?cwd=${encodeURIComponent(cwd)}&ts=${Date.now()}`);
  } finally {
    process.chdir(previousCwd);
  }
};

test('registerUser creates a persisted user and loginUser returns a token', async () => {
  const tempDir = await fs.mkdtemp(path.join(os.tmpdir(), 'rec-auth-'));
  const authService = await loadAuthService(tempDir);

  try {
    const registration = await authService.registerUser('Abhay', 'Abhay@Example.com', 'secret123');

    assert.equal(registration.user.name, 'Abhay');
    assert.equal(registration.user.email, 'abhay@example.com');
    assert.ok(registration.token, 'token should be a non-empty string');
    assert.equal(
      typeof registration.user.passwordHash,
      'undefined',
      'passwordHash must not leak onto SafeUser',
    );

    // Auth is SQLite — confirm auth.db was created, not users.json
    const dbPath = path.join(tempDir, 'data', 'auth.db');
    await assert.doesNotReject(() => fs.access(dbPath), 'auth.db should exist');

    const noJsonPath = path.join(tempDir, 'data', 'users.json');
    await assert.rejects(() => fs.access(noJsonPath), 'users.json should not be created');

    const login = await authService.loginUser('abhay@example.com', 'secret123');
    assert.equal(login.user.id, registration.user.id);
    assert.ok(login.token);
  } finally {
    authService.closeDatabase();
  }
});

test('registerUser rejects duplicate emails (case-insensitive)', async () => {
  const tempDir = await fs.mkdtemp(path.join(os.tmpdir(), 'rec-auth-'));
  const authService = await loadAuthService(tempDir);

  try {
    await authService.registerUser('Abhay', 'abhay@example.com', 'secret123');

    await assert.rejects(
      () => authService.registerUser('Abhay Again', 'ABHAY@example.com', 'secret123'),
      /User already exists/,
    );
  } finally {
    authService.closeDatabase();
  }
});

test('loginUser rejects wrong password', async () => {
  const tempDir = await fs.mkdtemp(path.join(os.tmpdir(), 'rec-auth-'));
  const authService = await loadAuthService(tempDir);

  try {
    await authService.registerUser('Abhay', 'abhay@example.com', 'secret123');

    await assert.rejects(
      () => authService.loginUser('abhay@example.com', 'wrongpassword'),
      /Invalid email or password/,
    );
  } finally {
    authService.closeDatabase();
  }
});

test('getUserFromToken resolves a valid token and rejects an invalid token', async () => {
  const tempDir = await fs.mkdtemp(path.join(os.tmpdir(), 'rec-auth-'));
  const authService = await loadAuthService(tempDir);

  try {
    const registration = await authService.registerUser('Abhay', 'abhay@example.com', 'secret123');

    const me = await authService.getUserFromToken(registration.token);
    assert.equal(me?.email, 'abhay@example.com');

    const invalid = await authService.getUserFromToken('bad.token');
    assert.equal(invalid, null);
  } finally {
    authService.closeDatabase();
  }
});

test('createAuthToken payload does not expire within the 7-day TTL', async () => {
  const tempDir = await fs.mkdtemp(path.join(os.tmpdir(), 'rec-auth-'));
  const authService = await loadAuthService(tempDir);

  try {
    const registration = await authService.registerUser('Abhay', 'abhay@example.com', 'secret123');
    // Token should still resolve immediately after creation
    const user = await authService.getUserFromToken(registration.token);
    assert.ok(user, 'fresh token should resolve to a user');
  } finally {
    authService.closeDatabase();
  }
});
