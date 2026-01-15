import axiosInstance from './axiosInstance';
import type { User } from '../types';

export const signUp = (email: string, password: string): Promise<User> => {
    return axiosInstance.post('/users/sign-up', { email, password });
};

export const logIn = (email: string, password: string): Promise<User> => {
    return axiosInstance.post('/users/log-in', { email, password });
};
