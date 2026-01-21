'use client';

import { useMemo, useCallback } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { reservationApi, Reservation } from '@/lib/api/reservation';
import { useAuthStore } from '@/store/auth-store';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Calendar, ClipboardList, ChevronRight, Loader2 } from 'lucide-react';

export default function ReservationPage() {
    const { isAuthenticated } = useAuthStore();
    const router = useRouter();

    const { data: reservations, isLoading } = useQuery<Reservation[]>({
        queryKey: ['reservations'],
        queryFn: () => reservationApi.getReservations(),
        enabled: isAuthenticated,
    });

    // Memoize filtered reservations - must be called before any early returns (Rules of Hooks)
    const pendingReservations = useMemo(
        () => reservations?.filter(r => r.status === 'PENDING') || [],
        [reservations]
    );

    // Memoized navigation handlers
    const handleGoToLogin = useCallback(() => {
        router.push('/log-in');
    }, [router]);

    const handleBrowseMatches = useCallback(() => {
        router.push('/matches');
    }, [router]);

    const handlePayment = useCallback((reservationId: number) => {
        router.push(`/payment/${reservationId}`);
    }, [router]);

    if (!isAuthenticated) {
        return (
            <div className="flex flex-col items-center justify-center min-h-[80vh] p-6 text-center">
                <div className="bg-primary/10 p-4 rounded-full mb-4">
                    <ClipboardList className="h-8 w-8 text-primary" />
                </div>
                <h2 className="text-xl font-bold mb-2">Login Required</h2>
                <p className="text-muted-foreground mb-6">Please log in to view your reservations.</p>
                <Button onClick={handleGoToLogin}>Go to Login</Button>
            </div>
        );
    }

    if (isLoading) {
        return (
            <div className="flex items-center justify-center min-h-[80vh]">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
        );
    }

    return (
        <div className="p-4 pb-24 min-h-screen bg-muted/30">
            <header className="mb-6">
                <h1 className="text-2xl font-bold">My Reservations</h1>
                <p className="text-sm text-muted-foreground">Review and complete your bookings</p>
            </header>

            {pendingReservations.length === 0 ? (
                <Card className="border-dashed">
                    <CardContent className="flex flex-col items-center justify-center p-8 text-center">
                        <div className="bg-muted p-3 rounded-full mb-4">
                            <Calendar className="h-6 w-6 text-muted-foreground" />
                        </div>
                        <h3 className="font-semibold mb-1">No pending reservations</h3>
                        <p className="text-sm text-muted-foreground mb-4">Find a match and pick your seats to get started.</p>
                        <Button variant="outline" onClick={handleBrowseMatches}>
                            Browse Matches
                        </Button>
                    </CardContent>
                </Card>
            ) : (
                <div className="space-y-4">
                    {pendingReservations.map((res) => (
                        <Card key={res.id} className="overflow-hidden border-primary/20 shadow-sm">
                            <div className="bg-primary/5 px-4 py-2 border-b border-primary/10 flex justify-between items-center">
                                <span className="text-[10px] font-bold uppercase tracking-wider text-primary">Pending Payment</span>
                                <span className="text-xs text-muted-foreground">Reservation #{res.id}</span>
                            </div>
                            <CardHeader className="p-4 pb-2">
                                <CardTitle className="text-lg">Match #{res.matchId}</CardTitle>
                            </CardHeader>
                            <CardContent className="p-4 pt-0 space-y-3">
                                <div className="flex items-center text-sm text-muted-foreground">
                                    <ClipboardList className="h-3.5 w-3.5 mr-2" />
                                    {res.seatIds?.length || 0} Seats Selected
                                </div>

                                <div className="pt-2 flex items-center justify-end border-t border-border/50">
                                    <Button
                                        size="sm"
                                        className="gap-2"
                                        onClick={() => handlePayment(res.id)}
                                    >
                                        Pay Now
                                        <ChevronRight className="h-4 w-4" />
                                    </Button>
                                </div>
                            </CardContent>
                        </Card>
                    ))}
                </div>
            )}
        </div>
    );
}
