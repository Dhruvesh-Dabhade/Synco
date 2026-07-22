"use client";

import React from "react";
import { motion } from "framer-motion";
import { ChevronRight, Sparkles } from "lucide-react";
import { SectionEyebrow, SyncoButton, gradientStyle } from "@/components/ui/primitives";

// ─── FEATURE TRIAGE ──────────────────────────────────────────────────────
const FeatureTriage = () => {
  return (
    <section id="solutions" className="max-w-6xl mx-auto px-6 py-20 md:py-28 border-t border-white/10">
      <div className="grid md:grid-cols-2 gap-10 md:gap-16 items-start">
        {/* Left Column */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7 }}
          viewport={{ once: true }}
          className="space-y-6"
        >
          <SectionEyebrow label="Continuity" tag="Local-First" />
          <h2 className="text-3xl md:text-5xl font-semibold tracking-tight leading-[1.02]">
            Synco: Control your phone{" "}
            <br />
            without reaching for it.
          </h2>
          <p className="text-white/60 text-base leading-[1.6] max-w-md">
            Synco links your Android phone and desktop into a single system. Mirror notifications,
            control media playback, and manage incoming calls directly from your keyboard and mouse,
            fully offline over your local Wi-Fi.
          </p>

          {/* Chips */}
          <div className="flex flex-wrap gap-2 pt-4">
            {[
              "Notification Mirroring",
              "Real-time Call Handling",
              "Media Telemetry & Control",
              "Audio Routing Intelligence",
            ].map((chip) => (
              <span
                key={chip}
                className="text-xs text-white/70 px-3 py-1.5 rounded-full border border-white/10 bg-white/[0.03]"
              >
                {chip}
              </span>
            ))}
          </div>
        </motion.div>

        {/* Right Column - Card */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7, delay: 0.1 }}
          viewport={{ once: true }}
          className="liquid-glass rounded-2xl p-5 space-y-3"
        >
          <div className="text-xs text-white/60">Today · 154 state events synchronized</div>

          {[
            {
              title: "Audio Routing",
              count: "Active",
              color: "#ffffff",
              items: [
                "Spotify — Blinding Lights",
                "Output Device: Desktop Speakers",
              ],
            },
            {
              title: "Notification Hub",
              count: 3,
              color: "#e5e5e5",
              items: ["WhatsApp — Priya Sharma", "Slack — #design-sync"],
            },
            {
              title: "Bluetooth Awareness",
              count: "2 connected",
              color: "#a3a3a3",
              items: ["Sony WH-1000XM4 — 80% battery", "Pixel Watch — Connected"],
            },
            {
              title: "System Security",
              count: "Verified",
              color: "#525252",
              items: ["Zero-Trust Session — Ephemeral Key", "End-to-End Encrypted"],
            },
          ].map((category, i) => (
            <div key={category.title} className="liquid-glass rounded-lg p-3">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-semibold text-white">
                  {category.title}
                </span>
                <span
                  className="text-xs font-bold"
                  style={{ color: category.color }}
                >
                  {category.count}
                </span>
              </div>
              <div className="space-y-1">
                {category.items.map((item) => (
                  <div key={item} className="text-xs text-white/50 truncate">
                    {item}
                  </div>
                ))}
              </div>
            </div>
          ))}
        </motion.div>
      </div>
    </section>
  );
};

