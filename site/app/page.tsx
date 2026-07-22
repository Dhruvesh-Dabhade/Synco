"use client";

import {
  Navbar,
  Hero,
  MacOSMenuBar,
  InboxMockup,
} from "@/components/sections/LandingPage";
import {
  FeatureTriage,
  ProtocolSpecs,
  Testimonials,
  Pricing,
  FinalCTA,
} from "@/components/sections/LandingSections";

export default function Home() {
  return (
    <div className="relative min-h-screen overflow-x-hidden bg-[#0c0c0c] text-white">
      {/* Background Video - Fixed Full Screen */}
      <div className="fixed inset-0 z-0 pointer-events-none">
        <video
          autoPlay
          loop
          muted
          playsInline
          className="w-full h-full object-cover pointer-events-none"
          src="https://d8j0ntlcm91z4.cloudfront.net/user_38xzZboKViGWJOttwIXH07lWA1P/hf_20260508_064122_c4750c0e-7476-4b44-94a2-a85a65c63bf2.mp4"
        />
      </div>

      {/* Vertical Guide Lines (hidden on mobile) */}
      <div className="hidden md:block pointer-events-none fixed inset-y-0 left-1/2 -translate-x-[calc(50%+36rem)] w-px bg-white/10 z-[5]" />
      <div className="hidden md:block pointer-events-none fixed inset-y-0 left-1/2 translate-x-[calc(-50%+36rem)] w-px bg-white/10 z-[5]" />

      {/* Global SVG Noise Filter for Hero */}
      <svg
        style={{ position: "absolute", width: 0, height: 0 }}
        aria-hidden="true"
      >
        <filter id="c3-noise">
          <feTurbulence
            type="fractalNoise"
            baseFrequency="0.9"
            numOctaves="2"
            stitchTiles="stitch"
          />
          <feColorMatrix
            type="matrix"
            values="0 0 0 0 0  0 0 0 0 0  0 0 0 0 0  0 0 0 0.35 0"
          />
          <feComposite in2="SourceGraphic" operator="in" result="noise" />
          <feBlend in="SourceGraphic" in2="noise" mode="multiply" />
        </filter>
      </svg>

      {/* Content - relative z-10 to layer above video */}
      <div className="relative z-10">
        {/* Navbar */}
        <Navbar />

        {/* Hero Section */}
        <Hero />

        {/* macOS Menu Bar */}
        <MacOSMenuBar />

        {/* Inbox Mockup */}
        <InboxMockup />

        {/* Feature Triage */}
        <FeatureTriage />

        {/* Protocol Specs */}
        <ProtocolSpecs />

        {/* Testimonials */}
        <Testimonials />

        {/* Pricing */}
        <Pricing />

        {/* Final CTA */}
        <FinalCTA />

        {/* Footer */}
        <footer className="w-full bg-black/60 backdrop-blur-sm text-zinc-500 py-16 px-6 border-t border-white/5">
          <div className="max-w-6xl mx-auto flex flex-col gap-8">
            <div className="grid grid-cols-2 md:grid-cols-4 gap-8 text-[14px]">
              <div className="flex flex-col gap-3">
                <h4 className="font-semibold text-white">Product</h4>
                <a href="#" className="text-white/60 hover:text-white transition-apple">
                  Overview
                </a>
                <a href="#" className="text-white/60 hover:text-white transition-apple">
                  Features
                </a>
                <a href="#" className="text-white/60 hover:text-white transition-apple">
                  Security specs
                </a>
                <a href="#" className="text-white/60 hover:text-white transition-apple">
                  Releases
                </a>
              </div>
              <div className="flex flex-col gap-3">
                <h4 className="font-semibold text-white">Technology</h4>
                <a href="#" className="text-white/60 hover:text-white transition-apple">
                  Local-first sync
                </a>
                <a href="#" className="text-white/60 hover:text-white transition-apple">
                  Zero-trust architecture
                </a>
                <a href="#" className="text-white/60 hover:text-white transition-apple">
                  E2EE protocol
                </a>
                <a href="#" className="text-white/60 hover:text-white transition-apple">
                  Latency profiles
                </a>
              </div>
              <div className="flex flex-col gap-3">
                <h4 className="font-semibold text-white">Company</h4>
                <a href="#" className="text-white/60 hover:text-white transition-apple">
                  About us
                </a>
                <a href="#" className="text-white/60 hover:text-white transition-apple">
                  Press kit
                </a>
                <a href="#" className="text-white/60 hover:text-white transition-apple">
                  Contact
                </a>
              </div>
              <div className="flex flex-col gap-3">
                <h4 className="font-semibold text-white">Legal</h4>
                <a href="#" className="text-white/60 hover:text-white transition-apple">
                  Privacy policy
                </a>
                <a href="#" className="text-white/60 hover:text-white transition-apple">
                  Terms of service
                </a>
                <a href="#" className="text-white/60 hover:text-white transition-apple">
                  License agreement
                </a>
              </div>
            </div>
            <hr className="border-white/10" />
            <div className="flex flex-col md:flex-row items-center justify-between text-[12px] gap-4">
              <p className="text-white/50">
                Copyright © {new Date().getFullYear()} Synco Inc. All rights
                reserved.
              </p>
              <div className="flex gap-4 text-white/50">
                <a href="#" className="hover:text-white transition-apple">
                  Privacy Policy
                </a>
                <span>|</span>
                <a href="#" className="hover:text-white transition-apple">
                  Terms of Use
                </a>
                <span>|</span>
                <a href="#" className="hover:text-white transition-apple">
                  Sales Policy
                </a>
              </div>
            </div>
          </div>
        </footer>
      </div>
    </div>
  );
}
