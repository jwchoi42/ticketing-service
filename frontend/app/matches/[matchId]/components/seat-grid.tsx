'use client';

import { memo } from 'react';
import { Seat } from '@/lib/api/site';
import { SeatButton } from './seat-button';
import { Check } from 'lucide-react';

interface SeatGridProps {
    seats: Seat[] | undefined;
    myHeldSeatIds: Set<number>;
    onSeatClick: (seat: Seat) => void;
}

export const SeatGrid = memo(function SeatGrid({ seats, myHeldSeatIds, onSeatClick }: SeatGridProps) {
    return (
        <div className="w-full overflow-x-auto pb-12 flex flex-col items-center">
            {/* Field Direction Cue */}
            <div className="w-full min-w-[300px] h-8 bg-slate-200/50 rounded-lg mb-8 flex items-center justify-center text-[10px] text-slate-500 font-bold tracking-[0.2em] uppercase border border-slate-300 border-dashed">
                FIELD DIRECTION
            </div>

            {/* Seat Grid with content-visibility for performance (rendering-content-visibility) */}
            <div
                className="grid gap-2 mb-10"
                style={{
                    display: 'grid',
                    gridTemplateColumns: `repeat(10, minmax(0, 1fr))`,
                    width: 'fit-content',
                    contentVisibility: 'auto',
                    containIntrinsicSize: '0 500px',
                }}
            >
                {seats?.map((seat) => (
                    <SeatButton
                        key={seat.id}
                        seat={seat}
                        isMyHold={myHeldSeatIds.has(seat.id)}
                        onSeatClick={onSeatClick}
                    />
                ))}
            </div>

            {/* Legend (2x2 Grid) */}
            <div className="bg-card border border-border/60 rounded-xl p-4 w-full max-w-sm shadow-sm">
                <h3 className="text-[10px] font-bold text-muted-foreground uppercase tracking-widest text-center mb-3">Seat Legend</h3>
                <div className="grid grid-cols-2 gap-4">
                    <div className="flex items-center gap-3">
                        <div className="h-6 w-6 rounded bg-white border border-slate-200" />
                        <span className="text-xs font-medium text-muted-foreground">Available</span>
                    </div>
                    <div className="flex items-center gap-3">
                        <div className="h-6 w-6 rounded bg-primary flex items-center justify-center">
                            <Check className="h-3 w-3 text-white" />
                        </div>
                        <span className="text-xs font-medium text-muted-foreground">My Held</span>
                    </div>
                    <div className="flex items-center gap-3">
                        <div className="h-6 w-6 rounded bg-white border border-slate-200 [background-image:linear-gradient(135deg,transparent_25%,oklch(0.693_0.166_235.5_/_0.2)_25%,oklch(0.693_0.166_235.5_/_0.2)_50%,transparent_50%,transparent_75%,oklch(0.693_0.166_235.5_/_0.2)_75%,oklch(0.693_0.166_235.5_/_0.2))] [background-size:4px_4px]" />
                        <span className="text-xs font-medium text-muted-foreground">Held (Others)</span>
                    </div>
                    <div className="flex items-center gap-3">
                        <div className="h-6 w-6 rounded bg-slate-200 opacity-60" />
                        <span className="text-xs font-medium text-muted-foreground">Occupied</span>
                    </div>
                </div>
            </div>

            {(!seats || seats.length === 0) && (
                <div className="text-center p-8 text-muted-foreground italic text-sm mt-4">
                    Connecting to live feed...
                </div>
            )}
        </div>
    );
});
