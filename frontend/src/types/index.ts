export interface BaseResponse<T> {
    status: number;
    data: T;
}

export interface User {
    id: number;
    email: string;
}

export interface Match {
    id: number;
    homeTeam: string;
    awayTeam: string;
    dateTime: string;
    stadium: string;
}

export interface Area {
    id: number;
    name: string;
}

export interface Section {
    id: number;
    name: string;
}

export interface Block {
    id: number;
    name: string;
}

export interface Seat {
    id: number;
    seatNumber: string;
    status: SeatStatus;
}

export type SeatStatus = 'AVAILABLE' | 'HOLD' | 'OCCUPIED';

export interface SeatStatusChange {
    seatId: number;
    status: SeatStatus;
}
