import { Router, type Request, type Response } from 'express';
import {
  getUserFromToken,
  loginUser,
  registerUser,
} from '../services/authService.js';

type AuthDependencies = {
  registerUser: typeof registerUser;
  loginUser: typeof loginUser;
  getUserFromToken: typeof getUserFromToken;
};

const defaultDependencies: AuthDependencies = {
  registerUser,
  loginUser,
  getUserFromToken,
};

const getBearerToken = (request: Request) => {
  const header = request.header('authorization');
  if (!header?.startsWith('Bearer ')) {
    return null;
  }

  return header.slice('Bearer '.length).trim();
};

const validateAuthInput = (name: string | undefined, email: string | undefined, password: string | undefined) => {
  if (name !== undefined && name.trim().length < 2) {
    return 'Name must be at least 2 characters long';
  }

  if (!email || !email.includes('@')) {
    return 'Valid email is required';
  }

  if (!password || password.length < 6) {
    return 'Password must be at least 6 characters long';
  }

  return null;
};

export const createAuthRouter = (
  dependencies: AuthDependencies = defaultDependencies,
) => {
  const router = Router();

  router.post('/register', async (req: Request, res: Response) => {
    const { name, email, password } = req.body as {
      name?: string;
      email?: string;
      password?: string;
    };

    const validationError = validateAuthInput(name, email, password);
    if (validationError) {
      res.status(400).json({ error: validationError });
      return;
    }

    try {
      const auth = await dependencies.registerUser(name!, email!, password!);
      res.status(201).json(auth);
    } catch (error: unknown) {
      const msg = (error as Error).message;
      if (msg === 'User already exists') {
        res.status(409).json({ error: msg });
        return;
      }
      res.status(500).json({ error: 'Registration failed' });
    }
  });

  router.post('/login', async (req: Request, res: Response) => {
    const { email, password } = req.body as {
      email?: string;
      password?: string;
    };

    const validationError = validateAuthInput(undefined, email, password);
    if (validationError) {
      res.status(400).json({ error: validationError });
      return;
    }

    try {
      const auth = await dependencies.loginUser(email!, password!);
      res.status(200).json(auth);
    } catch (error: unknown) {
      const msg = (error as Error).message;
      if (msg === 'Invalid email or password') {
        res.status(401).json({ error: msg });
        return;
      }
      res.status(500).json({ error: 'Login failed' });
    }
  });

  router.get('/me', async (req: Request, res: Response) => {
    const token = getBearerToken(req);
    if (!token) {
      res.status(401).json({ error: 'Authorization token is required' });
      return;
    }

    const user = await dependencies.getUserFromToken(token);
    if (!user) {
      res.status(401).json({ error: 'Invalid or expired token' });
      return;
    }

    res.status(200).json({ user });
  });

  return router;
};

export default createAuthRouter();
