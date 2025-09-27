type TradeType = "BUY" | "SELL";

export interface TradeHistoryEntry {
  itemNo: number;
  companyName: string;
  tradeType: TradeType;
  price: number;
  volume: number;
  createdAt: string;
  totalPrice?: number;
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
    tendencyI: number;
    tendencyE: number;
    tendencyS: number;
    tendencyN: number;
    tendencyF: number;
    tendencyT: number;
    tendencyJ: number;
    tendencyP: number;
    tendencyResult: string;
    createdAt: string;
  } | null;

  account?: {
    originalMoney: number;
    cash: number;
    haveStock: number;
    totalReward: number;
    totalAsset: number;
    returnPct: number | null;
  };
  tradeHistory?: TradeHistoryEntry[];
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

export interface MyPageDataPaged extends Omit<MyPageData, "tradeHistory"> {
  tradeHistory?: TradeHistoryPage | null;
}
