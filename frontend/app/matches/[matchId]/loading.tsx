export default function MatchDetailLoading() {
    return (
        <div className="flex flex-col min-h-screen bg-background text-foreground">
            {/* Header skeleton */}
            <div className="bg-background border-b border-border/50 sticky top-0 z-40">
                <div className="px-4 h-14 flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <div className="h-6 w-6 bg-muted rounded animate-pulse" />
                        <div className="space-y-1">
                            <div className="h-4 w-16 bg-muted rounded animate-pulse" />
                            <div className="h-3 w-12 bg-muted rounded animate-pulse" />
                        </div>
                    </div>
                    <div className="text-right space-y-1">
                        <div className="h-4 w-24 bg-muted rounded animate-pulse" />
                        <div className="h-3 w-16 bg-muted rounded animate-pulse" />
                    </div>
                </div>
            </div>

            {/* Zone selector skeleton */}
            <div className="sticky top-[56px] z-30 bg-background border-b shadow-sm pt-4">
                <div className="px-4 pb-3 flex gap-2">
                    {[1, 2, 3, 4].map((i) => (
                        <div key={i} className="h-8 w-20 bg-muted rounded-full animate-pulse" />
                    ))}
                </div>
            </div>

            {/* Main content skeleton */}
            <main className="flex-1 p-4 overflow-auto bg-slate-50/50 pb-40">
                <div className="flex flex-col items-center justify-center h-64 text-muted-foreground space-y-2">
                    <div className="h-8 w-8 border-2 border-primary border-t-transparent rounded-full animate-spin" />
                    <p className="font-medium">Loading seat map...</p>
                </div>
            </main>

            {/* Bottom bar skeleton */}
            <div className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[480px] bg-background border-t p-6 rounded-t-3xl">
                <div className="h-14 bg-muted rounded-2xl animate-pulse" />
            </div>
        </div>
    );
}
