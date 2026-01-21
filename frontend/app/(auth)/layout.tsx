import type { Metadata } from 'next';

export const metadata: Metadata = {
    robots: {
        index: false, // Don't index auth pages
    },
};

export default function AuthLayout({
    children,
}: {
    children: React.ReactNode;
}) {
    return children;
}
