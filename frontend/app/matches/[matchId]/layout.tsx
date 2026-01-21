import type { Metadata } from 'next';

// Static metadata for seat selection pages
export const metadata: Metadata = {
    title: 'Select Seats | Ticketing Service',
    description: 'Choose your seats for the match',
};

export default function MatchDetailLayout({
    children,
}: {
    children: React.ReactNode;
}) {
    return children;
}
