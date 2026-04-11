import dotenv from 'dotenv';
import { createApp } from './app.js';

dotenv.config();

const app = createApp();
const PORT = process.env.PORT || 8000;
app.listen(PORT, () => {
    console.log(`-----------------------------------------------`);
    console.log(`🚀 Server running at http://localhost:${PORT}`);
    console.log(`✅ Health Check: http://localhost:${PORT}/health`);
    console.log(`-----------------------------------------------`);
});
