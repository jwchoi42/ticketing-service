'use client';

import { useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { AlertCircle } from 'lucide-react';

export default function MatchesError({
    error,
    reset,
}: {
    error: Error & { digest?: string };
    reset: () => void;
}) {
    useEffect(() => {
        console.error('Matches page error:', error);
    }, [error]);

    return (
        <div className="flex flex-col items-center justify-center min-h-[80vh] p-6 text-center">
            <div className="bg-destructive/10 p-4 rounded-full mb-4">
                <AlertCircle className="h-8 w-8 text-destructive" />
            </div>
            <h2 className="text-xl font-bold mb-2">Failed to load matches</h2>
            <p className="text-muted-foreground mb-6">
                Something went wrong while loading the matches.
            </p>
            <Button onClick={reset}>Try again</Button>
        </div>
    );
}
