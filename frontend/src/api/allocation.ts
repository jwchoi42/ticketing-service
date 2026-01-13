import axiosInstance from './axiosInstance';

export const holdSeat = (matchId: number, seatId: number, userId: number): Promise<void> => {
    return axiosInstance.post(`/matches/${matchId}/allocation/seats/${seatId}/hold`, { userId });
};

export const releaseSeat = (matchId: number, seatId: number, userId: number): Promise<void> => {
    return axiosInstance.post(`/matches/${matchId}/allocation/seats/${seatId}/release`, { userId });
};

export const completeAllocation = (matchId: number, userId: number, seatIds: number[]): Promise<void> => {
    return axiosInstance.post(`/matches/${matchId}/allocation/seats/confirm`, { userId, seatIds });
};
