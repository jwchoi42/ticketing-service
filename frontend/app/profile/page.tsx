'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/store/auth-store';
import { Button } from '@/components/ui/button';
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

    const handleLogout = () => {
        logout();
        toast.success('로그아웃 되었습니다.');
        router.push('/log-in');
    };

    if (!isAuthenticated || !user) return null;

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
