import type { Metadata } from "next";
// import localFont from "next/font/local";
import "./globals.css";
import QueryProvider from "@/components/providers/query-provider";
import { Toaster } from "sonner";

import { Header } from "@/components/layout/header";
import { MobileNav } from "@/components/layout/mobile-nav";

export const metadata: Metadata = {
  title: "Ticketing Service",
  description: "Real-time ticketing service frontend",
  viewport: {
    width: "device-width",
    initialScale: 1,
    maximumScale: 1,
    userScalable: false,
  },
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
            toastOptions={{
              classNames: {
                icon: 'text-primary',
              }
            }}
          />
        </QueryProvider>
      </body>
    </html>
  );
}
