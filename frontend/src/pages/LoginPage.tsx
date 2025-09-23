import * as React from 'react';
import { Navigate } from 'react-router';
import { useAuth } from '../stores/auth';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export default function LoginPage() {
  const { user, loading, bootstrap } = useAuth();

  // 새로고침/첫 진입 시 refresh → me 시도
  React.useEffect(() => {
    void bootstrap();
  }, [bootstrap]);

  // 이미 로그인된 경우 메인으로 이동
  if (!loading && user) {
    return <Navigate to="/" replace />;
  }

  const loginGoogle = () => {
    window.location.href = `${API_BASE}/api/users/auth/google`;
  };

  const loginKakao = () => {
    window.location.href = `${API_BASE}/api/users/auth/kakao`;
  };

  return (
    <div className="min-h-dvh flex items-center justify-center bg-gray-50">
      <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-sm">
        <h1 className="mb-4 text-2xl font-semibold">로그인</h1>

        <div className="space-y-3">
          <button
            onClick={loginGoogle}
            disabled={loading}
            className="w-full rounded-lg bg-black px-4 py-3 text-white hover:opacity-90 disabled:opacity-60"
          >
            Google로 로그인
          </button>

          <button
            onClick={loginKakao}
            disabled={loading}
            className="w-full rounded-lg bg-yellow-300 px-4 py-3 hover:brightness-95 disabled:opacity-60"
          >
            Kakao로 로그인
          </button>

          <p className="pt-2 text-center text-xs text-gray-500">
            로그인 후 새로고침해도 자동으로 로그인 상태가 유지됩니다(세션 범위).
          </p>
        </div>
      </div>
    </div>
  );
}
