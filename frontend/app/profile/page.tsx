'use client';

import { useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/store/auth-store';
import { Button } from '@/components/ui/button';
import { Loader2 } from 'lucide-react';
import { toast } from 'sonner';

export default function ProfilePage() {
    const { user, isAuthenticated, logout } = useAuthStore();
    const router = useRouter();

    useEffect(() => {
        if (!isAuthenticated) {
            toast.error('로그인이 필요합니다.');
            router.push('/log-in');
        }
    }, [isAuthenticated, router]);

    // Memoized logout handler to prevent recreation on every render
    const handleLogout = useCallback(() => {
        logout();
        toast.success('로그아웃 되었습니다.');
        router.push('/log-in');
    }, [logout, router]);

    // Show loading state instead of null for better UX
    if (!isAuthenticated || !user) {
        return (
            <div className="flex items-center justify-center min-h-[80vh]">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
        );
    }

    return (
        <div className="p-6 pb-24 min-h-screen bg-background text-foreground">
            <h1 className="text-2xl font-bold mb-6">Profile</h1>

            <div className="bg-card rounded-xl p-6 shadow-sm space-y-4">
                <div>
                    <label className="text-sm font-medium text-muted-foreground">Email</label>
                    <p className="text-lg font-semibold">{user.email}</p>
                </div>

                <div className="pt-4">
                    <Button className="w-full" onClick={handleLogout}>
                        Sign Out
                    </Button>
                </div>
            </div>
        </div>
    );
}
