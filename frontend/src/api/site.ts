import axiosInstance from './axiosInstance';
import type { Area, Section, Block, Seat } from '../types';

interface AreasResponse { areas: Area[] }
interface SectionsResponse { sections: Section[] }
interface BlocksResponse { blocks: Block[] }
interface SeatsResponse { seats: Seat[] }

export const getAreas = (): Promise<Area[]> => {
    return axiosInstance.get<AreasResponse>(`/site/areas`).then((res) => res.data.areas || []);
};

export const getSections = (areaId: number): Promise<Section[]> => {
    return axiosInstance.get<SectionsResponse>(`/site/areas/${areaId}/sections`).then((res) => res.data.sections || []);
};

export const getBlocks = (sectionId: number): Promise<Block[]> => {
    return axiosInstance.get<BlocksResponse>(`/site/sections/${sectionId}/blocks`).then((res) => res.data.blocks || []);
};

export const getSeats = (blockId: number): Promise<Seat[]> => {
    return axiosInstance.get<SeatsResponse>(`/site/blocks/${blockId}/seats`).then((res) => res.data.seats || []);
};

export const getSeatStatuses = (matchId: number, blockId: number): Promise<Seat[]> => {
    return axiosInstance.get<SeatsResponse>(`/matches/${matchId}/blocks/${blockId}/seats`).then((res) => res.data.seats || []);
};
