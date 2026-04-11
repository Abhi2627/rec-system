// src/services/aiService.ts
import axios from 'axios';

const AI_SERVICE_URL = process.env.AI_SERVICE_URL || 'http://localhost:8001';

export const getAIRecommendations = async (userQuery: string) => {
    try {
        const response = await axios.post(`${AI_SERVICE_URL}/recommend`, {
            query: userQuery,
            top_k: 5
        });
        return response.data;
    } catch (error: any) {
        if (error.response) {
            console.error("AI Service Error (Recommend):", error.response.status, error.response.data);
        } else {
            console.error("Failed to connect to AI Service (Recommend):", error.message);
        }
        throw new Error("AI Engine unreachable");
    }
};

export const rerankMovies = async (userQuery: string, movies: any[]) => {
    try {
        const response = await axios.post(`${AI_SERVICE_URL}/rerank`, {
            query: userQuery,
            movies: movies
        });
        return response.data;
    } catch (error: any) {
        if (error.response) {
            console.error("AI Service Error (Rerank):", error.response.status, error.response.data);
        } else {
            console.error("Failed to connect to AI Service (Rerank):", error.message);
        }
        throw new Error("AI Rerank Engine unreachable");
    }
};
