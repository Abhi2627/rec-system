import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { DatabaseSync } from 'node:sqlite';

// ── Types ─────────────────────────────────────────────────────────────────────

export type UserRecord = {
  id: string;
  name: string;
  email: string;
  passwordHash: string;
  createdAt: string;
};

export type SafeUser = Omit<UserRecord, 'passwordHash'>;

// ── Database setup ────────────────────────────────────────────────────────────

const DATA_DIR = path.resolve(process.cwd(), 'data');
const DB_PATH = path.join(DATA_DIR, 'auth.db');

let db: DatabaseSync | null = null;

/**
 * Close and release the SQLite auth connection.
 * Call this in tests that use a temp working directory so the singleton
 * resets for the next test case.
 */
export const closeDatabase = (): void => {
  if (db) {
    db.close();
    db = null;
  }
};

const getDb = (): DatabaseSync => {
  if (db) return db;

  fs.mkdirSync(DATA_DIR, { recursive: true });
  db = new DatabaseSync(DB_PATH);

  db.exec(`
    PRAGMA journal_mode = WAL;

    CREATE TABLE IF NOT EXISTS users (
      id          TEXT PRIMARY KEY,
      name        TEXT NOT NULL,
      email       TEXT NOT NULL UNIQUE,
      password_hash TEXT NOT NULL,
      created_at  TEXT NOT NULL
    );
  `);

  return db;
};

// ── Helpers ───────────────────────────────────────────────────────────────────

const toSafeUser = (row: {
  id: string;
  name: string;
  email: string;
  created_at: string;
}): SafeUser => ({
  id: row.id,
  name: row.name,
  email: row.email,
  createdAt: row.created_at,
});

const normalizeEmail = (email: string) => email.trim().toLowerCase();

const hashPassword = (password: string): string => {
  const salt = crypto.randomBytes(16).toString('hex');
  // Use scryptSync so the DB write is a single synchronous transaction — no async race
  const derivedKey = crypto.scryptSync(password, salt, 64);
  return `${salt}:${derivedKey.toString('hex')}`;
};

const verifyPassword = (password: string, passwordHash: string): boolean => {
  const [salt, key] = passwordHash.split(':');
  if (!salt || !key) return false;

  const derivedKey = crypto.scryptSync(password, salt, 64);
  const storedKey = Buffer.from(key, 'hex');
  return (
    storedKey.length === derivedKey.length &&
    crypto.timingSafeEqual(storedKey, derivedKey)
  );
};

// ── Token ─────────────────────────────────────────────────────────────────────

const DEFAULT_AUTH_SECRET = 'dev-auth-secret-change-me';
const TOKEN_TTL_MS = 1_000 * 60 * 60 * 24 * 7; // 7 days

const getAuthSecret = () => process.env['AUTH_SECRET'] ?? DEFAULT_AUTH_SECRET;

const base64UrlEncode = (value: string) =>
  Buffer.from(value)
    .toString('base64')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/g, '');

const base64UrlDecode = (value: string) => {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/');
  const padding =
    normalized.length % 4 === 0 ? '' : '='.repeat(4 - (normalized.length % 4));
  return Buffer.from(`${normalized}${padding}`, 'base64').toString('utf8');
};

type TokenPayload = { sub: string; email: string; exp: number };

export const createAuthToken = (user: SafeUser): string => {
  const payload: TokenPayload = {
    sub: user.id,
    email: user.email,
    exp: Date.now() + TOKEN_TTL_MS,
  };
  const encodedPayload = base64UrlEncode(JSON.stringify(payload));
  const signature = crypto
    .createHmac('sha256', getAuthSecret())
    .update(encodedPayload)
    .digest('base64url');
  return `${encodedPayload}.${signature}`;
};

export const verifyAuthToken = (token: string): TokenPayload | null => {
  const [encodedPayload, signature] = token.split('.');
  if (!encodedPayload || !signature) return null;

  const expectedSignature = crypto
    .createHmac('sha256', getAuthSecret())
    .update(encodedPayload)
    .digest('base64url');

  const provided = Buffer.from(signature);
  const expected = Buffer.from(expectedSignature);
  if (
    provided.length !== expected.length ||
    !crypto.timingSafeEqual(provided, expected)
  ) {
    return null;
  }

  try {
    const payload = JSON.parse(base64UrlDecode(encodedPayload)) as TokenPayload;
    if (payload.exp <= Date.now()) return null;
    return payload;
  } catch {
    return null;
  }
};

// ── Public API ────────────────────────────────────────────────────────────────

export const registerUser = async (
  name: string,
  email: string,
  password: string,
): Promise<{ user: SafeUser; token: string }> => {
  const normalizedEmail = normalizeEmail(email);
  const database = getDb();

  const existing = database
    .prepare('SELECT id FROM users WHERE email = ?')
    .get(normalizedEmail);

  if (existing) {
    throw new Error('User already exists');
  }

  const user: UserRecord = {
    id: crypto.randomUUID(),
    name: name.trim(),
    email: normalizedEmail,
    passwordHash: hashPassword(password),
    createdAt: new Date().toISOString(),
  };

  database.prepare(`
    INSERT INTO users (id, name, email, password_hash, created_at)
    VALUES (?, ?, ?, ?, ?)
  `).run(user.id, user.name, user.email, user.passwordHash, user.createdAt);

  const safeUser: SafeUser = { id: user.id, name: user.name, email: user.email, createdAt: user.createdAt };
  return { user: safeUser, token: createAuthToken(safeUser) };
};

export const loginUser = async (
  email: string,
  password: string,
): Promise<{ user: SafeUser; token: string }> => {
  const normalizedEmail = normalizeEmail(email);
  const database = getDb();

  const row = database
    .prepare('SELECT id, name, email, password_hash, created_at FROM users WHERE email = ?')
    .get(normalizedEmail) as { id: string; name: string; email: string; password_hash: string; created_at: string } | undefined;

  if (!row || !verifyPassword(password, row.password_hash)) {
    throw new Error('Invalid email or password');
  }

  const safeUser = toSafeUser(row);
  return { user: safeUser, token: createAuthToken(safeUser) };
};

export const getUserFromToken = async (token: string): Promise<SafeUser | null> => {
  const payload = verifyAuthToken(token);
  if (!payload) return null;

  const database = getDb();
  const row = database
    .prepare('SELECT id, name, email, created_at FROM users WHERE id = ?')
    .get(payload.sub) as { id: string; name: string; email: string; created_at: string } | undefined;

  return row ? toSafeUser(row) : null;
};
