import axios from 'axios';

const AI_SERVICE_URL = process.env['AI_SERVICE_URL'] ?? 'http://localhost:8001';

export interface AIRecommendation {
  id: number;
  title: string;
  score: number;
  overview: string;
}

export interface AIRecommendResponse {
  query: string;
  recommendations: AIRecommendation[];
}

export interface AIRerankResponse {
  query: string;
  results: unknown[];
}

export const getAIRecommendations = async (
  userQuery: string,
): Promise<AIRecommendResponse> => {
  try {
    const response = await axios.post<AIRecommendResponse>(
      `${AI_SERVICE_URL}/recommend`,
      { query: userQuery, top_k: 5 },
    );
    return response.data;
  } catch (error: unknown) {
    const axiosErr = error as { response?: { status: number; data: unknown }; message?: string };
    if (axiosErr.response) {
      console.error('AI Service Error (Recommend):', axiosErr.response.status, axiosErr.response.data);
    } else {
      console.error('Failed to connect to AI Service (Recommend):', axiosErr.message);
    }
    throw new Error('AI Engine unreachable');
  }
};

export const rerankMovies = async (
  userQuery: string,
  movies: Record<string, unknown>[],
): Promise<AIRerankResponse> => {
  try {
    const response = await axios.post<AIRerankResponse>(
      `${AI_SERVICE_URL}/rerank`,
      { query: userQuery, movies },
    );
    return response.data;
  } catch (error: unknown) {
    const axiosErr = error as { response?: { status: number; data: unknown }; message?: string };
    if (axiosErr.response) {
      console.error('AI Service Error (Rerank):', axiosErr.response.status, axiosErr.response.data);
    } else {
      console.error('Failed to connect to AI Service (Rerank):', axiosErr.message);
    }
    throw new Error('AI Rerank Engine unreachable');
  }
};
