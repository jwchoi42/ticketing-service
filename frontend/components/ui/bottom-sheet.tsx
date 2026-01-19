'use client';

import * as React from 'react';
import { cn } from '@/lib/utils';
import { X } from 'lucide-react';

interface BottomSheetProps {
    isOpen: boolean;
    onClose: () => void;
    title?: string;
    children: React.ReactNode;
}

export function BottomSheet({ isOpen, onClose, title, children }: BottomSheetProps) {
    const [isVisible, setIsVisible] = React.useState(isOpen);

    React.useEffect(() => {
        if (isOpen) {
            setIsVisible(true);
            document.body.style.overflow = 'hidden';
        } else {
            const timer = setTimeout(() => setIsVisible(false), 300); // Animation duration
            document.body.style.overflow = '';
            return () => clearTimeout(timer);
        }
    }, [isOpen]);

    if (!isVisible && !isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center">
            {/* Backdrop */}
            <div
                className={cn(
                    "fixed inset-0 bg-black/50 transition-opacity duration-300",
                    isOpen ? "opacity-100" : "opacity-0"
                )}
                onClick={onClose}
            />

            {/* Sheet Content */}
            <div
                className={cn(
                    "relative w-full max-w-lg bg-background rounded-t-[20px] shadow-lg transition-transform duration-300 ease-out transform",
                    isOpen ? "translate-y-0" : "translate-y-full",
                    "sm:rounded-[20px] sm:translate-y-0 sm:scale-100", // On desktop, could behave like a modal if desired, but here focusing on mobile
                )}
                style={{ maxHeight: '85vh' }}
            >
                <div className="flex flex-col h-full max-h-[85vh]">
                    {/* Handle bar for visual cue */}
                    <div className="flex justify-center pt-3 pb-1 sm:hidden">
                        <div className="w-12 h-1.5 bg-muted rounded-full" />
                    </div>

                    {/* Header */}
                    <div className="flex items-center justify-between px-6 py-4 border-b">
                        <h3 className="text-lg font-semibold">{title}</h3>
                        <button onClick={onClose} className="p-2 -mr-2 text-muted-foreground hover:text-foreground">
                            <X className="w-5 h-5" />
                        </button>
                    </div>

                    {/* Content */}
                    <div className="flex-1 overflow-y-auto p-6">
                        {children}
                    </div>
                </div>
            </div>
        </div>
    );
}
