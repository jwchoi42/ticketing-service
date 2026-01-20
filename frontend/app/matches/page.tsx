import { Suspense } from 'react';
import type { Metadata } from 'next';
import { Match } from '@/lib/api/matches';
import { MatchList } from './match-list';

// Page metadata for SEO
export const metadata: Metadata = {
    title: 'Matches | Ticketing Service',
    description: 'Browse available matches and book your tickets',
};

// Force dynamic rendering - don't pre-render at build time
export const dynamic = 'force-dynamic';

// Server-side data fetching (eliminates client-side waterfall)
async function getMatches(): Promise<Match[]> {
    try {
        const baseUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';
        const response = await fetch(`${baseUrl}/matches`, {
            // Use no-store for real-time match data (force-dynamic already set)
            cache: 'no-store',
        });

        if (!response.ok) {
            console.error('Failed to fetch matches:', response.status);
            return [];
        }

        const data = await response.json();
        return data.data.matches;
    } catch (error) {
        console.error('Error fetching matches:', error);
        return [];
    }
}

// Pre-compute formatted dates on the server
function formatMatchDates(matches: Match[]): Record<number, string> {
    const formatted: Record<number, string> = {};
    for (const match of matches) {
        const date = new Date(match.dateTime);
        formatted[match.id] = `${date.toLocaleDateString()} ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
    }
    return formatted;
}

function MatchListSkeleton() {
    return (
        <div className="px-4 py-2 pb-24 min-h-screen bg-background text-foreground">
            <div className="space-y-4">
                {[1, 2, 3].map((i) => (
                    <div key={i} className="bg-card rounded-lg shadow-sm p-4 animate-pulse">
                        <div className="h-6 bg-muted rounded w-3/4 mb-2" />
                        <div className="h-4 bg-muted rounded w-1/2 mb-4" />
                        <div className="h-4 bg-muted rounded w-1/3 mb-4" />
                        <div className="h-10 bg-muted rounded w-full" />
                    </div>
                ))}
            </div>
        </div>
    );
}

async function MatchListServer() {
    const matches = await getMatches();
    const formattedDates = formatMatchDates(matches);

    return <MatchList matches={matches} formattedDates={formattedDates} />;
}

export default function MatchListPage() {
    return (
        <Suspense fallback={<MatchListSkeleton />}>
            <MatchListServer />
        </Suspense>
    );
}
