'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { Seat } from '@/lib/api/site';
import { toast } from 'sonner';

export type ConnectionStatus = 'connected' | 'disconnected' | 'reconnecting';

interface UseSSEProps {
    matchId: string;
    blockId: number | null;
    enabled: boolean;
}

interface UseSSEReturn {
    seats: Seat[];
    status: ConnectionStatus;
    error: Event | null;
}

export function useSSE({ matchId, blockId, enabled }: UseSSEProps): UseSSEReturn {
    const [seats, setSeats] = useState<Seat[]>([]);
    const [status, setStatus] = useState<ConnectionStatus>('disconnected');
    const [error, setError] = useState<Event | null>(null);
    const eventSourceRef = useRef<EventSource | null>(null);
    const retryCountRef = useRef(0);
    const MAX_RETRIES = 3;

    const connectRef = useRef<() => void>(() => { });

    // Handle state reset during render when dependencies change
    const [prevInfo, setPrevInfo] = useState({ blockId, enabled });
    if (prevInfo.blockId !== blockId || prevInfo.enabled !== enabled) {
        setPrevInfo({ blockId, enabled });
        if (!enabled || !blockId) {
            if (seats.length > 0) setSeats([]);
            if (status !== 'disconnected') setStatus('disconnected');
        }
    }

    const connect = useCallback(() => {
        if (!enabled || !blockId) return;

        // Close existing connection if any
        if (eventSourceRef.current) {
            eventSourceRef.current.close();
        }

        const url = `${process.env.NEXT_PUBLIC_API_URL}/matches/${matchId}/blocks/${blockId}/seats/events`;
        console.log('Connecting to SSE:', url);

        const es = new EventSource(url);
        eventSourceRef.current = es;

        es.onopen = () => {
            console.log('SSE Connected');
            setStatus('connected');
            setError(null);
            retryCountRef.current = 0;
        };

        es.onerror = (e) => {
            console.error('SSE Error:', e);
            setError(e);
            es.close();

            if (retryCountRef.current < MAX_RETRIES) {
                setStatus('reconnecting');
                retryCountRef.current++;
                toast.error(`Connection lost. Reconnecting... (${retryCountRef.current}/${MAX_RETRIES})`);
                setTimeout(() => connectRef.current(), 1000 * retryCountRef.current);
            } else {
                setStatus('disconnected');
                toast.error('Failed to connect to real-time server. Please refresh.');
            }
        };

        // Event: snapshot (Initial Load)
        es.addEventListener('snapshot', (event) => {
            try {
                const payload = JSON.parse(event.data);
                const newSeats = payload.data?.seats || [];
                setSeats(newSeats);
            } catch (err) {
                console.error('Failed to parse snapshot:', err);
            }
        });

        // Event: changes (Updates)
        es.addEventListener('changes', (event) => {
            try {
                const payload = JSON.parse(event.data);
                const changes = payload.data?.changes || [];

                setSeats((prev) => {
                    const next = [...prev];
                    changes.forEach((change: { seatId: number; status: string }) => {
                        const idx = next.findIndex(s => s.id === change.seatId);
                        if (idx !== -1) {
                            next[idx] = { ...next[idx], status: change.status as Seat['status'] };
                        }
                    });
                    return next;
                });
            } catch (err) {
                console.error('Failed to parse changes:', err);
            }
        });

    }, [matchId, blockId, enabled]);

    useEffect(() => {
        connectRef.current = connect;
    }, [connect]);

    useEffect(() => {
        if (enabled && blockId) {
            connect();
        }

        return () => {
            if (eventSourceRef.current) {
                console.log('Closing SSE connection');
                eventSourceRef.current.close();
            }
        };
    }, [connect, enabled, blockId]);

    return { seats, status, error };
}
