import { useEffect, useMemo, useState } from 'react';
import useAuthGuard from '../hooks/useAuthGuard';
import { PortfolioApi, type Holding, type PortfolioSummary } from '../api/portfolio';

export default function PortfolioPage() {
  useAuthGuard('/login');
  const [summary, setSummary] = useState<PortfolioSummary | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      setLoading(true); setError(null);
      try {
        const s = await PortfolioApi.getHoldings();
        setSummary(s);
      } catch (e: any) {
        setError(e?.message || '불러오기 실패');
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const rows = summary?.holdings ?? [];
  const totals = useMemo(() => ({
    invested: summary?.totalInvested ?? 0,
    marketValue: summary?.totalMarketValue ?? 0,
    pnl: summary?.totalPnl ?? 0,
    rate: summary?.totalPnlRate ?? 0,
  }), [summary]);

  return (
    <div className="min-h-screen bg-slate-900 text-white p-6">
      <div className="max-w-6xl mx-auto space-y-6">
        <h1 className="text-2xl font-bold">보유 종목/수익률</h1>

        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <Stat title="투자원금" value={totals.invested} />
          <Stat title="평가금액" value={totals.marketValue} />
          <Stat title="손익" value={totals.pnl} valueClass={totals.pnl>=0?'text-rose-400':'text-sky-400'} />
          <Stat title="수익률" value={`${totals.rate.toFixed(2)} %`} />
        </div>

        <div className="bg-slate-800 rounded-xl p-4 border border-slate-700">
          <div className="overflow-x-auto">
            {loading ? (
              <p className="text-slate-300">불러오는 중…</p>
            ) : error ? (
              <p className="text-amber-400">{error}</p>
            ) : (
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-slate-300">
                    <th className="text-left p-2">종목</th>
                    <th className="text-right p-2">수량</th>
                    <th className="text-right p-2">평균단가</th>
                    <th className="text-right p-2">현재가</th>
                    <th className="text-right p-2">투자원금</th>
                    <th className="text-right p-2">평가금액</th>
                    <th className="text-right p-2">손익</th>
                    <th className="text-right p-2">수익률</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((h: Holding) => (
                    <tr key={h.stockNo} className="border-t border-slate-700">
                      <td className="p-2">{h.companyName} ({h.ticker})</td>
                      <td className="p-2 text-right">{h.quantity.toLocaleString()}</td>
                      <td className="p-2 text-right">{Number(h.avgPrice).toLocaleString()}</td>
                      <td className="p-2 text-right">{h.currentPrice?.toLocaleString?.()}</td>
                      <td className="p-2 text-right">{h.invested.toLocaleString()}</td>
                      <td className="p-2 text-right">{h.marketValue.toLocaleString()}</td>
                      <td className={`p-2 text-right ${h.pnl>=0?'text-rose-400':'text-sky-400'}`}>{h.pnl.toLocaleString()}</td>
                      <td className="p-2 text-right">{h.pnlRate.toFixed(2)}%</td>
                    </tr>
                  ))}
                  {rows.length===0 && (
                    <tr><td className="p-3 text-slate-400" colSpan={8}>보유 종목이 없습니다.</td></tr>
                  )}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function Stat({title, value, valueClass}:{title:string; value: any; valueClass?: string}){
  return (
    <div className="bg-slate-800 rounded-xl p-4 border border-slate-700">
      <div className="text-slate-400 text-sm">{title}</div>
      <div className={`text-xl font-bold ${valueClass??''}`}>{typeof value==='number'? value.toLocaleString(): value}</div>
    </div>
  );
}

