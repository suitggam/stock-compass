import { useEffect, useMemo, useState } from "react";
import { OrdersApi } from "../api/orders";
import { useAuth } from "../stores/auth";
import type {
  OrderRequest,
  OrderResponse,
  OrderType,
  TradeType,
} from "../types/order";
import {
  StocksApi,
  type StockItemOption,
  type StockCandlePoint,
} from "../api/stocks";
import { PortfolioApi, type PortfolioSummary } from "../api/portfolio";
import VolumeChart from "../components/Chart/VolumeChart";
import CanvasCandle from "../components/Chart/CanvasCandle";
import useAuthGuard from "../hooks/useAuthGuard";

function StockTradePage() {
  const { user } = useAuth();
  useAuthGuard("/login");

  const [items, setItems] = useState<StockItemOption[]>([]);
  const [query, setQuery] = useState<string>("");
  const [selectedTicker, setSelectedTicker] = useState<string>("");
  const [selectedStockNo, setSelectedStockNo] = useState<number | null>(null);

  const [side, setSide] = useState<TradeType>("BUY");
  const [orderType, setOrderType] = useState<OrderType>("MARKET");
  const [price, setPrice] = useState<number | "">("");
  const [volume, setVolume] = useState<number | "">("");
  const [triggerPrice, setTriggerPrice] = useState<number | "">("");
  const [expiresAt, setExpiresAt] = useState<string>("");

  const [pending, setPending] = useState<OrderResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [portfolio, setPortfolio] = useState<PortfolioSummary | null>(null);
  const [range, setRange] = useState<"1M" | "3M" | "6M" | "1Y" | "5Y" | "ALL">(
    "5Y"
  );

  // Load stock list
  useEffect(() => {
    (async () => {
      const list = await StocksApi.listItems();
      setItems(list);
    })();
  }, []);

  // Map ticker -> stockNo (by index from seed order)
  const stockOptions = useMemo(() => {
    const base = items.map((it) => ({
      label: `${it.companyName} (${it.ticker})`,
      ticker: it.ticker,
      stockNo: it.itemNo,
    }));
    const q = query.trim();
    if (!q) return base;
    const ql = q.toLowerCase();
    return base.filter(
      (o) =>
        o.label.toLowerCase().includes(ql) ||
        o.ticker.toLowerCase().includes(ql)
    );
  }, [items, query]);

  useEffect(() => {
    if (!user) return;
    OrdersApi.getUserPending(user.userNo)
      .then(setPending)
      .catch(() => setPending([]));
  }, [user]);

  useEffect(() => {
    if (!user) return;
    PortfolioApi.getHoldings()
      .then(setPortfolio)
      .catch(() => setPortfolio(null));
  }, [user]);

  // const resetForm = () => {
  //   setSide("BUY");
  //   setOrderType("MARKET");
  //   setPrice("");
  //   setVolume("");
  //   setTriggerPrice("");
  //   setExpiresAt("");
  // };

  const onSelect = (ticker: string) => {
    setSelectedTicker(ticker);
    const opt = stockOptions.find((o) => o.ticker === ticker);
    setSelectedStockNo(opt?.stockNo ?? null);
  };

  const canSubmit =
    !!user &&
    selectedStockNo != null &&
    volume !== "" &&
    (orderType !== "LIMIT" || price !== "");

  const submit = async () => {
    if (!user || selectedStockNo == null) return;
    setLoading(true);
    setMessage(null);
    try {
      const body: OrderRequest = {
        userNo: user.userNo,
        stockNo: selectedStockNo,
        type: side,
        orderType,
        price: price === "" ? null : Number(price),
        volume: Number(volume),
        triggerPrice: triggerPrice === "" ? null : Number(triggerPrice),
        expiresAt: expiresAt || null,
      };
      const res = await OrdersApi.create(body);
      setMessage(res.message || "주문이 제출되었습니다");
      // refresh pending
      const list = await OrdersApi.getUserPending(user.userNo);
      setPending(list);
      // keep form for quick repeat
    } catch (e: any) {
      setMessage(e?.message || "주문 실패");
    } finally {
      setLoading(false);
    }
  };

  const cancel = async (tradeNo: number) => {
    if (!user) return;
    setLoading(true);
    try {
      await OrdersApi.cancel(tradeNo, user.userNo);
      const list = await OrdersApi.getUserPending(user.userNo);
      setPending(list);
    } finally {
      setLoading(false);
    }
  };

  const edit = (o: OrderResponse) => {
    setSide(o.type);
    setOrderType(o.orderType);
    setSelectedStockNo(o.stockNo);
    setSelectedTicker(
      stockOptions.find((s) => s.stockNo === o.stockNo)?.ticker || ""
    );
    setPrice(o.price ?? "");
    setVolume(o.volume ?? "");
    setTriggerPrice(o.triggerPrice ?? "");
    setExpiresAt(o.expiresAt ?? "");
  };

  // History chart state
  const [candle, setCandle] = useState<StockCandlePoint[]>([]);
  const [rangeIdx, setRangeIdx] = useState<{
    startIndex: number;
    endIndex: number;
  } | null>(null);

  useEffect(() => {
    (async () => {
      if (!selectedStockNo) {
        setCandle([]);
        return;
      }
      const to = new Date();
      const from = new Date();
      switch (range) {
        case "1M":
          from.setMonth(to.getMonth() - 1);
          break;
        case "3M":
          from.setMonth(to.getMonth() - 3);
          break;
        case "6M":
          from.setMonth(to.getMonth() - 6);
          break;
        case "1Y":
          from.setFullYear(to.getFullYear() - 1);
          break;
        case "5Y":
          from.setFullYear(to.getFullYear() - 5);
          break;
        case "ALL":
          from.setFullYear(to.getFullYear() - 10);
          break; // 임시: 10년
      }
      const toStr = to.toISOString().slice(0, 10);
      const fromStr = from.toISOString().slice(0, 10);
      try {
        const points = await StocksApi.candleHistory(
          selectedStockNo,
          fromStr,
          toStr
        );
        setCandle(points);
        setRangeIdx(null); // 기간 바꾸면 전체로 초기화
      } catch (e) {
        setCandle([]);
      }
    })();
  }, [selectedStockNo, range]);

  // 이동평균 계산 및 뷰 슬라이싱
  const dataWithMA = useMemo(() => {
    const calcMA = (period: number) => {
      const out = new Array<number | null>(candle.length).fill(null);
      let sum = 0;
      for (let i = 0; i < candle.length; i++) {
        sum += candle[i].close;
        if (i >= period) sum -= candle[i - period].close;
        if (i >= period - 1) out[i] = Math.round(sum / period);
      }
      return out;
    };
    const ma5 = calcMA(5);
    const ma20 = calcMA(20);
    const ma60 = calcMA(60);
    return candle.map((d, i) => ({
      ...d,
      ma5: ma5[i],
      ma20: ma20[i],
      ma60: ma60[i],
    }));
  }, [candle]);

  const viewData = useMemo(() => {
    const start = rangeIdx?.startIndex ?? 0;
    const end =
      rangeIdx?.endIndex ?? (dataWithMA.length ? dataWithMA.length - 1 : 0);
    return dataWithMA
      .slice(start, end + 1)
      .map((d) => ({ ...d, date: String(d.date) }));
  }, [dataWithMA, rangeIdx]);

  return (
    <div className="min-h-screen bg-slate-900 text-white p-6">
      <div className="max-w-[1600px] mx-auto grid grid-cols-1 xl:grid-cols-12 gap-6 items-start">
        <div className="bg-slate-800 rounded-xl p-5 border border-slate-700 order-2 xl:col-span-2">
          <h2 className="text-xl font-bold mb-4">주문</h2>

          {/* Side tabs */}
          <div className="flex gap-2 mb-4">
            {(["BUY", "SELL"] as TradeType[]).map((s) => (
              <button
                key={s}
                onClick={() => setSide(s)}
                className={`px-4 py-2 rounded ${
                  side === s ? "bg-emerald-600" : "bg-slate-700"
                }`}
              >
                {s === "BUY" ? "매수" : "매도"}
              </button>
            ))}
          </div>

          {/* Search + selector */}
          <label className="block text-sm mb-1">종목</label>
          <div className="flex gap-2 mb-3">
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="이름/티커 검색"
              className="flex-1 bg-slate-700 rounded px-3 py-2"
            />
            <select
              value={selectedTicker}
              onChange={(e) => onSelect(e.target.value)}
              className="w-1/2 bg-slate-700 rounded px-3 py-2"
            >
              <option value="" disabled>
                종목 선택
              </option>
              {stockOptions.map((opt) => (
                <option key={opt.stockNo} value={opt.ticker}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          {/* Order type */}
          <label className="block text-sm mb-1">주문유형</label>
          <select
            value={orderType}
            onChange={(e) => setOrderType(e.target.value as OrderType)}
            className="w-full bg-slate-700 rounded px-3 py-2 mb-3"
          >
            <option value="MARKET">시장가</option>
            <option value="LIMIT">지정가</option>
            <option value="STOP_LOSS">손절</option>
            <option value="TAKE_PROFIT">익절</option>
            <option value="TIME_LIMIT">시간지정</option>
          </select>

          {orderType === "LIMIT" && (
            <div className="mb-3">
              <label className="block text-sm mb-1">가격(원)</label>
              <input
                value={price}
                onChange={(e) =>
                  setPrice(e.target.value === "" ? "" : Number(e.target.value))
                }
                type="number"
                className="w-full bg-slate-700 rounded px-3 py-2"
              />
            </div>
          )}

          {(orderType === "STOP_LOSS" || orderType === "TAKE_PROFIT") && (
            <div className="mb-3">
              <label className="block text-sm mb-1">트리거 가격(원)</label>
              <input
                value={triggerPrice}
                onChange={(e) =>
                  setTriggerPrice(
                    e.target.value === "" ? "" : Number(e.target.value)
                  )
                }
                type="number"
                className="w-full bg-slate-700 rounded px-3 py-2"
              />
            </div>
          )}

          {orderType === "TIME_LIMIT" && (
            <div className="mb-3">
              <label className="block text-sm mb-1">만료 시각</label>
              <input
                value={expiresAt}
                onChange={(e) => setExpiresAt(e.target.value)}
                type="datetime-local"
                className="w-full bg-slate-700 rounded px-3 py-2"
              />
            </div>
          )}

          <div className="mb-4">
            <label className="block text-sm mb-1">수량</label>
            <input
              value={volume}
              onChange={(e) =>
                setVolume(e.target.value === "" ? "" : Number(e.target.value))
              }
              type="number"
              className="w-full bg-slate-700 rounded px-3 py-2"
            />
          </div>

          <button
            disabled={!canSubmit || loading}
            onClick={submit}
            className={`w-full py-3 rounded font-bold ${
              side === "BUY" ? "bg-rose-600" : "bg-cyan-600"
            } disabled:opacity-50`}
          >
            {side === "BUY" ? "현금매수" : "현금매도"}
          </button>

          {message && <p className="mt-3 text-sm text-amber-400">{message}</p>}
        </div>

        <div className="bg-slate-800 rounded-xl p-5 border border-slate-700 order-4 xl:col-span-2">
          <h2 className="text-xl font-bold mb-4">내 미체결 주문</h2>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-slate-300">
                  <th className="text-left p-2">종목</th>
                  <th className="text-left p-2">유형</th>
                  <th className="text-right p-2">가격</th>
                  <th className="text-right p-2">수량</th>
                  <th className="text-left p-2">상태</th>
                  <th className="p-2">액션</th>
                </tr>
              </thead>
              <tbody>
                {pending.map((o) => (
                  <tr key={o.tradeNo} className="border-t border-slate-700">
                    <td className="p-2">
                      {stockOptions.find((s) => s.stockNo === o.stockNo)
                        ?.label || `#${o.stockNo}`}
                    </td>
                    <td className="p-2">
                      {o.type}/{o.orderType}
                    </td>
                    <td className="p-2 text-right">
                      {o.price?.toLocaleString?.() ?? "-"}
                    </td>
                    <td className="p-2 text-right">{o.volume}</td>
                    <td className="p-2">{o.status}</td>
                    <td className="p-2 text-right flex gap-2 justify-end">
                      <button
                        className="px-3 py-1 rounded bg-slate-700"
                        onClick={() => edit(o)}
                      >
                        수정
                      </button>
                      <button
                        className="px-3 py-1 rounded bg-red-600"
                        onClick={() => cancel(o.tradeNo)}
                      >
                        취소
                      </button>
                    </td>
                  </tr>
                ))}
                {pending.length === 0 && (
                  <tr>
                    <td className="p-3 text-slate-400" colSpan={6}>
                      미체결 주문이 없습니다.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        <div className="bg-slate-800 rounded-xl p-5 border border-slate-700 xl:col-span-8 order-1">
          <h2 className="text-xl font-bold mb-4">차트</h2>
          <div className="flex gap-2 mb-3">
            {(["1M", "3M", "6M", "1Y", "5Y", "ALL"] as const).map((r) => (
              <button
                key={r}
                onClick={() => setRange(r)}
                className={`px-3 py-1 rounded ${
                  range === r
                    ? "bg-amber-600 text-white"
                    : "bg-slate-700 text-slate-200"
                }`}
              >
                {r}
              </button>
            ))}
          </div>
          {viewData.length > 0 ? (
            // 빠르게 확인하려면 CanvasCandle로 교체(의존성 없음)
            <CanvasCandle data={viewData as any} />
          ) : (
            <p className="text-slate-400">
              종목을 선택하면 5개년 종가 차트를 표시합니다. 데이터가 없으면 수집
              완료 후 다시 시도하세요.
            </p>
          )}
          <div className="mt-6">
            {viewData.length > 0 && (
              <VolumeChart data={viewData as any} onRangeChange={setRangeIdx} />
            )}
          </div>
        </div>
      </div>
      {/* 내 포트폴리오 요약 (모의투자 페이지 내부) */}
      <div className="bg-slate-800 rounded-xl p-5 border border-slate-700 mt-6 order-3 xl:col-span-2 xl:mt-0">
        <h2 className="text-xl font-bold mb-4">내 포트폴리오</h2>
        {portfolio ? (
          <>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
              <StatMini title="투자원금" value={portfolio.totalInvested} />
              <StatMini title="평가금액" value={portfolio.totalMarketValue} />
              <StatMini
                title="손익"
                value={portfolio.totalPnl}
                valueClass={
                  portfolio.totalPnl >= 0 ? "text-rose-400" : "text-sky-400"
                }
              />
              <StatMini
                title="수익률"
                value={`${portfolio.totalPnlRate.toFixed(2)} %`}
              />
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-slate-300">
                    <th className="text-left p-2">종목</th>
                    <th className="text-right p-2">수량</th>
                    <th className="text-right p-2">평균단가</th>
                    <th className="text-right p-2">현재가</th>
                    <th className="text-right p-2">손익</th>
                    <th className="text-right p-2">수익률</th>
                  </tr>
                </thead>
                <tbody>
                  {portfolio.holdings.map((h) => (
                    <tr key={h.stockNo} className="border-t border-slate-700">
                      <td className="p-2">
                        {h.companyName} ({h.ticker})
                      </td>
                      <td className="p-2 text-right">
                        {h.quantity.toLocaleString()}
                      </td>
                      <td className="p-2 text-right">
                        {Number(h.avgPrice).toLocaleString()}
                      </td>
                      <td className="p-2 text-right">
                        {h.currentPrice?.toLocaleString?.()}
                      </td>
                      <td
                        className={`p-2 text-right ${
                          h.pnl >= 0 ? "text-rose-400" : "text-sky-400"
                        }`}
                      >
                        {h.pnl.toLocaleString()}
                      </td>
                      <td className="p-2 text-right">
                        {h.pnlRate.toFixed(2)}%
                      </td>
                    </tr>
                  ))}
                  {portfolio.holdings.length === 0 && (
                    <tr>
                      <td className="p-3 text-slate-400" colSpan={6}>
                        보유 종목이 없습니다.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </>
        ) : (
          <p className="text-slate-400">
            포트폴리오 불러오는 중 또는 데이터가 없습니다.
          </p>
        )}
      </div>

      {/* Bottom: 전체 요약 (수익률/손익/평가금액 등) */}
      <div className="max-w-[1600px] mx-auto mt-6">
        <h2 className="text-xl font-bold mb-4">전체 요약</h2>
        {portfolio ? (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <StatBig title="투자원금" value={portfolio.totalInvested} />
            <StatBig title="평가금액" value={portfolio.totalMarketValue} />
            <StatBig
              title="손익"
              value={portfolio.totalPnl}
              valueClass={
                portfolio.totalPnl >= 0 ? "text-rose-400" : "text-sky-400"
              }
            />
            <StatBig
              title="수익률"
              value={`${portfolio.totalPnlRate.toFixed(2)} %`}
            />
          </div>
        ) : (
          <p className="text-slate-400">
            포트폴리오 불러오는 중 또는 데이터가 없습니다.
          </p>
        )}
      </div>
    </div>
  );
}

function StatMini({
  title,
  value,
  valueClass,
}: {
  title: string;
  value: any;
  valueClass?: string;
}) {
  return (
    <div className="bg-slate-900 rounded-xl p-4 border border-slate-700">
      <div className="text-slate-400 text-sm">{title}</div>
      <div className={`text-lg font-bold ${valueClass ?? ""}`}>
        {typeof value === "number" ? value.toLocaleString() : value}
      </div>
    </div>
  );
}

function StatBig({
  title,
  value,
  valueClass,
}: {
  title: string;
  value: any;
  valueClass?: string;
}) {
  return (
    <div className="bg-slate-800 rounded-xl p-5 border border-slate-700">
      <div className="text-slate-400 text-sm">{title}</div>
      <div className={`text-2xl font-extrabold ${valueClass ?? ""}`}>
        {typeof value === "number" ? value.toLocaleString() : value}
      </div>
    </div>
  );
}

export default StockTradePage;
