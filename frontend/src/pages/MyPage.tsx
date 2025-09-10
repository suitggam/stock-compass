import React, { useEffect, useState } from 'react';
import { api, token } from '../api/client';
import { useNavigate } from 'react-router-dom';

// 서버 UserProfileDto에 맞춘 타입
type User = {
  userNo: number;
  nickname: string;
  socialEmail: string;
  createdAt: string; // ISO 문자열
  totalReward: number;
  cash: number;
};

const fmtCurrency = (n?: number | null) =>
  n === undefined || n === null ? '-' : new Intl.NumberFormat('ko-KR').format(n) + '원';

const fmtDate = (iso?: string | null) => (iso ? new Date(iso).toISOString().slice(0, 10) : '-');

const MyPage: React.FC = () => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const nav = useNavigate();

  useEffect(() => {
    const ensureAccess = async () => {
      // access 없으면 refresh로 새로 받기
      if (!token.get()) {
        try {
          const data = await api.post<{ accessToken: string }>('/api/auth/refresh', {});
          if (data?.accessToken) token.set(data.accessToken);
        } catch {
          nav('/', { replace: true });
          return;
        }
      }

      try {
        const data = await api.get<User>('/api/users/me'); // Authorization 헤더는 client.ts에서 처리
        setUser(data);
      } catch {
        nav('/', { replace: true });
      } finally {
        setLoading(false);
      }
    };

    ensureAccess();
  }, [nav]);

  if (loading) {
    return <div className="min-h-screen grid place-items-center">불러오는 중…</div>;
  }

  if (!user) {
    return (
      <div className="min-h-screen grid place-items-center">사용자 정보를 가져오지 못했습니다.</div>
    );
  }

  // 화면에서 실제 사용해 경고 제거
  const name = user.nickname ?? '사용자';
  const joined = fmtDate(user.createdAt);
  const cash = fmtCurrency(user.cash ?? 0);
  const favCount = 0; // TODO: /api/favorite 연동 시 교체
  const gameCount = 0; // TODO: 랭킹/게임 통계 API 생기면 교체

  // --- 이하 레이아웃 예시(필요 시 네 기존 마크업으로 교체 가능) ---
  return (
    <div className="w-[1920px] min-h-[1200px] px-64 bg-gradient-to-br from-indigo-500 to-purple-800 text-white">
      <div className="py-16">
        <h1 className="text-4xl font-bold mb-6">마이페이지</h1>

        <div className="grid grid-cols-2 gap-8">
          <div className="rounded-2xl bg-white/10 p-6">
            <h2 className="text-2xl font-semibold mb-4">프로필</h2>
            <p>
              <span className="opacity-80">닉네임:</span>{' '}
              <span className="font-medium">{name}</span>
            </p>
            <p>
              <span className="opacity-80">이메일:</span>{' '}
              <span className="font-medium">{user.socialEmail}</span>
            </p>
            <p>
              <span className="opacity-80">가입일:</span>{' '}
              <span className="font-medium">{joined}</span>
            </p>
            <p>
              <span className="opacity-80">유저번호:</span>{' '}
              <span className="font-medium">{user.userNo}</span>
            </p>
          </div>

          <div className="rounded-2xl bg-white/10 p-6">
            <h2 className="text-2xl font-semibold mb-4">자산</h2>
            <p>
              <span className="opacity-80">총 상금/보상:</span>{' '}
              <span className="font-medium">{fmtCurrency(user.totalReward)}</span>
            </p>
            <p>
              <span className="opacity-80">보유 현금:</span>{' '}
              <span className="font-medium">{cash}</span>
            </p>
          </div>

          <div className="rounded-2xl bg-white/10 p-6">
            <h2 className="text-2xl font-semibold mb-4">활동</h2>
            <p>
              <span className="opacity-80">관심 종목 수:</span>{' '}
              <span className="font-medium">{favCount}</span>
            </p>
            <p>
              <span className="opacity-80">게임/랭킹 참여:</span>{' '}
              <span className="font-medium">{gameCount}</span>
            </p>
          </div>

          <div className="rounded-2xl bg-white/10 p-6">
            <button
              className="px-4 py-2 rounded-lg bg-white text-indigo-600 font-semibold"
              onClick={() => {
                token.clear();
                nav('/', { replace: true });
              }}
            >
              로그아웃
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default MyPage;
