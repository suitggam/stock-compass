export interface kospi200_5years {
  id: number;
  ticker: string;
  company_name: string;
  date: string;
  open_price: number;
  high_price: number;
  low_price: number;
  close_price: number;
  volume: number;
  market_cap: number;
}
export const mockData: kospi200_5years[] = [
  {
    id: 1,
    ticker: "005930",
    company_name: "삼성전자",
    date: "2025-09-10",
    open_price: 59300,
    high_price: 59400,
    low_price: 58200,
    close_price: 59000,
    volume: 16017098,
    market_cap: 3522171,
  },
];
