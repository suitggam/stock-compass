import React, { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { token } from '../api/client'; // token.set/get

type LoginPayload = {
  userNo?: number;
  socialEmail?: string;
  nickname?: string;
  accessToken?: string;
  refreshToken?: string;
};

/** Base64URL → JSON 파싱 시도 */
const tryBase64Url = (s: string): LoginPayload | null => {
  try {
    const pad = (4 - (s.length % 4)) % 4;
    const b64 = s.replace(/-/g, '+').replace(/_/g, '/') + '='.repeat(pad);
    const json = atob(b64);
    return JSON.parse(json) as LoginPayload;
  } catch {
    return null;
  }
};

/** URI-encoded JSON → 파싱 시도 */
const tryPlainJson = (s: string): LoginPayload | null => {
  try {
    return JSON.parse(decodeURIComponent(s)) as LoginPayload;
  } catch {
    return null;
  }
};

function decodePayload(raw: string | null): LoginPayload | null {
  if (!raw) return null;
  return tryBase64Url(raw) ?? tryPlainJson(raw);
}

const OAuthSuccess: React.FC = () => {
  const [qp] = useSearchParams();
  const nav = useNavigate();

  useEffect(() => {
    // 1) payload(JSON/Base64URL) 우선 파싱
    const parsed = decodePayload(qp.get('payload'));

    // 2) 토큰 추출 (fallback: ?access= & ?refresh=)
    const access: string | undefined = parsed?.accessToken ?? qp.get('access') ?? undefined;

    const refresh: string | undefined = parsed?.refreshToken ?? qp.get('refresh') ?? undefined;

    if (access && access.length > 0) {
      // access 저장
      token.set(access);

      // refresh/유저정보 보관(선택)
      if (refresh) localStorage.setItem('refreshToken', refresh);
      if (parsed?.nickname) localStorage.setItem('nickname', parsed.nickname);
      if (typeof parsed?.userNo === 'number') {
        localStorage.setItem('userNo', String(parsed.userNo));
      }

      // 마이페이지로 이동 (쿼리스트링 제거)
      nav('/mypage', { replace: true });
    } else {
      // access 없으면 실패 페이지로
      nav('/oauth/fail?reason=missing_access', { replace: true });
    }
  }, [qp, nav]);

  return <div className="min-h-screen grid place-items-center text-gray-600">로그인 처리중…</div>;
};

export default OAuthSuccess;
