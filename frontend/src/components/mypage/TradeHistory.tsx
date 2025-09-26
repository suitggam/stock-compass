import type { MyPageData } from '../../types/MyPageData';
import { Link } from 'react-router';

type Props = {
  items?: NonNullable<MyPageData['tradeHistory']> | undefined;
};

export default function TradeHistory({ items = [] }: Props) {
  const empty = !items || items.length === 0;

  // 간단 KPI 예시 (실제 로직은 백엔드 스펙 나오면 교체)
  const totalGames = items.length;
  const lastTotal = items.at(-1)?.price ?? 0; // 임시 KPI
  const totalReturnPct = 0; // TODO: 백엔드 제공되면 계산
  const rank = 0; // TODO: 백엔드 제공되면 표시

  return (
    <section className="w-full bg-slate-700 backdrop-blur-xl rounded-2xl shadow-lg p-6 border border-slate-600 relative">
      <div className="flex items-center gap-2 mb-5">
        <h3 className="text-xl sm:text-2xl font-extrabold text-white">모의 투자 히스토리</h3>
      </div>

      {/* KPI */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div className="rounded-2xl p-5 text-center bg-slate-600 border border-slate-500 hover:border-amber-400 transition-all">
          <div className="text-amber-400 text-xl sm:text-2xl font-bold">{totalGames}원</div>
          <div className="text-slate-300 text-xs">총 투자 금액</div>
        </div>
        <div className="rounded-2xl p-5 text-center bg-slate-600 border border-slate-500 hover:border-amber-400 transition-all">
          <div className="text-amber-400 text-xl sm:text-2xl font-bold">
            {lastTotal.toLocaleString()}원
          </div>
          <div className="text-slate-300 text-xs">보유 자산</div>
        </div>
        <div className="rounded-2xl p-5 text-center bg-slate-600 border border-slate-500 hover:border-amber-400 transition-all">
          <div className="text-amber-400 text-xl sm:text-2xl font-bold">{totalReturnPct}%</div>
          <div className="text-slate-300 text-xs">총 수익률(임시)</div>
        </div>
        <div className="rounded-2xl p-5 text-center bg-slate-600 border border-slate-500 hover:border-amber-400 transition-all">
          <div className="text-amber-400 text-xl sm:text-2xl font-bold">{rank || '-'}</div>
          <div className="text-slate-300 text-xs">현재 자산 등수(임시)</div>
        </div>
      </div>

      {/* 리스트 */}
      {empty ? (
        <div className="text-center py-8">
          <div className="text-slate-400 mb-4">투자 내역이 없습니다.</div>
          <Link
            to="/trade"
            className="px-4 py-2 bg-gradient-to-r from-amber-500 to-amber-600 text-white rounded-lg hover:from-amber-600 hover:to-amber-700 transition-all"
          >
            모의투자 시작하기
          </Link>
        </div>
      ) : (
        <div className="flex flex-col gap-3.5">
          {items.map((it) => (
            <div
              key={it.tradeNo}
              className="p-5 rounded-2xl flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 bg-slate-600 border border-slate-500 hover:bg-slate-500 hover:border-amber-400 transition-all"
            >
              <div className="min-w-0">
                <div className="text-slate-400 text-sm">
                  {new Date(it.createdAt).toLocaleString()}
                </div>
              </div>
              <div className="flex items-center gap-3 sm:gap-6">
                <div className="px-4 py-2 rounded-[10px] text-sm font-bold text-center bg-slate-500 text-amber-300 border border-slate-400">
                  {it.volume}주 · {it.price.toLocaleString()}원
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
