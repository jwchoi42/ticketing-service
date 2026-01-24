import type { Metadata, Viewport } from "next";
// import localFont from "next/font/local";
import "./globals.css";
import QueryProvider from "@/components/providers/query-provider";
import { Toaster } from "sonner";

import { Header } from "@/components/layout/header";
import { MobileNav } from "@/components/layout/mobile-nav";

// Extracted to constant to prevent object recreation on every render
const TOAST_OPTIONS = {
  classNames: {
    icon: 'text-primary',
  }
} as const;

export const metadata: Metadata = {
  title: "Ticketing Service",
  description: "Real-time ticketing service frontend",
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  maximumScale: 1,
  userScalable: false,
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body className="antialiased min-h-screen bg-muted/30">
        <QueryProvider>
          <div className="mx-auto w-full max-w-[480px] min-h-screen bg-background shadow-2xl relative border-x border-border/40">
            <Header />
            <main>
              {children}
            </main>
            <MobileNav />
          </div>
          <Toaster
            position="top-center"
            toastOptions={TOAST_OPTIONS}
          />
        </QueryProvider>
      </body>
    </html>
  );
}
