// hooks/useTrade.ts
import { useState } from "react";
import { buyStock } from "../api/TradeApi";
import type { TradeHistoryDto, TradeBuyRequest } from "../types/Trade";

export const useTrade = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleBuyStock = async (
    request: TradeBuyRequest
  ): Promise<TradeHistoryDto | null> => {
    setLoading(true);
    setError(null);

    const data = await buyStock(request);
    return data;
  };

  //   const handleSellStock = async (
  //     request: TradeBuyRequest
  //   ): Promise<TradeHistoryDto | null> => {
  //     setLoading(true);
  //     setError(null);

  //     try {
  //       const data = await sellStock(request);
  //       return data;
  //     } catch (err: any) {
  //       let errorMessage = "매도 주문 처리 중 오류가 발생했습니다.";

  //       // 에러 타입별 메시지 처리
  //       if (err.response?.status === 400) {
  //         errorMessage = err.response.data?.message || "잘못된 요청입니다.";
  //       } else if (err.response?.status === 401) {
  //         errorMessage = "로그인이 필요합니다.";
  //       } else if (err.response?.status === 403) {
  //         errorMessage = "권한이 없습니다.";
  //       } else if (err.response?.status === 500) {
  //         errorMessage = "서버 오류가 발생했습니다.";
  //       } else if (err.code === "NETWORK_ERROR") {
  //         errorMessage = "네트워크 연결을 확인해주세요.";
  //       } else if (err.message) {
  //         errorMessage = err.message;
  //       }

  //       setError(errorMessage);
  //       return null;
  //     } finally {
  //       setLoading(false);
  //     }
  //   };

  return {
    loading,
    error,
    buyStock: handleBuyStock,
    // sellStock: handleSellStock,
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
