import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Settings, RefreshCw, ExternalLink } from 'lucide-react';
import { StatusIndicatorDot } from './components/StatusIndicatorDot';
import { UrlConfigModal } from './components/UrlConfigModal';
import { TommiOsLoadingScreen } from './components/TommiOsLoadingScreen';
import { TommiOsErrorPage } from './components/TommiOsErrorPage';
import { TommiOsSystemCrashPage } from './components/TommiOsSystemCrashPage';
import { initializeAndroidBridge } from './utils/androidBridge';

const DEFAULT_URL = 'https://tommi-os.local:3000';
const STORAGE_KEY = 'tommi_os_target_url';

export const App: React.FC = () => {
  const [targetUrl, setTargetUrl] = useState<string>(() => {
    try {
      return localStorage.getItem(STORAGE_KEY) || DEFAULT_URL;
    } catch {
      return DEFAULT_URL;
    }
  });

  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isError, setIsError] = useState<boolean>(false);
  const [fatalError, setFatalError] = useState<string | null>(null);
  const [isConfigOpen, setIsConfigOpen] = useState<boolean>(false);
  const [iframeKey, setIframeKey] = useState<number>(0);

  const probeAbortControllerRef = useRef<AbortController | null>(null);
  const timeoutRef = useRef<number | null>(null);
  const iframeRef = useRef<HTMLIFrameElement | null>(null);

  // Initialize Android Bridge support on mount
  useEffect(() => {
    initializeAndroidBridge(iframeRef.current);
  }, []);

  // Active Connection Probe to detect unresolvable DNS / down servers early
  const verifyConnectivity = useCallback(async (url: string) => {
    if (probeAbortControllerRef.current) {
      probeAbortControllerRef.current.abort();
    }
    const controller = new AbortController();
    probeAbortControllerRef.current = controller;

    try {
      // Create a fast probe timeout (3.5 seconds)
      const timeoutPromise = new Promise((_, reject) =>
        setTimeout(() => reject(new Error('Connection timed out')), 3500)
      );

      const probePromise = fetch(url, {
        method: 'HEAD',
        mode: 'no-cors',
        cache: 'no-store',
        signal: controller.signal,
      });

      await Promise.race([probePromise, timeoutPromise]);
      // If we reach here, server responded at network layer
      setIsLoading(false);
      setIsError(false);
    } catch {
      // DNS failure (e.g. tommi-os.local unresolved), refused connection, or timeout
      if (!controller.signal.aborted) {
        setIsLoading(false);
        setIsError(true);
      }
    }
  }, []);

  useEffect(() => {
    if (isLoading) {
      verifyConnectivity(targetUrl);
    }

    return () => {
      if (probeAbortControllerRef.current) {
        probeAbortControllerRef.current.abort();
      }
      if (timeoutRef.current) {
        window.clearTimeout(timeoutRef.current);
      }
    };
  }, [isLoading, targetUrl, iframeKey, verifyConnectivity]);

  const handleSaveUrl = (newUrl: string) => {
    const trimmed = newUrl.trim();
    if (trimmed) {
      setTargetUrl(trimmed);
      try {
        localStorage.setItem(STORAGE_KEY, trimmed);
      } catch (err) {
        console.warn('Failed to persist to localStorage', err);
      }
      setIsConfigOpen(false);
      setIsError(false);
      setIsLoading(true);
      setIframeKey((prev) => prev + 1);
    }
  };

  const handleRetry = () => {
    setIsError(false);
    setFatalError(null);
    setIsLoading(true);
    setIframeKey((prev) => prev + 1);
  };

  const handleDiagnosticsClick = () => {
    if (probeAbortControllerRef.current) {
      probeAbortControllerRef.current.abort();
    }
    if (timeoutRef.current) {
      window.clearTimeout(timeoutRef.current);
    }
    setIsLoading(false);
    setIsError(true);
  };

  return (
    <div className="flex flex-col h-screen w-screen bg-[#0B0C10] text-[#C5C6C7] overflow-hidden select-none">
      {/* Top App Bar - shown during loading, error states, or diagnostic flows */}
      {(isLoading || isError || fatalError !== null) && (
        <header
          id="top-app-bar"
          className="flex-shrink-0 h-14 bg-[#0B0C10] border-b border-[#45A29E]/20 px-4 flex items-center justify-between z-40 transition-all duration-300"
        >
          {/* Left spacer for optical center balance */}
          <div className="w-10 flex items-center" />

          {/* Center Title & Status Indicator Dot */}
          <div className="flex items-center justify-center space-x-2.5">
            <span className="font-mono text-sm md:text-base font-bold tracking-[0.2em] text-[#66FCF1]">
              TOMMI OS
            </span>
            <StatusIndicatorDot isError={isError || fatalError !== null} />
          </div>

          {/* Action Controls */}
          <div className="flex items-center space-x-1">
            <button
              id="settings-button"
              onClick={() => setIsConfigOpen(true)}
              title="Configure connection URL"
              className="p-2 text-[#66FCF1] hover:bg-[#1F2833] rounded-lg transition-colors"
            >
              <Settings className="w-5 h-5" />
            </button>
          </div>
        </header>
      )}

      {/* Floating minimal overlay control when successfully connected in full screen */}
      {!isLoading && !isError && !fatalError && (
        <div
          id="fullscreen-floating-controls"
          className="fixed top-3 right-3 z-50 flex items-center space-x-1.5 opacity-40 hover:opacity-100 focus-within:opacity-100 transition-opacity duration-200 bg-[#0B0C10]/80 backdrop-blur-md px-2 py-1 rounded-full border border-[#45A29E]/30 shadow-lg"
        >
          <button
            id="floating-reload-btn"
            onClick={handleRetry}
            title="Reload View"
            className="p-1.5 text-[#45A29E] hover:text-[#66FCF1] hover:bg-[#1F2833] rounded-full transition-colors"
          >
            <RefreshCw className="w-3.5 h-3.5" />
          </button>
          <a
            href={targetUrl}
            target="_blank"
            rel="noopener noreferrer"
            title="Open direct URL in new tab"
            className="p-1.5 text-[#45A29E] hover:text-[#66FCF1] hover:bg-[#1F2833] rounded-full transition-colors"
          >
            <ExternalLink className="w-3.5 h-3.5" />
          </a>
          <button
            id="floating-settings-btn"
            onClick={() => setIsConfigOpen(true)}
            title="Configure connection URL"
            className="p-1.5 text-[#66FCF1] hover:bg-[#1F2833] rounded-full transition-colors"
          >
            <Settings className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {/* Main View Area - Full edge-to-edge screen when connected */}
      <main id="main-content-viewport" className="relative flex-1 w-full h-full overflow-hidden bg-[#0B0C10]">
        {fatalError ? (
          <TommiOsSystemCrashPage
            errorMessage={fatalError}
            onRetry={handleRetry}
          />
        ) : isError ? (
          <TommiOsErrorPage
            currentUrl={targetUrl}
            onRetry={handleRetry}
            onEditUrlClick={() => setIsConfigOpen(true)}
          />
        ) : (
          <>
            {/* Embedded Web View with full Camera, Microphone, Geolocation, and Android Bridge capabilities */}
            <iframe
              ref={iframeRef}
              key={iframeKey}
              id="tommi-os-webview"
              src={targetUrl}
              title="Tommi OS Runtime"
              allow="camera *; microphone *; geolocation *; display-capture *; autoplay *; clipboard-read *; clipboard-write *; fullscreen *"
              allowFullScreen
              className={`w-full h-full border-0 transition-opacity duration-500 ${
                isLoading ? 'opacity-0 pointer-events-none' : 'opacity-100'
              }`}
              sandbox="allow-scripts allow-same-origin allow-forms allow-popups allow-modals allow-downloads"
              onLoad={() => {
                setIsLoading(false);
                setIsError(false);
              }}
              onError={() => {
                setIsLoading(false);
                setIsError(true);
              }}
            />

            {/* Immersive Boot & AI Initialization Loading Screen Overlay */}
            {isLoading && (
              <div className="absolute inset-0 z-30">
                <TommiOsLoadingScreen
                  onDiagnosticsClick={handleDiagnosticsClick}
                />
              </div>
            )}
          </>
        )}
      </main>

      {/* URL Config inline overlay modal for smart network troubleshooting */}
      <UrlConfigModal
        isOpen={isConfigOpen}
        currentUrl={targetUrl}
        onClose={() => setIsConfigOpen(false)}
        onSave={handleSaveUrl}
      />
    </div>
  );
};

export default App;
