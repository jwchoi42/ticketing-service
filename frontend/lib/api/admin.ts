import api from '@/lib/axios';


export interface CreateMatchRequest {
    dateTime: string; // ISO 8601
    homeTeam: string;
    awayTeam: string;
    stadium: string;
}

export interface UpdateMatchRequest {
    dateTime?: string;
    homeTeam?: string;
    awayTeam?: string;
    stadium?: string;
}

import { useAuthStore } from '@/store/auth-store';

export const adminApi = {
    createMatch: async (data: CreateMatchRequest): Promise<void> => {
        const { user } = useAuthStore.getState();
        await api.post('/admin/matches', data, {
            params: { userId: user?.id }
        });
    },

    updateMatch: async (matchId: number, data: UpdateMatchRequest): Promise<void> => {
        const { user } = useAuthStore.getState();
        await api.put(`/admin/matches/${matchId}`, data, {
            params: { userId: user?.id }
        });
    },

    deleteMatch: async (matchId: number): Promise<void> => {
        const { user } = useAuthStore.getState();
        await api.delete(`/admin/matches/${matchId}`, {
            params: { userId: user?.id }
        });
    },

    openMatch: async (matchId: number): Promise<void> => {
        const { user } = useAuthStore.getState();
        await api.post(`/admin/matches/${matchId}/open`, {}, {
            params: { userId: user?.id }
        });
    }
};
