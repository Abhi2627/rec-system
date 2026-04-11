import express, { type Request, type Response } from 'express';
import cors from 'cors';
import helmet from 'helmet';
import discoveryRoutes, {
  createDiscoveryRouter,
} from './routes/discoveryRoutes.js';

export const createApp = (
  router = discoveryRoutes,
) => {
  const app = express();

  app.use(helmet());
  app.use(cors());
  app.use(express.json());
  app.use('/api/discovery', router);

  app.get('/health', (req: Request, res: Response) => {
    res.status(200).json({ status: 'OK', message: 'Discovery Engine is running' });
  });

  return app;
};

export { createDiscoveryRouter };
