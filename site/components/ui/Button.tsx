"use client";

import React from "react";
import { motion, HTMLMotionProps } from "framer-motion";

export interface ButtonProps extends Omit<HTMLMotionProps<"button">, "variant"> {
  variant?: "primary" | "secondary" | "dark-utility" | "pearl" | "store-hero";
  children: React.ReactNode;
  className?: string;
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ variant = "primary", children, className = "", ...props }, ref) => {
    // Base styles common to all buttons
    const baseStyles = "inline-flex items-center justify-center font-sans transition-apple focus-visible:outline focus-visible:outline-2 focus-visible:outline-primary-focus focus-visible:outline-offset-2 cursor-pointer select-none";

    // Variant style mappings based on Apple design specification
    const variantStyles = {
      primary: "bg-primary text-white text-[17px] font-normal leading-[1.47] tracking-[-0.374px] rounded-pill px-[22px] py-[11px] hover:bg-[#0071e3]",
      secondary: "bg-transparent text-primary border border-primary text-[17px] font-normal leading-[1.47] tracking-[-0.374px] rounded-pill px-[22px] py-[11px] hover:bg-primary/5",
      "dark-utility": "bg-ink text-white text-[14px] font-normal leading-[1.29] tracking-[-0.224px] rounded-sm px-[15px] py-[8px] hover:bg-[#272729]",
      pearl: "bg-surface-pearl text-ink-muted-80 border-3 border-divider-soft text-[14px] font-normal leading-[1.43] tracking-[-0.224px] rounded-md px-[14px] py-[8px] hover:bg-canvas-parchment",
      "store-hero": "bg-primary text-white text-[18px] font-light leading-[1.0] rounded-pill px-[28px] py-[14px] hover:bg-[#0071e3]",
    };

    return (
      <motion.button
        ref={ref}
        whileTap={{ scale: 0.95 }}
        transition={{ type: "spring", stiffness: 400, damping: 25 }}
        className={`${baseStyles} ${variantStyles[variant]} ${className}`}
        {...props}
      >
        {children}
      </motion.button>
    );
  }
);

Button.displayName = "Button";
