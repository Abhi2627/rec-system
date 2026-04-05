// src/services/movieService.ts
import axios from 'axios';

// 1. Define the 'Shape' of the data (Type Safety)
export interface Movie {
    id: number;
    title: string;
    overview: string;
    poster_path: string;
    release_date: string;
    vote_average: number;
}

const TMDB_BASE_URL = 'https://api.themoviedb.org/3';

export const getTrendingMovies = async (): Promise<Movie[]> => {
    const token = process.env.TMDB_TOKEN;

    if (!token) {
        throw new Error("TMDB_TOKEN is missing in .env file");
    }

    try {
        const response = await axios.get(`${TMDB_BASE_URL}/trending/movie/week`, {
            headers: {
                Authorization: `Bearer ${token}`,
                Accept: 'application/json'
            }
        });

        // Mapping the data ensures we only send what the Mobile App needs
        return response.data.results.map((m: any) => ({
            id: m.id,
            title: m.title,
            overview: m.overview,
            poster_path: `https://image.tmdb.org/t/p/w500${m.poster_path}`,
            release_date: m.release_date,
            vote_average: m.vote_average
        }));
    } catch (error) {
        console.error("Error in MovieService:", error);
        throw error;
    }
};