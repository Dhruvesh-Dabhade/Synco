"use client";

import React from "react";
import { ChevronRight } from "lucide-react";

/**
 * SyncoLogo – Custom technical gear/S symbol SVG
 */
export const SyncoLogo = ({ className = "w-4 h-4" }: { className?: string }) => (
  <svg viewBox="0 0 512 512" className={className} fill="none" xmlns="http://www.w3.org/2000/svg">
    <defs>
      {/* Background Gradient */}
      <linearGradient id="bgGradient" x1="0" y1="0" x2="512" y2="512" gradientUnits="userSpaceOnUse">
        <stop offset="0%" stopColor="#0A0A0B" />
        <stop offset="100%" stopColor="#1A1A22" />
      </linearGradient>
    </defs>

    {/* Background (with sleek rounded corners) */}
    <rect width="512" height="512" rx="112" fill="url(#bgGradient)" />

    {/* Subtle circular border */}
    <path
      d="M256,58a198,198 0,1 0,0 396a198,198 0,1 0,0 -396Z"
      stroke="#1A1A1E"
      strokeWidth="4"
      fill="none" />

    {/* Gear Frame with Cyan Stroke */}
    <path
      d="M256,100C248.8,100 242.4,105.6 240.8,112.8L236.8,132.8C222.4,136 208.8,141.6 196.8,149.6L180,137.6C174.4,133.6 166.4,134.4 161.6,139.2L139.2,161.6C134.4,166.4 133.6,174.4 137.6,180L149.6,196.8C141.6,208.8 136,222.4 132.8,236.8L112.8,240.8C105.6,242.4 100,248.8 100,256C100,263.2 105.6,269.6 112.8,271.2L132.8,275.2C136,289.6 141.6,303.2 149.6,315.2L137.6,332C133.6,337.6 134.4,345.6 139.2,350.4L161.6,372.8C166.4,377.6 174.4,378.4 180,374.4L196.8,362.4C208.8,370.4 222.4,376 236.8,379.2L240.8,399.2C242.4,406.4 248.8,412 256,412C263.2,412 269.6,406.4 271.2,399.2L275.2,379.2C289.6,376 303.2,370.4 315.2,362.4L332,374.4C337.6,378.4 345.6,377.6 350.4,372.8L372.8,350.4C377.6,345.6 378.4,337.6 374.4,332L362.4,315.2C370.4,303.2 376,289.6 379.2,275.2L399.2,271.2C406.4,269.6 412,263.2 412,256C412,248.8 406.4,242.4 399.2,240.8L379.2,236.8C376,222.4 370.4,208.8 362.4,196.8L374.4,180C378.4,174.4 377.6,166.4 372.8,161.6L350.4,139.2C345.6,134.4 337.6,133.6 332,137.6L315.2,149.6C303.2,141.6 289.6,136 275.2,132.8L271.2,112.8C269.6,105.6 263.2,100 256,100Z"
      stroke="#00F0FF"
      strokeWidth="4"
      strokeLinecap="round"
      strokeLinejoin="round"
      fill="none" />

    {/* Thin inner circular details */}
    <path
      d="M256,160C202.981,160 160,202.981 160,256C160,309.019 202.981,352 256,352C309.019,352 352,309.019 352,256C352,202.981 309.019,160 256,160Z"
      stroke="#00F0FF"
      strokeWidth="2"
      strokeOpacity="0.3"
      fill="none" />

    {/* Glow for Technical S */}
    <path
      d="M310,210C310,190 290,180 260,180C220,180 200,200 200,220C200,240 220,250 260,260C300,270 320,280 320,310C320,340 290,360 250,360C210,360 190,340 190,320"
      stroke="#00F0FF"
      strokeWidth="24"
      strokeOpacity="0.15"
      strokeLinecap="round"
      strokeLinejoin="round"
      fill="none" />

    {/* Main Technical S */}
    <path
      d="M310,210C310,190 290,180 260,180C220,180 200,200 200,220C200,240 220,250 260,260C300,270 320,280 320,310C320,340 290,360 250,360C210,360 190,340 190,320"
      stroke="#00F0FF"
      strokeWidth="14"
      strokeLinecap="round"
      strokeLinejoin="round"
      fill="none" />

    {/* Circuit Dot on S (Top) */}
    <path
      d="M310,204a6,6 0,1 0,0 12a6,6 0,1 0,0 -12Z"
      fill="#0A0A0B"
      stroke="#00F0FF"
      strokeWidth="3" />

    {/* Circuit Dot on S (Bottom) */}
    <path
      d="M190,314a6,6 0,1 0,0 12a6,6 0,1 0,0 -12Z"
      fill="#0A0A0B"
      stroke="#00F0FF"
      strokeWidth="3" />

    {/* Circuit Dot on S (Center) */}
    <path
      d="M260,266a4,4 0,1 0,0 8a4,4 0,1 0,0 -8Z"
      fill="#00F0FF" />

    {/* Inner Detail Connection Lines */}
    <path
      d="M240,255L280,265"
      stroke="#00F0FF"
      strokeWidth="2"
      strokeOpacity="0.5" />
  </svg>
);

