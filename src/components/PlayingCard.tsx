import React from 'react';
import { BalootCard } from '../game/balootTypes';
import { SUIT_INFO } from '../game/balootEngine';

interface PlayingCardProps {
  card: BalootCard;
  size?: 'sm' | 'md' | 'hand';
  selected?: boolean;
  disabled?: boolean;
  highlightTrump?: boolean;
  animatedThrow?: boolean;
  onClick?: () => void;
}

export const PlayingCard: React.FC<PlayingCardProps> = ({
  card,
  size = 'hand',
  selected = false,
  disabled = false,
  highlightTrump = false,
  animatedThrow = false,
  onClick,
}) => {
  const info = SUIT_INFO[card.suit];

  const sizeClasses =
    size === 'hand'
      ? 'w-[74px] h-[104px] sm:w-[88px] sm:h-[124px] rounded-2xl'
      : size === 'md'
        ? 'w-[68px] h-[96px] sm:w-[78px] sm:h-[110px] rounded-xl'
        : 'w-[46px] h-[66px] rounded-lg';

  const rankText =
    size === 'hand'
      ? 'text-lg sm:text-xl'
      : size === 'md'
        ? 'text-base sm:text-lg'
        : 'text-xs';

  const suitCenterText =
    size === 'hand'
      ? 'text-3xl sm:text-4xl'
      : size === 'md'
        ? 'text-2xl sm:text-3xl'
        : 'text-lg';

  return (
    <button
      type="button"
      disabled={disabled || !onClick}
      onClick={onClick}
      className={`relative bg-white select-none flex flex-col justify-between p-1.5 sm:p-2 border-2 transition-all duration-200 ${sizeClasses} ${
        selected
          ? '-translate-y-4 sm:-translate-y-5 scale-105 border-[#FFD000] shadow-[0_14px_28px_rgba(0,43,255,0.38)] ring-2 ring-[#FFD000] z-20'
          : highlightTrump
            ? 'border-[#FFD000] shadow-md'
            : 'border-slate-200 shadow-md'
      } ${
        disabled && onClick
          ? 'opacity-45 cursor-not-allowed'
          : onClick
            ? 'cursor-pointer hover:-translate-y-1.5 active:scale-95'
            : 'cursor-default'
      } ${animatedThrow ? 'animate-card-throw' : ''}`}
      aria-label={`${card.rank} ${info.arabic}`}
    >
      {/* Top-right corner */}
      <div className={`flex flex-col items-center leading-none self-start ${info.colorClass}`}>
        <span className={`font-bold ${rankText}`}>{card.rank}</span>
        <span className={size === 'sm' ? 'text-[10px]' : 'text-xs sm:text-sm'}>{info.symbol}</span>
      </div>

      {/* Center Pip */}
      <div
        className={`absolute inset-0 flex items-center justify-center pointer-events-none ${info.colorClass} ${suitCenterText}`}
      >
        {info.symbol}
      </div>

      {/* Bottom-left corner */}
      <div
        className={`flex flex-col items-center leading-none self-end rotate-180 ${info.colorClass}`}
      >
        <span className={`font-bold ${rankText}`}>{card.rank}</span>
        <span className={size === 'sm' ? 'text-[10px]' : 'text-xs sm:text-sm'}>{info.symbol}</span>
      </div>

      {/* Tap again to throw indicator when lifted */}
      {selected && size === 'hand' && (
        <span className="absolute -top-3 left-1/2 -translate-x-1/2 bg-[#002BFF] text-white text-[10px] font-bold px-2 py-0.5 rounded-md shadow whitespace-nowrap">
          ارمِ
        </span>
      )}
    </button>
  );
};

export const CardBack: React.FC<{ small?: boolean }> = ({ small = false }) => (
  <div
    className={`${
      small ? 'w-7 h-10 rounded-md' : 'w-10 h-14 rounded-lg'
    } overflow-hidden border border-white/80 shadow bg-[#002BFF]`}
  >
    <img
      src="/assets/img_card_back.jpg"
      alt=""
      className="w-full h-full object-cover"
    />
  </div>
);
