export interface StockInfos {
  id: number;
  ticker: string;
  companyName: string;
  date: string;
  open_price: number;
  high_price: number;
  low_price: number;
  close_price: number;
  volume: number;
  market_cap: number;
}
export const mockData: StockInfos[] = [
  {
    id: 1,
    ticker: "005930",
    companyName: "삼성전자",
    date: "2025-09-10",
    open_price: 59300,
    high_price: 59400,
    low_price: 58200,
    close_price: 59000,
    volume: 16017098,
    market_cap: 3522171,
  },
];
