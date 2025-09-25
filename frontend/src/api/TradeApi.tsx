// TradeApi.tsx
import type { TradeBuyRequest, TradeHistoryDto } from "../types/Trade";
import { api } from "./client";

// 실제 매수 요청 API 호출
export const buyStock = async (
  request: TradeBuyRequest
): Promise<TradeHistoryDto> => {
  // 기존의 axios를 api 객체로 변경
  const response = await api.post<TradeHistoryDto>(
    `/api/trade/buy/${request.ticker}`,
    {
      price: request.price,
      volume: request.volume,
    }
  );

  return response;
};
