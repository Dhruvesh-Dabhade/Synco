"use client";

import React, { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import dynamic from "next/dynamic";
import { Button } from "../ui/Button";
import { ContainerScroll } from "../ui/container-scroll-animation";

// Loaded client-side only — Three.js needs window/WebGL
const GenerativeMountainScene = dynamic(
  () => import("../ui/GenerativeMountainScene").then((m) => m.GenerativeMountainScene),
  { ssr: false }
);

// Interactive Mockup displaying the local synchronization between phone & computer
const SyncMockup = () => {
  const [isPlaying, setIsPlaying] = useState(true);
  const [mediaProgress, setMediaProgress] = useState(35);
  const [activeTrack, setActiveTrack] = useState({
    title: "After Hours",
    artist: "The Weeknd",
    duration: "6:01",
  });
  const [notifications, setNotifications] = useState<string[]>([]);
  const [batteryLevel, setBatteryLevel] = useState(84);
  const [isCharging, setIsCharging] = useState(false);

  // Sync media progress bar
  useEffect(() => {
    let interval: NodeJS.Timeout;
    if (isPlaying) {
      interval = setInterval(() => {
        setMediaProgress((prev) => (prev >= 100 ? 0 : prev + 0.5));
      }, 300);
    }
    return () => clearInterval(interval);
  }, [isPlaying]);

  // Simulate incoming notification mirroring
  useEffect(() => {
    const notifyTrigger = setInterval(() => {
      const msgs = [
        "Message from Sarah: Let's meet at 5!",
        "Calendar: Standup in 10 mins",
        "Slack: Team update posted",
        "WhatsApp: Voice message received",
      ];
      const randomMsg = msgs[Math.floor(Math.random() * msgs.length)];
      
      // Trigger notification flow
      setNotifications([randomMsg]);
      // Clear notification after 4 seconds
      setTimeout(() => {
        setNotifications([]);
      }, 4000);
    }, 9000);

    return () => clearInterval(notifyTrigger);
  }, []);

  return (
    <div className="relative w-full max-w-4xl mx-auto h-[450px] flex items-center justify-center overflow-hidden rounded-lg bg-surface-black border border-white/5 py-12 px-4 sm:px-8">
      {/* Background radial gradient glow for atmosphere */}
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,rgba(0,102,204,0.15)_0%,transparent_60%)] pointer-events-none" />

      {/* Sync connection waves */}
      <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
        <svg className="w-full h-full absolute opacity-30" viewBox="0 0 800 400">
          <motion.ellipse
            cx="400"
            cy="200"
            rx="180"
            ry="180"
            fill="none"
            stroke="#2997ff"
            strokeWidth="1.5"
            strokeDasharray="4 8"
            animate={{ rotate: 360 }}
            transition={{ repeat: Infinity, duration: 25, ease: "linear" }}
          />
          <motion.ellipse
            cx="400"
            cy="200"
            rx="120"
            ry="120"
            fill="none"
            stroke="#0066cc"
            strokeWidth="1"
            strokeDasharray="5 5"
            animate={{ rotate: -360 }}
            transition={{ repeat: Infinity, duration: 15, ease: "linear" }}
          />
        </svg>
      </div>

      {/* Synced devices grid */}
      <div className="relative w-full z-10 flex flex-col md:flex-row items-center justify-around gap-12 max-w-3xl">
        
        {/* DESKTOP MONITOR MOCKUP */}
        <div className="relative w-[280px] sm:w-[320px] aspect-[16/10] bg-[#161617] rounded-lg border-2 border-white/10 p-2 shadow-product flex flex-col justify-between">
          {/* Desktop wallpaper screen */}
          <div className="relative w-full h-full bg-[#0a0a0c] rounded border border-white/5 overflow-hidden flex flex-col justify-between p-3">
            {/* Desktop top-bar */}
            <div className="flex items-center justify-between text-[9px] text-zinc-500 border-b border-white/5 pb-1">
              <div className="flex items-center gap-1.5">
                <span className="w-1.5 h-1.5 rounded-full bg-red-500/50" />
                <span className="w-1.5 h-1.5 rounded-full bg-yellow-500/50" />
                <span className="w-1.5 h-1.5 rounded-full bg-green-500/50" />
              </div>
              <div className="flex items-center gap-2">
                <span>Local Network</span>
                <span className="w-2.5 h-1.5 bg-[#2997ff]/20 rounded-xs flex items-center justify-center text-[7px] text-[#2997ff] font-bold px-0.5">E2EE</span>
              </div>
            </div>

            {/* Desktop Dashboard Area */}
            <div className="flex-1 flex flex-col justify-center items-center gap-3">
              {/* Media widget on Desktop */}
              <div className="w-[180px] bg-white/5 backdrop-blur-md border border-white/10 rounded-md p-2 flex items-center gap-2">
                <div className="w-8 h-8 rounded bg-zinc-800 flex items-center justify-center text-xs text-white overflow-hidden relative">
                  <div className="absolute inset-0 bg-[#0066cc]/40 animate-pulse" />
                  🎵
                </div>
                <div className="flex-1 min-w-0">
                  <div className="text-[9px] font-semibold text-white truncate">{activeTrack.title}</div>
                  <div className="text-[7px] text-zinc-400 truncate">{activeTrack.artist}</div>
                  {/* Sync progress bar */}
                  <div className="w-full bg-white/10 h-0.5 rounded-full mt-1.5 overflow-hidden">
                    <div 
                      className="bg-[#2997ff] h-full transition-all duration-300"
                      style={{ width: `${mediaProgress}%` }}
                    />
                  </div>
                </div>
              </div>

              {/* Notification Mirror Box */}
              <div className="h-10 w-[180px] relative">
                <AnimatePresence>
                  {notifications.map((notif, index) => (
                    <motion.div
                      key={notif + index}
                      initial={{ opacity: 0, y: 10, scale: 0.95 }}
                      animate={{ opacity: 1, y: 0, scale: 1 }}
                      exit={{ opacity: 0, scale: 0.95 }}
                      className="absolute inset-0 bg-[#272729]/90 border border-white/10 backdrop-blur-md rounded-md p-1.5 flex items-center gap-1.5 text-left"
                    >
                      <span className="text-xs">💬</span>
                      <div className="flex-1 min-w-0">
                        <div className="text-[8px] font-semibold text-white">Phone Notification</div>
                        <div className="text-[7px] text-zinc-300 truncate">{notif.replace(/.*:\s*/, "")}</div>
                      </div>
                    </motion.div>
                  ))}
                </AnimatePresence>
              </div>
            </div>

            {/* Desktop Status Dock */}
            <div className="flex items-center justify-between text-[8px] text-zinc-500 border-t border-white/5 pt-1.5">
              <div className="flex items-center gap-1.5">
                <span>📱 Synced</span>
                <span className="text-[7px] text-emerald-400">● Live</span>
              </div>
              <div className="flex items-center gap-1">
                <span>Battery: {batteryLevel}%</span>
                <div className="w-3.5 h-1.5 border border-zinc-500 rounded-xs p-0.5 flex items-center">
                  <div className="bg-emerald-400 h-full rounded-2xs" style={{ width: `${batteryLevel}%` }} />
                </div>
              </div>
            </div>
          </div>
          {/* Stand */}
          <div className="absolute -bottom-6 left-1/2 -translate-x-1/2 w-14 h-6 bg-[#2a2a2c] rounded-b-md border-x border-b border-white/10 flex items-end justify-center">
            <div className="w-24 h-1 bg-[#1c1c1e] rounded-full shadow-lg" />
          </div>
        </div>

        {/* CONNECTION BRIDGE */}
        <div className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 hidden md:flex flex-col items-center gap-1">
          <div className="w-8 h-8 rounded-full bg-white/5 backdrop-blur-md border border-white/15 flex items-center justify-center text-sm text-[#2997ff] shadow-lg">
            🔒
          </div>
          <span className="text-[9px] text-[#2997ff] uppercase tracking-widest font-semibold font-mono">E2EE Stream</span>
        </div>

        {/* SMARTPHONE MOCKUP */}
        <div className="relative w-[130px] aspect-[9/19] bg-[#161617] rounded-[24px] border-4 border-white/15 p-1.5 shadow-product flex flex-col justify-between">
          {/* Screen */}
          <div className="relative w-full h-full bg-[#0a0a0c] rounded-[18px] overflow-hidden flex flex-col justify-between p-2.5">
            {/* Dynamic Island / Notch */}
            <div className="absolute top-1 left-1/2 -translate-x-1/2 w-10 h-3 bg-black rounded-full z-20 flex items-center justify-around px-1">
              <div className="w-1.5 h-1.5 rounded-full bg-zinc-900" />
              <div className="w-1 h-1 rounded-full bg-[#0066cc]/50" />
            </div>

            {/* Phone header */}
            <div className="flex items-center justify-between text-[7px] text-zinc-500 pt-1">
              <span>9:41</span>
              <div className="flex items-center gap-1">
                <span>5G</span>
                <span>📶</span>
              </div>
            </div>

            {/* Phone Screen Dashboard */}
            <div className="flex-1 flex flex-col justify-center gap-4 py-2">
              {/* Media widget on Mobile */}
              <div className="bg-zinc-900/80 border border-white/5 rounded-xl p-2 text-center flex flex-col items-center gap-1.5">
                <div className="w-12 h-12 rounded-lg bg-zinc-800 flex items-center justify-center text-lg overflow-hidden relative shadow-md">
                  <div className="absolute inset-0 bg-gradient-to-tr from-[#0066cc]/40 to-transparent" />
                  🎵
                </div>
                <div className="w-full">
                  <div className="text-[9px] font-bold text-white leading-tight truncate">{activeTrack.title}</div>
                  <div className="text-[7px] text-zinc-400 truncate">{activeTrack.artist}</div>
                </div>
                {/* Media controls */}
                <div className="flex items-center justify-center gap-3 my-0.5">
                  <button className="text-[8px] text-zinc-400 hover:text-white" onClick={() => setMediaProgress(0)}>⏮</button>
                  <button className="text-[10px] text-white bg-[#0066cc] w-5 h-5 rounded-full flex items-center justify-center shadow-sm" onClick={() => setIsPlaying(!isPlaying)}>
                    {isPlaying ? "⏸" : "▶"}
                  </button>
                  <button className="text-[8px] text-zinc-400 hover:text-white" onClick={() => setMediaProgress(100)}>⏭</button>
                </div>
                {/* Slider bar */}
                <div className="w-full bg-white/15 h-0.5 rounded-full overflow-hidden">
                  <div 
                    className="bg-[#0066cc] h-full"
                    style={{ width: `${mediaProgress}%` }}
                  />
                </div>
              </div>

              {/* Status information on mobile */}
              <div className="bg-zinc-900/60 rounded-lg p-1.5 border border-white/5 text-[7px] flex flex-col gap-1">
                <div className="flex items-center justify-between text-zinc-400">
                  <span>Battery</span>
                  <span className="text-white font-semibold">{batteryLevel}%</span>
                </div>
                <div className="w-full bg-white/10 h-1 rounded-sm overflow-hidden">
                  <div className="bg-emerald-400 h-full rounded-sm" style={{ width: `${batteryLevel}%` }} />
                </div>
              </div>
            </div>

            {/* Notification triggers on mobile (simulated button taps) */}
            <div className="w-full flex justify-center pb-1">
              <button 
                onClick={() => {
                  setNotifications(["Manual Test: Connected successfully!"]);
                  setTimeout(() => setNotifications([]), 4000);
                }} 
                className="w-full bg-white/5 hover:bg-white/10 active:scale-95 border border-white/10 text-[7px] text-zinc-300 py-1 rounded-pill transition-apple uppercase tracking-wider font-mono font-bold"
              >
                Send Sync Ping
              </button>
            </div>
          </div>
        </div>
        
      </div>
    </div>
  );
};

