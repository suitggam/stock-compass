// src/pages/MyPage.tsx
import * as React from 'react';
import { Link, Navigate } from 'react-router';
import { useAuth } from '@/stores/auth';
import useAuthGuard from '@/hooks/useAuthGuard';

export default function MyPage() {
  useAuthGuard('/');

  const { user, loading, bootstrap, logout } = useAuth();

  React.useEffect(() => {
    void bootstrap();
  }, [bootstrap]);

  if (loading) {
    return <div className="min-h-dvh grid place-items-center text-gray-600">불러오는 중…</div>;
  }

  if (!user) {
    return <Navigate to="/" replace />;
  }

  return (
    <div className="min-h-dvh bg-gray-50 py-10">
      <div className="mx-auto w-full max-w-3xl space-y-6 px-4">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-semibold">마이페이지</h1>

          <div className="flex gap-2">
            <Link to="/" className="rounded-lg bg-gray-200 px-4 py-2 text-sm hover:bg-gray-300">
              홈으로
            </Link>
            <button
              onClick={() => logout()}
              className="rounded-lg bg-black px-4 py-2 text-sm text-white hover:opacity-90"
            >
              로그아웃
            </button>
          </div>
        </div>

        <section className="rounded-2xl bg-white p-5 shadow-sm">
          <h2 className="mb-4 text-lg font-medium">프로필</h2>
          <dl className="grid grid-cols-1 gap-y-3 sm:grid-cols-2">
            <div className="flex items-center justify-between rounded-lg bg-gray-50 p-3">
              <dt className="text-gray-600">닉네임</dt>
              <dd className="font-medium">{user.nickname}</dd>
            </div>
            <div className="flex items-center justify-between rounded-lg bg-gray-50 p-3">
              <dt className="text-gray-600">이메일</dt>
              <dd className="font-medium">{user.socialEmail}</dd>
            </div>
            <div className="flex items-center justify-between rounded-lg bg-gray-50 p-3">
              <dt className="text-gray-600">가입일</dt>
              <dd className="font-medium">{new Date(user.createdAt).toLocaleString()}</dd>
            </div>
            <div className="flex items-center justify-between rounded-lg bg-gray-50 p-3">
              <dt className="text-gray-600">보유 캐시</dt>
              <dd className="font-medium">{user.cash.toLocaleString()}원</dd>
            </div>
            <div className="flex items-center justify-between rounded-lg bg-gray-50 p-3">
              <dt className="text-gray-600">리워드 합계</dt>
              <dd className="font-medium">{user.totalReward.toLocaleString()}P</dd>
            </div>
          </dl>
        </section>
      </div>
    </div>
  );
}
