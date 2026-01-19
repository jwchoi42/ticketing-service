import api from '@/lib/axios';

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
    name: string; // e.g., "101블록"
}

export interface Seat {
    id: number;
    rowNumber: number;
    seatNumber: number;
    status?: string; // "AVAILABLE" | "HOLD" | "OCCUPIED" 
}

export const siteApi = {
    getAreas: async (): Promise<Area[]> => {
        const response = await api.get('/site/areas');
        return response.data.data.areas;
    },

    getSections: async (areaId: number): Promise<Section[]> => {
        const response = await api.get(`/site/areas/${areaId}/sections`);
        return response.data.data.sections;
    },

    getBlocks: async (sectionId: number): Promise<Block[]> => {
        const response = await api.get(`/site/sections/${sectionId}/blocks`);
        return response.data.data.blocks;
    },

    getSeats: async (blockId: number): Promise<Seat[]> => {
        const response = await api.get(`/site/blocks/${blockId}/seats`);
        return response.data.data.seats;
    },
};