// ─── PROTOCOL SPECS & NETWORK LATENCY ─────────────────────────────────────
const ProtocolSpecs = () => {
  const specs = [
    {
      title: "Direct Hotspot",
      latency: "0.4ms",
      desc: "Lowest latency profile. Direct point-to-point connection bypassing local routers.",
      color: "from-cyan-400 to-blue-500",
      bandwidth: "800+ Mbps",
    },
    {
      title: "Local Wi-Fi",
      latency: "0.8ms",
      desc: "Standard LAN connection. Seamless state mirroring through your existing home/office router.",
      color: "from-purple-500 to-indigo-500",
      bandwidth: "300+ Mbps",
    },
    {
      title: "Bluetooth LE",
      latency: "1.2ms",
      desc: "Ultra-low power connection for notifications, battery telemetry, and proximity status.",
      color: "from-emerald-400 to-teal-500",
      bandwidth: "2 Mbps",
    },
    {
      title: "Cryptographic Tunnel",
      latency: "<0.1ms",
      desc: "E2EE session verification using Curve25519 (ECDH) & AES-256-GCM. 100% cloud-free.",
      color: "from-amber-400 to-orange-500",
      bandwidth: "Secure Session",
    },
  ];

  return (
    <section id="documentation" className="max-w-6xl mx-auto px-6 py-16 md:py-24 border-t border-white/10">
      <div className="text-center mb-16 space-y-3">
        <SectionEyebrow label="Protocol & Telemetry" tag="Cloud-Free" />
        <h2 className="text-3xl md:text-5xl font-semibold tracking-tight text-white mt-4">
          Synco Protocol: Uncompromising latency. Total privacy.
        </h2>
        <p className="text-white/60 text-sm max-w-lg mx-auto">
          Every device state transition, notification mirroring, and audio route decision happens 
          locally with zero internet round-trips.
        </p>
      </div>

      <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
        {specs.map((spec, i) => (
          <motion.div
            key={spec.title}
            initial={{ opacity: 0, y: 15 }}
            whileInView={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: i * 0.1 }}
            viewport={{ once: true }}
            className="liquid-glass rounded-2xl p-6 flex flex-col justify-between hover:border-cyan-500/20 transition-all group duration-300"
          >
            <div>
              <div className="flex items-center justify-between mb-4">
                <span className="text-xs font-semibold text-white/50">{spec.title}</span>
                <span className={`text-[10px] font-bold px-2 py-0.5 rounded bg-white/5 text-white/80`}>
                  {spec.bandwidth}
                </span>
              </div>
              <div className="text-4xl font-extrabold tracking-tight text-white mb-2 group-hover:text-cyan-400 transition-colors">
                {spec.latency}
              </div>
              <p className="text-white/55 text-xs leading-relaxed">
                {spec.desc}
              </p>
            </div>
            <div className="mt-6 h-1 w-full bg-white/5 rounded-full overflow-hidden">
              <div className={`h-full bg-gradient-to-r ${spec.color} w-3/4 rounded-full`} />
            </div>
          </motion.div>
        ))}
      </div>
    </section>
  );
};

// ─── TESTIMONIALS ────────────────────────────────────────────────────────
const Testimonials = () => {
  const testimonials = [
    {
      quote:
        "Synco gave our leadership team four hours of their week back. It feels like control technology from the future.",
      name: "Parker Wilf",
      role: "Group Product Manager",
      company: "MERCURY",
    },
    {
      quote:
        "The real-time notification mirroring and call handling are seamless. I no longer reach for my phone during deep work.",
      name: "Andrew von Rosenbach",
      role: "Senior Engineering Program Manager",
      company: "COHERE",
    },
    {
      quote:
        "Offline-first synchronization that actually works instantly. Zero cloud setup means our corporate security team approved it immediately.",
      name: "Mathies Christensen",
      role: "Engineering Manager",
      company: "LUNAR",
    },
  ];

  return (
    <section id="blog" className="max-w-6xl mx-auto px-6 py-20 md:py-28 border-t border-white/10">
      <div className="grid md:grid-cols-3 gap-8">
        {testimonials.map((testimonial, i) => (
          <motion.figure
            key={i}
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, delay: i * 0.1 }}
            viewport={{ once: true }}
            className="liquid-glass rounded-2xl p-6"
          >
            <blockquote className="text-sm text-white/80 leading-[1.6] mb-4">
              "{testimonial.quote}"
            </blockquote>
            <figcaption className="border-t border-white/10 pt-5">
              <div className="text-sm font-semibold text-white">
                {testimonial.name}
              </div>
              <div className="text-xs text-white/50">
                {testimonial.role}
              </div>
              <div className="text-xs text-white font-semibold tracking-wide uppercase mt-1">
                {testimonial.company}
              </div>
            </figcaption>
          </motion.figure>
        ))}
      </div>
    </section>
  );
};

