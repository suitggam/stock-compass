import React, { useEffect, useState } from 'react';
import { api, token } from '../api/client';
import { useNavigate } from 'react-router';

type User = {
  userNo: number;
  nickname: string;
  socialEmail: string;
  createdAt: string;
  totalReward: number;
  cash: number;
};

const fmtCurrency = (n?: number | null) =>
  n === undefined || n === null ? '-' : new Intl.NumberFormat('ko-KR').format(n) + '원';

const MyPage: React.FC = () => {
  const nav = useNavigate();
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const ensureAccess = async () => {
      if (!token.get()) {
        try {
          const data = await api.post<{ accessToken: string }>('/api/auth/refresh', {});
          if (data?.accessToken) token.set(data.accessToken);
        } catch (e) {
          // 비로그인 → 메인으로
          console.debug('[refresh in mypage] redirect:', e);
          nav('/', { replace: true });
          return;
        }
      }

      try {
        const data = await api.get<User>('/api/users/me');
        setUser(data);
      } catch (e) {
        console.debug('[me] failed:', e);
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
      <div className="min-h-screen grid place-items-center">유저 정보를 불러오지 못했어요.</div>
    );
  }

  return (
    <div className="min-h-screen p-6">
      <h1 className="text-2xl font-bold mb-4">마이페이지</h1>

      <div className="max-w-xl">
        <div className="border rounded-lg p-4 mb-4">
          <div className="mb-2">
            <b>유저번호</b> : {user.userNo}
          </div>
          <div className="mb-2">
            <b>이메일</b> : {user.socialEmail}
          </div>
          <div className="mb-2">
            <b>닉네임</b> : {user.nickname}
          </div>
          <div className="mb-2">
            <b>가입일</b> : {new Date(user.createdAt).toLocaleString()}
          </div>
        </div>

        <div className="border rounded-lg p-4">
          <div className="mb-2">
            <b>총 리워드</b> : {fmtCurrency(user.totalReward)}
          </div>
          <div className="mb-2">
            <b>보유 캐시</b> : {fmtCurrency(user.cash)}
          </div>
        </div>

        <div className="mt-6 flex gap-2">
          <button className="px-3 py-2 rounded border" onClick={() => nav('/')}>
            돌아가기
          </button>
          <button
            className="px-3 py-2 rounded bg-rose-500 text-white"
            onClick={async () => {
              try {
                await api.post<void>('/api/auth/logout', {});
              } catch (e) {
                console.debug('[logout in mypage] ignored:', e);
              } finally {
                token.clear();
                nav('/', { replace: true });
              }
            }}
          >
            로그아웃
          </button>
        </div>
      </div>
    </div>
  );
};

export default MyPage;
