import axiosInstance from './axiosInstance';

export interface ReservationRequest {
    matchId: number;
    userId: number;
    seatIds: number[];
}

export interface Reservation {
    id: number;
    userId: number;
    matchId: number;
    status: string;
    seatIds: number[];
}

export interface Payment {
    id: number;
    reservationId: number;
    amount: number;
    method: string;
    status: string;
}

export const createReservation = (request: ReservationRequest): Promise<Reservation> => {
    return axiosInstance.post('/reservations', request);
};

export const requestPayment = (reservationId: number, amount: number): Promise<Payment> => {
    return axiosInstance.post('/payments/request', {
        reservationId,
        amount,
        method: 'CARD'
    });
};

export const confirmPayment = (paymentId: number, amount: number): Promise<Payment> => {
    return axiosInstance.post('/payments/confirm', {
        paymentId,
        paymentKey: `test_key_${Date.now()}`,
        orderId: `order_${Date.now()}`,
        amount
    });
};
