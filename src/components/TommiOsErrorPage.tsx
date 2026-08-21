import React, { useState } from 'react';
import { WifiOff, Info, Edit3, RotateCw } from 'lucide-react';

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
  const [imgError, setImgError] = useState(false);

  return (
    <div
      id="tommi-error-page"
      className="w-full h-full flex flex-col items-center justify-center overflow-y-auto px-6 py-10 bg-[#0B0C10] text-[#C5C6C7]"
    >
      <div className="max-w-md w-full flex flex-col items-center my-auto">
        {/* 404 Header Tag */}
        <div className="inline-flex items-center px-3 py-1 rounded bg-[#FF3B30]/15 border border-[#FF3B30]/60">
          <span className="font-mono text-[11px] font-bold tracking-widest text-[#FF3B30]">
            ERROR 404 - SYSTEM NOT FOUND
          </span>
        </div>

        {/* Floating Robot Avatar */}
        <div className="relative mt-8 mb-4 w-48 h-48 flex items-center justify-center animate-float">
          {/* Radial Cyan Halo Base */}
          <div className="absolute w-36 h-36 rounded-full bg-radial from-[#66FCF1]/25 to-transparent blur-md" />

          <div className="relative z-10 w-32 h-32 rounded-full overflow-hidden border-2 border-[#66FCF1] flex items-center justify-center bg-[#1F2833] shadow-[0_0_20px_rgba(102,252,241,0.35)]">
            {!imgError ? (
              <img
                src="/img_app_icon.jpg"
                alt="Floating robot diagnostic avatar"
                onError={() => setImgError(true)}
                className="w-full h-full object-cover"
              />
            ) : (
              <WifiOff className="w-14 h-14 text-[#66FCF1]" />
            )}
          </div>
        </div>

        {/* Title */}
        <h1 className="text-2xl font-bold text-white text-center tracking-tight">
          Tommi OS Not Found
        </h1>

        {/* Subtitle with current unreachable address */}
        <p className="mt-2 text-xs md:text-sm font-mono text-[#C5C6C7]/70 text-center px-4 break-all">
          Could not connect to <span className="text-[#66FCF1]">{currentUrl}</span>
        </p>

        {/* Troubleshooting Checklist Card */}
        <div className="mt-7 w-full bg-[#1F2833] border border-[#45A29E]/40 rounded-xl p-5 shadow-lg">
          <div className="flex items-center space-x-2 pb-3 border-b border-[#45A29E]/20">
            <Info className="w-4 h-4 text-[#66FCF1]" />
            <h2 className="font-mono text-xs font-bold tracking-wider text-[#66FCF1] uppercase">
              TROUBLESHOOTING CHECKLIST
            </h2>
          </div>

          <div className="mt-4 space-y-4 text-left">
            {/* Step 1 */}
            <div className="flex items-start space-x-3">
              <div className="flex-shrink-0 w-6 h-6 rounded-full bg-[#66FCF1]/10 border border-[#66FCF1] flex items-center justify-center">
                <span className="font-mono text-xs font-bold text-[#66FCF1]">1</span>
              </div>
              <div>
                <h3 className="text-sm font-bold text-white leading-tight">Check the Router</h3>
                <p className="mt-0.5 text-xs text-[#C5C6C7]/80 leading-relaxed">
                  Verify your phone or browser is connected to the same Wi-Fi network as the Tommi OS server.
                </p>
              </div>
            </div>

            {/* Step 2 */}
            <div className="flex items-start space-x-3">
              <div className="flex-shrink-0 w-6 h-6 rounded-full bg-[#66FCF1]/10 border border-[#66FCF1] flex items-center justify-center">
                <span className="font-mono text-xs font-bold text-[#66FCF1]">2</span>
              </div>
              <div>
                <h3 className="text-sm font-bold text-white leading-tight">Check Power Connection</h3>
                <p className="mt-0.5 text-xs text-[#C5C6C7]/80 leading-relaxed">
                  Ensure the Tommi OS host machine is fully booted, running, and plugged into power.
                </p>
              </div>
            </div>

            {/* Step 3 */}
            <div className="flex items-start space-x-3">
              <div className="flex-shrink-0 w-6 h-6 rounded-full bg-[#66FCF1]/10 border border-[#66FCF1] flex items-center justify-center">
                <span className="font-mono text-xs font-bold text-[#66FCF1]">3</span>
              </div>
              <div>
                <h3 className="text-sm font-bold text-white leading-tight">Verify Port &amp; Firewall</h3>
                <p className="mt-0.5 text-xs text-[#C5C6C7]/80 leading-relaxed">
                  Ensure that service port 3000 is open and not blocked by the host machine&apos;s firewall.
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Action Buttons Row */}
        <div className="mt-7 w-full flex items-center gap-3">
          <button
            type="button"
            id="edit-url-btn"
            onClick={onEditUrlClick}
            className="flex-1 flex items-center justify-center gap-2 h-12 rounded-lg border border-[#66FCF1] text-[#66FCF1] hover:bg-[#66FCF1]/10 font-semibold text-sm transition-all active:scale-98"
          >
            <Edit3 className="w-4 h-4" />
            <span>Change IP</span>
          </button>

          <button
            type="button"
            id="retry-connection-btn"
            onClick={onRetry}
            className="flex-[1.2] flex items-center justify-center gap-2 h-12 rounded-lg bg-[#66FCF1] text-[#0B0C10] hover:bg-[#66FCF1]/90 font-bold text-sm transition-all shadow-[0_0_15px_rgba(102,252,241,0.3)] active:scale-98"
          >
            <RotateCw className="w-4 h-4" />
            <span>Reconnect</span>
          </button>
        </div>
      </div>
    </div>
  );
};
