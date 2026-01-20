'use client';

import { memo, useCallback } from 'react';
import { Seat } from '@/lib/api/site';
import { Button } from '@/components/ui/button';
import { Check } from 'lucide-react';
import { cn } from '@/lib/utils';

interface SeatButtonProps {
    seat: Seat;
    isMyHold: boolean;
    onSeatClick: (seat: Seat) => void;
}

export const SeatButton = memo(function SeatButton({ seat, isMyHold, onSeatClick }: SeatButtonProps) {
    const isOtherHold = seat.status === 'HOLD' && !isMyHold;

    const handleClick = useCallback(() => {
        onSeatClick(seat);
    }, [onSeatClick, seat]);

    return (
        <Button
            variant="outline"
            className={cn(
                "h-9 w-9 p-0 text-xs rounded-md shadow-sm transition-all active:scale-90 touch-manipulation font-bold border-transparent",
                seat.status === 'AVAILABLE' && "bg-white border-slate-200 text-slate-700 hover:border-primary hover:text-primary",
                isMyHold && "bg-primary text-primary-foreground border-primary shadow-md z-10 hover:bg-primary/90 hover:text-primary-foreground",
                isOtherHold && "bg-white text-primary/40 border-slate-200 [background-image:linear-gradient(135deg,transparent_25%,oklch(0.693_0.166_235.5_/_0.2)_25%,oklch(0.693_0.166_235.5_/_0.2)_50%,transparent_50%,transparent_75%,oklch(0.693_0.166_235.5_/_0.2)_75%,oklch(0.693_0.166_235.5_/_0.2))] [background-size:6px_6px] cursor-not-allowed",
                seat.status === 'OCCUPIED' && "bg-slate-200 border-slate-200 text-slate-400 cursor-not-allowed opacity-60"
            )}
            onClick={handleClick}
            disabled={seat.status === 'OCCUPIED' || isOtherHold}
        >
            {isMyHold ? <Check className="h-4 w-4 stroke-[3]" /> : seat.seatNumber}
        </Button>
    );
});
