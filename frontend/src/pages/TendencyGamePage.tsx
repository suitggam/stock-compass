// TendencyGamePage.tsx 파일
import SummaryStats from '../components/TendencyGame/SummaryStats';
import TradePanel from '../components/TendencyGame/TradePanel';
import TradeRecord from '../components/TendencyGame/TradeRecord';
import StockOverview from '../components/TendencyGame/StockOverview';
import StockHighlights from '../components/TendencyGame/StockHighlights';
import TradeSuccessModal from '../components/TendencyGame/TradeSuccessModal';
import GameFinishModal from '../components/TendencyGame/GameFinishModal';
import { useTendencyGame } from '../hooks/useTendencyGame';
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router';

export default function TendencyGamePage() {
  const {
    state,
    loading,
    error,
    summaryItems,
    tradeAmount,
    setTradeAmount,
    order,
    nextWeek,
    finish,
    tradeSuccessModal,
    closeTradeSuccessModal,
    gameFinishModal,
    closeGameFinishModal,
  } = useTendencyGame();
  const navigate = useNavigate();

  const handleGoHome = () => {
    navigate('/');
  };

  const [currentChartData, setCurrentChartData] = useState(null);

  useEffect(() => {
    if (state && state.stockOverview) {
      const so = state.stockOverview;

      // 💡 state.currentWeek 대신 state.week를 사용합니다.
      const currentWeekIndex = state.week ?? 1;
      const chartLabels = so.chart.labels.slice(0, currentWeekIndex);
      const chartPrices = so.chart.prices.slice(0, currentWeekIndex);

      setCurrentChartData({
        labels: chartLabels,
        datasets: [{ label: 'Price', data: chartPrices }],
      });
    }
  }, [state]);

  if (loading && !state)
    return <div className="min-h-screen grid place-items-center">불러오는 중…</div>;
  if (error && !state)
    return <div className="min-h-screen grid place-items-center text-red-600">{error}</div>;
  if (!state) return null;

  const so = state.stockOverview;
  const tp = state.tradePanel;

  return (
    <div className="min-h-screen p-5">
      <SummaryStats items={summaryItems} />

      <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-3">
        <section className="space-y-4 lg:col-span-2">
          {currentChartData && (
            <StockOverview
              companyName={so.companyAlias}
              currentWeek={so.currentDate}
              nextWeek={so.finalWeek ? '종료' : so.nextDate ?? ''}
              price={so.price}
              change={so.change}
              rate={so.changeRate}
              chartData={currentChartData}
            />
          )}
          <StockHighlights keywords={state.highlights.keywords} news={state.highlights.news} />
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
            onBuy={() => order('BUY', tradeAmount)}
            onSell={() => order('SELL', tradeAmount)}
            onNextWeek={nextWeek}
            onEndGame={async () => {
              const res = await finish();
              if (res) {
                alert(
                  `게임 종료\n총 자산: ${new Intl.NumberFormat('ko-KR').format(
                    res.totalAsset,
                  )}원\n실현 손익: ${new Intl.NumberFormat('ko-KR').format(
                    res.realizedProfit,
                  )}원\n수익률: ${res.totalYield.toFixed(2)}%\n성향: ${res.tendencyType}\n추천: ${
                    res.recommendation
                  }`,
                );
              }
            }}
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

      <TradeSuccessModal
        isOpen={tradeSuccessModal.isOpen}
        onClose={closeTradeSuccessModal}
        tradeType={tradeSuccessModal.tradeType}
        quantity={tradeSuccessModal.quantity}
        price={tradeSuccessModal.price}
      />

      <GameFinishModal
        isOpen={gameFinishModal.isOpen}
        onClose={closeGameFinishModal}
        onGoHome={handleGoHome}
        result={gameFinishModal.result}
      />
    </div>
  );
}
