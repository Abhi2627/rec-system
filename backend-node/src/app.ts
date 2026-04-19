import express, { type Request, type Response } from 'express';
import cors from 'cors';
import helmet from 'helmet';
import authRoutes, { createAuthRouter } from './routes/authRoutes.js';
import discoveryRoutes, {
  createDiscoveryRouter,
} from './routes/discoveryRoutes.js';
import personalizationRoutes, {
  createPersonalizationRouter,
} from './routes/personalizationRoutes.js';
import type { Router } from 'express';

export const createApp = (
  router: Router = discoveryRoutes,
  authRouter: Router = authRoutes,
  personalizationRouter: Router = personalizationRoutes,
) => {
  const app = express();

  app.use(helmet());
  app.use(cors());
  app.use(express.json());
  app.use('/api/auth', authRouter);
  app.use('/api/discovery', router);
  app.use('/api/personalization', personalizationRouter);

  app.get('/', (_req: Request, res: Response) => {
    res.status(200).json({
      status: 'OK',
      message: 'Rec System backend is running',
      endpoints: {
        health: '/health',
        auth: '/api/auth',
        discovery: '/api/discovery',
        personalization: '/api/personalization',
      },
    });
  });

  app.get('/health', (_req: Request, res: Response) => {
    res.status(200).json({ status: 'OK', message: 'Discovery Engine is running' });
  });

  return app;
};

export { createDiscoveryRouter, createAuthRouter, createPersonalizationRouter };
