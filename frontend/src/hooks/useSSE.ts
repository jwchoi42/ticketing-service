import { useEffect, useState, useRef, useCallback } from 'react';
import type { Seat, SeatStatusChange } from '../types';

export type ConnectionStatus = 'connecting' | 'connected' | 'reconnecting' | 'disconnected' | 'error';

interface SSEOptions {
    onSnapshot: (seats: Seat[]) => void;
    onChanges: (changes: SeatStatusChange[]) => void;
    onError?: (error: unknown) => void;
}

export const useSSE = (matchId: number | null, blockId: number | null, options: SSEOptions) => {
    const [status, setStatus] = useState<ConnectionStatus>('disconnected');
    const [retryCount, setRetryCount] = useState(0);
    const eventSourceRef = useRef<EventSource | null>(null);
    const optionsRef = useRef(options);
    const maxRetries = 5;

    // Keep options updated without triggering reconnects
    useEffect(() => {
        optionsRef.current = options;
    }, [options]);

    const connectRef = useRef<() => void>(() => { });

    const connect = useCallback(() => {
        if (matchId === null || blockId === null) {
            setStatus('disconnected');
            return;
        }

        if (eventSourceRef.current) {
            eventSourceRef.current.close();
        }

        const url = `${import.meta.env.VITE_API_URL}/matches/${matchId}/blocks/${blockId}/seats/events`;
        const es = new EventSource(url);
        eventSourceRef.current = es;

        // If we are already error/disconnected, we are now 'connecting'
        // If we were 'reconnecting', keep it 'reconnecting'
        setStatus(prev => prev === 'reconnecting' ? 'reconnecting' : 'connecting');

        es.addEventListener('snapshot', (event) => {
            try {
                const parsed = JSON.parse(event.data);
                const data = parsed.data || parsed;
                if (data.seats) {
                    optionsRef.current.onSnapshot(data.seats);
                    setStatus('connected');
                    setRetryCount(0);
                }
            } catch (err) {
                console.error('Failed to parse snapshot:', err);
            }
        });

        es.addEventListener('changes', (event) => {
            try {
                const parsed = JSON.parse(event.data);
                const data = parsed.data || parsed;
                if (data.changes) {
                    optionsRef.current.onChanges(data.changes);
                }
            } catch (err) {
                console.error('Failed to parse changes:', err);
            }
        });

        es.onerror = (err) => {
            console.error('SSE Error:', err);
            es.close();

            setRetryCount(prev => {
                const next = prev + 1;
                if (next < maxRetries) {
                    setStatus('reconnecting');
                    setTimeout(() => {
                        connectRef.current();
                    }, 3000);
                } else {
                    setStatus('error');
                    if (optionsRef.current.onError) optionsRef.current.onError(err);
                }
                return next;
            });
        };

        return es;
    }, [matchId, blockId]);

    // Keep connectRef updated
    useEffect(() => {
        connectRef.current = connect;
    }, [connect]);

    useEffect(() => {
        // eslint-disable-next-line react-hooks/set-state-in-effect
        setRetryCount(0);

        if (eventSourceRef.current) {
            eventSourceRef.current.close();
            eventSourceRef.current = null;
        }

        if (matchId !== null && blockId !== null) {
            connect();
        } else {
            setStatus('disconnected');
        }

        return () => {
            if (eventSourceRef.current) {
                eventSourceRef.current.close();
                eventSourceRef.current = null;
            }
        };
    }, [matchId, blockId, connect]);

    return { status, retryCount, maxRetries };
};