export const Hero = () => {
  return (
    <section className="relative w-full min-h-screen text-white flex flex-col">
      {/* Three.js generative mountain terrain — absolute behind all content */}
      <GenerativeMountainScene />

      {/* Overlay: softens the terrain so text remains legible */}
      <div className="absolute inset-0 z-[1] bg-black/55 pointer-events-none" aria-hidden="true" />
      
      {/* 1. Frosted Sub-Navigation Header — z-10 keeps it above canvas + overlay */}
      <nav className="sticky top-0 w-full h-[52px] bg-black/70 backdrop-blur-md border-b border-white/8 z-50 transition-apple">
        <div className="max-w-[1440px] h-full mx-auto px-6 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="text-[21px] font-semibold text-white tracking-[-0.374px] font-sans">
              Synco
            </span>
            <span className="text-[10px] text-[#2997ff] border border-[#2997ff]/30 px-1.5 py-0.25 rounded-pill font-mono font-bold uppercase tracking-wider">
              Local Alpha
            </span>
          </div>
          <div className="flex items-center gap-6">
            <div className="hidden sm:flex items-center gap-6 text-[14px] text-zinc-400">
              <a href="#overview" className="hover:text-white transition-apple">Overview</a>
              <a href="#security" className="hover:text-white transition-apple">Security</a>
              <a href="#specifications" className="hover:text-white transition-apple">Specs</a>
            </div>
            <Button variant="dark-utility">
              Download
            </Button>
          </div>
        </div>
      </nav>

      {/* 2. Main Hero Content Container */}
      <div className="relative z-10 flex-1 w-full max-w-[1440px] mx-auto px-6 pt-16 pb-24 flex flex-col items-center text-center justify-between gap-12">
        
        {/* Core messaging stack */}
        <div className="max-w-3xl flex flex-col items-center gap-6">
          
          {/* Subtle security/local-first pill badge */}
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.2 }}
            className="inline-flex items-center gap-2 bg-[#2997ff]/10 border border-[#2997ff]/20 px-3 py-1 rounded-pill"
          >
            <span className="w-1.5 h-1.5 rounded-full bg-[#2997ff] animate-ping" />
            <span className="text-[12px] text-[#2997ff] font-semibold tracking-wide uppercase font-sans">
              Zero-Trust Local Synchronization
            </span>
          </motion.div>

          {/* Hero Main H1 */}
          <motion.h1
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.3 }}
            className="text-hero-display max-w-2xl leading-none text-white font-sans sm:text-[64px]"
          >
            Your phone and desktop. <br />
            <span className="text-[#2997ff]">One unified system.</span>
          </motion.h1>

          {/* Tagline */}
          <motion.p
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.4 }}
            className="text-[21px] font-light text-zinc-400 max-w-xl font-sans"
          >
            One system. Zero cloud. Total control. Keep notifications, media telemetry, and audio states synchronized end-to-end directly on your local network.
          </motion.p>

          {/* Call-to-actions */}
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.5 }}
            className="flex flex-col sm:flex-row gap-4 items-center justify-center mt-4 w-full sm:w-auto"
          >
            <Button variant="primary" className="w-full sm:w-auto">
              Download for Desktop
            </Button>
            <Button variant="secondary" className="w-full sm:w-auto">
              How it works
            </Button>
          </motion.div>
        </div>

      </div>

      {/* 3. Scroll-driven 3D showcase */}
      <div className="relative z-10 w-full">
        <ContainerScroll
          titleComponent={
            <div className="flex flex-col items-center gap-3 mb-6">
              <span className="text-[12px] uppercase tracking-widest text-zinc-500 font-bold font-mono">
                Interactive Sync Simulator
              </span>
              <p className="text-[17px] font-light text-zinc-400 max-w-md">
                Click the buttons to trigger a live connection stream between devices.
              </p>
            </div>
          }
        >
          <SyncMockup />
        </ContainerScroll>
      </div>
    </section>
  );
};
