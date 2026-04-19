import os from 'node:os';
import dotenv from 'dotenv';
import { createApp } from './app.js';

dotenv.config();

const app = createApp();
const PORT = Number(process.env['PORT']) || 8000;

/** Resolve the machine's primary LAN IPv4 address at startup for convenience logging. */
const getLocalIp = (): string => {
  const interfaces = os.networkInterfaces();
  for (const iface of Object.values(interfaces)) {
    for (const entry of iface ?? []) {
      if (entry.family === 'IPv4' && !entry.internal) {
        return entry.address;
      }
    }
  }
  return '127.0.0.1';
};

app.listen(PORT, '0.0.0.0', () => {
  const localIp = getLocalIp();
  console.log('-----------------------------------------------');
  console.log(`🚀 Server running on port ${PORT}`);
  console.log(`💻 Local:   http://127.0.0.1:${PORT}`);
  console.log(`📱 Network: http://${localIp}:${PORT}`);
  console.log(`✅ Health:  http://${localIp}:${PORT}/health`);
  console.log('-----------------------------------------------');
});
