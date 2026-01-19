'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Trophy, Ticket, ClipboardList } from 'lucide-react';
import { cn } from '@/lib/utils';
import { useAuthStore } from '@/store/auth-store';

export function MobileNav() {
    const pathname = usePathname();
    const user = useAuthStore((state) => state.user);

    // Hide on auth pages if desired, or keep generic. 
    // Usually app-like nav is visible on main authenticated views.
    // If not logged in, maybe show Home/Login?
    // Let's assume generic visibility, or hide on specific routes.
    const isAuthPage = pathname.startsWith('/log-in') || pathname.startsWith('/sign-up');
    const isSeatSelectionPage = /^\/matches\/[^\/]+$/.test(pathname);
    if (isAuthPage || isSeatSelectionPage) return null;

    const navItems = [
        {
            label: 'Matches',
            href: '/matches',
            icon: Trophy,
        },
        {
            label: 'Reservation',
            href: '/reservation',
            icon: ClipboardList,
        },
        {
            label: 'Tickets',
            href: '/tickets',
            icon: Ticket,
        },
    ];

    return (
        <div className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[480px] z-50 bg-background border-t border-border pb-safe-area-bottom">
            <div className="flex justify-around items-center h-16">
                {navItems.map((item) => {
                    const isActive = pathname === item.href || (item.href !== '/' && pathname.startsWith(item.href));
                    return (
                        <Link
                            key={item.href}
                            href={item.href}
                            className={cn(
                                "flex flex-col items-center justify-center w-full h-full space-y-1 text-xs font-medium transition-colors",
                                isActive
                                    ? "text-primary"
                                    : "text-muted-foreground hover:text-foreground"
                            )}
                        >
                            <item.icon className="h-5 w-5" />
                            <span>{item.label}</span>
                        </Link>
                    );
                })}
            </div>
        </div>
    );
}
