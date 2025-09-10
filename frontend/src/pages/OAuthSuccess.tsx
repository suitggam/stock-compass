import React, { useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { api, token } from "../api/client";

const GUARD_KEY = "oauth_refresh_guard_ts";

const OAuthSuccess: React.FC = () => {
  const nav = useNavigate();
  const ran = useRef(false);

  useEffect(() => {
    if (ran.current) return;
    ran.current = true;

    // ⬇️ 이전에 남아있던 만료 access 헤더가 refresh를 방해하지 않도록 먼저 정리
    token.clear();

    // React 18 StrictMode 재마운트 대비(짧은 TTL)
    const now = Date.now();
    const last = Number(sessionStorage.getItem(GUARD_KEY) || "0");
    if (now - last < 8000) return;
    sessionStorage.setItem(GUARD_KEY, String(now));

    (async () => {
      try {
        const res = await api.post<{ accessToken: string }>(
          "/api/auth/refresh",
          {}
        );
        if (res?.accessToken) {
          token.set(res.accessToken);
          nav("/", { replace: true });
        } else {
          nav("/oauth/fail?reason=missing_access", { replace: true });
        }
      } catch {
        nav("/oauth/fail?reason=refresh_failed", { replace: true });
      }
    })();
  }, [nav]);

  return (
    <div className="min-h-screen grid place-items-center text-gray-600">
      로그인 처리중…
    </div>
  );
};

export default OAuthSuccess;
