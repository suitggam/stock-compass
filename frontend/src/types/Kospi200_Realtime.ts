export interface Kospi200_Realtime {
  ticker: string;
  company_name: string;
  price: number;
  rate: number;
}
export const mockData: Kospi200_Realtime[] = [
  {
    ticker: "005930",
    company_name: "삼성전자",
    price: 86300,
    rate: -0.11574074074074073,
  },
];
