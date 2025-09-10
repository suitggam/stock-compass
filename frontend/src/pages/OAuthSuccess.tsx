import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, token } from '../api/client';

const OAuthSuccess: React.FC = () => {
  const nav = useNavigate();

  useEffect(() => {
    api
      .post<{ accessToken: string }>('/api/auth/refresh', {})
      .then((res) => {
        if (res?.accessToken) {
          token.set(res.accessToken);
          nav('/', { replace: true });
        } else {
          nav('/oauth/fail?reason=missing_access', { replace: true });
        }
      })
      .catch(() => nav('/oauth/fail?reason=refresh_failed', { replace: true }));
  }, [nav]);

  return <div className="min-h-screen grid place-items-center text-gray-600">로그인 처리중…</div>;
};

export default OAuthSuccess;
