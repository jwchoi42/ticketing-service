'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { AlertCircle, ChevronLeft } from 'lucide-react';

export default function MatchDetailError({
    error,
    reset,
}: {
    error: Error & { digest?: string };
    reset: () => void;
}) {
    const router = useRouter();

    useEffect(() => {
        console.error('Match detail page error:', error);
    }, [error]);

    return (
        <div className="flex flex-col min-h-screen bg-background text-foreground">
            {/* Header */}
            <div className="bg-background border-b border-border/50 sticky top-0 z-40">
                <div className="px-4 h-14 flex items-center">
                    <button
                        onClick={() => router.push('/matches')}
                        className="p-1 -ml-1 text-muted-foreground hover:text-foreground transition-colors"
                    >
                        <ChevronLeft className="h-6 w-6" />
                    </button>
                </div>
            </div>

            {/* Error content */}
            <div className="flex-1 flex flex-col items-center justify-center p-6 text-center">
                <div className="bg-destructive/10 p-4 rounded-full mb-4">
                    <AlertCircle className="h-8 w-8 text-destructive" />
                </div>
                <h2 className="text-xl font-bold mb-2">Failed to load match</h2>
                <p className="text-muted-foreground mb-6">
                    Something went wrong while loading the seat selection.
                </p>
                <div className="flex gap-3">
                    <Button variant="outline" onClick={() => router.push('/matches')}>
                        Back to matches
                    </Button>
                    <Button onClick={reset}>Try again</Button>
                </div>
            </div>
        </div>
    );
}
