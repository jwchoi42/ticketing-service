import api from '@/lib/axios';

export interface Match {
    id: number;
    homeTeam: string;
    awayTeam: string;
    dateTime: string; // "yyyy-MM-dd HH:mm:ss"
    stadium: string;
    status: 'DRAFT' | 'OPEN';
}

export const matchApi = {
    getMatches: async (): Promise<Match[]> => {
        const response = await api.get('/matches');
        return response.data.data.matches;
    },

    getMatch: async (matchId: string): Promise<Match> => {
        const response = await api.get(`/matches/${matchId}`);
        return response.data.data;
    }
};
