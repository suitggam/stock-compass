import SummaryStats from "../components/TendencyGame/SummaryStats";
import type { SummaryStatItem } from "../components/TendencyGame/SummaryStats";
import TradePanel from "../components/TendencyGame/TradePanel";
import TradeRecord from "../components/TendencyGame/TradeRecord";
import StockOverview from "../components/TendencyGame/StockOverview";
import StockHighlights from "../components/TendencyGame/StockHighlights";
import { useTendencyGame } from "../hooks/useTendencyGame";
import { useEffect, useMemo, useState } from "react";
import { extractKeywords } from "../api/StockInfosApi";

export default function TendencyGamePage() {
  const { state, loading, error, summaryItems, tradeAmount, setTradeAmount, order, nextWeek, finish } =
    useTendencyGame();

  const [keywords, setKeywords] = useState<string[]>([]);
  const [news, setNews] = useState<Array<{ title: string; url: string; date: string }>>([]);

  const so = state?.stockOverview;

  const startDate = useMemo(() => so?.currentDate, [so?.currentDate]);
  const endDate = useMemo(() => {
    const next = so?.nextDate;
    if (!next || !startDate) return startDate;
    const d = new Date(next);
    d.setDate(d.getDate() - 1);
    return d.toISOString().slice(0, 10);
  }, [so?.nextDate, startDate]);

  useEffect(() => {
    if (!so?.ticker || !so?.companyAlias || !startDate || !endDate) return;
    
    const run = async () => {
      try {
        const res = await extractKeywords(so.ticker, so.companyAlias, startDate, endDate);
        setKeywords(res.keywords.map((k) => k.keyword).slice(0, 5));
        setNews(res.news);
      } catch {
        setKeywords([]);
        setNews([]);
      }
    };
    void run();
  }, [so?.ticker, so?.companyAlias, startDate, endDate]);

  if (loading && !state) return <div className="min-h-screen grid place-items-center">불러오는 중…</div>;
  if (error && !state) return <div className="min-h-screen grid place-items-center text-red-600">{error}</div>;
  if (!state || !so) return null;

  const tp = state.tradePanel;

  return (
    <div className="min-h-screen p-5">
      <SummaryStats items={summaryItems as SummaryStatItem[]} />

      <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-3">
        <section className="space-y-4 lg:col-span-2">
          <StockOverview
            companyName={so.companyAlias}
            currentWeek={so.currentDate}
            nextWeek={so.finalWeek ? "종료" : so.nextDate ?? ""}
            price={so.price}
            change={so.change}
            rate={so.changeRate}
            chartData={{ labels: so.chart.labels, datasets: [{ label: "Price", data: so.chart.prices }] }}
          />

          <StockHighlights keywords={keywords} news={news} />
        </section>

        <section className="space-y-4">
          <TradePanel
            stockCount={tp.stockCount}
            totalValue={tp.stockValuation}
            averageCost={tp.averageCost}
            evaluationProfit={tp.evaluationProfit}
            evaluationRate={tp.evaluationRate}
            tradeAmount={tradeAmount}
            onTradeAmountChange={setTradeAmount}
            onBuy={() => order("BUY", tradeAmount)}
            onSell={() => order("SELL", tradeAmount)}
            onNextWeek={nextWeek}
            onEndGame={async () => {
              const res = await finish();
              if (res) {
                alert(
                  `게임 종료\n총 자산: ${new Intl.NumberFormat("ko-KR").format(res.totalAsset)}원\n실현 손익: ${new Intl.NumberFormat(
                    "ko-KR",
                  ).format(res.realizedProfit)}원\n수익률: ${res.totalYield.toFixed(2)}%\n성향: ${res.tendencyType}\n추천: ${res.recommendation}`,
                );
              }
            }}
            term={"0주"}
            onTermChange={() => {}}
            maxAffordable={tp.maxAffordable}
            maxSellable={tp.maxSellable}
          />

          <TradeRecord
            items={state.trades.map((t) => ({
              gameTradeType: t.type,
              gameTradePrice: t.price,
              gameTradeDate: t.tradeDate,
              qty: t.quantity,
            }))}
          />
        </section>
      </div>
    </div>
  );
}