// ─── PRICING ─────────────────────────────────────────────────────────────
const Pricing = () => {
  return (
    <section id="pricing" className="c3-pricing-section relative py-20">
      <svg
        className="absolute inset-0 w-full h-full"
        style={{ pointerEvents: "none" }}
      >
        <filter id="c3-noise">
          <feTurbulence
            type="fractalNoise"
            baseFrequency="0.5"
            numOctaves="2"
            stitchTiles="stitch"
          />
          <feComponentTransfer>
            <feFuncA type="linear" slope="0.075" />
          </feComponentTransfer>
          <feComposite in2="SourceGraphic" operator="in" result="noise" />
          <feBlend in="SourceGraphic" in2="noise" mode="overlay" />
        </filter>
      </svg>

      <div className="c3-watermark-container">
        <div className="c3-watermark-main">
          <span className="c3-watermark-line-1">Free forever.</span>
          <span className="c3-watermark-line-2">No catches.</span>
        </div>
      </div>

      <div className="flex justify-center w-full px-6 relative z-[4]">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
          viewport={{ once: true }}
          className="liquid-glass rounded-[44px] p-8 md:p-12 max-w-2xl w-full border border-cyan-500/30 bg-black/60 backdrop-blur-xl relative overflow-hidden group hover:border-cyan-500/60 transition-all duration-500"
        >
          {/* Subtle gradient light sweep */}
          <div className="absolute -inset-px bg-gradient-to-r from-transparent via-cyan-500/10 to-transparent -translate-x-full group-hover:translate-x-full transition-transform duration-1000" />
          
          <div className="relative z-10 flex flex-col md:flex-row gap-8 items-start">
            <div className="flex-1 space-y-4">
              <span className="px-3 py-1 rounded-full text-xs font-semibold bg-cyan-500/10 text-cyan-400 border border-cyan-500/20">
                100% Free &amp; Open
              </span>
              <h3 className="text-3xl md:text-4xl font-bold tracking-tight text-white">
                Synco Community Edition
              </h3>
              <p className="text-white/60 text-sm leading-[1.6]">
                Synco is built on the belief that cross-device synchronization should be private, local-first, and accessible to everyone. No subscription, no telemetry, no tracking.
              </p>
              
              <ul className="space-y-3 pt-2">
                {[
                  "Unlimited connected devices & streams",
                  "E2EE cryptographic pairing",
                  "Zero cloud dependency (100% local Wi-Fi)",
                  "Automatic low-latency audio routing",
                  "Full notification and media telemetry",
                  "Open source code structure & developer APIs"
                ].map((feature) => (
                  <li key={feature} className="flex items-center gap-3 text-xs text-white/80">
                    <div className="w-5 h-5 rounded-full bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center flex-shrink-0">
                      <svg
                        className="w-3.5 h-3.5 text-cyan-400"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="3"
                      >
                        <path d="M20 6L9 17l-5-5" />
                      </svg>
                    </div>
                    {feature}
                  </li>
                ))}
              </ul>
            </div>
            
            <div className="w-full md:w-auto md:min-w-[200px] flex flex-col items-center justify-center md:self-stretch border-t md:border-t-0 md:border-l border-white/10 pt-6 md:pt-0 md:pl-8">
              <div className="text-center mb-6">
                <span className="text-white/40 text-xs uppercase tracking-wider">Price</span>
                <div className="text-5xl font-extrabold text-white mt-1">$0</div>
                <span className="text-cyan-400 text-xs font-semibold">Free Forever</span>
              </div>
              <SyncoButton label="Download Synco" className="w-full" />
              <span className="text-[10px] text-white/40 mt-3 text-center">
                Windows, macOS, &amp; Linux
              </span>
            </div>
          </div>
        </motion.div>
      </div>
    </section>
  );
};

// ─── FINAL CTA ───────────────────────────────────────────────────────────
const FinalCTA = () => {
  return (
    <section className="max-w-6xl mx-auto px-6 py-20 md:py-32">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        whileInView={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.8 }}
        viewport={{ once: true }}
        className="liquid-glass relative overflow-hidden rounded-3xl px-8 py-16 md:py-24 text-center"
      >
        {/* Radial glow */}
        <div
          aria-hidden="true"
          className="absolute inset-0 pointer-events-none opacity-30"
          style={{
            background:
              "radial-gradient(600px circle at 50% 0%, rgba(255,255,255,0.15), transparent 70%)",
          }}
        />

        <div className="relative z-10">
          <h2 className="text-4xl md:text-6xl font-semibold tracking-tight leading-[1.02]">
            Ditch the friction.{" "}
            <br />
            Bridge your workspace.
          </h2>

          <p className="mt-6 text-white/60 max-w-md mx-auto text-sm leading-[1.6]">
            Join thousands of developers, power users, and remote workers who connect their phone
            and computer into a single unified control surface.
          </p>

          <div className="mt-8 flex flex-col sm:flex-row gap-4 items-center justify-center">
            <SyncoButton label="Download Synco" />
            <button className="rounded-full border border-white/15 text-white text-sm font-medium px-5 py-3 hover:bg-white/5 transition-apple flex items-center gap-2">
              Read docs
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </motion.div>
    </section>
  );
};

export {
  FeatureTriage,
  ProtocolSpecs,
  Testimonials,
  Pricing,
  FinalCTA,
};
