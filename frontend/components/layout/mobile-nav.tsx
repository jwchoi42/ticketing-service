'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Trophy, Ticket, ClipboardList } from 'lucide-react';
import { cn } from '@/lib/utils';

// Hoisted outside component to prevent recreation on every render
const SEAT_SELECTION_PAGE_REGEX = /^\/matches\/[^\/]+$/;

// Static navigation items - hoisted to prevent array recreation
const NAV_ITEMS = [
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
] as const;

export function MobileNav() {
    const pathname = usePathname();

    // Hide on auth pages or seat selection page
    const isAuthPage = pathname.startsWith('/log-in') || pathname.startsWith('/sign-up');
    const isSeatSelectionPage = SEAT_SELECTION_PAGE_REGEX.test(pathname);
    if (isAuthPage || isSeatSelectionPage) return null;

    return (
        <div className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[480px] z-50 bg-background border-t border-border pb-safe-area-bottom">
            <div className="flex justify-around items-center h-16">
                {NAV_ITEMS.map((item) => {
                    // Check if current path matches or starts with nav item href
                    const isActive = pathname === item.href || pathname.startsWith(item.href + '/');
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