// Backward-compatible alias for any residual imports
export const AppleLogo = SyncoLogo;

/**
 * LogoMark – abstract 4-quadrant curve mark
 */
export const LogoMark = ({ className = "w-8 h-8" }: { className?: string }) => (
  <svg viewBox="0 0 256 256" className={className} fill="currentColor">
    <path d="M 0 128 C 70.692 128 128 185.308 128 256 L 64 256 C 64 220.654 35.346 192 0 192 Z M 256 192 C 220.654 192 192 220.654 192 256 L 128 256 C 128 185.308 185.308 128 256 128 Z M 128 0 C 128 70.692 70.692 128 0 128 L 0 64 C 35.346 64 64 35.346 64 0 Z M 192 0 C 192 35.346 220.654 64 256 64 L 256 128 C 185.308 128 128 70.692 128 0 Z" />
  </svg>
);

/**
 * SyncoButton – rounded-full white pill with Synco logo + label + chevron
 */
export const SyncoButton = ({
  label = "Download Synco",
  full = false,
  className = "",
}: {
  label?: string;
  full?: boolean;
  className?: string;
}) => (
  <a
    href="https://github.com/Dhruvesh-Dabhade/Synco"
    target="_blank"
    rel="noopener noreferrer"
    className={`group inline-flex items-center justify-center gap-2 rounded-full bg-white text-black font-medium text-sm px-5 py-3 transition-all hover:bg-white/90 active:scale-[0.98] no-underline ${
      full ? "w-full" : ""
    } ${className}`}
  >
    <SyncoLogo className="w-4 h-4" />
    <span>{label}</span>
    <ChevronRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
  </a>
);

export const AppleButton = SyncoButton;

/**
 * SectionEyebrow – small tag with optional pill badge
 */
export const SectionEyebrow = ({
  label,
  tag,
}: {
  label: string;
  tag?: string;
}) => (
  <div className="flex items-center gap-2">
    <span className="w-1.5 h-1.5 rounded-full bg-white" />
    <span className="text-xs text-white/70 font-medium uppercase tracking-wide">
      {label}
    </span>
    {tag && (
      <span className="px-2 py-0.5 rounded-full border border-white/10 text-white/50 text-xs">
        {tag}
      </span>
    )}
  </div>
);

/**
 * Gradient style for shiny animated text (used on "Revitalized")
 */
export const gradientStyle = {
  backgroundImage:
    "linear-gradient(to right, #091020 0%, #0B2551 12.5%, #A4F4FD 32.5%, #00d2ff 50%, #0B2551 67.5%, #091020 87.5%, #091020 100%)",
  backgroundSize: "200% auto",
  WebkitBackgroundClip: "text",
  backgroundClip: "text",
  color: "transparent",
  WebkitTextFillColor: "transparent",
  filter: "url(#c3-noise)",
};
