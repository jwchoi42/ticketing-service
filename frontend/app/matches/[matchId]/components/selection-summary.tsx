'use client';

import { memo, useMemo } from 'react';
import { Button } from '@/components/ui/button';
import { ChevronUp } from 'lucide-react';
import { cn } from '@/lib/utils';
import { HeldSeatItem } from './held-seat-item';
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
    AlertDialogTrigger,
} from '@/components/ui/alert-dialog';

interface SelectionSummaryProps {
    myHeldSeatIds: Set<number>;
    heldSeatsInfo: Map<number, { blockName: string; seatNumber: number | string }>;
    isSummaryExpanded: boolean;
    onToggleSummary: () => void;
    onExpandSummary: () => void;
    onReleaseSeat: (seatId: number) => void;
    onClearAllSeats: () => void;
    onConfirmSeats: () => void;
}

export const SelectionSummary = memo(function SelectionSummary({
    myHeldSeatIds,
    heldSeatsInfo,
    isSummaryExpanded,
    onToggleSummary,
    onExpandSummary,
    onReleaseSeat,
    onClearAllSeats,
    onConfirmSeats,
}: SelectionSummaryProps) {
    // Memoize held seat ids array for iteration
    const heldSeatIdsArray = useMemo(() => Array.from(myHeldSeatIds), [myHeldSeatIds]);

    return (
        <div className={cn(
            "fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[480px] bg-background border-t border-primary/10 shadow-[0_-8px_30px_rgba(0,0,0,0.12)] z-50 transition-all duration-300 ease-in-out rounded-t-3xl",
            isSummaryExpanded ? "h-auto max-h-[80vh]" : "h-auto"
        )}>
            {/* Expand Handle */}
            <button
                onClick={onToggleSummary}
                className="w-full pt-3 pb-1 flex justify-center items-center group active:opacity-70 transition-opacity"
                disabled={myHeldSeatIds.size === 0}
            >
                <div className="w-10 h-1 bg-muted rounded-full group-hover:bg-muted-foreground/30 transition-colors" />
            </button>

            {/* Expanded Content */}
            {isSummaryExpanded && myHeldSeatIds.size > 0 && (
                <div className="px-6 py-2 overflow-y-auto max-h-[calc(80vh-140px)] bg-background">
                    <div className="flex items-center justify-between mb-6">
                        <h4 className="text-lg font-bold">
                            선택 좌석 <span className="text-primary ml-1">{myHeldSeatIds.size}</span>
                        </h4>
                        <button
                            onClick={onClearAllSeats}
                            className="text-xs font-medium text-muted-foreground/60 hover:text-foreground transition-colors"
                        >
                            전체삭제
                        </button>
                    </div>

                    <div className="space-y-0 divide-y divide-border/40">
                        {heldSeatIdsArray.map((id) => {
                            const info = heldSeatsInfo.get(id);
                            return (
                                <HeldSeatItem
                                    key={id}
                                    seatId={id}
                                    blockName={info?.blockName || 'Unknown'}
                                    seatNumber={info?.seatNumber || '?'}
                                    onRelease={onReleaseSeat}
                                />
                            );
                        })}
                    </div>
                </div>
            )}

            {/* Bottom Action Area */}
            <div className="p-6 pt-2 bg-background">
                {!isSummaryExpanded && myHeldSeatIds.size > 0 && (
                    <div
                        className="flex items-center justify-center gap-1 mb-4 text-xs font-bold text-muted-foreground/60 cursor-pointer"
                        onClick={onExpandSummary}
                    >
                        {myHeldSeatIds.size}개 좌석 선택됨 <ChevronUp className="h-3 w-3" />
                    </div>
                )}

                <AlertDialog>
                    <AlertDialogTrigger asChild>
                        <Button
                            className={cn(
                                "w-full h-14 text-base font-bold rounded-2xl transition-all duration-300",
                                myHeldSeatIds.size > 0
                                    ? "bg-primary text-primary-foreground shadow-lg shadow-primary/20 hover:bg-primary/90"
                                    : "bg-muted text-muted-foreground cursor-not-allowed opacity-50"
                            )}
                            disabled={myHeldSeatIds.size === 0}
                        >
                            {myHeldSeatIds.size > 0 ? '선택 완료' : '좌석을 선택해주세요'}
                        </Button>
                    </AlertDialogTrigger>
                    <AlertDialogContent className="max-w-[340px] rounded-3xl">
                        <AlertDialogHeader>
                            <AlertDialogTitle className="text-center text-xl">좌석을 확정하시겠습니까?</AlertDialogTitle>
                            <AlertDialogDescription className="text-center">
                                선택하신 {myHeldSeatIds.size}개의 좌석을 <br />
                                확정하시려면 확인 버튼을 눌러주세요.
                            </AlertDialogDescription>
                        </AlertDialogHeader>
                        <AlertDialogFooter className="flex-row gap-2 sm:flex-row sm:justify-center">
                            <AlertDialogCancel className="flex-1 h-12 rounded-xl mt-0 border-none bg-muted hover:bg-muted/80">취소</AlertDialogCancel>
                            <AlertDialogAction
                                className="flex-1 h-12 rounded-xl bg-primary hover:bg-primary/90"
                                onClick={onConfirmSeats}
                            >
                                확인
                            </AlertDialogAction>
                        </AlertDialogFooter>
                    </AlertDialogContent>
                </AlertDialog>
            </div>
        </div>
    );
});
