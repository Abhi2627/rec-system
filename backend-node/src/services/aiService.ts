// src/services/aiService.ts
import axios from 'axios';

const AI_SERVICE_URL = 'http://localhost:8001';

export const getAIRecommendations = async (userQuery: string) => {
    try {
        const response = await axios.post(`${AI_SERVICE_URL}/recommend`, {
            query: userQuery,
            top_k: 5
        });
        return response.data;
    } catch (error) {
        console.error("Failed to connect to AI Service:", error);
        throw new Error("AI Engine unreachable");
    }
};