import { useMemo, useState } from "react";
import SummaryStats, { type SummaryStatItem } from "../components/TendencyGame/SummaryStats";
import TradePanel from "../components/TendencyGame/TradePanel";
import TradeRecord from "../components/TendencyGame/TradeRecord";
import StockOverview from "../components/TendencyGame/StockOverview";
import StockHighlights from "../components/TendencyGame/StockHighlights";
import type { ChartLineData } from "../types/tendency";

const formatNumber = (n: number) => new Intl.NumberFormat("ko-KR").format(Math.round(n));
const formatCurrency = (n: number) => `${formatNumber(n)}원`;
const formatPercent = (rate: number) => {
  const value = Number.isFinite(rate) ? rate : 0;
  const sign = value > 0 ? "+" : "";
  return `${sign}${value.toFixed(2)}%`;
};

export default function TendencyGamePage() {
  // 기본 목업 데이터 (추후 API 연동 시 교체)
  const [gameCompanyName] = useState("익명 기업 A");
  const [gameCurrentWeek, setGameCurrentWeek] = useState("2023-04-13");
  const [gameNextWeek, setGameNextWeek] = useState("2023-04-20");
  const [gamePrice, setGamePrice] = useState(55_700);
  const [prevClose, setPrevClose] = useState(54_500);

  const [gameCash, setGameCash] = useState(8_234_500);
  const [gameStockCnt, setGameStockCnt] = useState(34);
  const [gameAverageCost, setGameAverageCost] = useState(52_430);

  const [gameTradeAmount, setGameTradeAmount] = useState(1);
  const [term, setTerm] = useState("0주");

  const chartData: ChartLineData = useMemo(() => {
    const labels: string[] = [];
    const data: number[] = [];
    for (let i = 9; i >= 0; i--) {
      labels.push(`W-${i}`);
      data.push(50_000 + Math.round(Math.sin(i / 2) * 2_000) + i * 200);
    }
    return { labels, datasets: [{ label: "Price", data }] };
  }, []);

  const gameKeyword = ["AI 반도체", "대규모 투자", "기술분석", "해외 진출"];
  const gameNews = [
    { id: "n1", title: "AI 반도체 시장 성장, 수혜 전망", summary: "생산 라인 증설과 함께 수요 증가." },
    { id: "n2", title: "대규모 설비투자 계획 발표", summary: "설비 투자로 경쟁력 강화 기대." },
  ];

  const gameTotalPrice = gameStockCnt * gamePrice;
  const invested = gameStockCnt * gameAverageCost;
  const gameEvaluationProfit = gameTotalPrice - invested;
  const rate = invested > 0 ? (gameEvaluationProfit / invested) * 100 : 0;
  const gameRealizedAsset = 127_100;
  const gameTotalAsset = gameCash + gameTotalPrice;

  const summaryItems: SummaryStatItem[] = useMemo(
    () => [
      {
        id: "cash",
        label: "보유 현금",
        value: formatCurrency(gameCash),
      },
      {
        id: "stock",
        label: "보유 주식",
        value: `${formatNumber(gameStockCnt)}주`,
        helper: formatCurrency(gameTotalPrice),
      },
      {
        id: "asset",
        label: "총 자산",
        value: formatCurrency(gameTotalAsset),
      },
      {
        id: "realized",
        label: "실현 손익",
        value: formatCurrency(gameRealizedAsset),
      },
      {
        id: "yield",
        label: "전체 수익률",
        value: formatPercent(rate),
        helper: formatCurrency(gameEvaluationProfit),
        tone: gameEvaluationProfit >= 0 ? "positive" : "negative",
      },
    ],
    [gameCash, gameStockCnt, gameTotalPrice, gameTotalAsset, gameRealizedAsset, gameEvaluationProfit, rate],
  );

  const [items, setItems] = useState<
    { gameTradeType: "BUY" | "SELL"; gameTradePrice: number; gameTradeDate: string; qty?: number }[]
  >([]);

  const addRecord = (type: "BUY" | "SELL", qty: number, price: number) => {
    setItems((prev) => [{ gameTradeType: type, gameTradePrice: price, gameTradeDate: new Date().toISOString(), qty }, ...prev]);
  };

  const maxAffordable = Math.floor(gameCash / gamePrice);

  const onBuy = () => {
    const qty = gameTradeAmount;
    if (qty <= 0 || qty > maxAffordable) return;
    const cost = qty * gamePrice;
    const newQty = gameStockCnt + qty;
    const newInvested = invested + cost;
    const newAvg = newQty > 0 ? Math.round(newInvested / newQty) : gameAverageCost;
    setGameCash((c) => c - cost);
    setGameStockCnt(newQty);
    setGameAverageCost(newAvg);
    addRecord("BUY", qty, gamePrice);
  };

  const onSell = () => {
    const qty = gameTradeAmount;
    if (qty <= 0 || qty > gameStockCnt) return;
    const income = qty * gamePrice;
    const newQty = gameStockCnt - qty;
    const remainingInvested = Math.max(invested - gameAverageCost * qty, 0);
    const newAvg = newQty > 0 ? Math.round(remainingInvested / newQty) : gameAverageCost;
    setGameCash((c) => c + income);
    setGameStockCnt(newQty);
    setGameAverageCost(newAvg);
    addRecord("SELL", qty, gamePrice);
  };

  const onNextWeek = () => {
    const changePct = (Math.random() - 0.5) * 0.12;
    const nextPrice = Math.max(100, Math.round(gamePrice * (1 + changePct)));
    setPrevClose(gamePrice);
    setGamePrice(nextPrice);

    const d = new Date(gameCurrentWeek);
    d.setDate(d.getDate() + 7);
    const next = new Date(d);
    next.setDate(next.getDate() + 7);
    const fmt = (x: Date) => x.toISOString().slice(0, 10);
    setGameCurrentWeek(fmt(d));
    setGameNextWeek(fmt(next));
  };

  const onEndGame = () => {
    alert(`게임 종료\n총 자산: ${formatCurrency(gameTotalAsset)}\n보유 수량: ${gameStockCnt}주`);
  };

  const change = gamePrice - prevClose;
  const changeRate = prevClose > 0 ? (change / prevClose) * 100 : 0;

  return (
    <div className="min-h-screen p-5">
      <SummaryStats items={summaryItems} />

      <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-3">
        <section className="space-y-4 lg:col-span-2">
          <StockOverview
            companyName={gameCompanyName}
            currentWeek={gameCurrentWeek}
            nextWeek={gameNextWeek}
            price={gamePrice}
            change={change}
            rate={changeRate}
            chartData={chartData}
          />

          <StockHighlights keywords={gameKeyword} news={gameNews} />
        </section>

        <section className="space-y-4">
          <TradePanel
            stockCount={gameStockCnt}
            totalValue={gameTotalPrice}
            averageCost={gameAverageCost}
            evaluationProfit={gameEvaluationProfit}
            evaluationRate={rate}
            tradeAmount={gameTradeAmount}
            onTradeAmountChange={(n) => setGameTradeAmount(n)}
            onBuy={onBuy}
            onSell={onSell}
            onNextWeek={onNextWeek}
            onEndGame={onEndGame}
            term={term}
            onTermChange={setTerm}
            maxAffordable={maxAffordable}
            maxSellable={gameStockCnt}
          />

          <TradeRecord items={items} />
        </section>
      </div>
    </div>
  );
}
