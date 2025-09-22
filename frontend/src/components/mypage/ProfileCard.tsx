import * as React from 'react';
import { useAuth } from '../../stores/auth';
import NicknameDialog from './NicknameDialog';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

// 헤더와 동일한 이니셜 생성 로직
function getInitials(name: string) {
  const trimmed = (name || '').trim();
  if (!trimmed) return '?';
  const parts = trimmed.split(/\s+/);
  if (parts.length >= 2) return (parts[0][0] + parts[1][0]).toUpperCase();
  return trimmed.slice(0, 2).toUpperCase();
}

type Props = {
  favoriteCount?: number; // 관심 종목 개수
  gameCount?: number; // 추후 필요시 사용
};

export default function ProfileCard({ favoriteCount = 0, gameCount = 0 }: Props) {
  const user = useAuth((s) => s.user);
  const setUser = useAuth((s) => s.setUser);
  const accessToken = useAuth((s) => s.accessToken);
  const logout = useAuth((s) => s.logout);

  const [openNick, setOpenNick] = React.useState(false);
  const onSavedNickname = (newNickname: string) => {
    if (!user) return;
    setUser({ ...user, nickname: newNickname });
  };

  const onDeleteAccount = async () => {
    const ok = window.confirm('정말로 회원탈퇴 하시겠어요? 이 작업은 되돌릴 수 없습니다.');
    if (!ok) return;
    try {
      await fetch(`${API_BASE}/api/users/me`, {
        method: 'DELETE',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
        },
      });
    } finally {
      await logout();
    }
  };

  if (!user) return null;

  return (
    // 카드 높이가 커져도 하단이 비지 않게 flex-col + mt-auto 앵커
    <section className="h-full flex flex-col w-full bg-white/85 backdrop-blur-xl rounded-2xl shadow-[0_8px_40px_rgba(0,0,0,0.16)] p-6 border border-black/5 relative overflow-visible">
      {/* 상단 액센트 바 */}
      <div className="absolute inset-x-0 top-0 h-1 rounded-t-2xl bg-gradient-to-r from-black via-neutral-800 to-amber-500" />

      {/* ── 상단(아바타/이름/가입일) */}
      <div>
        {/* 아바타 */}
        <div className="relative flex justify-center">
          {user.avatarUrl ? (
            <img
              src={user.avatarUrl}
              alt="프로필"
              className="w-24 h-24 rounded-full object-cover shadow-[0_12px_30px_rgba(0,0,0,0.35)] ring-4 ring-amber-400/20"
              referrerPolicy="no-referrer"
            />
          ) : (
            <div className="w-24 h-24 rounded-full bg-amber-500/90 text-slate-900 flex items-center justify-center font-extrabold text-3xl shadow-[0_12px_30px_rgba(0,0,0,0.35)] ring-4 ring-amber-400/20 select-none">
              {getInitials(user.nickname)}
            </div>
          )}
        </div>

        <div className="mt-5 flex items-center justify-center gap-2 flex-wrap">
          <h2 className="text-2xl font-extrabold text-neutral-900 tracking-tight">
            {user.nickname}
          </h2>
          <button
            onClick={() => setOpenNick(true)}
            className="inline-flex items-center gap-1 px-2 py-1 rounded-md border border-neutral-200 text-sm text-neutral-700 hover:bg-neutral-50"
            title="닉네임 변경"
            aria-label="닉네임 변경"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path
                d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25Z"
                stroke="currentColor"
                strokeWidth="1.5"
              />
              <path
                d="M14.06 6.19l3.75 3.75L20.5 7.25l-3.75-3.75-2.69 2.69Z"
                stroke="currentColor"
                strokeWidth="1.5"
              />
            </svg>
            <span>변경</span>
          </button>
        </div>

        <p className="mt-1 text-center text-neutral-500 text-sm">
          • 가입일: {new Date(user.createdAt).toLocaleDateString()}
        </p>
      </div>

      {/* ── 하단(요약 3칸 + 탈퇴 버튼) : 카드 바닥에 고정 */}
      <div className="mt-6 flex flex-col gap-5 mt-auto">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {/* 관심 종목 */}
          <div className="rounded-xl p-4 text-center bg-amber-400/10 border border-amber-500/20">
            <div className="text-amber-600 text-2xl font-bold">
              {favoriteCount.toLocaleString()}
            </div>
            <div className="text-neutral-500 text-xs">관심 종목</div>
          </div>

          {/* 게임 횟수 (추후 데이터 붙이면 교체) */}
          <div className="rounded-xl p-4 text-center bg-amber-400/10 border border-amber-500/20">
            <div className="text-amber-600 text-2xl font-bold">{gameCount.toLocaleString()}</div>
            <div className="text-neutral-500 text-xs">게임 횟수</div>
          </div>

          {/* 잔고 */}
          <div className="rounded-xl p-4 text-center bg-amber-400/10 border border-amber-500/20 sm:col-span-2">
            <div className="text-amber-600 text-2xl font-bold">{user.cash.toLocaleString()}원</div>
            <div className="text-neutral-500 text-xs">나의 잔고</div>
          </div>
        </div>

        <div className="flex justify-end">
          <button
            onClick={onDeleteAccount}
            className="px-4 py-2 rounded-lg bg-red-600 text-white hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-400/50"
          >
            회원탈퇴
          </button>
        </div>
      </div>

      {/* 닉네임 모달 */}
      <NicknameDialog
        open={openNick}
        initialNickname={user.nickname}
        onClose={() => setOpenNick(false)}
        onSaved={onSavedNickname}
      />
    </section>
  );
}
