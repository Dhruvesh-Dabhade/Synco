"use client";

import React from "react";
import { motion } from "framer-motion";
import {
  Search,
  Sparkles,
  Bluetooth,
  MoreHorizontal,
  PhoneCall,
  PhoneOff,
  Music,
  Bell,
  Menu,
  ChevronRight,
  Wifi,
  Shield,
} from "lucide-react";
import {
  SyncoLogo,
  LogoMark,
  SyncoButton,
  SectionEyebrow,
  gradientStyle,
} from "@/components/ui/primitives";

// ─── NAVBAR ──────────────────────────────────────────────────────────────
const Navbar = () => {
  const navLinks = [
    "Solutions",
    "Pricing",
    "Blog",
    "Documentation",
  ];

  return (
    <motion.nav
      initial={{ opacity: 0, y: -10 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6, ease: "easeOut" }}
      className="sticky top-0 z-50 w-full border-b border-white/10 bg-black/40 backdrop-blur-md"
    >
      <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
        {/* Left: Logo */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.6 }}
        >
          <LogoMark className="w-8 h-8" />
        </motion.div>

        {/* Center: Nav Links (desktop only) */}
        <div className="hidden md:flex gap-8 items-center">
          {navLinks.map((link, i) => (
            <motion.a
              key={link}
              href={`#${link.toLowerCase()}`}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{
                duration: 0.6,
                delay: 0.1 + i * 0.05,
                ease: "easeOut",
              }}
              className="text-white/70 text-sm font-medium hover:text-white transition-apple"
            >
              {link}
            </motion.a>
          ))}
        </div>

        {/* Right: Button / Menu */}
        <div className="flex items-center gap-4">
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.6, delay: 0.3 }}
            className="hidden md:block"
          >
            <SyncoButton label="Download Synco" />
          </motion.div>
          <button className="md:hidden w-10 h-10 rounded-full border border-white/10 bg-white/5 flex items-center justify-center hover:bg-white/10 transition-apple">
            <Menu className="w-5 h-5 text-white" />
          </button>
        </div>
      </div>
    </motion.nav>
  );
};

// ─── HERO ────────────────────────────────────────────────────────────────
const Hero = () => {
  return (
    <section className="relative pt-16 md:pt-28 pb-20 text-center flex flex-col items-center">
      <div className="max-w-6xl mx-auto px-6">
        {/* Headline */}
        <motion.h1
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{
            duration: 0.8,
            delay: 0.3,
            ease: [0.22, 1, 0.36, 1],
          }}
          className="text-4xl md:text-7xl font-semibold tracking-tight leading-[0.9]"
        >
          Synco: Your devices.{" "}
          <span style={{ ...gradientStyle }} className="animate-shiny">
            One system.
          </span>
        </motion.h1>

        {/* Description */}
        <motion.p
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{
            duration: 0.8,
            delay: 0.5,
            ease: [0.22, 1, 0.36, 1],
          }}
          className="mt-8 text-white/60 max-w-md mx-auto text-base leading-[1.5]"
        >
          Synco bridges your Android phone and desktop into a single
          synchronized control surface — no cloud, no lag, total privacy.
        </motion.p>

        {/* CTA Button */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{
            duration: 0.8,
            delay: 0.7,
            ease: [0.22, 1, 0.36, 1],
          }}
          className="mt-12 flex flex-col items-center gap-3"
        >
          <SyncoButton label="Download Synco" />
          <span className="text-xs text-white/40">
            Available for Android &amp; Windows / macOS
          </span>
        </motion.div>
      </div>
    </section>
  );
};

