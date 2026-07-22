import type { Metadata } from "next";
import { Inter } from "next/font/google";
import { BackgroundPills } from "../components/ui/BackgroundPills";
import { ScrollPerspective } from "../components/ui/ScrollPerspective";
import "./globals.css";

const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin"],
  display: "swap",
});

export const metadata: Metadata = {
  title: "Synco — Local-First Sync System for Android & Desktop",
  description:
    "Synco is a local-first, zero-trust system that connects your Android phone and desktop computer into a single synchronized control surface. No cloud, no lag, complete privacy.",
  keywords: [
    "synco",
    "synco app",
    "synco download",
    "local-first sync",
    "zero-trust system",
    "Android desktop sync",
    "offline sync tool",
    "phone desktop synchronization",
    "privacy first sync",
    "notification mirroring",
  ],
  robots: {
    index: true,
    follow: true,
    "max-snippet": -1,
    "max-image-preview": "large",
    "max-video-preview": -1,
  },
  openGraph: {
    type: "website",
    locale: "en_US",
    url: "https://synco.kesug.com/",
    siteName: "Synco",
    title: "Synco — Local-First Sync System for Android & Desktop",
    description:
      "Synco connects your Android phone and desktop into one unified system. Notifications, media, calls — all synced locally with zero cloud.",
    images: [
      {
        url: "https://synco.kesug.com/icon1.png",
        width: 48,
        height: 48,
        alt: "Synco logo",
      },
    ],
  },
  twitter: {
    card: "summary_large_image",
    title: "Synco — Local-First Sync System for Android & Desktop",
    description:
      "Synco connects your Android phone and desktop into one unified system. Notifications, media, calls — all synced locally with zero cloud.",
  },
  verification: {
    google: "googlec4ecc835fab689b2.html",
  },
  alternates: {
    canonical: "https://synco.kesug.com/",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className={`${inter.variable} h-full antialiased`}>
      <head>
        <script async src="https://www.googletagmanager.com/gtag/js?id=G-ZJW7XYEW8B" />
        <script
          dangerouslySetInnerHTML={{
            __html: `window.dataLayer=window.dataLayer||[];function gtag(){dataLayer.push(arguments)}gtag('js',new Date());gtag('config','G-ZJW7XYEW8B');`,
          }}
        />
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{
            __html: JSON.stringify({
              "@context": "https://schema.org",
              "@type": "SoftwareApplication",
              name: "Synco",
              applicationCategory: "UtilityApplication",
              operatingSystem: "Android, Windows, macOS, Linux",
              description:
                "Synco is a local-first, zero-trust system that connects your Android phone and desktop computer into a single synchronized control surface. No cloud, no lag, complete privacy.",
              offers: {
                "@type": "Offer",
                price: "0",
                priceCurrency: "USD",
              },
              author: {
                "@type": "Organization",
                name: "Synco Inc.",
              },
            }),
          }}
        />
      </head>
      <body className="relative min-h-full flex flex-col bg-black">
        {/*
          Global ambient background — fixed pill shapes behind entire site.
          z-0 · pointer-events-none · never scrolls with page.
        */}
        <BackgroundPills />

        {/*
          Scroll-driven 3D perspective wrapper.
          Gives the whole site a subtle "page rises to meet you" tilt
          that flattens as the user scrolls down.
          z-10 sits above the fixed pill background.
        */}
        <div className="relative z-10 flex flex-col flex-1">
          <ScrollPerspective>
            {children}
          </ScrollPerspective>
        </div>
      </body>
    </html>
  );
}
