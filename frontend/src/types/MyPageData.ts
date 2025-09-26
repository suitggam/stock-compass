type TradeType = 'BUY' | 'SELL';

export interface TradeHistoryEntry {
  tradeNo: number;
  tradeType: TradeType;
  price: number;
  volume: number;
  totalPrice: number;
  ticker: string;
  companyName?: string;
  createdAt: string;
}
export interface MyPageData {
  userNo: number;
  socialEmail: string;
  nickname: string;
  cancel: boolean;
  createdAt: string | null;
  avatarUrl?: string;
  favorites?: { itemId: number; name: string; ticker: string }[];
  gameResult?: {
    gameNo: number;
    userNo: number;
    tendency_i: number;
    tendency_e: number;
    tendency_s: number;
    tendency_n: number;
    tendency_f: number;
    tendency_t: number;
    tendency_j: number;
    tendency_p: number;
    createdAt: string;
  } | null;
  tradeHistory?: TradeHistoryEntry[] | null;
}

export interface PageResponseDto<T> {
  dtoList: T[];
  pageRequestDto: { page: number; size: number };
  totalCount: number;
  prev: boolean;
  next: boolean;
  pageNumberList: number[];
  prevPage: number;
  nextPage: number;
  totalPage: number;
  current: number;
}

export type TradeHistoryPage = PageResponseDto<TradeHistoryEntry>;

export interface MyPageDataPaged extends Omit<MyPageData, 'tradeHistory'> {
  tradeHistory?: TradeHistoryPage | null;
}
