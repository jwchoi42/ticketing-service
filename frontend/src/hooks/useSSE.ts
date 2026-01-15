import { useEffect, useState, useRef, useCallback } from 'react';
import type { Seat, SeatStatusChange } from '../types';

type ConnectionStatus = 'connected' | 'reconnecting' | 'disconnected' | 'error';

interface SSEOptions {
    onSnapshot: (seats: Seat[]) => void;
    onChanges: (changes: SeatStatusChange[]) => void;
    onError?: (error: any) => void;
}

export const useSSE = (matchId: number | null, blockId: number | null, options: SSEOptions) => {
    const [status, setStatus] = useState<ConnectionStatus>('disconnected');
    const [retryCount, setRetryCount] = useState(0);
    const eventSourceRef = useRef<EventSource | null>(null);
    const maxRetries = 3;

    const connect = useCallback(() => {
        if (matchId === null || blockId === null) return;

        if (eventSourceRef.current) {
            eventSourceRef.current.close();
        }

        const url = `${import.meta.env.VITE_API_URL}/matches/${matchId}/blocks/${blockId}/seats/events`;
        const es = new EventSource(url);
        eventSourceRef.current = es;

        setStatus('connected');

        es.addEventListener('snapshot', (event) => {
            try {
                const parsed = JSON.parse(event.data);
                const data = parsed.data || parsed;
                if (data.seats) {
                    options.onSnapshot(data.seats);
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
                    options.onChanges(data.changes);
                }
            } catch (err) {
                console.error('Failed to parse changes:', err);
            }
        });

        es.onerror = (err) => {
            console.error('SSE Error:', err);
            es.close();

            if (retryCount < maxRetries) {
                setStatus('reconnecting');
                setRetryCount(prev => prev + 1);
                setTimeout(connect, 3000); // Retry after 3 seconds
            } else {
                setStatus('error');
                if (options.onError) options.onError(err);
            }
        };

        return es;
    }, [matchId, blockId, retryCount, options]);

    useEffect(() => {
        let es: EventSource | undefined;
        if (matchId !== null && blockId !== null) {
            es = connect();
        }
        return () => {
            if (es) {
                es.close();
                setStatus('disconnected');
            }
        };
    }, [matchId, blockId]);

    return { status, retryCount };
};
