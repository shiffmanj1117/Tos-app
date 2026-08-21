import React, { useState, useEffect } from 'react';
import { Cpu } from 'lucide-react';

interface TommiOsLoadingScreenProps {
  onDiagnosticsClick: () => void;
}

const TICKER_MESSAGES = [
  'TOMMI IS INITIALIZING...',
  '> POWERING UP COGNITIVE CORE',
  '> LOADING LOCAL INTELLIGENCE',
  '> INITIALIZING MEMORY',
  '> CONNECTING VISION SYSTEM',
  '> CALIBRATING VOICE ENGINE',
  '> CHECKING SYSTEM STATUS',
  '> PREPARING TOMMI',
  '> COGNITIVE SYSTEM ONLINE',
];

export const TommiOsLoadingScreen: React.FC<TommiOsLoadingScreenProps> = ({
  onDiagnosticsClick,
}) => {
  const [currentMessageIndex, setCurrentMessageIndex] = useState(0);
  const [imgError, setImgError] = useState(false);

  useEffect(() => {
    const interval = setInterval(() => {
      setCurrentMessageIndex((prev) => (prev + 1) % TICKER_MESSAGES.length);
    }, 2200);
    return () => clearInterval(interval);
  }, []);

  const currentMessage = TICKER_MESSAGES[currentMessageIndex];

  return (
    <div
      id="tommi-loading-screen"
      className="relative w-full h-full flex flex-col items-center justify-center bg-radial from-[#1F2833]/50 to-[#0B0C10] select-none p-6"
    >
      <div className="flex flex-col items-center justify-center max-w-md w-full">
        {/* Central Orbital Thinking Component */}
        <div className="relative w-64 h-64 flex items-center justify-center">
          {/* Glowing Pulse Core */}
          <div className="absolute w-40 h-40 rounded-full bg-radial from-[#66FCF1]/30 to-transparent animate-pulse-slow pointer-events-none" />

          {/* SVG Inner Orbital Ring (Rotating Dashed Circle) */}
          <svg
            className="absolute w-52 h-52 animate-spin-slow pointer-events-none"
            viewBox="0 0 200 200"
          >
            <circle
              cx="100"
              cy="100"
              r="85"
              fill="none"
              stroke="#66FCF1"
              strokeWidth="2"
              strokeDasharray="25 35"
              strokeOpacity="0.7"
            />
          </svg>

          {/* SVG Outer Orbital Ring (Reverse Rotating Dashed Circle) */}
          <svg
            className="absolute w-60 h-60 animate-spin-reverse-slow pointer-events-none"
            viewBox="0 0 240 240"
          >
            <circle
              cx="120"
              cy="120"
              r="105"
              fill="none"
              stroke="#45A29E"
              strokeWidth="1.5"
              strokeDasharray="15 50"
              strokeOpacity="0.4"
            />
          </svg>

          {/* Central Logo and Monospace Typography */}
          <div className="relative z-10 flex flex-col items-center justify-center text-center">
            <div className="w-16 h-16 rounded-full overflow-hidden border-2 border-[#66FCF1] flex items-center justify-center bg-[#0B0C10] shadow-[0_0_15px_rgba(102,252,241,0.4)]">
              {!imgError ? (
                <img
                  src="/img_app_icon.jpg"
                  alt="TOMMI OS Core"
                  onError={() => setImgError(true)}
                  className="w-full h-full object-cover"
                />
              ) : (
                <Cpu className="w-8 h-8 text-[#66FCF1] animate-pulse" />
              )}
            </div>

            <div className="mt-2.5">
              <span className="font-mono text-xl font-extrabold tracking-[0.35em] text-white block">
                TOMMI
              </span>
              <span className="font-mono text-[9px] font-bold tracking-[0.2em] text-[#45A29E] block mt-0.5">
                INTELLIGENT OS
              </span>
            </div>
          </div>
        </div>

        {/* Terminal Readout Ticker */}
        <div className="mt-10 h-14 w-full flex items-center justify-center px-4">
          <p
            key={currentMessageIndex}
            className={`font-mono text-xs md:text-sm font-bold tracking-widest text-center transition-all duration-500 animate-in fade-in ${
              currentMessage.startsWith('>') ? 'text-[#66FCF1]' : 'text-white'
            }`}
          >
            {currentMessage}
          </p>
        </div>

        {/* Futuristic Scanning Micro-Bar */}
        <div className="mt-3 w-40 h-[2px] bg-[#1F2833] relative overflow-hidden rounded">
          <div className="absolute top-0 left-0 w-12 h-full bg-gradient-to-r from-transparent via-[#66FCF1] to-transparent animate-scan" />
        </div>
      </div>

      {/* Override button for diagnostic escapes */}
      <div className="absolute bottom-8 left-0 right-0 flex justify-center px-4">
        <button
          type="button"
          id="escape-diagnostics-btn"
          onClick={onDiagnosticsClick}
          className="font-mono text-[11px] font-bold tracking-widest text-[#66FCF1]/60 hover:text-[#66FCF1] transition-colors uppercase px-4 py-2 rounded hover:bg-[#1F2833]/50"
        >
          ESCAPE SYSTEM OVERRIDE: CHANGE IP / DIAGNOSTICS
        </button>
      </div>
    </div>
  );
};
