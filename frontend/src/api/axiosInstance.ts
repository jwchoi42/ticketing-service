import axios from 'axios';

const axiosInstance = axios.create({
    baseURL: import.meta.env.VITE_API_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Response interceptor to unwrap the "data" field from the common response structure
axiosInstance.interceptors.response.use(
    (response) => {
        // If the response follows the { status, data } format, return response.data.data
        // but we need to check if it has the data property
        if (response.data && response.data.data !== undefined) {
            return response.data.data;
        }
        return response.data;
    },
    (error) => {
        // Global error handling can be added here
        return Promise.reject(error);
    }
);

export default axiosInstance;
