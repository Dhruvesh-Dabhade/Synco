# Synco — Technical Specification

This document outlines the technical stack, architectural patterns, and development guidelines for the Synco landing page and interactive interface.

## Tech Stack

### 1. Framework
- **Next.js (App Router)**: Utilizing Next.js 16/17 and React 19 for rendering, SEO, page loading speed, and modern component lifecycle.

### 2. Styling
- **Tailwind CSS v4**: Leveraging the new CSS-based styling system (`@theme` in CSS, native variable injection) to manage the design tokens specified in [apple-design.md](apple-design.md).
- Custom variables mapped in CSS for pixel-perfect Apple typography and spacing ratios.

### 3. Motion & Animation
- **Framer Motion**: Used for high-end micro-interactions, scroll-driven interactive state revelations, and transitions between synchronized device states.

### 4. Interactive 3D (Future integration)
- **Three.js & React Three Fiber (@react-three/fiber)**: Used to render real-time interactive 3D models of the Android phone and Desktop PC, demonstrating real-time synchronized state transformations.

## Core Architectural Guidelines

### 1. Modular Directory Structure
To prevent code sprawl and keep components highly reusable, we enforce a strict separation of concerns:
- `components/ui/`: Dumb, atomic, reusable primitives (e.g., Buttons, Inputs, Dividers, Badges) that are highly customizable via props.
- `components/sections/`: High-level layout blocks composing the page (e.g., Hero, FeatureGrid, Navigation, Footer).
- `lib/`: Helper utilities, hooks, and logic (e.g., state matching, audio routing, mathematical helpers).
- `docs/`: Technical and design specifications.

### 2. Styling Conventions
- Never inline color hex values or hardcode dimensions that break the spacing grid. Use semantic Tailwind utility classes mapped to our Apple design tokens.
- Maintain a strict dark/light surface rhythm. Product tiles alternate light (`#ffffff` / `#f5f5f7`) and dark (`#272729` / `#000000`) backgrounds.

### 3. Motion & Performance
- Maintain a smooth 60fps on mobile.
- Use GPU-accelerated CSS properties (`transform`, `opacity`) for animations in Framer Motion.
- Lazy-load heavy components (like 3D canvas rendering with React Three Fiber) using Next.js dynamic imports (`next/dynamic`) to optimize PageSpeed and SEO rankings.
