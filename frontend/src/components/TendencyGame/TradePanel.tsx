const formatNumber = (n: number) => new Intl.NumberFormat('ko-KR').format(Math.max(0, Math.round(n)));
const formatCurrency = (n: number) => `${formatNumber(n)}원`;
const formatPercent = (n: number) => {
  const value = Number.isFinite(n) ? n : 0;
  const sign = value > 0 ? '+' : '';
  return `${sign}${value.toFixed(2)}%`;
};

type TradePanelProps = {
  stockCount: number;
  totalValue: number;
  averageCost: number;
  evaluationProfit: number;
  evaluationRate: number;
  tradeAmount: number;
  onTradeAmountChange(n: number): void;
  onBuy(): void;
  onSell(): void;
  onNextWeek(): void;
  onEndGame(): void;
  term: string;
  onTermChange(term: string): void;
  maxAffordable: number;
  maxSellable: number;
};

export default function TradePanel({
  stockCount,
  totalValue,
  averageCost,
  evaluationProfit,
  evaluationRate,
  tradeAmount,
  onTradeAmountChange,
  onBuy,
  onSell,
  onNextWeek,
  onEndGame,
  term,
  onTermChange,
  maxAffordable,
  maxSellable,
}: TradePanelProps) {
  const infoItems = [
    { label: '보유 주식', value: `${formatNumber(stockCount)}주` },
    { label: '평가 금액', value: formatCurrency(totalValue) },
    { label: '평균 단가', value: formatCurrency(averageCost) },
    {
      label: '평가 손익',
      value: formatCurrency(evaluationProfit),
      helper: formatPercent(evaluationRate),
      tone: evaluationProfit >= 0 ? 'text-emerald-500' : 'text-rose-500',
    },
  ];

  const canBuy = tradeAmount > 0 && tradeAmount <= Math.max(0, Math.floor(maxAffordable));
  const canSell = tradeAmount > 0 && tradeAmount <= Math.max(0, Math.floor(maxSellable));

  const quickSet = (amount: number) => {
    onTradeAmountChange(Math.max(0, Math.floor(amount)));
  };

  return (
    <aside className="space-y-4">
      <section className="space-y-4 rounded-2xl bg-white p-5 shadow-sm">
        <div className="grid grid-cols-2 gap-3 text-sm">
          {infoItems.map((item) => (
            <div key={item.label}>
              <p className="text-xs text-slate-500">{item.label}</p>
              <p className="mt-1 font-semibold text-slate-900">{item.value}</p>
              {item.helper ? (
                <p className={`text-xs font-semibold ${item.tone ?? 'text-slate-400'}`}>{item.helper}</p>
              ) : null}
            </div>
          ))}
        </div>

        <div className="flex items-center justify-between">
          <label className="text-sm text-slate-700">투자 금액</label>
          <select
            className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
            value={term}
            onChange={(e) => onTermChange(e.target.value)}
          >
            <option value="0주">0주</option>
            <option value="2주">2주</option>
            <option value="4주">4주</option>
          </select>
        </div>

        <div className="flex items-end gap-3">
          <div className="flex-1">
            <label className="mb-1 block text-sm text-slate-700">수량</label>
            <input
              type="number"
              min={0}
              className="w-full rounded-lg border border-slate-300 px-3 py-2"
              value={tradeAmount}
              onChange={(e) => quickSet(Number(e.target.value) || 0)}
            />
          </div>
          <div className="flex flex-col gap-2 text-xs">
            {[1, 5, 10].map((preset) => (
              <button
                key={preset}
                type="button"
                onClick={() => quickSet(preset)}
                className="rounded-lg border border-slate-300 px-3 py-2 text-slate-700 hover:bg-slate-50"
              >
                {preset}주
              </button>
            ))}
            <button
              type="button"
              onClick={() => quickSet(maxAffordable)}
              className="rounded-lg border border-slate-300 px-3 py-2 text-slate-700 hover:bg-slate-50"
            >
              전액 매수
            </button>
            <button
              type="button"
              onClick={() => quickSet(maxSellable)}
              className="rounded-lg border border-slate-300 px-3 py-2 text-slate-700 hover:bg-slate-50"
            >
              전량 매도
            </button>
          </div>
        </div>

        <div className="flex gap-2">
          <button
            type="button"
            onClick={onBuy}
            disabled={!canBuy}
            className="flex-1 rounded-lg bg-emerald-600 px-4 py-2 font-semibold text-white transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-40"
          >
            매수
          </button>
          <button
            type="button"
            onClick={onSell}
            disabled={!canSell}
            className="flex-1 rounded-lg bg-rose-500 px-4 py-2 font-semibold text-white transition hover:bg-rose-600 disabled:cursor-not-allowed disabled:opacity-40"
          >
            매도
          </button>
        </div>

        <div className="flex gap-2">
          <button
            type="button"
            onClick={onNextWeek}
            className="flex-1 rounded-lg border border-slate-300 px-4 py-2 font-semibold text-slate-700 hover:bg-slate-50"
          >
            다음 주로
          </button>
          <button
            type="button"
            onClick={onEndGame}
            className="flex-1 rounded-lg border border-indigo-400 px-4 py-2 font-semibold text-indigo-600 hover:bg-indigo-50"
          >
            종료
          </button>
        </div>
      </section>
    </aside>
  );
}
