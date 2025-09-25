// TradeApi.tsx
import type { TradeRequest, TradeHistoryDto, UserAsset } from "../types/Trade";
import { api } from "./client";

export const userAsset = async (): Promise<UserAsset> => {
  const response = await api.get<UserAsset>(`/api/trade/userAsset`);

  return response;
};

// 실제 매수 요청 API 호출
export const buyStock = async (
  request: TradeRequest
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

export const sellStock = async (
  request: TradeRequest
): Promise<TradeHistoryDto> => {
  // 기존의 axios를 api 객체로 변경
  const response = await api.post<TradeHistoryDto>(
    `/api/trade/sell/${request.ticker}`,
    {
      price: request.price,
      volume: request.volume,
    }
  );

  return response;
};
