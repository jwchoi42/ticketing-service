'use client';

import { useState, useRef, useEffect, useCallback, useMemo } from 'react';
import Link from 'next/link';
import { useAuthStore } from '@/store/auth-store';
import { useRouter, usePathname } from 'next/navigation';
import { LogOut, User } from 'lucide-react';

// Hoisted regex outside component to prevent recreation on every render
const SEAT_SELECTION_PAGE_REGEX = /^\/matches\/[^\/]+$/;

export function Header() {
    const { user, isAuthenticated, logout } = useAuthStore();
    const router = useRouter();
    const pathname = usePathname();
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const menuRef = useRef<HTMLDivElement>(null);

    // Memoized click outside handler to prevent recreation
    const handleClickOutside = useCallback((event: MouseEvent) => {
        if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
            setIsMenuOpen(false);
        }
    }, []);

    // Close menu when clicking outside - optimized to only add/remove when needed
    useEffect(() => {
        if (isMenuOpen) {
            document.addEventListener('mousedown', handleClickOutside);
            return () => {
                document.removeEventListener('mousedown', handleClickOutside);
            };
        }
    }, [isMenuOpen, handleClickOutside]);

    // Memoize page check to prevent recalculation on unrelated state changes
    const shouldHideHeader = useMemo(() => {
        const isAuthPage = pathname.startsWith('/log-in') || pathname.startsWith('/sign-up');
        const isSeatSelectionPage = SEAT_SELECTION_PAGE_REGEX.test(pathname);
        return isAuthPage || isSeatSelectionPage;
    }, [pathname]);

    // Memoized handlers
    const handleLogout = useCallback(() => {
        logout();
        setIsMenuOpen(false);
        router.push('/matches');
    }, [logout, router]);

    const handleProfile = useCallback(() => {
        setIsMenuOpen(false);
        router.push('/profile');
    }, [router]);

    const toggleMenu = useCallback(() => {
        setIsMenuOpen(prev => !prev);
    }, []);

    // Early return AFTER all hooks
    if (shouldHideHeader) return null;

    return (
        <header className="sticky top-0 z-40 w-full bg-background/80 backdrop-blur-md border-b border-border">
            <div className="px-4 h-14 flex items-center justify-between">
                <Link href="/matches" className="flex items-center gap-2 hover:opacity-80 transition-opacity">
                    <span className="font-bold text-primary text-xl">TicketPass</span>
                </Link>

                {isAuthenticated && user && (
                    <div className="relative" ref={menuRef}>
                        {/* Avatar Button */}
                        <button
                            onClick={toggleMenu}
                            className="h-9 w-9 rounded-full bg-primary/10 border border-primary/20 flex items-center justify-center hover:bg-primary/20 transition-colors focus:outline-none focus:ring-2 focus:ring-primary/50"
                        >
                            <User className="h-5 w-5 text-primary" />
                        </button>

                        {/* Dropdown Menu */}
                        {isMenuOpen && (
                            <div className="absolute right-0 mt-2 w-48 bg-background rounded-lg shadow-lg border border-border py-1 animate-in fade-in-0 zoom-in-95 duration-150">
                                {/* User Info */}
                                <div className="px-4 py-2 border-b border-border">
                                    <p className="text-sm font-medium truncate">{user.email}</p>
                                </div>

                                {/* Menu Items */}
                                <button
                                    onClick={handleProfile}
                                    className="w-full px-4 py-2.5 text-left text-sm flex items-center gap-3 hover:bg-primary/10 hover:text-primary transition-colors"
                                >
                                    <User className="h-4 w-4" />
                                    Profile
                                </button>
                                <button
                                    onClick={handleLogout}
                                    className="w-full px-4 py-2.5 text-left text-sm flex items-center gap-3 hover:bg-primary/10 hover:text-primary transition-colors"
                                >
                                    <LogOut className="h-4 w-4" />
                                    Logout
                                </button>
                            </div>
                        )}
                    </div>
                )}
            </div>
        </header>
    );
}
