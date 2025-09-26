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
  mockInvestHistory?:
    | { id: number; symbol: string; quantity: number; price: number; tradedAt: string }[]
    | null;
}
