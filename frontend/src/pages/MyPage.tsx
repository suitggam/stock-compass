import { useEffect, useState } from 'react';
import { Navigate } from 'react-router';
import { useAuth } from '../stores/auth';
import ProfileCard from '../components/mypage/ProfileCard';
import PersonalityResult from '../components/mypage/PersonalityResult';
import Watchlist from '../components/mypage/Watchlist';
import MockInvestmentHistory from '../components/mypage/MockInvestmentHistory';
import type { MyPageData } from '../types/MyPageData';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export default function MyPage() {
  const loading = useAuth((s) => s.loading);
  const authUser = useAuth((s) => s.user);
  const token = useAuth((s) => s.accessToken);

  const [data, setData] = useState<MyPageData | null>(null);
  const [pending, setPending] = useState(true);

  useEffect(() => {
    if (!token) {
      setPending(false);
      return;
    }
    (async () => {
      try {
        const res = await fetch(`${API_BASE}/api/mypage/me`, {
          credentials: 'include',
          headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        });
        if (res.ok) setData(await res.json());
      } finally {
        setPending(false);
      }
    })();
  }, [token]);

  if (loading || pending)
    return <div className="min-h-dvh grid place-items-center text-neutral-300">불러오는 중…</div>;
  if (!authUser) return <Navigate to="/" replace />;

  return (
    <div className="min-h-dvh bg-slate-800 py-8">
      <div className="mx-auto w-full max-w-[1400px] px-4 sm:px-5 space-y-6">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
          <ProfileCard /* 필요하다면 data를 내려서 아바타/잔고/가입일 등 사용 */ />
          <PersonalityResult data={data?.personality ?? null} />
        </div>

        <Watchlist items={data?.favorites ?? []} />
        <MockInvestmentHistory items={data?.mockInvestHistory ?? []} />
      </div>
    </div>
  );
}
