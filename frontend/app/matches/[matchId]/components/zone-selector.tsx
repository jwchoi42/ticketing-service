'use client';

import { memo } from 'react';
import { Area, Section } from '@/lib/api/site';
import { cn } from '@/lib/utils';
import {
    Carousel,
    CarouselContent,
    CarouselItem,
} from "@/components/ui/carousel";

// Extracted to constant to prevent object recreation on every render
const CAROUSEL_OPTS = { align: "start" as const, dragFree: true };

interface ZoneSelectorProps {
    areas: Area[] | undefined;
    sections: Section[] | undefined;
    selectedAreaId: number | null;
    selectedSectionId: number | null;
    onAreaSelect: (areaId: number) => void;
    onSectionSelect: (sectionId: number) => void;
}

export const ZoneSelector = memo(function ZoneSelector({
    areas,
    sections,
    selectedAreaId,
    selectedSectionId,
    onAreaSelect,
    onSectionSelect,
}: ZoneSelectorProps) {
    return (
        <header className="sticky top-[56px] z-30 bg-background border-b shadow-sm pt-4">
            {/* Area Selection */}
            <div className="px-4 pb-3">
                <Carousel opts={CAROUSEL_OPTS} className="w-full">
                    <CarouselContent className="-ml-2">
                        {areas?.map(area => (
                            <CarouselItem key={area.id} className="pl-2 basis-auto">
                                <button
                                    onClick={() => onAreaSelect(area.id)}
                                    className={cn(
                                        "px-4 py-1.5 rounded-full text-sm font-medium whitespace-nowrap transition-colors",
                                        selectedAreaId === area.id
                                            ? "bg-primary text-primary-foreground shadow-sm"
                                            : "bg-muted text-muted-foreground hover:bg-muted/80"
                                    )}
                                >
                                    {area.name}
                                </button>
                            </CarouselItem>
                        ))}
                    </CarouselContent>
                </Carousel>
            </div>

            {/* Section Selection */}
            {selectedAreaId && (
                <div className="px-4 pb-3 border-t pt-3 bg-muted/10">
                    <Carousel opts={CAROUSEL_OPTS} className="w-full">
                        <CarouselContent className="-ml-2">
                            {sections?.map(section => (
                                <CarouselItem key={section.id} className="pl-2 basis-auto">
                                    <button
                                        onClick={() => onSectionSelect(section.id)}
                                        className={cn(
                                            "px-3 py-1 rounded-md text-xs font-semibold whitespace-nowrap transition-all border",
                                            selectedSectionId === section.id
                                                ? "bg-primary/10 text-primary border-primary shadow-sm"
                                                : "bg-background border-border text-foreground hover:bg-accent"
                                        )}
                                    >
                                        {section.name}
                                    </button>
                                </CarouselItem>
                            ))}
                        </CarouselContent>
                    </Carousel>
                    {!sections?.length && <div className="text-xs text-muted-foreground py-1">Loading sections...</div>}
                </div>
            )}
        </header>
    );
});
