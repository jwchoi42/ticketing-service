import axiosInstance from './axiosInstance';
import type { Match } from '../types';

interface MatchesResponse { matches: Match[] }

export const getMatches = (): Promise<Match[]> => {
    return axiosInstance.get<MatchesResponse>('/matches').then(res => res.data.matches || []);
};
