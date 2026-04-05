import express, { type Request, type Response } from 'express';
import dotenv from 'dotenv';
import cors from 'cors';
import helmet from 'helmet';
import discoveryRoutes from './routes/discoveryRoutes.js';

// 1. Initialize configuration
dotenv.config();

const app = express();
const PORT = process.env.PORT || 8000;

// 2. Essential Middlewares
app.use(helmet()); // Security headers
app.use(cors());   // Enable Cross-Origin Resource Sharing
app.use(express.json()); // Parse JSON bodies
app.use('/api/discovery', discoveryRoutes); // Mount discovery routes

// 3. Health Check Route (Standard for MLOps/Production)
// Why: It allows monitoring tools to see if your service is alive.
app.get('/health', (req: Request, res: Response) => {
    res.status(200).json({ status: 'OK', message: 'Discovery Engine is running' });
});

// 4. Start the Server
app.listen(PORT, () => {
    console.log(`-----------------------------------------------`);
    console.log(`🚀 Server running at http://localhost:${PORT}`);
    console.log(`✅ Health Check: http://localhost:${PORT}/health`);
    console.log(`-----------------------------------------------`);
});