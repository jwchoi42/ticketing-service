'use client';

import { useState, useEffect, useRef, useCallback, useMemo } from 'react';
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
    seatMap: Map<number, Seat>;
    status: ConnectionStatus;
    error: Event | null;
}

const MAX_RETRIES = 3;

export function useSSE({ matchId, blockId, enabled }: UseSSEProps): UseSSEReturn {
    // Use Map for O(1) lookups instead of array
    const [seatMap, setSeatMap] = useState<Map<number, Seat>>(new Map());
    const [status, setStatus] = useState<ConnectionStatus>('disconnected');
    const [error, setError] = useState<Event | null>(null);
    const eventSourceRef = useRef<EventSource | null>(null);
    const retryCountRef = useRef(0);
    const connectRef = useRef<() => void>(() => { });

    const connect = useCallback(() => {
        if (!enabled || !blockId) return;

        // Close existing connection if any
        if (eventSourceRef.current) {
            eventSourceRef.current.close();
        }

        // Reset state before starting new connection
        setSeatMap(new Map());
        setStatus('reconnecting');

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

        // Event: snapshot (Initial Load) - convert array to Map
        es.addEventListener('snapshot', (event) => {
            try {
                const payload = JSON.parse(event.data);
                const newSeats: Seat[] = payload.data?.seats || [];
                const newMap = new Map<number, Seat>();
                for (const seat of newSeats) {
                    newMap.set(seat.id, seat);
                }
                setSeatMap(newMap);
            } catch (err) {
                console.error('Failed to parse snapshot:', err);
            }
        });

        // Event: changes (Updates) - O(1) updates with Map
        es.addEventListener('changes', (event) => {
            try {
                const payload = JSON.parse(event.data);
                const changes: Array<{ seatId: number; status: string }> = payload.data?.changes || [];

                setSeatMap((prev) => {
                    const next = new Map(prev);
                    for (const change of changes) {
                        const existing = next.get(change.seatId);
                        if (existing) {
                            next.set(change.seatId, { ...existing, status: change.status as Seat['status'] });
                        }
                    }
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

    // SSE connection effect - setState calls are intentional for connection state management
    /* eslint-disable react-hooks/set-state-in-effect */
    useEffect(() => {
        if (enabled && blockId) {
            connect();
        } else {
            // Reset state when disabled
            setSeatMap(new Map());
            setStatus('disconnected');
        }

        return () => {
            if (eventSourceRef.current) {
                console.log('Closing SSE connection');
                eventSourceRef.current.close();
            }
        };
    }, [connect, enabled, blockId]);
    /* eslint-enable react-hooks/set-state-in-effect */

    // Derive seats array from Map for backward compatibility, memoized
    const seats = useMemo(() => Array.from(seatMap.values()), [seatMap]);

    return { seats, seatMap, status, error };
}
