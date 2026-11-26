import React from 'react';
import { CheckCircle2, Lock } from 'lucide-react';
import { TableBgId } from '../game/balootTypes';

export const SaudiEmblemSvg: React.FC<{ color: string; className?: string }> = ({
  color,
  className = 'w-32 h-32',
}) => (
  <svg
    viewBox="0 0 160 160"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    className={className}
    aria-hidden="true"
  >
    {/* Palm Tree Trunk */}
    <path d="M76.5,50 L83.5,50 L85.2,89 L86.8,95 L73.2,95 L74.8,89 Z" fill={color} />
    {/* Crown Center Frond */}
    <path d="M80,49 C77.6,36 77.9,22 80,12 C82.1,22 82.4,36 80,49 Z" fill={color} />
    {/* Left Fronds */}
    <path d="M78.6,48 C74.5,35 70.2,23 64.5,15.5 C67.6,23.5 71.2,34.5 75.5,48.5 Z" fill={color} />
    <path d="M77.4,49 C70.5,36 62.5,26.5 52.5,22.5 C60.2,29 67.2,38 74.2,50 Z" fill={color} />
    <path d="M76.2,50.5 C66.5,39.5 55.2,33.5 44,33.5 C53.8,37.8 63.5,44 73.2,52 Z" fill={color} />
    <path d="M75.5,52.5 C63.5,44.5 50.5,43 39.5,47.5 C50.5,48.2 61.5,50.8 73,54.8 Z" fill={color} />
    <path d="M75.2,55 C62.5,51.5 49.5,54.5 41.5,63.5 C51.5,59.5 62.5,58 73.5,58.2 Z" fill={color} />
    <path d="M75.5,58.2 C65,59 55.5,65 51,75 C59,68.5 67,64.2 74.8,61.5 Z" fill={color} />
    {/* Right Fronds */}
    <path d="M81.4,48 C85.5,35 89.8,23 95.5,15.5 C92.4,23.5 88.8,34.5 84.5,48.5 Z" fill={color} />
    <path d="M82.6,49 C89.5,36 97.5,26.5 107.5,22.5 C99.8,29 92.8,38 85.8,50 Z" fill={color} />
    <path d="M83.8,50.5 C93.5,39.5 104.8,33.5 116,33.5 C106.2,37.8 96.5,44 86.8,52 Z" fill={color} />
    <path d="M84.5,52.5 C96.5,44.5 109.5,43 120.5,47.5 C109.5,48.2 98.5,50.8 87,54.8 Z" fill={color} />
    <path d="M84.8,55 C97.5,51.5 110.5,54.5 118.5,63.5 C108.5,59.5 97.5,58 86.5,58.2 Z" fill={color} />
    <path d="M84.5,58.2 C95,59 104.5,65 109,75 C101,68.5 93,64.2 85.2,61.5 Z" fill={color} />
    {/* Saber 1 */}
    <path
      d="M53.5,116.5 C68,107 88,97.5 112,93.5 C126,91.2 138.5,91.5 148,93.8 C137,96.8 124.5,99.5 111.5,102.5 C90,107.5 72,115.5 57.5,123 Z"
      fill={color}
    />
    <path
      d="M48.5,112 L52,109.5 L61.5,125 L58,127.5 L54.5,122 L44.5,128.5 C40.2,131 35.8,128.5 36.2,124 C36.6,120.5 40.5,119.5 42.6,121.6 C41.5,123.5 43.2,125.2 45.2,123.8 L51.5,118.8 Z"
      fill={color}
    />
    {/* Saber 2 */}
    <path
      d="M106.5,116.5 C92,107 72,97.5 48,93.5 C34,91.2 21.5,91.5 12,93.8 C23,96.8 35.5,99.5 48.5,102.5 C70,107.5 88,115.5 102.5,123 Z"
      fill={color}
    />
    <path
      d="M111.5,112 L108,109.5 L98.5,125 L102,127.5 L105.5,122 L115.5,128.5 C119.8,131 124.2,128.5 123.8,124 C123.4,120.5 119.5,119.5 117.4,121.6 C118.5,123.5 116.8,125.2 114.8,123.8 L108.5,118.8 Z"
      fill={color}
    />
  </svg>
);

