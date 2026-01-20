'use client';

import { useCallback, memo } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/store/auth-store';
import { toast } from 'sonner';
import { Match } from '@/lib/api/matches';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card';
import { Button } from '@/components/ui/button';

// Memoized MatchCard to prevent unnecessary re-renders
interface MatchCardProps {
    match: Match;
    formattedDateTime: string;
    onBookClick: (matchId: number) => void;
    onHover: (matchId: number) => void;
}

const MatchCard = memo(function MatchCard({ match, formattedDateTime, onBookClick, onHover }: MatchCardProps) {
    const handleClick = useCallback(() => {
        onBookClick(match.id);
    }, [onBookClick, match.id]);

    const handleMouseEnter = useCallback(() => {
        onHover(match.id);
    }, [onHover, match.id]);

    return (
        <Card
            className="bg-card text-card-foreground shadow-sm"
            onMouseEnter={handleMouseEnter}
        >
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
            <CardFooter>
                <Button className="w-full" onClick={handleClick}>
                    Book Tickets
                </Button>
            </CardFooter>
        </Card>
    );
});

interface MatchListProps {
    matches: Match[];
    formattedDates: Record<number, string>;
}

export function MatchList({ matches, formattedDates }: MatchListProps) {
    const router = useRouter();
    const { isAuthenticated } = useAuthStore();

    // Stable handler for booking click
    const handleBookClick = useCallback((matchId: number) => {
        if (!isAuthenticated) {
            toast.error('로그인이 필요합니다.');
            router.push('/log-in');
            return;
        }
        router.push(`/matches/${matchId}`);
    }, [isAuthenticated, router]);

    // Prefetch on hover for perceived speed (bundle-preload)
    const handleHover = useCallback((matchId: number) => {
        router.prefetch(`/matches/${matchId}`);
    }, [router]);

    return (
        <div className="px-4 py-2 pb-24 min-h-screen bg-background text-foreground">
            <div className="space-y-4">
                {matches.map((match) => (
                    <MatchCard
                        key={match.id}
                        match={match}
                        formattedDateTime={formattedDates[match.id] || ''}
                        onBookClick={handleBookClick}
                        onHover={handleHover}
                    />
                ))}
                {matches.length === 0 && (
                    <p className="text-center text-muted-foreground py-8">No matches scheduled.</p>
                )}
            </div>
        </div>
    );
}