// ─── MACOS MENU BAR ───────────────────────────────────────────────────────
const MacOSMenuBar = () => {
  const menuItems = ["File", "Edit", "View", "Go", "Window", "Help"];

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.6, delay: 0.9 }}
      className="w-full h-10 bg-black/40 backdrop-blur-md border-t border-b border-white/10"
    >
      <div className="max-w-6xl mx-auto px-6 h-full flex items-center justify-between text-xs">
        <div className="flex items-center gap-4">
          <SyncoLogo className="w-3.5 h-3.5" />
          <span className="font-bold text-white">Synco</span>
          {menuItems.map((item, i) => (
            <span
              key={item}
              className={`text-white/70 font-medium ${
                i > 2 ? "hidden sm:inline" : ""
              } ${i > 3 ? "hidden md:inline" : ""}`}
            >
              {item}
            </span>
          ))}
        </div>
        <div className="flex items-center gap-2 text-white/70">
          <Search className="w-3.5 h-3.5" />
          <span>Wed May 6 1:09 PM</span>
        </div>
      </div>
    </motion.div>
  );
};

// ─── SYNCO DASHBOARD MOCKUP ──────────────────────────────────────────────
const InboxMockup = () => {
  const [activePanel, setActivePanel] = React.useState(0);

  const notifications = [
    {
      app: "WhatsApp",
      title: "Priya Sharma",
      body: "Hey, are you joining the call at 3?",
      time: "9:41 AM",
      unread: true,
      avatar: "W",
      avatarGradient: "from-[#25d366] to-[#128c7e]",
    },
    {
      app: "Slack",
      title: "#design-sync",
      body: "Marcus posted new Figma frames for the hero.",
      time: "9:12 AM",
      unread: true,
      avatar: "S",
      avatarGradient: "from-purple-500 to-pink-500",
    },
    {
      app: "Phone",
      title: "Incoming call — Dad",
      body: "Ringing on your phone right now",
      time: "8:55 AM",
      unread: true,
      avatar: "📞",
      avatarGradient: "from-green-500 to-emerald-500",
      isCall: true,
    },
    {
      app: "Gmail",
      title: "David Lim",
      body: "Contract is signed — good to go.",
      time: "Yesterday",
      avatar: "G",
      avatarGradient: "from-red-500 to-orange-500",
    },
    {
      app: "Spotify",
      title: "Now Playing",
      body: "Blinding Lights — The Weeknd",
      time: "Live",
      avatar: "♫",
      avatarGradient: "from-[#1db954] to-[#191414]",
      isMedia: true,
    },
    {
      app: "Battery",
      title: "Phone battery — 34%",
      body: "Connect charger soon",
      time: "Live",
      avatar: "🔋",
      avatarGradient: "from-yellow-500 to-amber-600",
    },
  ];

  const activeNote = notifications[activePanel];

  return (
    <motion.div
      initial={{ opacity: 0, y: 40 }}
      whileInView={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.9, delay: 1.1 }}
      viewport={{ once: true }}
      className="max-w-6xl mx-auto px-6 py-16 md:py-24"
    >
      <div className="relative rounded-2xl overflow-hidden border border-white/10 bg-[#0e1014]/90 backdrop-blur-2xl">
        {/* Title bar */}
        <div className="h-10 bg-black/40 border-b border-white/10 px-4 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded-full bg-red-500" />
            <div className="w-3 h-3 rounded-full bg-yellow-500" />
            <div className="w-3 h-3 rounded-full bg-green-500" />
          </div>
          <span className="text-xs text-white/50">Synco — Control Panel</span>
          <div className="w-6" />
        </div>

        {/* Content */}
        <div className="grid grid-cols-12 h-[520px]">
          {/* Sidebar */}
          <div className="col-span-3 border-r border-white/10 bg-black/30 p-4 flex flex-col gap-4">
            <div className="flex items-center gap-2 px-3 py-2 rounded-lg bg-white/[0.04] border border-white/10">
              <div className="w-2 h-2 rounded-full bg-green-400 animate-pulse" />
              <span className="text-xs text-white/70 font-medium">Phone connected</span>
            </div>

            <div className="space-y-1">
              {[
                { label: "Notifications", count: 6, active: true, icon: <Bell className="w-3.5 h-3.5" /> },
                { label: "Media Control", count: null, icon: <Music className="w-3.5 h-3.5" /> },
                { label: "Calls", count: 1, icon: <PhoneCall className="w-3.5 h-3.5" /> },
                { label: "Bluetooth", count: null, icon: <Bluetooth className="w-3.5 h-3.5" /> },
                { label: "Network", count: null, icon: <Wifi className="w-3.5 h-3.5" /> },
                { label: "Security", count: null, icon: <Shield className="w-3.5 h-3.5" /> },
              ].map((item) => (
                <div
                  key={item.label}
                  className={`text-xs py-2 px-3 rounded-md cursor-pointer transition-apple flex items-center gap-2 ${
                    item.active
                      ? "bg-white/10 text-white"
                      : "text-white/60 hover:bg-white/5"
                  }`}
                >
                  <span className="opacity-60">{item.icon}</span>
                  {item.label}
                  {item.count && (
                    <span className="ml-auto text-white/40">({item.count})</span>
                  )}
                </div>
              ))}
            </div>

            <div className="border-t border-white/10 pt-3">
              <div className="text-xs uppercase tracking-widest text-white/50 font-semibold mb-2">
                Devices
              </div>
              <div className="flex gap-2">
                {[
                  { color: "bg-[#00d2ff]", label: "Phone" },
                  { color: "bg-[#A4F4FD]", label: "Desktop" },
                  { color: "bg-[#f59e0b]", label: "Tablet" },
                ].map((label) => (
                  <div
                    key={label.label}
                    className={`w-3 h-3 rounded-full ${label.color} cursor-pointer hover:scale-125 transition-transform`}
                    title={label.label}
                  />
                ))}
              </div>
            </div>
          </div>

          {/* Notification List */}
          <div className="col-span-4 border-r border-white/10 flex flex-col">
            {/* Search */}
            <div className="p-3 border-b border-white/10">
              <div className="flex items-center gap-2 bg-white/5 rounded-lg px-3 py-2">
                <Search className="w-4 h-4 text-white/40" />
                <input
                  type="text"
                  placeholder="Search notifications"
                  className="bg-transparent text-xs text-white/60 placeholder-white/30 focus:outline-none flex-1"
                />
              </div>
            </div>

            {/* Notification items */}
            <div className="flex-1 overflow-y-auto">
              {notifications.map((n, i) => (
                <div
                  key={i}
                  onClick={() => setActivePanel(i)}
                  className={`p-3 border-b border-white/5 cursor-pointer transition-apple ${
                    activePanel === i
                      ? "bg-white/10"
                      : "hover:bg-white/5"
                  }`}
                >
                  <div className="flex items-start gap-3">
                    <div
                      className={`w-10 h-10 rounded-full bg-gradient-to-br ${n.avatarGradient} flex items-center justify-center flex-shrink-0 text-xs font-bold text-white`}
                    >
                      {n.avatar}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-sm font-semibold text-white truncate">
                          {n.app}
                        </span>
                        <span className="text-xs text-white/40 flex-shrink-0">
                          {n.time}
                        </span>
                      </div>
                      <div className="text-xs text-white/60 truncate">
                        {n.title}
                      </div>
                      <div className="text-xs text-white/40 truncate mt-1">
                        {n.body}
                      </div>
                    </div>
                    {n.unread && (
                      <div className="w-2 h-2 rounded-full bg-[#2997ff] flex-shrink-0" />
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Detail Pane */}
          <div className="col-span-5 flex flex-col">
            {/* Toolbar */}
            <div className="h-10 border-b border-white/10 px-4 flex items-center gap-1">
              {activeNote?.isCall ? (
                <>
                  <button className="flex items-center gap-1.5 px-3 py-1 rounded-md bg-green-500/20 text-green-400 text-xs font-medium hover:bg-green-500/30 transition-apple">
                    <PhoneCall className="w-3.5 h-3.5" /> Answer
                  </button>
                  <button className="flex items-center gap-1.5 px-3 py-1 rounded-md bg-red-500/20 text-red-400 text-xs font-medium hover:bg-red-500/30 transition-apple">
                    <PhoneOff className="w-3.5 h-3.5" /> Decline
                  </button>
                </>
              ) : activeNote?.isMedia ? (
                <>
                  <button className="text-xs px-3 py-1 rounded-md bg-white/5 text-white/70 hover:bg-white/10 transition-apple">⏮</button>
                  <button className="text-xs px-3 py-1 rounded-md bg-white/10 text-white font-semibold hover:bg-white/15 transition-apple">⏸</button>
                  <button className="text-xs px-3 py-1 rounded-md bg-white/5 text-white/70 hover:bg-white/10 transition-apple">⏭</button>
                </>
              ) : (
                <>
                  <button className="w-7 h-7 rounded-md hover:bg-white/5 flex items-center justify-center text-white/70 hover:text-white transition-apple">
                    <Bell className="w-4 h-4" />
                  </button>
                  <button className="w-7 h-7 rounded-md hover:bg-white/5 flex items-center justify-center text-white/70 hover:text-white transition-apple">
                    <PhoneOff className="w-4 h-4" />
                  </button>
                </>
              )}
              <div className="flex-1" />
              <button className="w-7 h-7 rounded-md hover:bg-white/5 flex items-center justify-center text-white/70 hover:text-white transition-apple">
                <MoreHorizontal className="w-4 h-4" />
              </button>
            </div>

            {/* Detail Content */}
            <div className="flex-1 overflow-y-auto p-4 space-y-4">
              {/* Header */}
              <div className="border-b border-white/10 pb-4">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-center gap-3">
                    <div
                      className={`w-10 h-10 rounded-full bg-gradient-to-br ${activeNote?.avatarGradient} flex items-center justify-center flex-shrink-0 text-xs font-bold text-white`}
                    >
                      {activeNote?.avatar}
                    </div>
                    <div>
                      <div className="text-sm font-semibold text-white">
                        {activeNote?.title}
                      </div>
                      <div className="text-xs text-white/50">
                        {activeNote?.app} · {activeNote?.time}
                      </div>
                    </div>
                  </div>
                  <span className="px-2 py-1 rounded-full text-xs bg-[#00d2ff]/20 text-[#00d2ff] flex-shrink-0">
                    {activeNote?.isCall ? "Call" : activeNote?.isMedia ? "Media" : "Notification"}
                  </span>
                </div>
              </div>

              {/* AI Summary Card */}
              <div className="liquid-glass rounded-lg p-4 space-y-2">
                <div className="flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-[#A4F4FD]" />
                  <span className="text-sm font-semibold text-white">
                    Synco Intelligence
                  </span>
                </div>
                <p className="text-xs text-white/70 leading-relaxed">
                  {activeNote?.isCall
                    ? "Incoming call routed from your Android phone. You can answer or decline directly from your desktop — no need to reach for your phone."
                    : activeNote?.isMedia
                    ? "Your phone is the active audio owner. Playback controls are mirrored here in real-time over local Wi-Fi."
                    : "Notification mirrored from your phone over an encrypted local connection. No data left your network."}
                </p>
              </div>

              {/* Body */}
              <div className="space-y-3 text-sm text-white/70 leading-relaxed">
                <p className="font-medium text-white">{activeNote?.body}</p>
                <p>
                  {activeNote?.isCall
                    ? "Synco detected an incoming call on your Android device and mirrored it here instantly. Audio routing is handled automatically — your desktop speakers stay silent until you choose to engage."
                    : activeNote?.isMedia
                    ? "Synco keeps track of your phone's audio state in real-time. Seek, skip, or adjust volume without touching your phone."
                    : "This notification arrived on your phone and was forwarded to your desktop over your local Wi-Fi network — encrypted end-to-end, no cloud relay involved."}
                </p>
                <p className="text-white/50 text-xs">
                  Delivered over local network · End-to-end encrypted · 0 ms relay latency
                </p>
              </div>

              {/* Footer */}
              <p className="text-xs text-white/50 pt-2">
                — Synco · local-first sync engine v2.1
              </p>
            </div>
          </div>
        </div>
      </div>
    </motion.div>
  );
};

export { Navbar, Hero, MacOSMenuBar, InboxMockup };
