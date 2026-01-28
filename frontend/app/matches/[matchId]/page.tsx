'use client';

import { useState, useCallback, useMemo, useRef, useEffect } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { matchApi } from '@/lib/api/matches';
import { siteApi, Seat } from '@/lib/api/site';
import { allocationApi } from '@/lib/api/allocation';
import { useSSE } from '@/hooks/use-sse';
import { useAuthStore } from '@/store/auth-store';
import { ChevronLeft } from 'lucide-react';
import { toast } from 'sonner';

// Split components for better code organization and bundle optimization
import { SeatGrid } from './components/seat-grid';
import { ZoneSelector } from './components/zone-selector';
import { BlockNavigator } from './components/block-navigator';
import { SelectionSummary } from './components/selection-summary';

export default function SeatSelectionPage() {
    const params = useParams();
    const matchId = params.matchId as string;
    const { isAuthenticated } = useAuthStore();
    const router = useRouter();

    // --- State ---
    const [selectedAreaId, setSelectedAreaId] = useState<number | null>(null);
    const [selectedSectionId, setSelectedSectionId] = useState<number | null>(null);
    const [selectedBlockId, setSelectedBlockId] = useState<number | null>(null);
    const [myHeldSeatIds, setMyHeldSeatIds] = useState<Set<number>>(new Set());
    const [heldSeatsInfo, setHeldSeatsInfo] = useState<Map<number, { blockName: string, seatNumber: number | string }>>(new Map());
    const [isSummaryExpanded, setIsSummaryExpanded] = useState(false);
    const [slideDirection, setSlideDirection] = useState<'left' | 'right'>('right');

    // Ref to avoid stale closures in callbacks (rerender-functional-setstate / advanced-use-latest)
    const heldSeatIdsRef = useRef(myHeldSeatIds);
    useEffect(() => {
        heldSeatIdsRef.current = myHeldSeatIds;
    }, [myHeldSeatIds]);

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

    // Memoize derived values to prevent recalculation on every render
    const activeBlockId = useMemo(
        () => selectedBlockId ?? (blocks?.[0]?.id || null),
        [selectedBlockId, blocks]
    );

    // Build a Map for O(1) block lookups (js-index-maps)
    const blockMap = useMemo(() => {
        if (!blocks) return new Map();
        return new Map(blocks.map(b => [b.id, b]));
    }, [blocks]);

    const currentBlock = useMemo(
        () => (activeBlockId ? blockMap.get(activeBlockId) : undefined),
        [activeBlockId, blockMap]
    );

    // Memoize block index for navigation
    const currentBlockIndex = useMemo(() => {
        if (!blocks || !activeBlockId) return -1;
        return blocks.findIndex(b => b.id === activeBlockId);
    }, [blocks, activeBlockId]);

    // --- Real-time Seat Data ---
    const { seats, status } = useSSE({
        matchId,
        blockId: activeBlockId,
        enabled: !!activeBlockId
    });

    // --- Memoized Handlers ---
    const handleAreaSelect = useCallback((areaId: number) => {
        setSelectedAreaId(areaId);
        setSelectedSectionId(null);
        setSelectedBlockId(null);
    }, []);

    const handleSectionSelect = useCallback((sectionId: number) => {
        setSelectedSectionId(sectionId);
        setSelectedBlockId(null);
        setSlideDirection('right'); // Default entry direction
    }, []);

    const handlePrevBlock = useCallback(() => {
        if (!blocks || currentBlockIndex <= 0) return;
        setSlideDirection('left');
        setSelectedBlockId(blocks[currentBlockIndex - 1].id);
    }, [blocks, currentBlockIndex]);

    const handleNextBlock = useCallback(() => {
        if (!blocks || currentBlockIndex < 0 || currentBlockIndex >= blocks.length - 1) return;
        setSlideDirection('right');
        setSelectedBlockId(blocks[currentBlockIndex + 1].id);
    }, [blocks, currentBlockIndex]);

    // Stable reference for seat click handler
    const handleSeatClick = useCallback(async (seat: Seat) => {
        if (!isAuthenticated) {
            toast.error('로그인이 필요합니다.');
            router.push('/log-in');
            return;
        }
        if (seat.state === 'OCCUPIED') return;

        // Check if held by someone else using functional check
        setMyHeldSeatIds(currentHeldIds => {
            if (seat.state === 'HOLD' && !currentHeldIds.has(seat.id)) {
                return currentHeldIds; // No change - someone else holds it
            }
            return currentHeldIds;
        });

        try {
            if (seat.state === 'HOLD') {
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
                setMyHeldSeatIds(prev => {
                    const next = new Set(prev);
                    next.add(seat.id);
                    return next;
                });
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
    }, [isAuthenticated, router, matchId, currentBlock?.name]);

    // Handler for releasing individual seats
    const handleReleaseSeat = useCallback(async (seatId: number) => {
        try {
            await allocationApi.releaseSeat(matchId, seatId);
            setMyHeldSeatIds(prev => {
                const next = new Set(prev);
                next.delete(seatId);
                return next;
            });
            setHeldSeatsInfo(prev => {
                const next = new Map(prev);
                next.delete(seatId);
                return next;
            });
            // Close summary if last seat released
            setMyHeldSeatIds(current => {
                if (current.size <= 1) {
                    setIsSummaryExpanded(false);
                }
                return current;
            });
        } catch {
            toast.error('Failed to release seat');
        }
    }, [matchId]);

    // Handler for clearing all held seats - uses ref to avoid dependency on myHeldSeatIds
    // This prevents re-renders when myHeldSeatIds changes (rerender-functional-setstate)
    const handleClearAllSeats = useCallback(() => {
        toast.info("Clearing all selections...");
        // Use ref to get current value without dependency
        heldSeatIdsRef.current.forEach(id => allocationApi.releaseSeat(matchId, id));
        setMyHeldSeatIds(new Set());
        setHeldSeatsInfo(new Map());
        setIsSummaryExpanded(false);
    }, [matchId]);

    const handleConfirmSeats = useCallback(async () => {
        try {
            await allocationApi.confirmSeats(matchId, Array.from(myHeldSeatIds));
            toast.success('좌석이 확정되었습니다.');
            router.push('/reservation');
        } catch (err: unknown) {
            console.error('Confirmation Failed:', err);
            const message = err instanceof Error ? err.message : '좌석 확정에 실패했습니다.';
            toast.error(message);
        }
    }, [matchId, myHeldSeatIds, router]);

    const handleToggleSummary = useCallback(() => {
        if (myHeldSeatIds.size > 0) {
            setIsSummaryExpanded(prev => !prev);
        }
    }, [myHeldSeatIds.size]);

    const handleExpandSummary = useCallback(() => {
        setIsSummaryExpanded(true);
    }, []);

    const handleNavigateBack = useCallback(() => {
        router.push('/matches');
    }, [router]);

    if (!match) return <div className="p-8">Loading...</div>;

    return (
        <div className="flex flex-col min-h-screen bg-background text-foreground relative">
            {/* 1. Header with Step Indicator & Back Button */}
            <div className="bg-background border-b border-border/50 sticky top-0 z-40">
                <div className="px-4 h-14 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <button
                            onClick={handleNavigateBack}
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
            <ZoneSelector
                areas={areas}
                sections={sections}
                selectedAreaId={selectedAreaId}
                selectedSectionId={selectedSectionId}
                onAreaSelect={handleAreaSelect}
                onSectionSelect={handleSectionSelect}
            />

            {/* 3. Block Navigation & Status */}
            {selectedSectionId && (
                <BlockNavigator
                    blocks={blocks}
                    currentBlock={currentBlock}
                    currentBlockIndex={currentBlockIndex}
                    status={status}
                    onPrevBlock={handlePrevBlock}
                    onNextBlock={handleNextBlock}
                />
            )}

            {/* 4. Main Content (Seat Map) - Improved mobile scroll and layout */}
            <main className="flex-1 overflow-hidden bg-slate-50/50 flex flex-col">
                <div className="w-full max-w-lg mx-auto flex-1 flex flex-col h-full">
                    {!selectedSectionId ? (
                        <div className="flex flex-col items-center justify-center h-64 text-muted-foreground space-y-2">
                            <p className="font-medium text-sm">영역과 구역을 먼저 선택해주세요</p>
                        </div>
                    ) : !activeBlockId ? (
                        <div className="flex items-center justify-center h-64 text-muted-foreground text-sm">블록 정보를 불러오는 중...</div>
                    ) : (
                        <SeatGrid
                            blockId={activeBlockId} // Pass ID for internal keying
                            seats={seats}
                            myHeldSeatIds={myHeldSeatIds}
                            onSeatClick={handleSeatClick}
                            direction={slideDirection}
                            status={status}
                        />
                    )}
                </div>
            </main>

            {/* 5. Selection Summary & Confirmation (Always Visible) */}
            <SelectionSummary
                myHeldSeatIds={myHeldSeatIds}
                heldSeatsInfo={heldSeatsInfo}
                isSummaryExpanded={isSummaryExpanded}
                onToggleSummary={handleToggleSummary}
                onExpandSummary={handleExpandSummary}
                onReleaseSeat={handleReleaseSeat}
                onClearAllSeats={handleClearAllSeats}
                onConfirmSeats={handleConfirmSeats}
            />
        </div>
    );
}
