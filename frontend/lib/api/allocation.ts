import api from '@/lib/axios';
import { useAuthStore } from '@/store/auth-store';

export const allocationApi = {
    holdSeat: async (matchId: string, seatId: number) => {
        const { user } = useAuthStore.getState();
        if (!user) throw new Error('User not authenticated');

        // POST /api/matches/{matchId}/allocation/seats/{seatId}/hold
        await api.post(`/matches/${matchId}/allocation/seats/${seatId}/hold`, {
            userId: user.id
        });
    },

    releaseSeat: async (matchId: string, seatId: number) => {
        const { user } = useAuthStore.getState();
        if (!user) throw new Error('User not authenticated');

        // POST /api/matches/{matchId}/allocation/seats/{seatId}/release
        await api.post(`/matches/${matchId}/allocation/seats/${seatId}/release`, {
            userId: user.id
        });
    },

    confirmSeats: async (matchId: string, seatIds: number[]) => {
        const { user } = useAuthStore.getState();
        if (!user) throw new Error('User not authenticated');

        // POST /api/matches/{matchId}/allocation/seats/confirm
        const response = await api.post(`/matches/${matchId}/allocation/seats/confirm`, {
            userId: user.id,
            seatIds
        });
        return response.data;
    },
};
