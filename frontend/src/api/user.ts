import axiosInstance from './axiosInstance';
import type { User } from '../types';

export const signUp = (email: string): Promise<User> => {
    return axiosInstance.post('/users/sign-up', { email, password: 'password' }); // Fixed password as per simplified requirement
};

export const logIn = (email: string): Promise<User> => {
    return axiosInstance.post('/users/log-in', { email, password: 'password' });
};
