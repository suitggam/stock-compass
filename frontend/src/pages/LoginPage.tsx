import * as React from 'react';
import { Link } from 'react-router';
import { useAuth } from '../stores/auth';

const API_BASE = 'http://j13a301.p.ssafy.io:8080/';

export default function LoginPage() {
  const { user, loading, bootstrap, logout } = useAuth();

  // 첫 진입/새로고침 시: refresh → me (스토어 내부에서 처리)
  React.useEffect(() => {
    // 이미 한 번 불렸더라도 부작용 없음
    void bootstrap();
  }, [bootstrap]);

  const loginGoogle = () => {
    window.location.href = `${API_BASE}/users/auth/google`;
  };

  const loginKakao = () => {
    window.location.href = `${API_BASE}/users/auth/kakao`;
  };

  if (loading) {
    return <div className="min-h-dvh grid place-items-center text-gray-600">불러오는 중…</div>;
  }

  return (
    <div className="min-h-dvh flex items-center justify-center bg-gray-50">
      <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-sm">
        <h1 className="mb-4 text-2xl font-semibold">로그인</h1>

        {user ? (
          <div className="space-y-4">
            <p className="text-gray-700">
              안녕하세요, <b>{user.nickname}</b> 님!
            </p>

            <div className="rounded-lg border p-4 text-sm text-gray-700">
              <div className="flex items-center justify-between">
                <span>이메일</span>
                <span className="font-medium">{user.socialEmail}</span>
              </div>
              <div className="mt-2 flex items-center justify-between">
                <span>보유 캐시</span>
                <span className="font-medium">{user.cash.toLocaleString()}원</span>
              </div>
              <div className="mt-2 flex items-center justify-between">
                <span>리워드 합계</span>
                <span className="font-medium">{user.totalReward.toLocaleString()}P</span>
              </div>
            </div>

            <div className="flex gap-2">
              <Link
                to="/mypage"
                className="flex-1 rounded-lg bg-black px-4 py-2 text-center text-white hover:opacity-90"
              >
                마이페이지로
              </Link>
              <button
                onClick={() => logout()}
                className="flex-1 rounded-lg bg-gray-200 px-4 py-2 hover:bg-gray-300"
              >
                로그아웃
              </button>
            </div>
          </div>
        ) : (
          <div className="space-y-3">
            <button
              onClick={loginGoogle}
              className="w-full rounded-lg bg-black px-4 py-3 text-white hover:opacity-90"
            >
              Google로 로그인
            </button>
            <button
              onClick={loginKakao}
              className="w-full rounded-lg bg-yellow-300 px-4 py-3 hover:brightness-95"
            >
              Kakao로 로그인
            </button>

            <p className="pt-2 text-center text-xs text-gray-500">
              로그인 후 새로고침해도 자동으로 로그인 상태가 유지됩니다(세션 범위).
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
