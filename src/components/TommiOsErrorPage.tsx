import React, { useState } from 'react';
import {
  Wifi,
  Zap,
  Radio,
  Edit3,
  RotateCw,
  ShieldAlert,
  Terminal,
  Copy,
  Check,
  AlertOctagon,
} from 'lucide-react';

interface TommiOsErrorPageProps {
  currentUrl: string;
  onRetry: () => void;
  onEditUrlClick: () => void;
}

export const TommiOsErrorPage: React.FC<TommiOsErrorPageProps> = ({
  currentUrl,
  onRetry,
  onEditUrlClick,
}) => {
  const [copied, setCopied] = useState(false);
  const [imgError, setImgError] = useState(false);

  const handleCopyUrl = () => {
    navigator.clipboard.writeText(currentUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div
      id="tommi-connection-error-page"
      className="relative w-full h-full flex flex-col items-center justify-center overflow-y-auto px-4 py-8 bg-[#0B0C10] text-[#C5C6C7] select-none"
    >
      {/* Cyber Grid Matrix Background */}
      <div
        className="absolute inset-0 pointer-events-none opacity-20"
        style={{
          backgroundImage: `
            linear-gradient(to right, rgba(102, 252, 241, 0.08) 1px, transparent 1px),
            linear-gradient(to bottom, rgba(102, 252, 241, 0.08) 1px, transparent 1px)
          `,
          backgroundSize: '36px 36px',
        }}
      />

      {/* Cyberpunk Radial Glows */}
      <div className="absolute top-1/6 left-1/2 -translate-x-1/2 w-[30rem] h-[30rem] rounded-full bg-radial from-[#66FCF1]/15 via-[#1F2833]/10 to-transparent blur-3xl pointer-events-none" />

      <div className="relative z-10 max-w-xl w-full flex flex-col items-center my-auto">
        {/* Status Badge */}
        <div className="inline-flex items-center space-x-2 px-4 py-1.5 rounded-full bg-[#FF3B30]/15 border border-[#FF3B30]/60 shadow-[0_0_20px_rgba(255,59,48,0.25)] backdrop-blur-md">
          <span className="w-2.5 h-2.5 rounded-full bg-[#FF3B30] shadow-[0_0_8px_#FF3B30] animate-ping" />
          <span className="font-mono text-xs font-black tracking-[0.25em] text-[#FF4D4D] uppercase">
            HOST NOT REACHABLE
          </span>
        </div>

        {/* Floating 3D Tommi Robot Avatar */}
        <div className="relative mt-5 mb-2 w-52 h-52 flex items-center justify-center">
          {/* Cyan Energy Field */}
          <div className="absolute w-44 h-44 rounded-full bg-radial from-[#66FCF1]/35 to-transparent blur-2xl animate-pulse-slow pointer-events-none" />

          {/* Holographic Orbital Rings */}
          <svg className="absolute w-48 h-48 animate-spin-slow pointer-events-none" viewBox="0 0 200 200">
            <circle
              cx="100"
              cy="100"
              r="88"
              fill="none"
              stroke="#66FCF1"
              strokeWidth="2"
              strokeDasharray="14 26"
              strokeOpacity="0.7"
            />
          </svg>
          <svg className="absolute w-52 h-52 animate-spin-reverse-slow pointer-events-none" viewBox="0 0 220 220">
            <circle
              cx="110"
              cy="110"
              r="98"
              fill="none"
              stroke="#45A29E"
              strokeWidth="1.5"
              strokeDasharray="10 40"
              strokeOpacity="0.5"
            />
          </svg>

          {/* Floating Robot Image */}
          <div className="relative z-10 w-36 h-36 flex items-center justify-center animate-float p-1 drop-shadow-[0_12px_28px_rgba(102,252,241,0.45)]">
            {!imgError ? (
              <img
                src="/tommi_robot_icon.png"
                alt="Tommi Robot"
                referrerPolicy="no-referrer"
                onError={() => setImgError(true)}
                className="w-full h-full object-contain filter drop-shadow-[0_0_16px_rgba(102,252,241,0.6)]"
              />
            ) : (
              <div className="w-32 h-32 rounded-full bg-[#1F2833] border-2 border-[#66FCF1] flex items-center justify-center shadow-[0_0_20px_rgba(102,252,241,0.4)]">
                <AlertOctagon className="w-14 h-14 text-[#66FCF1]" />
              </div>
            )}
          </div>
        </div>

        {/* System Title */}
        <h1 className="text-2xl md:text-3xl font-extrabold text-white tracking-widest text-center font-mono">
          TOMMI OS OFFLINE
        </h1>

        {/* Unreachable Address Telemetry */}
        <div className="mt-2.5 flex items-center gap-2 bg-[#1F2833]/90 border border-[#45A29E]/50 hover:border-[#66FCF1] px-3.5 py-1.5 rounded-lg transition-all backdrop-blur-md shadow-md">
          <Terminal className="w-3.5 h-3.5 text-[#66FCF1]" />
          <span className="font-mono text-xs text-[#66FCF1] max-w-[280px] truncate">
            {currentUrl}
          </span>
          <button
            type="button"
            onClick={handleCopyUrl}
            title="Copy address"
            className="p-1 text-[#45A29E] hover:text-[#66FCF1] transition-colors rounded"
          >
            {copied ? <Check className="w-3.5 h-3.5 text-[#00FFCC]" /> : <Copy className="w-3.5 h-3.5" />}
          </button>
        </div>

        {/* Cyberpunk Troubleshooting Protocol Container */}
        <div className="mt-5 w-full bg-[#1F2833]/95 border border-[#45A29E]/50 rounded-xl p-5 shadow-[0_10px_35px_rgba(0,0,0,0.6)] backdrop-blur-lg relative overflow-hidden">
          {/* Cyber Corner Accents */}
          <div className="absolute top-0 left-0 w-3.5 h-3.5 border-t-2 border-l-2 border-[#66FCF1]" />
          <div className="absolute top-0 right-0 w-3.5 h-3.5 border-t-2 border-r-2 border-[#66FCF1]" />
          <div className="absolute bottom-0 left-0 w-3.5 h-3.5 border-b-2 border-l-2 border-[#66FCF1]" />
          <div className="absolute bottom-0 right-0 w-3.5 h-3.5 border-b-2 border-r-2 border-[#66FCF1]" />

          {/* Header */}
          <div className="flex items-center justify-between pb-3 border-b border-[#45A29E]/30">
            <div className="flex items-center space-x-2">
              <ShieldAlert className="w-4 h-4 text-[#66FCF1]" />
              <h2 className="font-mono text-xs font-bold tracking-[0.18em] text-[#66FCF1] uppercase">
                SYSTEM RECOVERY PROTOCOL
              </h2>
            </div>
            <span className="font-mono text-[10px] text-[#45A29E] tracking-wider uppercase">
              DNS / LINK FAILURE
            </span>
          </div>

          {/* 3 Core Diagnostic Cards: ROUTER, POWER, and WIFI SIGNAL */}
          <div className="mt-4 space-y-3 text-left">
            {/* 1. CHECK THE ROUTER */}
            <div className="flex items-start space-x-3.5 p-3 rounded-lg bg-[#0B0C10]/70 border border-[#45A29E]/30 hover:border-[#66FCF1]/60 transition-colors">
              <div className="flex-shrink-0 w-8 h-8 rounded-lg bg-[#66FCF1]/10 border border-[#66FCF1]/60 flex items-center justify-center shadow-[0_0_10px_rgba(102,252,241,0.2)]">
                <Radio className="w-4 h-4 text-[#66FCF1]" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between">
                  <h3 className="text-xs font-extrabold text-white font-mono tracking-wider uppercase">
                    1. CHECK THE ROUTER
                  </h3>
                  <span className="font-mono text-[9px] text-[#66FCF1] uppercase font-bold tracking-wider">
                    GATEWAY
                  </span>
                </div>
                <p className="mt-1 text-xs text-[#C5C6C7]/90 leading-relaxed font-sans">
                  Ensure your router is active and that your client device is on the exact same local network/subnet as the Tommi OS server.
                </p>
              </div>
            </div>

            {/* 2. CHECK POWER */}
            <div className="flex items-start space-x-3.5 p-3 rounded-lg bg-[#0B0C10]/70 border border-[#45A29E]/30 hover:border-[#66FCF1]/60 transition-colors">
              <div className="flex-shrink-0 w-8 h-8 rounded-lg bg-[#66FCF1]/10 border border-[#66FCF1]/60 flex items-center justify-center shadow-[0_0_10px_rgba(102,252,241,0.2)]">
                <Zap className="w-4 h-4 text-[#66FCF1]" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between">
                  <h3 className="text-xs font-extrabold text-white font-mono tracking-wider uppercase">
                    2. CHECK POWER
                  </h3>
                  <span className="font-mono text-[9px] text-[#66FCF1] uppercase font-bold tracking-wider">
                    HARDWARE
                  </span>
                </div>
                <p className="mt-1 text-xs text-[#C5C6C7]/90 leading-relaxed font-sans">
                  Confirm the Tommi OS host hardware is plugged in, powered on, has steady status LEDs, and that the backend service is executing.
                </p>
              </div>
            </div>

            {/* 3. CHECK WI-FI SIGNAL */}
            <div className="flex items-start space-x-3.5 p-3 rounded-lg bg-[#0B0C10]/70 border border-[#45A29E]/30 hover:border-[#66FCF1]/60 transition-colors">
              <div className="flex-shrink-0 w-8 h-8 rounded-lg bg-[#66FCF1]/10 border border-[#66FCF1]/60 flex items-center justify-center shadow-[0_0_10px_rgba(102,252,241,0.2)]">
                <Wifi className="w-4 h-4 text-[#66FCF1]" />
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between">
                  <h3 className="text-xs font-extrabold text-white font-mono tracking-wider uppercase">
                    3. CHECK WI-FI SIGNAL
                  </h3>
                  <span className="font-mono text-[9px] text-[#66FCF1] uppercase font-bold tracking-wider">
                    WIRELESS
                  </span>
                </div>
                <p className="mt-1 text-xs text-[#C5C6C7]/90 leading-relaxed font-sans">
                  Verify strong Wi-Fi signal coverage with no wireless drops or IP isolation preventing local device discovery.
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Action Controls */}
        <div className="mt-5 w-full flex items-center gap-3">
          <button
            type="button"
            id="error-change-ip-btn"
            onClick={onEditUrlClick}
            className="flex-1 flex items-center justify-center gap-2 h-12 rounded-lg border border-[#66FCF1]/70 bg-[#1F2833]/80 hover:bg-[#66FCF1]/15 text-[#66FCF1] font-mono font-bold text-xs tracking-wider uppercase transition-all shadow-[0_0_12px_rgba(102,252,241,0.15)] active:scale-98"
          >
            <Edit3 className="w-4 h-4" />
            <span>CHANGE IP / URL</span>
          </button>

          <button
            type="button"
            id="error-reconnect-link-btn"
            onClick={onRetry}
            className="flex-[1.2] flex items-center justify-center gap-2 h-12 rounded-lg bg-[#66FCF1] hover:bg-[#66FCF1]/90 text-[#0B0C10] font-mono font-extrabold text-xs tracking-wider uppercase transition-all shadow-[0_0_20px_rgba(102,252,241,0.4)] active:scale-98"
          >
            <RotateCw className="w-4 h-4" />
            <span>RECONNECT LINK</span>
          </button>
        </div>
      </div>
    </div>
  );
};
