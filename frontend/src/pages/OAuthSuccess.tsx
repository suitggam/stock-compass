import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router'; // ✅ dom 아님
import { api, token } from '../api/client';

const GUARD_KEY = 'oauth_refresh_guard_ts';

export default function OAuthSuccess() {
  const nav = useNavigate();
  const ran = useRef(false);

  useEffect(() => {
    if (ran.current) return;
    ran.current = true;

    token.clear();

    const now = Date.now();
    const last = Number(sessionStorage.getItem(GUARD_KEY) || '0');
    if (now - last < 8000) return;
    sessionStorage.setItem(GUARD_KEY, String(now));

    (async () => {
      try {
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
}
