import React, { useState, useEffect } from 'react';
import { Settings, X, Globe, Server, Check } from 'lucide-react';

interface UrlConfigModalProps {
  isOpen: boolean;
  currentUrl: string;
  onClose: () => void;
  onSave: (newUrl: string) => void;
}

export const UrlConfigModal: React.FC<UrlConfigModalProps> = ({
  isOpen,
  currentUrl,
  onClose,
  onSave,
}) => {
  const [tempUrl, setTempUrl] = useState(currentUrl);

  useEffect(() => {
    setTempUrl(currentUrl);
  }, [currentUrl, isOpen]);

  if (!isOpen) return null;

  const handlePresetSelect = (url: string) => {
    setTempUrl(url);
  };

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    if (tempUrl.trim()) {
      onSave(tempUrl.trim());
    }
  };

  return (
    <div
      id="url-config-modal-overlay"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-[#0B0C10]/90 backdrop-blur-sm"
      onClick={onClose}
    >
      <div
        id="url-config-modal-card"
        className="w-full max-w-lg bg-[#1F2833] border border-[#66FCF1]/60 rounded-xl shadow-[0_0_25px_rgba(102,252,241,0.15)] p-6 text-[#C5C6C7] overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-[#45A29E]/30">
          <div className="flex items-center space-x-2.5">
            <Settings className="w-5 h-5 text-[#66FCF1]" />
            <h2 className="font-mono text-base font-bold tracking-wider text-[#66FCF1]">
              CONFIGURE CONNECTION
            </h2>
          </div>
          <button
            id="modal-close-btn"
            onClick={onClose}
            className="p-1 text-[#C5C6C7] hover:text-white rounded hover:bg-[#0B0C10] transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Warning / Informational note */}
        <div className="mt-4 text-xs font-mono text-[#C5C6C7] leading-relaxed bg-[#0B0C10]/50 p-3 rounded-lg border border-[#45A29E]/30">
          Android emulators, local devices, and web containers can struggle to resolve <code className="text-[#66FCF1]">.local</code> DNS names. If connection fails, configure a direct IP address or use the default gateway loopback to connect to your host server.
        </div>

        {/* Quick Shortcuts */}
        <div className="mt-5">
          <p className="text-[11px] font-mono font-bold text-[#45A29E] tracking-wide mb-2 uppercase">
            QUICK SHORTCUTS:
          </p>
          <div className="grid grid-cols-2 gap-2.5">
            <button
              type="button"
              id="preset-emulator-host-btn"
              onClick={() => handlePresetSelect('http://10.0.2.2:3000')}
              className={`flex flex-col items-center justify-center p-2.5 rounded-lg border transition-all text-center ${
                tempUrl === 'http://10.0.2.2:3000'
                  ? 'bg-[#0B0C10] border-[#66FCF1] text-[#66FCF1] ring-1 ring-[#66FCF1]'
                  : 'bg-[#0B0C10]/80 border-[#45A29E]/50 text-[#C5C6C7] hover:border-[#45A29E]'
              }`}
            >
              <span className="font-mono text-[11px] font-bold flex items-center gap-1">
                <Server className="w-3 h-3 text-[#66FCF1]" />
                EMULATOR HOST
              </span>
              <span className="font-mono text-[10px] text-[#C5C6C7]/80 mt-0.5">
                10.0.2.2:3000
              </span>
            </button>

            <button
              type="button"
              id="preset-default-host-btn"
              onClick={() => handlePresetSelect('https://tommi-os.local:3000')}
              className={`flex flex-col items-center justify-center p-2.5 rounded-lg border transition-all text-center ${
                tempUrl === 'https://tommi-os.local:3000'
                  ? 'bg-[#0B0C10] border-[#66FCF1] text-[#66FCF1] ring-1 ring-[#66FCF1]'
                  : 'bg-[#0B0C10]/80 border-[#45A29E]/50 text-[#C5C6C7] hover:border-[#45A29E]'
              }`}
            >
              <span className="font-mono text-[11px] font-bold flex items-center gap-1">
                <Globe className="w-3 h-3 text-[#66FCF1]" />
                DEFAULT HOST
              </span>
              <span className="font-mono text-[10px] text-[#C5C6C7]/80 mt-0.5">
                tommi-os.local:3000
              </span>
            </button>
          </div>
        </div>

        {/* Input Form */}
        <form onSubmit={handleSave} className="mt-5 space-y-4">
          <div>
            <label
              htmlFor="target-url-input"
              className="block font-mono text-xs font-semibold text-[#66FCF1] mb-1.5"
            >
              Tommi OS Address
            </label>
            <div className="relative">
              <input
                id="target-url-input"
                type="text"
                value={tempUrl}
                onChange={(e) => setTempUrl(e.target.value)}
                placeholder="https://tommi-os.local:3000"
                className="w-full bg-[#0B0C10] border border-[#45A29E] focus:border-[#66FCF1] focus:outline-none rounded-lg px-3.5 py-2.5 text-sm font-mono text-white placeholder-[#C5C6C7]/40 shadow-inner"
                autoFocus
              />
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex items-center justify-end space-x-3 pt-3 border-t border-[#45A29E]/30">
            <button
              type="button"
              id="cancel-url-btn"
              onClick={onClose}
              className="px-4 py-2 text-xs font-mono font-bold text-[#C5C6C7] hover:text-white transition-colors"
            >
              CANCEL
            </button>
            <button
              type="submit"
              id="save-url-btn"
              className="flex items-center gap-1.5 px-4 py-2 bg-[#66FCF1] text-[#0B0C10] hover:bg-[#66FCF1]/90 rounded font-mono text-xs font-bold transition-all shadow-[0_0_12px_rgba(102,252,241,0.3)] active:scale-95"
            >
              <Check className="w-3.5 h-3.5" />
              SAVE & CONNECT
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
