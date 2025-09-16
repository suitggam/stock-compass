type Item = { gameTradeType: "BUY" | "SELL"; gameTradePrice: number; gameTradeDate: string; qty?: number };
type Props = { items: Item[] };

const formatCurrency = (v: number) => `${new Intl.NumberFormat("ko-KR").format(Math.round(v))}원`;

export default function TradeRecord({ items }: Props) {
  return (
    <div className="rounded-2xl bg-white p-5 shadow-sm">
      <div className="mb-3 flex items-center justify-between">
        <div className="font-bold text-slate-900">최근 매수/매도 내역</div>
      </div>
      <div className="max-h-96 overflow-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-slate-500">
              <th className="py-2 text-left font-medium">구분</th>
              <th className="py-2 text-right font-medium">가격</th>
              <th className="py-2 text-right font-medium">수량</th>
              <th className="py-2 text-right font-medium">일자</th>
            </tr>
          </thead>
          <tbody>
            {items.length === 0 ? (
              <tr>
                <td colSpan={4} className="py-6 text-center text-slate-400">
                  거래 내역이 없습니다.
                </td>
              </tr>
            ) : (
              items.map((t, idx) => (
                <tr key={`${t.gameTradeDate}-${idx}`} className="border-t border-slate-100">
                  <td className={`py-2 font-semibold ${t.gameTradeType === "BUY" ? "text-emerald-600" : "text-rose-600"}`}>
                    {t.gameTradeType === "BUY" ? "매수" : "매도"}
                  </td>
                  <td className="py-2 text-right">{formatCurrency(t.gameTradePrice)}</td>
                  <td className="py-2 text-right">{t.qty ?? "-"}</td>
                  <td className="py-2 text-right">{t.gameTradeDate.slice(0, 10)}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
