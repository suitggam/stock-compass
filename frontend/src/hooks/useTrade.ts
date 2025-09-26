// hooks/useTrade.ts
import { useState } from "react";
import { buyStock, sellStock } from "../api/TradeApi";
import type { TradeHistoryDto, TradeRequest } from "../types/Trade";

export const useTrade = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleBuyStock = async (
    request: TradeRequest
  ): Promise<TradeHistoryDto | null> => {
    setLoading(true);
    try {
      return await buyStock(request);
    } finally {
      setLoading(false);
    }
  };
  const handleSellStock = async (
    request: TradeRequest
  ): Promise<TradeHistoryDto | null> => {
    setLoading(true);
    try {
      return await sellStock(request);
    } finally {
      setLoading(false);
    }
  };

  return {
    loading,
    error,
    buyStock: handleBuyStock,
    sellStock: handleSellStock,
    clearError: () => setError(null),
  };
};

// 사용 예시
/*
import { useTrade } from '../hooks/useTrade';

function TradeComponent() {
  const { loading, error, buyStock, sellStock, clearError } = useTrade();

  const handleBuy = async () => {
    const result = await buyStock({
      ticker: 'AAPL',
      price: 150000,
      volume: 10
    });

    if (result) {
      console.log('매수 성공:', result);
      // 성공 처리
    }
    // 에러는 error 상태에서 확인 가능
  };

  return (
    <div>
      {error && <div className="error">{error}</div>}
      <button onClick={handleBuy} disabled={loading}>
        {loading ? '처리중...' : '매수'}
      </button>
    </div>
  );
}
*/
