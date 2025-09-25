export interface History {
  tradeType: HistoryType;
  price: number;
  createAt: Date;
}

export interface TradeHistoryDto {
  tradeType: "BUY" | "SELL";
  price: number;
  volume: number;
  totalPrice: number;
  createAt: string; // LocalDateTime는 ISO 8601 문자열로 전송됨
}

export interface TradeRequest {
  ticker: string;
  price: number;
  volume: number;
}

export interface UserAsset {
  cash: number;
  haveStock: number;
  originalMoney: number;
}

type HistoryType = "BUY" | "SELL";

export const mockData: History[] = [
  {
    tradeType: "BUY",
    price: 10000,
    createAt: new Date(),
  },
  {
    tradeType: "SELL",
    price: 10000,
    createAt: new Date(),
  },
];
