export default function MatchesLoading() {
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
