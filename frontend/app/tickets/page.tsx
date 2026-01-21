'use client';

import { useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/store/auth-store';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Ticket, Calendar } from 'lucide-react';

export default function TicketsPage() {
    const { isAuthenticated } = useAuthStore();
    const router = useRouter();

    // Memoized navigation handlers to prevent recreation on every render
    const handleGoToLogin = useCallback(() => {
        router.push('/log-in');
    }, [router]);

    const handleBrowseMatches = useCallback(() => {
        router.push('/matches');
    }, [router]);

    if (!isAuthenticated) {
        return (
            <div className="flex flex-col items-center justify-center min-h-[80vh] p-6 text-center">
                <div className="bg-primary/10 p-4 rounded-full mb-4">
                    <Ticket className="h-8 w-8 text-primary" />
                </div>
                <h2 className="text-xl font-bold mb-2">Login Required</h2>
                <p className="text-muted-foreground mb-6">Please log in to view your tickets.</p>
                <Button onClick={handleGoToLogin}>Go to Login</Button>
            </div>
        );
    }

    return (
        <div className="p-4 pb-24 min-h-screen bg-muted/30">
            <header className="mb-6">
                <h1 className="text-2xl font-bold">My Tickets</h1>
                <p className="text-sm text-muted-foreground">View your purchased tickets</p>
            </header>

            <Card className="border-dashed">
                <CardContent className="flex flex-col items-center justify-center p-8 text-center">
                    <div className="bg-muted p-3 rounded-full mb-4">
                        <Calendar className="h-6 w-6 text-muted-foreground" />
                    </div>
                    <h3 className="font-semibold mb-1">No tickets yet</h3>
                    <p className="text-sm text-muted-foreground mb-4">Complete a reservation to get your tickets.</p>
                    <Button variant="outline" onClick={handleBrowseMatches}>
                        Browse Matches
                    </Button>
                </CardContent>
            </Card>
        </div>
    );
}
