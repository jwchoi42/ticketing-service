import axiosInstance from './axiosInstance';
import type { Area, Section, Block, Seat } from '../types';

export const getAreas = (): Promise<Area[]> => {
    return axiosInstance.get(`/site/areas`).then((res: any) => res.areas || []);
};

export const getSections = (areaId: number): Promise<Section[]> => {
    return axiosInstance.get(`/site/areas/${areaId}/sections`).then((res: any) => res.sections || []);
};

export const getBlocks = (sectionId: number): Promise<Block[]> => {
    return axiosInstance.get(`/site/sections/${sectionId}/blocks`).then((res: any) => res.blocks || []);
};

export const getSeats = (blockId: number): Promise<Seat[]> => {
    return axiosInstance.get(`/site/blocks/${blockId}/seats`).then((res: any) => res.seats || []);
};

export const getSeatStatuses = (matchId: number, blockId: number): Promise<Seat[]> => {
    return axiosInstance.get(`/matches/${matchId}/blocks/${blockId}/seats`).then((res: any) => res.seats || []);
};
