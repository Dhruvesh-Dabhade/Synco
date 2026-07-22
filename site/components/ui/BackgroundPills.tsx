"use client";

import React from "react";
import { motion } from "framer-motion";

// Each pill definition: position, size, rotation, gradient colours, animation
const PILLS = [
  // Top-left — teal/green
  {
    id: 0,
    top: "8%",
    left: "-4%",
    width: 420,
    height: 90,
    rotate: -18,
    colors: ["#0d9488", "#134e4a"],
    delay: 0,
    duration: 12,
    opacity: 0.28,
  },
  // Top-centre-right — indigo/purple
  {
    id: 1,
    top: "5%",
    left: "42%",
    width: 340,
    height: 76,
    rotate: -8,
    colors: ["#4f46e5", "#1e1b4b"],
    delay: 1.5,
    duration: 15,
    opacity: 0.22,
  },
  // Top-right — rose/pink
  {
    id: 2,
    top: "12%",
    right: "-3%",
    width: 380,
    height: 84,
    rotate: 14,
    colors: ["#be185d", "#4c0519"],
    delay: 0.8,
    duration: 14,
    opacity: 0.20,
  },
  // Mid-left — dark teal
  {
    id: 3,
    top: "44%",
    left: "-8%",
    width: 460,
    height: 96,
    rotate: 10,
    colors: ["#0e7490", "#083344"],
    delay: 2,
    duration: 16,
    opacity: 0.18,
  },
  // Mid-right — violet
  {
    id: 4,
    top: "50%",
    right: "-6%",
    width: 400,
    height: 88,
    rotate: -22,
    colors: ["#7c3aed", "#2e1065"],
    delay: 0.4,
    duration: 13,
    opacity: 0.20,
  },
  // Bottom-left — emerald
  {
    id: 5,
    bottom: "18%",
    left: "8%",
    width: 360,
    height: 80,
    rotate: 5,
    colors: ["#059669", "#022c22"],
    delay: 1.2,
    duration: 17,
    opacity: 0.18,
  },
  // Bottom-right — fuchsia
  {
    id: 6,
    bottom: "10%",
    right: "5%",
    width: 440,
    height: 92,
    rotate: -12,
    colors: ["#a21caf", "#3b0764"],
    delay: 2.5,
    duration: 14,
    opacity: 0.20,
  },
  // Extra: centre-top small accent — cyan
  {
    id: 7,
    top: "28%",
    left: "30%",
    width: 260,
    height: 60,
    rotate: 30,
    colors: ["#0891b2", "#164e63"],
    delay: 3,
    duration: 18,
    opacity: 0.13,
  },
];

export function BackgroundPills() {
  return (
    // fixed: stays behind the entire site as you scroll
    // pointer-events-none: never intercepts clicks
    // z-0: above the html bg but below all page content
    <div
      aria-hidden="true"
      style={{
        position: "fixed",
        inset: 0,
        zIndex: 0,
        pointerEvents: "none",
        overflow: "hidden",
        background: "#000000",  // site-wide base canvas
      }}
    >
      {PILLS.map((pill) => (
        <motion.div
          key={pill.id}
          style={{
            position: "absolute",
            top:    pill.top,
            left:   pill.left,
            right:  (pill as any).right,
            bottom: (pill as any).bottom,
            width:  pill.width,
            height: pill.height,
            borderRadius: "9999px",
            // Linear gradient simulating studio gloss on a dark pill
            background: `linear-gradient(135deg, ${pill.colors[0]} 0%, ${pill.colors[1]} 100%)`,
            opacity: pill.opacity,
            // Soft glow halo — simulates photographic light bleed
            filter: "blur(2px)",
            transform: `rotate(${pill.rotate}deg)`,
            transformOrigin: "center center",
          }}
          // Calm vertical float loop
          animate={{
            y: [0, -18, 0],
            opacity: [pill.opacity, pill.opacity * 0.75, pill.opacity],
          }}
          transition={{
            duration: pill.duration,
            delay: pill.delay,
            repeat: Infinity,
            ease: "easeInOut",
          }}
        />
      ))}

      {/* Very faint global radial vignette to deepen edges */}
      <div
        style={{
          position: "absolute",
          inset: 0,
          background:
            "radial-gradient(ellipse at 50% 40%, transparent 30%, rgba(0,0,0,0.6) 100%)",
        }}
      />
    </div>
  );
}
