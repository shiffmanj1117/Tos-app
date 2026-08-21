/**
 * Android Bridge & Web-to-Native Interface Handler for Tommi OS
 * Facilitates two-way communication between Tommi OS web view and native Android host,
 * and polyfills permissions and hardware capability queries.
 */

// Define Android interface types that might be injected by WebView JavascriptInterface
declare global {
  interface Window {
    AndroidBridge?: {
      postMessage?: (msg: string) => void;
      requestPermission?: (perm: string) => boolean | Promise<boolean>;
      getFineLocation?: () => string;
      onCameraAction?: (action: string) => void;
      onAudioAction?: (action: string) => void;
      [key: string]: unknown;
    };
    Android?: {
      postMessage?: (msg: string) => void;
      showToast?: (toast: string) => void;
      [key: string]: unknown;
    };
    TommiNative?: {
      postMessage?: (msg: string) => void;
      [key: string]: unknown;
    };
  }
}

export function initializeAndroidBridge(iframeRef?: HTMLIFrameElement | null): void {
  // Relay messages between parent and child iframe if embedded
  const handleMessage = (event: MessageEvent) => {
    try {
      const data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;

      // Handle Android Bridge permission or hardware requests from iframe
      if (data && data.type === 'TOMMI_ANDROID_BRIDGE_REQUEST') {
        const { action, payload } = data;

        if (window.AndroidBridge && typeof window.AndroidBridge[action] === 'function') {
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          const result = (window.AndroidBridge as any)[action](payload);
          event.source?.postMessage(
            JSON.stringify({
              type: 'TOMMI_ANDROID_BRIDGE_RESPONSE',
              action,
              result,
            }),
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            { targetOrigin: '*' } as any
          );
        } else if (window.Android && typeof window.Android[action] === 'function') {
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          const result = (window.Android as any)[action](payload);
          event.source?.postMessage(
            JSON.stringify({
              type: 'TOMMI_ANDROID_BRIDGE_RESPONSE',
              action,
              result,
            }),
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            { targetOrigin: '*' } as any
          );
        }
      }
    } catch {
      // Non-JSON or standard postMessage, ignore safely
    }
  };

  window.addEventListener('message', handleMessage);

  // Expose bridge relay object if not already present
  if (!window.AndroidBridge) {
    window.AndroidBridge = {
      postMessage: (msg: string) => {
        if (iframeRef && iframeRef.contentWindow) {
          iframeRef.contentWindow.postMessage({ type: 'ANDROID_EVENT', data: msg }, '*');
        }
      },
      requestPermission: async (perm: string) => {
        if (perm === 'camera' || perm === 'microphone') {
          try {
            const stream = await navigator.mediaDevices.getUserMedia({
              video: perm === 'camera',
              audio: perm === 'microphone',
            });
            stream.getTracks().forEach((t) => t.stop());
            return true;
          } catch {
            return false;
          }
        }
        if (perm === 'geolocation') {
          return new Promise<boolean>((resolve) => {
            navigator.geolocation.getCurrentPosition(
              () => resolve(true),
              () => resolve(false),
              { enableHighAccuracy: true, timeout: 5000 }
            );
          });
        }
        return true;
      },
    };
  }
}
