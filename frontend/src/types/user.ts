import type { UserAsset } from "./Trade";

export interface User {
  userNo: number;
  nickname: string;
  socialEmail: string;
  createdAt: string;
  totalReward: number;
  cash: number;

  avatarUrl?: string;
}

export const mockData2: UserAsset = {
  cash: 100000000,
  haveStock: 0,
  originalMoney: 100000000,
};
