import { useEffect, useState } from 'react';
import { API_BASE, api, token } from '../api/client';
import { useNavigate } from 'react-router-dom';
import NicknameDialog from '../components/NicknameDialog';

type User = {
  userNo: number;
  nickname: string;
  socialEmail: string;
  createdAt: string;
  totalReward: number;
  cash: number;
};

function MainPage() {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [openNickDialog, setOpenNickDialog] = useState(false);
  const nav = useNavigate();

  // 로그인 상태 확보(없으면 refresh → me)
  useEffect(() => {
    (async () => {
      try {
        if (!token.get()) {
          try {
            const res = await api.post<{ accessToken: string }>('/api/auth/refresh', {});
            if (res?.accessToken) token.set(res.accessToken);
          } catch {
            // 비로그인 상태
          }
        }
        if (token.get()) {
          const me = await api.get<User>('/api/users/me');
          setUser(me);
        } else {
          setUser(null);
        }
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const goGoogle = () => (window.location.href = `${API_BASE}/users/auth/google`);
  const goKakao = () => (window.location.href = `${API_BASE}/users/auth/kakao`);

  const onLogout = async () => {
    try {
      await api.post<void>('/api/auth/logout', {});
    } catch {
      // ignore
    } finally {
      token.clear();
      setUser(null);
    }
  };

  // “닉네임이 기본생성처럼 보이면 모달 추천” (정책에 맞게 조정)
  const looksAuto = (n: string) => /^user(_\d+)?$/i.test(n) || /_\d{4}$/.test(n);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-indigo-500 to-purple-800 text-white">
        불러오는 중…
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col items-center justify-center gap-6 bg-gradient-to-br from-indigo-500 to-purple-800">
      <h1 className="text-white text-3xl font-bold">메인페이지입니다.</h1>

      {!user ? (
        // 비로그인: 로그인 버튼 노출
        <div className="flex gap-3">
          <button
            onClick={goGoogle}
            className="px-5 py-3 rounded-xl bg-white text-gray-800 font-semibold shadow"
          >
            🔵 구글로 로그인
          </button>
          <button
            onClick={goKakao}
            className="px-5 py-3 rounded-xl bg-yellow-300 text-black font-semibold shadow"
          >
            🟡 카카오로 로그인
          </button>
        </div>
      ) : (
        // 로그인됨: 로그인 버튼 감춤 + 마이페이지/닉네임/로그아웃
        <div className="flex flex-col items-center gap-4">
          <div className="text-white/90">
            <span className="opacity-80">안녕하세요,</span>{' '}
            <span className="font-semibold">{user.nickname}</span>
            <span className="opacity-80">님!</span>
          </div>

          <div className="flex gap-3">
            <button
              onClick={() => nav('/mypage')}
              className="px-5 py-3 rounded-xl bg-white text-gray-800 font-semibold shadow"
            >
              마이페이지로 이동
            </button>
            <button
              onClick={() => setOpenNickDialog(true)}
              className="px-5 py-3 rounded-xl bg-white/90 text-indigo-700 font-semibold shadow"
            >
              닉네임 설정/변경
            </button>
            <button
              onClick={onLogout}
              className="px-5 py-3 rounded-xl bg-white/80 text-rose-700 font-semibold shadow"
            >
              로그아웃
            </button>
          </div>

          {looksAuto(user.nickname) && (
            <div className="text-sm text-yellow-100">
              기본 닉네임처럼 보여요.{' '}
              <button className="underline" onClick={() => setOpenNickDialog(true)}>
                변경하기
              </button>
            </div>
          )}
        </div>
      )}

      {/* 닉네임 모달 */}
      <NicknameDialog
        open={openNickDialog}
        initialNickname={user?.nickname}
        onClose={() => setOpenNickDialog(false)}
        onSaved={(newNick) => setUser((u) => (u ? { ...u, nickname: newNick } : u))}
      />
    </div>
  );
}
export default MainPage;
