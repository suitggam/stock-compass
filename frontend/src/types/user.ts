export interface User {
  userNo: number;
  nickname: string;
  socialEmail: string;
  createdAt: string;
  totalReward: number;
  cash: number;

  avatarUrl?: string;
}

export interface UserTrade {
  totalMoney: number;
  cash: number;
  haveStock: number;
  marginPercent: number;
}

export const mockData2: UserTrade = {
  totalMoney: 10000,
  cash: 8000,
  haveStock: 3000,
  marginPercent: 10,
};
