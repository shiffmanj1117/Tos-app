import React, { useState } from 'react';
import { AlertTriangle, RotateCw } from 'lucide-react';

interface TommiOsSystemCrashPageProps {
  errorMessage: string;
  onRetry: () => void;
}

export const TommiOsSystemCrashPage: React.FC<TommiOsSystemCrashPageProps> = ({
  errorMessage,
  onRetry,
}) => {
  const [imgError, setImgError] = useState(false);

  return (
    <div
      id="system-crash-page"
      className="w-full h-full flex flex-col items-center justify-center overflow-y-auto px-6 py-8 bg-[#0B0C10] text-[#C5C6C7]"
    >
      <div className="max-w-md w-full flex flex-col items-center my-auto">
        {/* Warning Tag */}
        <div className="inline-flex items-center px-3 py-1 rounded bg-[#FF9500]/15 border border-[#FF9500]/60">
          <span className="font-mono text-[11px] font-bold tracking-widest text-[#FF9500]">
            SYSTEM COMPATIBILITY ALERTER
          </span>
        </div>

        {/* Warning Avatar */}
        <div className="relative mt-6 mb-4 w-36 h-36 flex items-center justify-center">
          <div className="absolute w-28 h-28 rounded-full bg-radial from-[#FF9500]/20 to-transparent blur-sm" />
          <div className="relative z-10 w-24 h-24 rounded-full overflow-hidden border-2 border-[#FF9500] flex items-center justify-center bg-[#1F2833]">
            {!imgError ? (
              <img
                src="/img_app_icon.jpg"
                alt="Warning avatar"
                onError={() => setImgError(true)}
                className="w-full h-full object-cover"
              />
            ) : (
              <AlertTriangle className="w-10 h-10 text-[#FF9500]" />
            )}
          </div>
        </div>

        <h1 className="text-xl font-bold text-white text-center">
          Connection Frame Issue
        </h1>
        <p className="mt-1.5 text-xs text-[#C5C6C7]/80 text-center px-4">
          The embedded web runtime encountered an unexpected response or blocked frame.
        </p>

        {/* Details Card */}
        <div className="mt-4 w-full bg-[#1F2833] border border-[#FF9500]/30 rounded-lg p-3">
          <p className="font-mono text-xs text-[#FF9500] break-words">
            Details: {errorMessage}
          </p>
        </div>

        {/* Resolution checklist */}
        <div className="mt-5 w-full bg-[#1F2833] border border-[#FF9500]/30 rounded-xl p-5">
          <h2 className="font-mono text-xs font-bold text-[#FF9500] tracking-wider uppercase mb-3">
            HOW TO SOLVE THIS
          </h2>
          <div className="space-y-3 text-left">
            <div className="flex items-start space-x-3">
              <div className="flex-shrink-0 w-5 h-5 rounded-full bg-[#FF9500]/10 border border-[#FF9500] flex items-center justify-center">
                <span className="font-mono text-[10px] font-bold text-[#FF9500]">1</span>
              </div>
              <div>
                <h3 className="text-xs font-bold text-white">Check Server Security Policies</h3>
                <p className="text-[11px] text-[#C5C6C7]/80">
                  Ensure the target server does not send restrictive X-Frame-Options or CSP headers for embedded frames.
                </p>
              </div>
            </div>

            <div className="flex items-start space-x-3">
              <div className="flex-shrink-0 w-5 h-5 rounded-full bg-[#FF9500]/10 border border-[#FF9500] flex items-center justify-center">
                <span className="font-mono text-[10px] font-bold text-[#FF9500]">2</span>
              </div>
              <div>
                <h3 className="text-xs font-bold text-white">Direct URL Access</h3>
                <p className="text-[11px] text-[#C5C6C7]/80">
                  Try opening the server URL directly in a browser tab if cross-origin framing is disabled.
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Retry Button */}
        <button
          type="button"
          onClick={onRetry}
          className="mt-6 w-full flex items-center justify-center gap-2 h-12 rounded-lg bg-[#FF9500] text-[#0B0C10] font-bold text-sm hover:bg-[#FF9500]/90 transition-all active:scale-98"
        >
          <RotateCw className="w-4 h-4" />
          <span>Retry Initialization</span>
        </button>
      </div>
    </div>
  );
};
