"use client";

import React, { useRef } from "react";
import { useScroll, useTransform, motion } from "framer-motion";

/**
 * ScrollPerspective
 *
 * Wraps site content in a very subtle scroll-driven 3D perspective tilt.
 * As the user scrolls from top → bottom:
 *   - rotateX goes from a slight forward tilt (3deg) → flat (0deg)
 *   - scale goes from 0.98 → 1.0
 *
 * This creates the premium "the page rises to meet you" feel without
 * the heavy height overhead of ContainerScroll per section.
 */
export function ScrollPerspective({ children }: { children: React.ReactNode }) {
  const ref = useRef<HTMLDivElement>(null);

  const { scrollYProgress } = useScroll({
    // Track global document scroll
    offset: ["start start", "end end"],
  });

  // Subtle tilt: starts slightly angled, flattens as you scroll
  const rotateX = useTransform(scrollYProgress, [0, 0.15], [3, 0]);

  // Slight scale-up for the "zoom in as content reveals" feel
  const scale = useTransform(scrollYProgress, [0, 0.15], [0.985, 1]);

  // Faint y lift — content feels like it's rising toward the viewer
  const y = useTransform(scrollYProgress, [0, 0.15], [8, 0]);

  return (
    // Outer div provides the perspective depth
    <div
      ref={ref}
      style={{
        perspective: "1200px",
        perspectiveOrigin: "50% 0%",
      }}
    >
      <motion.div
        style={{
          rotateX,
          scale,
          y,
          // Ensure transform-origin is top-center so tilt anchors at the top
          transformOrigin: "50% 0%",
        }}
      >
        {children}
      </motion.div>
    </div>
  );
}
