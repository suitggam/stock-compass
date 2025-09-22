import { Navigate } from "react-router";
import { useAuth } from "../stores/auth";
import ProfileCard from "../components/mypage/ProfileCard";
import PersonalityResult from "../components/mypage/PersonalityResult";
import Watchlist from "../components/mypage/Watchlist";
import MockInvestmentHistory from "../components/mypage/MockInvestmentHistory";

export default function MyPage() {
  const loading = useAuth((s) => s.loading);
  const user = useAuth((s) => s.user);

  if (loading)
    return (
      <div className="min-h-dvh grid place-items-center text-neutral-300">
        불러오는 중…
      </div>
    );
  if (!user) return <Navigate to="/" replace />;

  return (
    <div className="min-h-dvh bg-slate-800 py-8">
      <div className="mx-auto w-full max-w-[1400px] px-4 sm:px-5 space-y-6">
        {/* 상단 2열: 기본 동작(동일 높이 유지) */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
          <ProfileCard />
          <PersonalityResult />
        </div>

        <Watchlist />
        <MockInvestmentHistory />
      </div>
    </div>
  );
}
