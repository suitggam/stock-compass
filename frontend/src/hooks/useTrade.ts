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

    const data = await buyStock(request);
    return data;
  };

  const handleSellStock = async (
    request: TradeRequest
  ): Promise<TradeHistoryDto | null> => {
    setLoading(true);

    const data = await sellStock(request);
    return data;
  };

  return {
    loading,
    error,
    buyStock: handleBuyStock,
    sellStock: handleSellStock,
    clearError: () => setError(null),
  };
};
