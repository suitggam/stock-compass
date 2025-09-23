// src/components/mypage/MockInvestmentHistory.tsx
import type { MyPageData } from '../../types/MyPageData';

type Props = {
  items?: NonNullable<MyPageData['mockInvestHistory']> | undefined;
};

export default function MockInvestmentHistory({ items = [] }: Props) {
  const empty = !items || items.length === 0;

  // 간단 KPI 예시 (실제 로직은 백엔드 스펙 나오면 교체)
  const totalGames = items.length;
  const lastTotal = items.at(-1)?.price ?? 0; // 임시 KPI
  const totalReturnPct = 0; // TODO: 백엔드 제공되면 계산
  const rank = 0; // TODO: 백엔드 제공되면 표시

  return (
    <section className="w-full bg-white/80 backdrop-blur-xl rounded-2xl shadow-[0_8px_40px_rgba(0,0,0,0.16)] p-6 border border-black/5 relative">
      <div className="absolute inset-x-0 top-0 h-1 rounded-t-2xl bg-gradient-to-r from-black via-neutral-800 to-amber-500" />
      <div className="flex items-center gap-2 mb-5">
        <div className="text-xs font-bold text-white/80">🎮</div>
        <h3 className="text-xl sm:text-2xl font-extrabold text-neutral-900">모의 투자 히스토리</h3>
      </div>

      {/* KPI */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div className="rounded-2xl p-5 text-center bg-amber-400/10 border border-amber-500/20">
          <div className="text-amber-600 text-xl sm:text-2xl font-bold">{totalGames}</div>
          <div className="text-neutral-500 text-xs">총 게임</div>
        </div>
        <div className="rounded-2xl p-5 text-center bg-amber-400/10 border border-amber-500/20">
          <div className="text-amber-600 text-xl sm:text-2xl font-bold">
            {lastTotal.toLocaleString()}원
          </div>
          <div className="text-neutral-500 text-xs">보유 자산(임시)</div>
        </div>
        <div className="rounded-2xl p-5 text-center bg-amber-400/10 border border-amber-500/20">
          <div className="text-amber-600 text-xl sm:text-2xl font-bold">{totalReturnPct}%</div>
          <div className="text-neutral-500 text-xs">총 수익률(임시)</div>
        </div>
        <div className="rounded-2xl p-5 text-center bg-amber-400/10 border border-amber-500/20">
          <div className="text-amber-600 text-xl sm:text-2xl font-bold">{rank || '-'}</div>
          <div className="text-neutral-500 text-xs">현재 자산 등수(임시)</div>
        </div>
      </div>

      {/* 리스트 */}
      {empty ? (
        <p className="text-sm text-neutral-500">내역이 없습니다.</p>
      ) : (
        <div className="flex flex-col gap-3.5">
          {items.map((it) => (
            <div
              key={it.id}
              className="p-5 rounded-2xl flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 bg-neutral-50/70 border border-neutral-200/70"
            >
              <div className="min-w-0">
                <div className="text-neutral-900 text-[15px] font-semibold truncate">
                  🎮 {it.symbol}
                </div>
                <div className="text-neutral-500 text-sm">
                  {new Date(it.tradedAt).toLocaleString()}
                </div>
              </div>
              <div className="flex items-center gap-3 sm:gap-6">
                <div className="px-4 py-2 rounded-[10px] text-sm font-bold text-center bg-neutral-100 text-neutral-700">
                  {it.quantity}주 · {it.price.toLocaleString()}원
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
