import React, { useState, useEffect, useRef } from 'react';
import { Settings, RefreshCw, ExternalLink } from 'lucide-react';
import { StatusIndicatorDot } from './components/StatusIndicatorDot';
import { UrlConfigModal } from './components/UrlConfigModal';
import { TommiOsLoadingScreen } from './components/TommiOsLoadingScreen';
import { TommiOsErrorPage } from './components/TommiOsErrorPage';
import { TommiOsSystemCrashPage } from './components/TommiOsSystemCrashPage';

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

  const timeoutRef = useRef<number | null>(null);

  // Network Timeout fallback (8 seconds, identical to Android app logic)
  useEffect(() => {
    if (isLoading) {
      if (timeoutRef.current) {
        window.clearTimeout(timeoutRef.current);
      }

      timeoutRef.current = window.setTimeout(() => {
        if (isLoading && !isError) {
          setIsLoading(false);
          setIsError(true);
        }
      }, 8000);
    }

    return () => {
      if (timeoutRef.current) {
        window.clearTimeout(timeoutRef.current);
      }
    };
  }, [isLoading, isError, targetUrl, iframeKey]);

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
    if (timeoutRef.current) {
      window.clearTimeout(timeoutRef.current);
    }
    setIsLoading(false);
    setIsError(true);
  };

  return (
    <div className="flex flex-col h-screen w-screen bg-[#0B0C10] text-[#C5C6C7] overflow-hidden select-none">
      {/* Small decorative top status bar matching the cyberpunk theme */}
      <header
        id="top-app-bar"
        className="flex-shrink-0 h-14 bg-[#0B0C10] border-b border-[#45A29E]/20 px-4 flex items-center justify-between z-40"
      >
        {/* Left spacer for optical center balance */}
        <div className="w-10 flex items-center">
          {!isLoading && !isError && (
            <button
              id="reload-view-btn"
              onClick={handleRetry}
              title="Reload View"
              className="p-2 text-[#45A29E] hover:text-[#66FCF1] hover:bg-[#1F2833] rounded-lg transition-colors"
            >
              <RefreshCw className="w-4 h-4" />
            </button>
          )}
        </div>

        {/* Center Title & Status Indicator Dot */}
        <div className="flex items-center justify-center space-x-2.5">
          <span className="font-mono text-sm md:text-base font-bold tracking-[0.2em] text-[#66FCF1]">
            TOMMI OS
          </span>
          <StatusIndicatorDot isError={isError || fatalError !== null} />
        </div>

        {/* Action Controls */}
        <div className="flex items-center space-x-1">
          {!isLoading && !isError && (
            <a
              href={targetUrl}
              target="_blank"
              rel="noopener noreferrer"
              title="Open direct URL in new tab"
              className="p-2 text-[#45A29E] hover:text-[#66FCF1] hover:bg-[#1F2833] rounded-lg transition-colors"
            >
              <ExternalLink className="w-4 h-4" />
            </a>
          )}
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

      {/* Main View Area */}
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
            {/* Embedded Web View */}
            <iframe
              key={iframeKey}
              id="tommi-os-webview"
              src={targetUrl}
              title="Tommi OS Runtime"
              className={`w-full h-full border-0 transition-opacity duration-500 ${
                isLoading ? 'opacity-0 pointer-events-none' : 'opacity-100'
              }`}
              sandbox="allow-scripts allow-same-origin allow-forms allow-popups allow-modals"
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
