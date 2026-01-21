export default function ProfileLoading() {
    return (
        <div className="p-6 pb-24 min-h-screen bg-background text-foreground">
            <div className="h-8 w-24 bg-muted rounded mb-6 animate-pulse" />
            <div className="bg-card rounded-xl p-6 shadow-sm space-y-4">
                <div className="space-y-2">
                    <div className="h-4 w-12 bg-muted rounded animate-pulse" />
                    <div className="h-6 w-48 bg-muted rounded animate-pulse" />
                </div>
                <div className="pt-4">
                    <div className="h-10 w-full bg-muted rounded animate-pulse" />
                </div>
            </div>
        </div>
    );
}
