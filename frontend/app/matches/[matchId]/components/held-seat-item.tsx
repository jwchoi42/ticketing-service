'use client';

import { memo, useCallback } from 'react';
import { X } from 'lucide-react';

interface HeldSeatItemProps {
    seatId: number;
    blockName: string;
    seatNumber: number | string;
    onRelease: (seatId: number) => void;
}

export const HeldSeatItem = memo(function HeldSeatItem({ seatId, blockName, seatNumber, onRelease }: HeldSeatItemProps) {
    const handleRelease = useCallback(() => {
        onRelease(seatId);
    }, [onRelease, seatId]);

    return (
        <div className="py-4 flex items-center justify-between group">
            <div className="space-y-1">
                <p className="text-sm font-bold text-foreground">일반석</p>
                <p className="text-xs text-muted-foreground/80 font-medium">
                    {blockName} · {seatNumber}번
                </p>
            </div>
            <div className="flex items-center gap-4">
                <span className="text-sm font-bold">170,000원</span>
                <button
                    onClick={handleRelease}
                    className="p-1 -mr-1 text-muted-foreground/40 hover:text-destructive transition-colors"
                >
                    <X className="h-4 w-4" />
                </button>
            </div>
        </div>
    );
});
