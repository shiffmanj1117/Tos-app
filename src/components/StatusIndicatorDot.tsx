import React from 'react';

interface StatusIndicatorDotProps {
  isError: boolean;
}

export const StatusIndicatorDot: React.FC<StatusIndicatorDotProps> = ({ isError }) => {
  return (
    <span
      id="status-indicator-dot"
      className={`inline-block w-2.5 h-2.5 rounded-full transition-colors duration-300 animate-pulse-slow ${
        isError ? 'bg-[#FF4D4D] shadow-[0_0_8px_#FF4D4D]' : 'bg-[#00FFCC] shadow-[0_0_8px_#00FFCC]'
      }`}
      title={isError ? 'Disconnected / System Error' : 'System Connected / Active'}
    />
  );
};
