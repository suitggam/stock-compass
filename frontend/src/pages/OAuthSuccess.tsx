import React, { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, token } from '../api/client';

const GUARD_KEY = 'oauth_refresh_guard_ts';

const OAuthSuccess: React.FC = () => {
  const nav = useNavigate();
  const ran = useRef(false);

  useEffect(() => {
    if (ran.current) return;
    ran.current = true;

    // 직전 access 제거(Authorization 헤더 안 붙도록)
    token.clear();

    // 새로고침 루프 가드 (8초 내 중복호출 방지)
    const now = Date.now();
    const last = Number(sessionStorage.getItem(GUARD_KEY) || '0');
    if (now - last < 8000) return;
    sessionStorage.setItem(GUARD_KEY, String(now));

    (async () => {
      try {
        // ★ 서버가 리프레시 쿠키를 읽어 accessToken을 내려줌
        const res = await api.post<{ accessToken: string }>('/api/auth/refresh', {});
        if (res?.accessToken) {
          token.set(res.accessToken);
          nav('/', { replace: true });
        } else {
          nav('/oauth/fail?reason=refresh_failed', { replace: true });
        }
      } catch {
        nav('/oauth/fail?reason=refresh_failed', { replace: true });
      }
    })();
  }, [nav]);

  return <div className="min-h-screen grid place-items-center text-gray-600">로그인 처리중…</div>;
};

export default OAuthSuccess;
