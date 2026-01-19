'use client';

import { useQuery } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/store/auth-store';
import { toast } from 'sonner';
import { matchApi } from '@/lib/api/matches';
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card';
import { Button } from '@/components/ui/button';

export default function MatchListPage() {
    const router = useRouter();
    const { isAuthenticated } = useAuthStore();
    const { data: matches, isLoading, error } = useQuery({
        queryKey: ['matches'],
        queryFn: matchApi.getMatches,
    });

    if (isLoading) return <div className="p-8 text-center bg-background text-foreground">Loading matches...</div>;
    if (error) return <div className="p-8 text-center text-red-500 bg-background">Failed to load matches</div>;

    return (
        <div className="px-4 py-2 pb-24 min-h-screen bg-background text-foreground">

            <div className="space-y-4">
                {matches?.map((match) => (
                    <Card key={match.id} className="bg-card text-card-foreground shadow-sm">
                        <CardHeader className="pb-2">
                            <div className="flex justify-between items-start">
                                <CardTitle className="text-lg">{match.homeTeam} <span className="text-muted-foreground text-sm font-normal">vs</span> {match.awayTeam}</CardTitle>
                            </div>
                            <CardDescription>{new Date(match.dateTime).toLocaleDateString()} {new Date(match.dateTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</CardDescription>
                        </CardHeader>
                        <CardContent className="pb-2">
                            <p className="text-sm text-muted-foreground">{match.stadium}</p>
                        </CardContent>
                        <CardFooter>
                            <Button
                                className="w-full"
                                onClick={() => {
                                    if (!isAuthenticated) {
                                        toast.error('로그인이 필요합니다.');
                                        router.push('/log-in');
                                        return;
                                    }
                                    router.push(`/matches/${match.id}`);
                                }}
                            >
                                Book Tickets
                            </Button>
                        </CardFooter>
                    </Card>
                ))}
                {matches?.length === 0 && (
                    <p className="text-center text-muted-foreground py-8">No matches scheduled.</p>
                )}
            </div>
        </div>
    );
}
