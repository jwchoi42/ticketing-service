'use client';

import { useCallback, memo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/store/auth-store';
import { toast } from 'sonner';
import { Match } from '@/lib/api/matches';
import { adminApi } from '@/lib/api/admin';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
// import { Badge } from '@/components/ui/badge'; // Badge not found, using span instead
import { Plus, Edit, Trash2, CalendarCheck, Lock } from 'lucide-react';
import { MatchFormSheet } from '@/components/matches/match-form-sheet';

// Memoized MatchCard to prevent unnecessary re-renders
interface MatchCardProps {
    match: Match;
    formattedDateTime: string;
    onBookClick: (matchId: number) => void;
    onHover: (matchId: number) => void;
    isAdmin: boolean;
    onEdit: (match: Match) => void;
    onDelete: (matchId: number) => void;
    onOpen: (matchId: number) => void;
}

const MatchCard = memo(function MatchCard({
    match,
    formattedDateTime,
    onBookClick,
    onHover,
    isAdmin,
    onEdit,
    onDelete,
    onOpen
}: MatchCardProps) {
    const handleClick = useCallback(() => {
        onBookClick(match.id);
    }, [onBookClick, match.id]);

    const handleMouseEnter = useCallback(() => {
        onHover(match.id);
    }, [onHover, match.id]);

    return (
        <Card
            className="bg-card text-card-foreground shadow-sm relative overflow-hidden"
            onMouseEnter={handleMouseEnter}
        >
            {/* Status Indicator for Admin */}
            {isAdmin && (
                <div className="absolute top-2 right-2 flex gap-2">
                    <span className={`px-2 py-0.5 text-xs font-bold rounded-full ${match.status === 'OPEN'
                        ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-100'
                        : 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-100'
                        }`}>
                        {match.status}
                    </span>
                </div>
            )}

            <CardHeader className="pb-2">
                <div className="flex justify-between items-start">
                    <CardTitle className="text-lg">
                        {match.homeTeam} <span className="text-muted-foreground text-sm font-normal">vs</span> {match.awayTeam}
                    </CardTitle>
                </div>
                <CardDescription>{formattedDateTime}</CardDescription>
            </CardHeader>
            <CardContent className="pb-2">
                <p className="text-sm text-muted-foreground">{match.stadium}</p>
            </CardContent>
            <CardFooter className="flex-col gap-2">
                {/* User Action: Book */}
                {match.status === 'OPEN' ? (
                    <Button className="w-full" onClick={handleClick}>
                        Book Tickets
                    </Button>
                ) : (
                    <Button className="w-full" variant="outline" disabled>
                        Coming Soon (Not Open)
                    </Button>
                )}

                {/* Admin Actions */}
                {isAdmin && (
                    <div className="w-full grid grid-cols-3 gap-2 mt-2 pt-2 border-t text-xs">
                        {match.status === 'DRAFT' && (
                            <Button
                                variant="outline"
                                size="sm"
                                className="w-full text-green-600 hover:text-green-700 hover:bg-green-50 border-green-200"
                                onClick={(e) => { e.stopPropagation(); onOpen(match.id); }}
                            >
                                <CalendarCheck className="w-3 h-3 mr-1" /> Open
                            </Button>
                        )}
                        {match.status === 'OPEN' && (
                            <Button
                                variant="outline"
                                size="sm"
                                className="w-full text-muted-foreground"
                                disabled
                            >
                                <Lock className="w-3 h-3 mr-1" /> Opened
                            </Button>
                        )}
                        <Button
                            variant="outline"
                            size="sm"
                            className="w-full"
                            onClick={(e) => { e.stopPropagation(); onEdit(match); }}
                        >
                            <Edit className="w-3 h-3 mr-1" /> Edit
                        </Button>
                        <Button
                            variant="destructive"
                            size="sm"
                            className="w-full"
                            onClick={(e) => { e.stopPropagation(); onDelete(match.id); }}
                        >
                            <Trash2 className="w-3 h-3 mr-1" /> Del
                        </Button>
                    </div>
                )}
            </CardFooter>
        </Card>
    );
});

interface MatchListProps {
    matches: Match[];
    formattedDates: Record<number, string>;
}

export function MatchList({ matches: initialMatches, formattedDates }: MatchListProps) {
    const router = useRouter();
    const { isAuthenticated, user } = useAuthStore();
    const isAdmin = isAuthenticated && user?.role === 'ADMIN';

    // Local state for optimistic updates / refreshing could be done via React Query invalidate
    // For now, let's assume parent re-fetches or we force refresh
    // Simplest: `router.refresh()` after actions

    // Dialog State
    const [isFormOpen, setIsFormOpen] = useState(false);
    const [editingMatch, setEditingMatch] = useState<Match | null>(null);

    // Stable handler for booking click
    const handleBookClick = useCallback((matchId: number) => {
        if (!isAuthenticated) {
            toast.error('로그인이 필요합니다.');
            router.push('/log-in');
            return;
        }
        router.push(`/matches/${matchId}`);
    }, [isAuthenticated, router]);

    // Prefetch on hover
    const handleHover = useCallback((matchId: number) => {
        router.prefetch(`/matches/${matchId}`);
    }, [router]);

    // Admin Handlers
    const handleCreate = () => {
        setEditingMatch(null);
        setIsFormOpen(true);
    };

    const handleEdit = useCallback((match: Match) => {
        setEditingMatch(match);
        setIsFormOpen(true);
    }, []);

    const handleDelete = useCallback(async (matchId: number) => {
        if (confirm('Are you sure you want to delete this match?')) {
            try {
                await adminApi.deleteMatch(matchId);
                toast.success('Match deleted');
                router.refresh();
            } catch {
                toast.error('Failed to delete match');
            }
        }
    }, [router]);

    const handleOpen = useCallback(async (matchId: number) => {
        if (confirm('Are you sure you want to OPEN this match? Use can start booking.')) {
            try {
                await adminApi.openMatch(matchId);
                toast.success('Match opened successfully');
                router.refresh();
            } catch {
                toast.error('Failed to open match');
            }
        }
    }, [router]);

    const handleFormSuccess = () => {
        router.refresh(); // Refresh Server Component to get new list
    };

    return (
        <div className="px-4 py-2 pb-24 min-h-screen bg-background text-foreground relative">
            {/* Admin FAB or Header Action */}
            {isAdmin && (
                <div className="flex justify-end mb-4">
                    <Button onClick={handleCreate}>
                        <Plus className="w-4 h-4 mr-2" /> Create Match
                    </Button>
                </div>
            )}

            <div className="space-y-4">
                {initialMatches.map((match) => (
                    <MatchCard
                        key={match.id}
                        match={match}
                        formattedDateTime={formattedDates[match.id] || ''}
                        onBookClick={handleBookClick}
                        onHover={handleHover}
                        isAdmin={isAdmin}
                        onEdit={handleEdit}
                        onDelete={handleDelete}
                        onOpen={handleOpen}
                    />
                ))}
                {initialMatches.length === 0 && (
                    <p className="text-center text-muted-foreground py-8">No matches scheduled.</p>
                )}
            </div>

            <MatchFormSheet
                isOpen={isFormOpen}
                onClose={() => setIsFormOpen(false)}
                onSuccess={handleFormSuccess}
                initialData={editingMatch}
            />
        </div>
    );
}
