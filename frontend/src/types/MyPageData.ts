export interface MyPageData {
  userNo: number;
  socialEmail: string;
  nickname: string;
  cancel: boolean;
  totalReward: number;
  cash: number;
  createdAt: string | null;
  avatarUrl?: string;
  favorites?: { itemId: number; name: string }[];
  personality?: { type: string; summary: string } | null;
  mockInvestHistory?:
    | { id: number; symbol: string; quantity: number; price: number; tradedAt: string }[]
    | null;
}
