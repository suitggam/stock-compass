export interface User {
  userNo: number;
  socialEmail?: string | null;
  nickname: string;
  cancel: boolean;
  createdAt?: string | null;
  updatedAt?: string | null;
  lastLoginAt?: string | null;
  top1?: number | null;
  top2?: number | null;
  top3?: number | null;
  topten?: number | null;
  asset?: number | null;
  cash?: number | null;
}
