import * as React from 'react';

type BarProps = {
  left: string;
  right: string;
  leftPct: number;
  leftLabel: string;
  rightLabel: string;
  gradientClass: string;
};

function ResultBar({ left, right, leftPct, leftLabel, rightLabel, gradientClass }: BarProps) {
  return (
    <div className="h-24">
      <div className="flex justify-between items-center text-neutral-900 font-semibold">
        <span>{left}</span>
        <span>{right}</span>
      </div>
      <div className="flex justify-between text-neutral-500 text-xs font-medium mt-1">
        <span>{leftPct}%</span>
        <span>{100 - leftPct}%</span>
      </div>
      <div className="flex justify-between text-neutral-500 text-[11px] mt-0.5">
        <span>{leftLabel}</span>
        <span>{rightLabel}</span>
      </div>
      <div className="h-8 bg-neutral-100 rounded-2xl shadow-[inset_0_2px_4px_rgba(0,0,0,0.06)] overflow-hidden mt-2">
        <div
          className={`h-8 ${gradientClass} rounded-2xl flex items-center justify-center`}
          style={{ width: `${leftPct}%` }}
        >
          <span className="text-white text-sm font-bold">{leftPct}%</span>
        </div>
      </div>
    </div>
  );
}

export default function PersonalityResult() {
  return (
    <section className="w-full bg-white/80 backdrop-blur-xl rounded-2xl shadow-[0_20px_60px_rgba(0,0,0,0.08)] p-6 border border-black/5 relative">
      <div className="absolute inset-x-0 top-0 h-1 rounded-t-2xl bg-gradient-to-r from-amber-500 via-amber-400 to-black" />
      <h2 className="text-center text-2xl font-extrabold text-neutral-900 mb-4 tracking-tight">
        투자 성격 유형 진단 결과
      </h2>

      <div className="space-y-6">
        <ResultBar
          left="I"
          right="E"
          leftPct={37}
          leftLabel="반응형"
          rightLabel="마이웨이형"
          gradientClass="bg-gradient-to-r from-amber-500 to-orange-600"
        />
        <ResultBar
          left="S"
          right="N"
          leftPct={32}
          leftLabel="신중형"
          rightLabel="무심형"
          gradientClass="bg-gradient-to-r from-neutral-900 to-neutral-700"
        />
        <ResultBar
          left="F"
          right="T"
          leftPct={60}
          leftLabel="단타형"
          rightLabel="장타형"
          gradientClass="bg-gradient-to-r from-orange-500 to-amber-400"
        />
        <ResultBar
          left="P"
          right="J"
          leftPct={55}
          leftLabel="재능형"
          rightLabel="노력형"
          gradientClass="bg-gradient-to-r from-neutral-800 to-black"
        />
      </div>
    </section>
  );
}
