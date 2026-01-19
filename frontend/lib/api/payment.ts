import api from '@/lib/axios';
import { useAuthStore } from '@/store/auth-store';

export interface Payment {
    id: string;
    reservationId: string;
    amount: number;
    status: 'PENDING' | 'SUCCESS' | 'FAILED';
    transactionId?: string;
}

export const paymentApi = {
    requestPayment: async (reservationId: string) => {
        const { user } = useAuthStore.getState();
        if (!user) throw new Error('User not authenticated');

        const response = await api.post('/payments/request', {
            reservationId,
            userId: user.id
        });
        return response.data;
    },

    confirmPayment: async (paymentId: string) => {
        const response = await api.post('/payments/confirm', {
            paymentId
        });
        return response.data;
    }
};
