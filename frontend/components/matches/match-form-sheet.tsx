'use client';

import { useState, useEffect } from 'react';
import { BottomSheet } from '@/components/ui/bottom-sheet';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Button } from '@/components/ui/button';
import { adminApi, CreateMatchRequest, UpdateMatchRequest } from '@/lib/api/admin';
import { Match } from '@/lib/api/matches';
import { toast } from 'sonner';

interface MatchFormSheetProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
    initialData?: Match | null; // If provided, it's Edit mode
}

export function MatchFormSheet({ isOpen, onClose, onSuccess, initialData }: MatchFormSheetProps) {
    const isEditMode = !!initialData;
    const [isLoading, setIsLoading] = useState(false);

    // Form State
    const [homeTeam, setHomeTeam] = useState('');
    const [awayTeam, setAwayTeam] = useState('');
    const [stadiumName, setStadiumName] = useState('');
    const [matchDate, setMatchDate] = useState(''); // Text input for simplicity, ideally DatePicker

    // Reset or Populate form on open
    useEffect(() => {
        if (isOpen) {
            if (initialData) {
                setHomeTeam(initialData.homeTeam);
                setAwayTeam(initialData.awayTeam);
                setStadiumName(initialData.stadium);
                setMatchDate(initialData.dateTime);
            } else {
                setHomeTeam('');
                setAwayTeam('');
                setStadiumName('');
                setMatchDate('');
            }
        }
    }, [isOpen, initialData]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsLoading(true);

        try {
            // Format date to ISO-8601 if needed (replace space with T)
            const formattedDate = matchDate.includes('T') ? matchDate : matchDate.replace(' ', 'T');

            if (isEditMode && initialData) {
                const updateData: UpdateMatchRequest = {
                    homeTeam,
                    awayTeam,
                    stadium: stadiumName,
                    dateTime: formattedDate
                };
                await adminApi.updateMatch(initialData.id, updateData);
                toast.success('Match updated successfully');
            } else {
                const createData: CreateMatchRequest = {
                    homeTeam,
                    awayTeam,
                    stadium: stadiumName,
                    dateTime: formattedDate
                };
                await adminApi.createMatch(createData);
                toast.success('Match created successfully');
            }
            onSuccess();
            onClose();
        } catch (error) {
            console.error('Failed to save match:', error);
            toast.error('Failed to save match');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <BottomSheet
            isOpen={isOpen}
            onClose={onClose}
            title={isEditMode ? 'Edit Match' : 'Create Match'}
        >
            <form onSubmit={handleSubmit} className="space-y-4">
                <div className="space-y-2">
                    <Label htmlFor="homeTeam">Home Team</Label>
                    <Input
                        id="homeTeam"
                        value={homeTeam}
                        onChange={(e) => setHomeTeam(e.target.value)}
                        placeholder="e.g. Bears"
                        required
                    />
                </div>

                <div className="space-y-2">
                    <Label htmlFor="awayTeam">Away Team</Label>
                    <Input
                        id="awayTeam"
                        value={awayTeam}
                        onChange={(e) => setAwayTeam(e.target.value)}
                        placeholder="e.g. Twins"
                        required
                    />
                </div>

                <div className="space-y-2">
                    <Label htmlFor="stadium">Stadium</Label>
                    <Input
                        id="stadium"
                        value={stadiumName}
                        onChange={(e) => setStadiumName(e.target.value)}
                        placeholder="e.g. Jamsil Baseball Stadium"
                        required
                    />
                </div>

                <div className="space-y-2">
                    <Label htmlFor="date">Date & Time (YYYY-MM-DD HH:MM:SS)</Label>
                    <Input
                        id="date"
                        value={matchDate}
                        onChange={(e) => setMatchDate(e.target.value)}
                        placeholder="2025-05-15T18:30:00"
                        required
                    />
                </div>

                <div className="pt-4">
                    <Button type="submit" className="w-full" disabled={isLoading}>
                        {isLoading ? 'Saving...' : (isEditMode ? 'Update Match' : 'Create Match')}
                    </Button>
                </div>
            </form>
        </BottomSheet>
    );
}
