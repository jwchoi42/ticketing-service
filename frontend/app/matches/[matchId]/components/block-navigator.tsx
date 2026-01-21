'use client';

import { memo } from 'react';
import { Block } from '@/lib/api/site';
import { Button } from '@/components/ui/button';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '@/lib/utils';

type ConnectionStatus = 'connected' | 'disconnected' | 'reconnecting';

interface BlockNavigatorProps {
    blocks: Block[] | undefined;
    currentBlock: Block | undefined;
    currentBlockIndex: number;
    status: ConnectionStatus;
    onPrevBlock: () => void;
    onNextBlock: () => void;
}

export const BlockNavigator = memo(function BlockNavigator({
    blocks,
    currentBlock,
    currentBlockIndex,
    status,
    onPrevBlock,
    onNextBlock,
}: BlockNavigatorProps) {
    return (
        <div className="flex items-center justify-between px-4 py-3 border-b bg-card">
            <Button
                variant="ghost"
                size="icon"
                className="h-10 w-10 text-muted-foreground"
                onClick={onPrevBlock}
                disabled={!blocks || currentBlockIndex <= 0}
            >
                <ChevronLeft className="h-6 w-6" />
            </Button>

            <div className="flex flex-col items-center">
                <span className="text-base font-bold text-foreground">
                    {currentBlock?.name || 'Select Block'}
                </span>

                <div className="flex items-center gap-1.5 mt-0.5">
                    <div className="relative flex h-1.5 w-1.5">
                        {status === 'reconnecting' && (
                            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-primary opacity-75"></span>
                        )}
                        <span className={cn(
                            "relative inline-flex rounded-full h-1.5 w-1.5",
                            status === 'connected' && "bg-primary",
                            status === 'disconnected' && "bg-primary/20",
                            status === 'reconnecting' && "bg-primary/60"
                        )} />
                    </div>
                    <span className={cn(
                        "text-[10px] font-bold uppercase tracking-wider",
                        status === 'connected' && "text-primary",
                        status === 'disconnected' && "text-primary/30",
                        status === 'reconnecting' && "text-primary/70"
                    )}>
                        {status === 'connected' ? 'Live' : status === 'reconnecting' ? 'Connecting' : 'Offline'}
                    </span>
                </div>
            </div>

            <Button
                variant="ghost"
                size="icon"
                className="h-10 w-10 text-muted-foreground"
                onClick={onNextBlock}
                disabled={!blocks || currentBlockIndex >= blocks.length - 1}
            >
                <ChevronRight className="h-6 w-6" />
            </Button>
        </div>
    );
});