export const TableBackgroundCanvas: React.FC<{
  bgId: TableBgId;
  mini?: boolean;
  className?: string;
}> = ({ bgId, mini = false, className = 'absolute inset-0 w-full h-full' }) => {
  const emblemSize = mini ? 'w-10 h-10' : 'w-36 h-36 sm:w-44 sm:h-44';
  const subLogoSize = mini ? 'w-8' : 'w-20 sm:w-24';

  if (bgId === 'classic_white') {
    return (
      <div className={`${className} bg-white flex items-center justify-center overflow-hidden select-none`}>
        <img
          src="/assets/shaddaha_logo.png"
          alt=""
          className={`${mini ? 'w-12' : 'w-44 sm:w-56'} opacity-25 object-contain pointer-events-none`}
        />
      </div>
    );
  }

  if (bgId === 'classic_green') {
    return (
      <div
        className={`${className} bg-[#0A6228] flex items-center justify-center overflow-hidden select-none`}
      >
        {/* Subtle felt grid lines */}
        <svg
          className="absolute inset-0 w-full h-full pointer-events-none"
          preserveAspectRatio="none"
          viewBox="0 0 400 400"
        >
          <defs>
            <pattern id={mini ? 'feltMini' : 'feltFull'} width="64" height="64" patternUnits="userSpaceOnUse">
              <path d="M 64 0 L 0 0 0 64" fill="none" stroke="rgba(255,255,255,0.06)" strokeWidth="1.5" />
            </pattern>
          </defs>
          <rect width="400" height="400" fill={`url(#${mini ? 'feltMini' : 'feltFull'})`} />
          <rect
            x="24"
            y="24"
            width="352"
            height="352"
            rx="28"
            fill="rgba(255,255,255,0.04)"
            stroke="rgba(255,255,255,0.16)"
            strokeWidth={mini ? '3' : '2'}
          />
        </svg>
        <img
          src="/assets/shaddaha_logo.png"
          alt=""
          className={`${mini ? 'w-12' : 'w-44 sm:w-56'} opacity-45 object-contain pointer-events-none relative z-10`}
        />
      </div>
    );
  }

  if (bgId === 'saudi_gold') {
    // Exact flat Saudi gold (#CDA200), crisp white Saudi emblem, subtle watermark logo — zero added gradients
    return (
      <div
        className={`${className} bg-[#CDA200] flex flex-col items-center justify-center overflow-hidden select-none`}
      >
        <SaudiEmblemSvg color="#FFFFFF" className={emblemSize} />
        <img
          src="/assets/shaddaha_logo.png"
          alt=""
          className={`${subLogoSize} opacity-30 object-contain mt-1 pointer-events-none`}
        />
      </div>
    );
  }

  if (bgId === 'saudi_royal_dark_gold') {
    // Exact flat royal dark green (#093F1E), crisp white geometric contour lines, gold Saudi emblem (#DFB400) — zero added gradients
    return (
      <div
        className={`${className} bg-[#093F1E] flex flex-col items-center justify-center overflow-hidden select-none`}
      >
        <svg
          className="absolute inset-0 w-full h-full pointer-events-none"
          viewBox="0 0 400 700"
          preserveAspectRatio="none"
        >
          <g
            stroke="rgba(255,255,255,0.9)"
            strokeWidth={mini ? '2.5' : '1.8'}
            strokeLinecap="round"
            strokeLinejoin="round"
            fill="none"
          >
            <path d="M 58 0 L 0 115" />
            <path d="M 232 0 L 198 80 L 18 168 L 400 88" />
            <path d="M 0 360 L 72 278 L 292 241 L 400 200" />
            <path d="M 400 381 L 370 414 L 400 407" />
            <path d="M 0 539 Q 184 476 400 453" />
            <path d="M 0 635 L 238 586 L 182 700" />
            <path d="M 400 620 L 358 700" />
          </g>
        </svg>
        <div className="relative z-10 flex flex-col items-center">
          <SaudiEmblemSvg color="#DFB400" className={emblemSize} />
          <img
            src="/assets/shaddaha_logo.png"
            alt=""
            className={`${subLogoSize} opacity-35 object-contain mt-1 pointer-events-none`}
          />
        </div>
      </div>
    );
  }

  // saudi_green: Exact flat Saudi green (#046631), crisp white Saudi emblem — zero added gradients
  return (
    <div
      className={`${className} bg-[#046631] flex flex-col items-center justify-center overflow-hidden select-none`}
    >
      <SaudiEmblemSvg color="#FFFFFF" className={emblemSize} />
      <img
        src="/assets/shaddaha_logo.png"
        alt=""
        className={`${subLogoSize} opacity-30 object-contain mt-1 pointer-events-none`}
      />
    </div>
  );
};

export const TableBackgroundThumbnail: React.FC<{
  bgId: TableBgId;
  selected: boolean;
  locked: boolean;
}> = ({ bgId, selected, locked }) => (
  <div
    className={`relative w-20 h-28 rounded-2xl overflow-hidden border-2 shrink-0 transition-transform ${
      selected ? 'border-[#002BFF] shadow-md' : 'border-slate-200'
    }`}
  >
    <TableBackgroundCanvas bgId={bgId} mini />
    {selected && (
      <span className="absolute top-1.5 left-1.5 bg-white rounded-full p-0.5 text-[#002BFF] shadow">
        <CheckCircle2 className="w-4 h-4" />
      </span>
    )}
    {!selected && locked && (
      <span className="absolute top-1.5 left-1.5 bg-[#FFD000] rounded-full p-1 text-[#111827] shadow">
        <Lock className="w-3.5 h-3.5" />
      </span>
    )}
  </div>
);
