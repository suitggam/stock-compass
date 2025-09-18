export interface Term {
  text: string;
  onSelect?: () => void;
}
export interface StockDetail {
  ticker: string;
  companyName: string;
  endPrice: number;
  rate: number;
}

export interface News {
  title: string;
  url: string;
  date: string;
}

export interface Keyword {
  keyword: string;
  count: number;
}

export interface ChartData {
  date: string;
  endPrice: number;
}

export interface Chart {
  term: string;
  data: ChartData[];
}

export const TermText: Term[] = [
  { text: "1 주" },
  { text: "1개월" },
  { text: "3개월" },
  { text: "6개월" },
  { text: "1 년" },
  { text: "5 년" },
];

export const mockData: StockDetail = {
  ticker: "005930",
  companyName: "삼성전자",
  endPrice: 73000,
  rate: 1.23,
};

export const mockData2: News[] = [
  {
    title: "Test1",
    url: "https://www.inflearn.com/course/%EC%BD%94%EB%93%9C%EB%A1%9C-%EB%B0%B0%EC%9A%B0%EB%8A%94-%EB%A6%AC%EC%95%A1%ED%8A%B8",
    date: "2025-08-18",
  },
  {
    title: "Test2",
    url: "https://www.inflearn.com/course/%EC%BD%94%EB%93%9C%EB%A1%9C-%EB%B0%B0%EC%9A%B0%EB%8A%94-%EB%A6%AC%EC%95%A1%ED%8A%B8",
    date: "2025-08-21",
  },
  {
    title: "Test3",
    url: "https://www.inflearn.com/course/%EC%BD%94%EB%93%9C%EB%A1%9C-%EB%B0%B0%EC%9A%B0%EB%8A%94-%EB%A6%AC%EC%95%A1%ED%8A%B8",
    date: "2025-08-23",
  },
  {
    title: "Test4",
    url: "https://www.inflearn.com/course/%EC%BD%94%EB%93%9C%EB%A1%9C-%EB%B0%B0%EC%9A%B0%EB%8A%94-%EB%A6%AC%EC%95%A1%ED%8A%B8",
    date: "2025-08-26",
  },
  {
    title: "Test5",
    url: "https://www.inflearn.com/course/%EC%BD%94%EB%93%9C%EB%A1%9C-%EB%B0%B0%EC%9A%B0%EB%8A%94-%EB%A6%AC%EC%95%A1%ED%8A%B8",
    date: "2025-08-29",
  },
  {
    title: "Test6",
    url: "https://www.inflearn.com/course/%EC%BD%94%EB%93%9C%EB%A1%9C-%EB%B0%B0%EC%9A%B0%EB%8A%94-%EB%A6%AC%EC%95%A1%ED%8A%B8",
    date: "2025-09-01",
  },
  {
    title: "Test7",
    url: "https://www.inflearn.com/course/%EC%BD%94%EB%93%9C%EB%A1%9C-%EB%B0%B0%EC%9A%B0%EB%8A%94-%EB%A6%AC%EC%95%A1%ED%8A%B8",
    date: "2025-09-03",
  },
  {
    title: "Test8",
    url: "https://www.inflearn.com/course/%EC%BD%94%EB%93%9C%EB%A1%9C-%EB%B0%B0%EC%9A%B0%EB%8A%94-%EB%A6%AC%EC%95%A1%ED%8A%B8",
    date: "2025-09-06",
  },
  {
    title: "Test9",
    url: "https://www.inflearn.com/course/%EC%BD%94%EB%93%9C%EB%A1%9C-%EB%B0%B0%EC%9A%B0%EB%8A%94-%EB%A6%AC%EC%95%A1%ED%8A%B8",
    date: "2025-09-11",
  },
  {
    title: "Test10",
    url: "https://www.inflearn.com/course/%EC%BD%94%EB%93%9C%EB%A1%9C-%EB%B0%B0%EC%9A%B0%EB%8A%94-%EB%A6%AC%EC%95%A1%ED%8A%B8",
    date: "2025-09-12",
  },
  {
    title: "Test11",
    url: "https://www.inflearn.com/course/%EC%BD%94%EB%93%9C%EB%A1%9C-%EB%B0%B0%EC%9A%B0%EB%8A%94-%EB%A6%AC%EC%95%A1%ED%8A%B8",
    date: "2025-09-17",
  },
];

export const mockData3: Keyword[] = [
  { keyword: "test1", count: 1 },
  { keyword: "test2", count: 2 },
  { keyword: "test3", count: 3 },
  { keyword: "test4", count: 4 },
  { keyword: "test5", count: 5 },
  { keyword: "test6", count: 6 },
  { keyword: "test7", count: 7 },
  { keyword: "test8", count: 8 },
  { keyword: "test9", count: 9 },
  { keyword: "test10", count: 10 },
  { keyword: "test11", count: 11 },
];

export const mockData4: ChartData[] = [
  { date: "2025-08-17", endPrice: 64000 },
  { date: "2025-08-18", endPrice: 61000 },
  { date: "2025-08-19", endPrice: 92000 },
  { date: "2025-08-20", endPrice: 52000 },
  { date: "2025-08-21", endPrice: 66000 },
  { date: "2025-08-22", endPrice: 65000 },
  { date: "2025-08-23", endPrice: 68000 },
  { date: "2025-08-24", endPrice: 62000 },
  { date: "2025-08-25", endPrice: 45000 },
  { date: "2025-08-26", endPrice: 88000 },
  { date: "2025-08-27", endPrice: 97000 },
  { date: "2025-08-28", endPrice: 48000 },
  { date: "2025-08-29", endPrice: 96000 },
  { date: "2025-08-30", endPrice: 72000 },
  { date: "2025-08-31", endPrice: 82000 },
  { date: "2025-09-01", endPrice: 54000 },
  { date: "2025-09-02", endPrice: 66000 },
  { date: "2025-09-03", endPrice: 81000 },
  { date: "2025-09-04", endPrice: 84000 },
  { date: "2025-09-05", endPrice: 87000 },
  { date: "2025-09-06", endPrice: 93000 },
  { date: "2025-09-07", endPrice: 73000 },
  { date: "2025-09-08", endPrice: 77000 },
  { date: "2025-09-09", endPrice: 82000 },
  { date: "2025-09-10", endPrice: 79000 },
  { date: "2025-09-11", endPrice: 71000 },
  { date: "2025-09-12", endPrice: 81000 },
  { date: "2025-09-13", endPrice: 62000 },
  { date: "2025-09-14", endPrice: 73000 },
  { date: "2025-09-15", endPrice: 81000 },
  { date: "2025-09-16", endPrice: 62000 },
  { date: "2025-09-17", endPrice: 73000 },
];
