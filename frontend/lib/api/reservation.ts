import api from '@/lib/axios';
import { useAuthStore } from '@/store/auth-store';

export interface Reservation {
    id: number;
    userId: number;
    matchId: number;
    seatIds: number[];
    status: 'PENDING' | 'PAID' | 'CANCELLED';
}

export const reservationApi = {
    createReservation: async (matchId: string) => {
        const { user } = useAuthStore.getState();
        if (!user) throw new Error('User not authenticated');

        // Note: Backend might require matchId and userId to convert confirmed allocations to a reservation
        const response = await api.post('/reservations', {
            matchId,
            userId: user.id
        });
        return response.data;
    },

    getReservations: async () => {
        const { user } = useAuthStore.getState();
        if (!user) throw new Error('User not authenticated');

        const response = await api.get('/reservations', {
            params: { userId: user.id }
        });
        return response.data.data; // Extract from SuccessResponse wrapper
    },

    getReservationById: async (reservationId: string) => {
        const response = await api.get(`/reservations/${reservationId}`);
        return response.data;
    }
};
