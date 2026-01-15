import axiosInstance from './axiosInstance';
import type { Match } from '../types';

export const getMatches = (): Promise<Match[]> => {
    return axiosInstance.get('/matches');
};
