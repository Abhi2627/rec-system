import test from 'node:test';
import assert from 'node:assert/strict';
import request from 'supertest';

import { createApp, createAuthRouter } from '../app.js';

test('POST /api/auth/register creates a user and token', async () => {
  const authRouter = createAuthRouter({
    registerUser: async (name, email) => ({
      user: {
        id: 'user-1',
        name,
        email,
        createdAt: '2026-04-18T00:00:00.000Z',
      },
      token: 'signed-token',
    }),
    loginUser: async () => {
      throw new Error('not used');
    },
    getUserFromToken: async () => null,
  });
  const app = createApp(undefined, authRouter);

  const response = await request(app).post('/api/auth/register').send({
    name: 'Abhay',
    email: 'abhay@example.com',
    password: 'secret123',
  });

  assert.equal(response.status, 201);
  assert.equal(response.body.user.email, 'abhay@example.com');
  assert.equal(response.body.token, 'signed-token');
});

test('POST /api/auth/register validates invalid payload', async () => {
  const app = createApp();

  const response = await request(app).post('/api/auth/register').send({
    name: 'A',
    email: 'bad-email',
    password: '123',
  });

  assert.equal(response.status, 400);
  assert.equal(response.body.error, 'Name must be at least 2 characters long');
});

test('POST /api/auth/login returns 401 for invalid credentials', async () => {
  const authRouter = createAuthRouter({
    registerUser: async () => {
      throw new Error('not used');
    },
    loginUser: async () => {
      throw new Error('Invalid email or password');
    },
    getUserFromToken: async () => null,
  });
  const app = createApp(undefined, authRouter);

  const response = await request(app).post('/api/auth/login').send({
    email: 'abhay@example.com',
    password: 'secret123',
  });

  assert.equal(response.status, 401);
  assert.equal(response.body.error, 'Invalid email or password');
});

test('GET /api/auth/me returns current user for valid token', async () => {
  const authRouter = createAuthRouter({
    registerUser: async () => {
      throw new Error('not used');
    },
    loginUser: async () => {
      throw new Error('not used');
    },
    getUserFromToken: async (token) =>
      token === 'good-token'
        ? {
            id: 'user-1',
            name: 'Abhay',
            email: 'abhay@example.com',
            createdAt: '2026-04-18T00:00:00.000Z',
          }
        : null,
  });
  const app = createApp(undefined, authRouter);

  const response = await request(app)
    .get('/api/auth/me')
    .set('Authorization', 'Bearer good-token');

  assert.equal(response.status, 200);
  assert.equal(response.body.user.name, 'Abhay');
});

test('GET /api/auth/me rejects missing token', async () => {
  const app = createApp();

  const response = await request(app).get('/api/auth/me');

  assert.equal(response.status, 401);
  assert.equal(response.body.error, 'Authorization token is required');
});
