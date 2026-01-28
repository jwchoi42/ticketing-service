'use client';

import { memo } from 'react';
import { Area, Section } from '@/lib/api/site';
import { cn } from '@/lib/utils';

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
        <header className="sticky top-[56px] z-30 bg-background border-b shadow-md overflow-hidden">
            <div className="grid grid-cols-2 h-full divide-x divide-slate-100 min-h-[80px]">
                {/* 1. Left Column: Areas - 50% width */}
                <div className="p-3 flex flex-col gap-2 bg-slate-50/30">
                    <span className="text-[9px] font-black text-slate-400 uppercase tracking-widest pl-1 mb-0.5">Area</span>
                    <div className="flex flex-col gap-2 max-h-[160px] overflow-y-auto pr-1">
                        {areas?.map(area => (
                            <button
                                key={area.id}
                                onClick={() => onAreaSelect(area.id)}
                                className={cn(
                                    "w-full py-2 px-3 rounded-lg text-[11px] font-bold text-left transition-all border",
                                    selectedAreaId === area.id
                                        ? "bg-slate-900 border-slate-900 text-white shadow-sm ring-2 ring-slate-900/10"
                                        : "bg-white border-slate-200 text-slate-500 hover:border-slate-300 hover:bg-slate-50"
                                )}
                            >
                                {area.name}
                            </button>
                        ))}
                    </div>
                </div>

                {/* 2. Right Column: Sections - 50% width */}
                <div className={cn(
                    "p-3 flex flex-col gap-2 bg-white transition-opacity duration-300",
                    !selectedAreaId && "opacity-40 grayscale pointer-events-none"
                )}>
                    <div className="flex items-center justify-between pl-1 mb-0.5">
                        <span className="text-[9px] font-black text-slate-400 uppercase tracking-widest">Section</span>
                        {!selectedSectionId && selectedAreaId && (
                            <span className="text-[9px] font-bold text-primary animate-pulse uppercase tracking-tighter">◀ Select One</span>
                        )}
                    </div>

                    <div className="flex flex-col gap-2 max-h-[160px] overflow-y-auto pr-1">
                        {!selectedAreaId ? (
                            <div className="flex flex-col items-center justify-center py-6 w-full border border-dashed border-slate-200 rounded-xl bg-slate-50/50">
                                <span className="text-[10px] font-bold text-slate-400/70">Select Area First</span>
                            </div>
                        ) : sections?.length ? (
                            sections.map(section => (
                                <button
                                    key={section.id}
                                    onClick={() => onSectionSelect(section.id)}
                                    className={cn(
                                        "w-full py-2 px-3 rounded-lg text-[11px] font-bold text-left transition-all border whitespace-nowrap",
                                        selectedSectionId === section.id
                                            ? "bg-primary border-primary text-primary-foreground shadow-sm ring-2 ring-primary/10"
                                            : "bg-white border-slate-200 text-slate-500 hover:border-slate-300"
                                    )}
                                >
                                    {section.name}
                                </button>
                            ))
                        ) : (
                            <div className="text-[10px] text-slate-400 italic py-2 text-center">Loading...</div>
                        )}
                    </div>
                </div>
            </div>
        </header>
    );
});
