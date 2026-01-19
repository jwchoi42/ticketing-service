import axios from 'axios';
import { useAuthStore } from '@/store/auth-store';

const api = axios.create({
    baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api',
    headers: {
        'Content-Type': 'application/json',
    },
});

// Request interceptor to add userId if available
api.interceptors.request.use(
    (config) => {
        // We need to access the store state outside of a component
        // Zustand's getState() allows this
        const { user } = useAuthStore.getState();
        if (user?.id) {
            // In this simplified auth scheme, we might pass userId in headers or body
            // For now, let's assume we might want a custom header, or just rely on body payloads as per spec
            // However, spec says "Exclude JWT", "Use userId in body".
            // But some generic header for tracking might be useful, or we just leave this empty for now
            // and let individual requests handle the body.
            // Let's add a custom header just in case debugging helps
            config.headers['X-User-Id'] = user.id;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// Response interceptor for global error handling
api.interceptors.response.use(
    (response) => response,
    (error) => {
        // Handle global errors like 401/403 or server errors here
        console.error('API Error:', error);
        return Promise.reject(error);
    }
);

export default api;
