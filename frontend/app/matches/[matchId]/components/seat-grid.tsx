import { memo, useMemo, useRef, useState, useEffect } from 'react';
import { Seat } from '@/lib/api/site';
import { ConnectionStatus } from '@/hooks/use-sse';
import { SeatButton } from './seat-button';
import { Check } from 'lucide-react';
import { cn } from '@/lib/utils';

interface SeatGridProps {
    blockId: number | null;
    seats: Seat[] | undefined;
    myHeldSeatIds: Set<number>;
    onSeatClick: (seat: Seat) => void;
    direction?: 'left' | 'right';
    status: ConnectionStatus;
}

export const SeatGrid = memo(function SeatGrid({ blockId, seats, myHeldSeatIds, onSeatClick, direction = 'right', status }: SeatGridProps) {
    const containerRef = useRef<HTMLDivElement>(null);
    const gridRef = useRef<HTMLDivElement>(null);
    const [scale, setScale] = useState(1);
    const [gridHeight, setGridHeight] = useState<number | 'auto'>('auto');
    const lastKnownHeightRef = useRef<number>(300); // Store last valid height
    const lastKnownDimensionsRef = useRef({ rows: 10, cols: 10 }); // Store last valid dimensions

    // Debounce the connection status to prevent flickering
    const [debouncedStatus, setDebouncedStatus] = useState<ConnectionStatus>('connected');

    useEffect(() => {
        if (status === 'connected') {
            setDebouncedStatus('connected');
            return;
        }
        const timer = setTimeout(() => {
            setDebouncedStatus(status);
        }, 1000);
        return () => clearTimeout(timer);
    }, [status]);

    // Group seats by row
    const { seatsByRow, maxColumns, isSkeleton } = useMemo(() => {
        if (!seats || seats.length === 0) {
            const skeletonMap = new Map<number, { id: string; seatNumber: number; rowNumber: number; status: string }[]>();
            const { rows: skelRows, cols: skelCols } = lastKnownDimensionsRef.current;

            for (let r = 1; r <= skelRows; r++) {
                skeletonMap.set(r, Array.from({ length: skelCols }, (_, i) => ({
                    id: `skel-${r}-${i}`, seatNumber: i + 1, rowNumber: r, status: 'skeleton'
                })));
            }
            return { seatsByRow: skeletonMap, maxColumns: skelCols, isSkeleton: true };
        }

        const grouped = new Map<number, Seat[]>();
        let maxCol = 0;
        let maxRow = 0;

        for (const seat of seats) {
            const row = seat.rowNumber;
            if (row > maxRow) maxRow = row;
            if (!grouped.has(row)) {
                grouped.set(row, []);
            }
            grouped.get(row)!.push(seat);
            if (seat.seatNumber > maxCol) maxCol = seat.seatNumber;
        }

        lastKnownDimensionsRef.current = { rows: maxRow, cols: maxCol };

        for (const [, rowSeats] of grouped) {
            rowSeats.sort((a, b) => a.seatNumber - b.seatNumber);
        }

        return { seatsByRow: grouped, maxColumns: maxCol, isSkeleton: false };
    }, [seats]);

    useEffect(() => {
        const updateScale = () => {
            if (!containerRef.current || !gridRef.current) return;

            const containerWidth = containerRef.current.offsetWidth;
            const containerHeight = containerRef.current.offsetHeight;
            const gridWidth = gridRef.current.scrollWidth;
            const originalGridHeight = gridRef.current.scrollHeight;

            if (gridWidth > 0 && containerWidth > 0 && containerHeight > 0) {
                const VERTICAL_UI_OFFSET = 270;

                const availableHeight = Math.max(200, containerHeight - VERTICAL_UI_OFFSET);
                const widthScale = (containerWidth - 32) / gridWidth;
                const heightScale = availableHeight / originalGridHeight;

                const finalScale = Math.min(widthScale, heightScale, 2.0);

                setScale(finalScale);
                const newHeight = originalGridHeight * finalScale;
                setGridHeight(newHeight);

                if (!isSkeleton) {
                    lastKnownHeightRef.current = newHeight;
                }
            }
        };

        const observer = new ResizeObserver(updateScale);
        if (containerRef.current) observer.observe(containerRef.current);
        if (gridRef.current) observer.observe(gridRef.current);

        updateScale();
        return () => observer.disconnect();
    }, [seatsByRow, isSkeleton]);

    const displayHeight = isSkeleton ? lastKnownHeightRef.current : gridHeight;

    const sortedRows = useMemo(() =>
        Array.from(seatsByRow.keys()).sort((a, b) => a - b),
        [seatsByRow]);

    return (
        <div ref={containerRef} className="w-full flex-1 flex flex-col items-center justify-start pt-4 min-h-0 relative overflow-hidden">
            <style jsx>{`
                @keyframes slideInRight {
                    from { transform: translateX(20px); opacity: 0; }
                    to { transform: translateX(0); opacity: 1; }
                }
                @keyframes slideInLeft {
                    from { transform: translateX(-20px); opacity: 0; }
                    to { transform: translateX(0); opacity: 1; }
                }
                .mobile-slide-right { animation: slideInRight 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards; }
                .mobile-slide-left { animation: slideInLeft 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards; }
            `}</style>

            {/* Connection Status Indicator - Absolute Top Right */}
            {/* Only show if NOT connected (after debounce) */}
            <div className={cn(
                "absolute top-2 right-4 flex items-center gap-1.5 transition-opacity duration-500",
                debouncedStatus === 'connected' ? "opacity-0 pointer-events-none" : "opacity-100"
            )}>
                <div className={cn(
                    "w-2 h-2 rounded-full animate-pulse",
                    debouncedStatus === 'reconnecting' ? "bg-amber-400" : "bg-red-500"
                )} />
                <span className="text-[10px] font-medium text-slate-400 uppercase tracking-wider">
                    {debouncedStatus === 'reconnecting' ? 'CONNECTING...' : 'OFFLINE'}
                </span>
            </div>

            {/* Field Direction Cue - Pinned Top */}
            <div className="w-full max-w-[200px] h-6 bg-slate-100 rounded-full mb-2 flex items-center justify-center text-[9px] text-slate-400 font-bold tracking-[0.2em] uppercase border border-slate-200 flex-none scale-90 z-10">
                FIELD
            </div>

            {/* Scaled Seat Grid Container - Centered */}
            <div
                className="w-full flex justify-center items-center overflow-visible relative flex-1"
                style={{ maxHeight: displayHeight === 'auto' ? 'auto' : `${displayHeight}px`, minHeight: '100px' }}
            >
                <div
                    ref={gridRef}
                    key={blockId} // Animate ONLY the content when block changes
                    className={cn(
                        "w-fit flex flex-col items-center origin-center transition-transform duration-300 ease-out",
                        direction === 'right' ? 'mobile-slide-right' : 'mobile-slide-left'
                    )}
                    style={{ transform: `scale(${scale})` }}
                >
                    {/* Header Row: Column Numbers */}
                    <div className="flex gap-1 mb-1">
                        <div className="w-6 h-5" />
                        {Array.from({ length: maxColumns }, (_, i) => (
                            <div
                                key={i}
                                className="w-7 h-5 flex items-center justify-center text-[10px] font-medium text-slate-400"
                            >
                                {i + 1}
                            </div>
                        ))}
                    </div>

                    {/* Rows with Labels */}
                    {sortedRows.map((rowNumber) => (
                        <div key={rowNumber} className="flex gap-1 mb-1 items-center">
                            <div className="w-6 h-7 flex items-center justify-center text-[10px] font-medium text-slate-300">
                                {rowNumber}
                            </div>
                            {seatsByRow.get(rowNumber)?.map((seat) => (
                                isSkeleton ? (
                                    <div key={seat.id} className="w-7 h-7 p-0.5">
                                        <div className="w-full h-full rounded-full bg-slate-100 animate-pulse" />
                                    </div>
                                ) : (
                                    <SeatButton
                                        key={seat.id}
                                        seat={seat}
                                        isMyHold={myHeldSeatIds.has(seat.id)}
                                        onSeatClick={onSeatClick}
                                    />
                                )
                            ))}
                        </div>
                    ))}
                </div>
            </div>

            {/* Seat Legend - Pinned Bottom (mt-auto) */}
            <div className="w-full max-w-[340px] mt-auto mb-28 px-4 h-fit flex-none z-10 relative mx-auto">
                <div className="bg-white/90 backdrop-blur-md border border-slate-200 rounded-2xl p-3 shadow-sm">
                    <div className="grid grid-cols-2 gap-x-6 gap-y-3">
                        <div className="flex items-center gap-2">
                            <div className="h-4 w-4 rounded-sm bg-white border border-slate-200" />
                            <span className="text-[10px] font-bold text-slate-500">Available</span>
                        </div>
                        <div className="flex items-center gap-2">
                            <div className="h-4 w-4 rounded-sm bg-primary flex items-center justify-center">
                                <Check className="h-2 w-2 text-white" />
                            </div>
                            <span className="text-[10px] font-bold text-slate-500">My Selection</span>
                        </div>
                        <div className="flex items-center gap-2">
                            <div className="h-4 w-4 rounded-sm bg-white border border-slate-200 [background-image:linear-gradient(135deg,transparent_25%,oklch(0.693_0.166_235.5_/_0.2)_25%,oklch(0.693_0.166_235.5_/_0.2)_50%,transparent_50%,transparent_75%,oklch(0.693_0.166_235.5_/_0.2)_75%,oklch(0.693_0.166_235.5_/_0.2))] [background-size:4px_4px]" />
                            <span className="text-[10px] font-bold text-slate-500">Reserved</span>
                        </div>
                        <div className="flex items-center gap-2">
                            <div className="h-4 w-4 rounded-sm bg-slate-100/60 opacity-60" />
                            <span className="text-[10px] font-bold text-slate-500">Sold Out</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
});
