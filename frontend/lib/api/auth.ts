import api from '@/lib/axios';
import { User } from '@/store/auth-store';

export const authApi = {
    signup: async (email: string, password: string): Promise<void> => {
        // Spec: POST /api/users/sign-up
        await api.post('/users/sign-up', { email, password });
    },

    login: async (email: string, password: string): Promise<User> => {
        // Spec: POST /api/users/log-in
        const response = await api.post('/users/log-in', { email, password });
        // Assuming the response.data contains the user object directly or nested in data
        // Common Response Format in spec: { status: 200, data: { ... } }
        // Let's assume the API returns the Common Format.
        // We should extract the user data.
        // If the backend returns the User object inside `data`, we return that.
        return response.data.data;
    },
};
