'use client';

import { useState } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { matchApi } from '@/lib/api/matches';
import { siteApi, Seat } from '@/lib/api/site';
import { allocationApi } from '@/lib/api/allocation';
import { useSSE } from '@/hooks/use-sse';
import { useAuthStore } from '@/store/auth-store';
import { Button } from '@/components/ui/button';
import { ChevronLeft, ChevronRight, Check, ChevronUp, X } from 'lucide-react';
import { cn } from '@/lib/utils';
import { toast } from 'sonner';
import {
    Carousel,
    CarouselContent,
    CarouselItem,
} from "@/components/ui/carousel";
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
} from "@/components/ui/alert-dialog";

export default function SeatSelectionPage() {
    const params = useParams();
    const matchId = params.matchId as string;

    // --- State ---
    const [selectedAreaId, setSelectedAreaId] = useState<number | null>(null);
    const [selectedSectionId, setSelectedSectionId] = useState<number | null>(null);
    const [selectedBlockId, setSelectedBlockId] = useState<number | null>(null);
    const [myHeldSeatIds, setMyHeldSeatIds] = useState<Set<number>>(new Set());
    const [heldSeatsInfo, setHeldSeatsInfo] = useState<Map<number, { blockName: string, seatNumber: number | string }>>(new Map());
    const [isSummaryExpanded, setIsSummaryExpanded] = useState(false);

    // --- Data Fetching ---
    const { data: match } = useQuery({
        queryKey: ['match', matchId],
        queryFn: () => matchApi.getMatch(matchId),
    });

    const { data: areas } = useQuery({
        queryKey: ['areas'],
        queryFn: siteApi.getAreas,
    });

    const { data: sections } = useQuery({
        queryKey: ['sections', selectedAreaId],
        queryFn: () => siteApi.getSections(selectedAreaId!),
        enabled: !!selectedAreaId,
    });

    const { data: blocks } = useQuery({
        queryKey: ['blocks', selectedSectionId],
        queryFn: () => siteApi.getBlocks(selectedSectionId!),
        enabled: !!selectedSectionId,
    });

    const activeBlockId = selectedBlockId ?? (blocks?.[0]?.id || null);
    const currentBlock = blocks?.find(b => b.id === activeBlockId);

    // --- Real-time Seat Data ---
    const { seats, status } = useSSE({
        matchId,
        blockId: activeBlockId,
        enabled: !!activeBlockId
    });

    // --- Logic ---
    const handleAreaSelect = (areaId: number) => {
        setSelectedAreaId(areaId);
        setSelectedSectionId(null);
        setSelectedBlockId(null);
    };

    const handleSectionSelect = (sectionId: number) => {
        setSelectedSectionId(sectionId);
        setSelectedBlockId(null);
    };

    const handlePrevBlock = () => {
        if (!blocks || !activeBlockId) return;
        const currentIndex = blocks.findIndex(b => b.id === activeBlockId);
        if (currentIndex > 0) {
            setSelectedBlockId(blocks[currentIndex - 1].id);
        }
    };

    const handleNextBlock = () => {
        if (!blocks || !activeBlockId) return;
        const currentIndex = blocks.findIndex(b => b.id === activeBlockId);
        if (currentIndex < blocks.length - 1) {
            setSelectedBlockId(blocks[currentIndex + 1].id);
        }
    };


    const { isAuthenticated } = useAuthStore();
    const router = useRouter();

    // --- Seat Click Handling ---
    const handleSeatClick = async (seat: Seat) => {
        if (!isAuthenticated) {
            toast.error('로그인이 필요합니다.');
            router.push('/log-in');
            return;
        }
        if (seat.status === 'OCCUPIED') return;

        // If seat is held by someone else, we can't click it
        if (seat.status === 'HOLD' && !myHeldSeatIds.has(seat.id)) return;

        try {
            if (seat.status === 'HOLD') {
                // Release
                await allocationApi.releaseSeat(matchId, seat.id);
                setMyHeldSeatIds(prev => {
                    const next = new Set(prev);
                    next.delete(seat.id);
                    return next;
                });
                setHeldSeatsInfo(prev => {
                    const next = new Map(prev);
                    next.delete(seat.id);
                    return next;
                });
            } else {
                // Hold
                await allocationApi.holdSeat(matchId, seat.id);
                setMyHeldSeatIds(prev => new Set(prev).add(seat.id));
                setHeldSeatsInfo(prev => {
                    const next = new Map(prev);
                    next.set(seat.id, {
                        blockName: currentBlock?.name || 'Unknown',
                        seatNumber: seat.seatNumber
                    });
                    return next;
                });
            }
        } catch (err) {
            console.error(err);
            toast.error('Failed to update seat status');
        }
    };

    const handleConfirmSeats = async () => {
        try {
            await allocationApi.confirmSeats(matchId, Array.from(myHeldSeatIds));
            toast.success('좌석이 확정되었습니다.');
            router.push('/reservation');
        } catch (err: unknown) {
            console.error('Confirmation Failed:', err);
            const message = err instanceof Error ? err.message : '좌석 확정에 실패했습니다.';
            toast.error(message);
        }
    };

    if (!match) return <div className="p-8">Loading...</div>;

    return (
        <div className="flex flex-col min-h-screen bg-background text-foreground relative">
            {/* 1. Header with Step Indicator & Back Button */}
            <div className="bg-background border-b border-border/50 sticky top-0 z-40">
                <div className="px-4 h-14 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <button
                            onClick={() => router.push('/matches')}
                            className="p-1 -ml-1 text-muted-foreground hover:text-foreground transition-colors"
                        >
                            <ChevronLeft className="h-6 w-6" />
                        </button>
                        <div>
                            <h2 className="text-sm font-bold">좌석 선택</h2>
                            <p className="text-[10px] text-muted-foreground font-medium uppercase tracking-tight">Step 1 of 3</p>
                        </div>
                    </div>

                    <div className="text-right">
                        <h1 className="text-xs font-bold leading-tight truncate max-w-[150px]">{match.homeTeam} vs {match.awayTeam}</h1>
                        <p className="text-[10px] text-muted-foreground">{new Date(match.dateTime).toLocaleDateString()}</p>
                    </div>
                </div>
            </div>

            {/* 2. Zone Selection (Carousel) */}
            <header className="sticky top-[56px] z-30 bg-background border-b shadow-sm pt-4">
                {/* Area Selection */}
                <div className="px-4 pb-3">
                    <Carousel opts={{ align: "start", dragFree: true }} className="w-full">
                        <CarouselContent className="-ml-2">
                            {areas?.map(area => (
                                <CarouselItem key={area.id} className="pl-2 basis-auto">
                                    <button
                                        onClick={() => handleAreaSelect(area.id)}
                                        className={cn(
                                            "px-4 py-1.5 rounded-full text-sm font-medium whitespace-nowrap transition-colors",
                                            selectedAreaId === area.id
                                                ? "bg-primary text-primary-foreground shadow-sm"
                                                : "bg-muted text-muted-foreground hover:bg-muted/80"
                                        )}
                                    >
                                        {area.name}
                                    </button>
                                </CarouselItem>
                            ))}
                        </CarouselContent>
                    </Carousel>
                </div>

                {/* Section Selection */}
                {selectedAreaId && (
                    <div className="px-4 pb-3 border-t pt-3 bg-muted/10">
                        <Carousel opts={{ align: "start", dragFree: true }} className="w-full">
                            <CarouselContent className="-ml-2">
                                {sections?.map(section => (
                                    <CarouselItem key={section.id} className="pl-2 basis-auto">
                                        <button
                                            onClick={() => handleSectionSelect(section.id)}
                                            className={cn(
                                                "px-3 py-1 rounded-md text-xs font-semibold whitespace-nowrap transition-all border",
                                                selectedSectionId === section.id
                                                    ? "bg-primary/10 text-primary border-primary shadow-sm"
                                                    : "bg-background border-border text-foreground hover:bg-accent"
                                            )}
                                        >
                                            {section.name}
                                        </button>
                                    </CarouselItem>
                                ))}
                            </CarouselContent>
                        </Carousel>
                        {!sections?.length && <div className="text-xs text-muted-foreground py-1">Loading sections...</div>}
                    </div>
                )}
            </header>

            {/* 3. Block Navigation & Status */}
            {selectedSectionId && (
                <div className="flex items-center justify-between px-4 py-3 border-b bg-card">
                    <Button variant="ghost" size="icon" className="h-10 w-10 text-muted-foreground" onClick={handlePrevBlock} disabled={!blocks || blocks.findIndex(b => b.id === activeBlockId) <= 0}>
                        <ChevronLeft className="h-6 w-6" />
                    </Button>

                    <div className="flex flex-col items-center">
                        <span className="text-base font-bold text-foreground">{currentBlock?.name || 'Select Block'}</span>

                        <div className="flex items-center gap-1.5 mt-0.5">
                            <div className="relative flex h-1.5 w-1.5">
                                {status === 'reconnecting' && (
                                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-primary opacity-75"></span>
                                )}
                                <span className={cn(
                                    "relative inline-flex rounded-full h-1.5 w-1.5",
                                    status === 'connected' && "bg-primary",
                                    status === 'disconnected' && "bg-primary/20",
                                    status === 'reconnecting' && "bg-primary/60"
                                )} />
                            </div>
                            <span className={cn(
                                "text-[10px] font-bold uppercase tracking-wider",
                                status === 'connected' && "text-primary",
                                status === 'disconnected' && "text-primary/30",
                                status === 'reconnecting' && "text-primary/70"
                            )}>
                                {status === 'connected' ? 'Live' : status === 'reconnecting' ? 'Connecting' : 'Offline'}
                            </span>
                        </div>
                    </div>

                    <Button variant="ghost" size="icon" className="h-10 w-10 text-muted-foreground" onClick={handleNextBlock} disabled={!blocks || blocks.findIndex(b => b.id === activeBlockId) >= blocks.length - 1}>
                        <ChevronRight className="h-6 w-6" />
                    </Button>
                </div>
            )}

            {/* 4. Main Content (Seat Map) */}
            <main className="flex-1 p-4 overflow-auto bg-slate-50/50 pb-40">
                {!selectedSectionId ? (
                    <div className="flex flex-col items-center justify-center h-64 text-muted-foreground space-y-2">
                        <p className="font-medium">Select an Area and Section above</p>
                    </div>
                ) : !activeBlockId ? (
                    <div className="flex items-center justify-center h-64 text-muted-foreground">Loading Blocks...</div>
                ) : (
                    <div className="w-full overflow-x-auto pb-12 flex flex-col items-center">
                        {/* Field Direction Cue */}
                        <div className="w-full min-w-[300px] h-8 bg-slate-200/50 rounded-lg mb-8 flex items-center justify-center text-[10px] text-slate-500 font-bold tracking-[0.2em] uppercase border border-slate-300 border-dashed">
                            FIELD DIRECTION
                        </div>

                        {/* Seat Grid */}
                        <div
                            className="grid gap-2 mb-10"
                            style={{
                                display: 'grid',
                                gridTemplateColumns: `repeat(10, minmax(0, 1fr))`,
                                width: 'fit-content'
                            }}
                        >
                            {seats?.map((seat) => {
                                const isMyHold = myHeldSeatIds.has(seat.id);
                                const isOtherHold = seat.status === 'HOLD' && !isMyHold;

                                return (
                                    <Button
                                        key={seat.id}
                                        variant="outline"
                                        className={cn(
                                            "h-9 w-9 p-0 text-xs rounded-md shadow-sm transition-all active:scale-90 touch-manipulation font-bold border-transparent",
                                            // Available
                                            seat.status === 'AVAILABLE' && "bg-white border-slate-200 text-slate-700 hover:border-primary hover:text-primary",
                                            // My Hold (Solid Primary)
                                            isMyHold && "bg-primary text-primary-foreground border-primary shadow-md z-10 hover:bg-primary/90 hover:text-primary-foreground",
                                            // Other Hold (Primary Hatching - Top Right to Bottom Left)
                                            // Using 135deg and primary color with transparency
                                            isOtherHold && "bg-white text-primary/40 border-slate-200 [background-image:linear-gradient(135deg,transparent_25%,rgba(var(--primary-rgb),0.15)_25%,rgba(var(--primary-rgb),0.15)_50%,transparent_50%,transparent_75%,rgba(var(--primary-rgb),0.15)_75%,rgba(var(--primary-rgb),0.15))] [background-size:6px_6px] cursor-not-allowed",
                                            // Actually since Tailwind oklch colors don't easily export RGB without extra config, I'll use a CSS variable or standard primary tone. 
                                            // I'll stick to a CSS-in-JS style using the fact that 'text-primary' color might be accessible or hardcoding a representative primary oklch value in the gradient if needed.
                                            // Let's use currentColor for the stripes if possible or just a hardcoded hex that matches sky-500.
                                            isOtherHold && "bg-white text-primary/40 border-slate-200 [background-image:linear-gradient(135deg,transparent_25%,oklch(0.693_0.166_235.5_/_0.2)_25%,oklch(0.693_0.166_235.5_/_0.2)_50%,transparent_50%,transparent_75%,oklch(0.693_0.166_235.5_/_0.2)_75%,oklch(0.693_0.166_235.5_/_0.2))] [background-size:6px_6px] cursor-not-allowed",
                                            // Occupied (Solid Gray)
                                            seat.status === 'OCCUPIED' && "bg-slate-200 border-slate-200 text-slate-400 cursor-not-allowed opacity-60"
                                        )}
                                        onClick={() => handleSeatClick(seat)}
                                        disabled={seat.status === 'OCCUPIED' || isOtherHold}
                                    >
                                        {isMyHold ? <Check className="h-4 w-4 stroke-[3]" /> : seat.seatNumber}
                                    </Button>
                                );
                            })}
                        </div>

                        {/* 5. Legend (2x2 Grid) */}
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

                        {(!seats || seats.length === 0) && <div className="text-center p-8 text-muted-foreground italic text-sm mt-4">Connecting to live feed...</div>}
                    </div>
                )}
            </main>

            {/* 6. Selection Summary & Confirmation (Always Visible) */}
            <div className={cn(
                "fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[480px] bg-background border-t border-primary/10 shadow-[0_-8px_30px_rgba(0,0,0,0.12)] z-50 transition-all duration-300 ease-in-out rounded-t-3xl",
                isSummaryExpanded ? "h-auto max-h-[80vh]" : "h-auto"
            )}>
                {/* Expand Handle */}
                <button
                    onClick={() => myHeldSeatIds.size > 0 && setIsSummaryExpanded(!isSummaryExpanded)}
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
                                onClick={() => {
                                    // Normally we would release all, but for now let's just toast and clear local
                                    // Real implementation would loop through and release or have a bulk release API
                                    toast.info("Clearing all selections...");
                                    myHeldSeatIds.forEach(id => allocationApi.releaseSeat(matchId, id));
                                    setMyHeldSeatIds(new Set());
                                    setHeldSeatsInfo(new Map());
                                    setIsSummaryExpanded(false);
                                }}
                                className="text-xs font-medium text-muted-foreground/60 hover:text-foreground transition-colors"
                            >
                                전체삭제
                            </button>
                        </div>

                        <div className="space-y-0 divide-y divide-border/40">
                            {Array.from(myHeldSeatIds).map((id) => {
                                const info = heldSeatsInfo.get(id);
                                return (
                                    <div key={id} className="py-4 flex items-center justify-between group">
                                        <div className="space-y-1">
                                            <p className="text-sm font-bold text-foreground">일반석</p> {/* Grade could be dynamic */}
                                            <p className="text-xs text-muted-foreground/80 font-medium">
                                                {info?.blockName} · {info?.seatNumber}번
                                            </p>
                                        </div>
                                        <div className="flex items-center gap-4">
                                            <span className="text-sm font-bold">170,000원</span>
                                            <button
                                                onClick={async () => {
                                                    try {
                                                        await allocationApi.releaseSeat(matchId, id);
                                                        setMyHeldSeatIds(prev => {
                                                            const next = new Set(prev);
                                                            next.delete(id);
                                                            return next;
                                                        });
                                                        setHeldSeatsInfo(prev => {
                                                            const next = new Map(prev);
                                                            next.delete(id);
                                                            return next;
                                                        });
                                                        if (myHeldSeatIds.size <= 1) setIsSummaryExpanded(false);
                                                    } catch {
                                                        toast.error('Failed to release seat');
                                                    }
                                                }}
                                                className="p-1 -mr-1 text-muted-foreground/40 hover:text-destructive transition-colors"
                                            >
                                                <X className="h-4 w-4" />
                                            </button>
                                        </div>
                                    </div>
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
                            onClick={() => setIsSummaryExpanded(true)}
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
                                    onClick={handleConfirmSeats}
                                >
                                    확인
                                </AlertDialogAction>
                            </AlertDialogFooter>
                        </AlertDialogContent>
                    </AlertDialog>
                </div>
            </div>
        </div>
    );
}
