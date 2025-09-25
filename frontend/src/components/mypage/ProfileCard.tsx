import * as React from 'react';
import { useAuth } from '../../stores/auth';
import NicknameDialog from './NicknameDialog';
import { api } from '../../api/client';

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
  gameCount?: number;
};

export default function ProfileCard({ favoriteCount = 0, gameCount = 0 }: Props) {
  const user = useAuth((s) => s.user);
  const setUser = useAuth((s) => s.setUser);
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
      await api.del<void>('/api/users/me');
    } finally {
      await logout();
    }
  };

  if (!user) return null;

  return (
    // 카드 높이가 커져도 하단이 비지 않게 flex-col + mt-auto 앵커
    <section className="h-full flex flex-col w-full bg-slate-700 backdrop-blur-xl rounded-2xl shadow-lg border border-slate-600 p-6 relative overflow-visible">
      {/* ── 상단(아바타/이름/가입일) */}
      <div>
        {/* 아바타 */}
        <div className="relative flex justify-center">
          {user.avatarUrl ? (
            <img
              src={user.avatarUrl}
              alt="프로필"
              className="w-24 h-24 rounded-full object-cover shadow-lg ring-4 ring-amber-500/30"
              referrerPolicy="no-referrer"
            />
          ) : (
            <div className="w-24 h-24 rounded-full bg-gradient-to-r from-amber-500 to-amber-600 text-white flex items-center justify-center font-extrabold text-3xl shadow-lg ring-4 ring-amber-400/30 select-none">
              {getInitials(user.nickname)}
            </div>
          )}
        </div>

        <div className="mt-5 flex items-center justify-center gap-2 flex-wrap">
          <h2 className="text-2xl font-extrabold text-white tracking-tight">{user.nickname}</h2>
          <button
            onClick={() => setOpenNick(true)}
            className="inline-flex items-center gap-1 px-2 py-1 rounded-md border border-slate-500 bg-slate-600 text-sm text-slate-200 hover:bg-slate-500 hover:border-amber-400 transition-all"
            title="닉네임 변경"
            aria-label="닉네임 변경"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden>
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

        <p className="mt-1 text-center text-slate-400 text-sm">
          • 가입일: {new Date(user.createdAt).toLocaleDateString()}
        </p>
      </div>

      {/* ── 하단(요약 3칸 + 탈퇴 버튼) : 카드 바닥에 고정 */}
      <div className="flex flex-col gap-5 mt-auto pt-6">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {/* 관심 종목 */}
          <div className="rounded-xl p-4 text-center bg-slate-600 border border-slate-500 hover:border-amber-400 transition-all">
            <div className="text-amber-400 text-2xl font-bold">
              {favoriteCount.toLocaleString()}
            </div>
            <div className="text-slate-300 text-xs">관심 종목</div>
          </div>

          {/* 게임 횟수 (추후 데이터 붙이면 교체) */}
          <div className="rounded-xl p-4 text-center bg-slate-600 border border-slate-500 hover:border-amber-400 transition-all">
            <div className="text-amber-400 text-2xl font-bold">{gameCount.toLocaleString()}</div>
            <div className="text-slate-300 text-xs">게임 횟수</div>
          </div>

          {/* 잔고 */}
          <div className="rounded-xl p-4 text-center bg-gradient-to-r from-amber-500/20 to-amber-600/20 border border-amber-400/50 sm:col-span-2">
            <div className="text-amber-300 text-2xl font-bold">{user.cash.toLocaleString()}원</div>
            <div className="text-slate-300 text-xs">나의 잔고</div>
          </div>
        </div>

        <div className="flex justify-end">
          <button
            onClick={onDeleteAccount}
            className="px-4 py-2 rounded-lg bg-red-600/80 text-white hover:bg-red-600 focus:outline-none focus:ring-2 focus:ring-red-400/50 transition-all"
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
