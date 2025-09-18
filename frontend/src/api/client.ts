// src/api/client.ts
// API 래퍼 (동시 401 처리, 자동 갱신) — 토큰은 Zustand(useAuth)에서만 관리

import { useAuth } from '@/stores/auth';

export const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

// 서버 엔드포인트
const REFRESH_PATH = '/users/auth/refresh';
const LOGOUT_PATH = '/users/logout';

// ---- 공통 응답 처리 ----
async function handle<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const msg = await res.text().catch(() => '');
    console.error(`[API ${res.status}] ${res.url} -> ${msg}`);
    throw new Error(msg || String(res.status));
  }
  const text = await res.text(); // 204 대비
  return text ? (JSON.parse(text) as T) : (undefined as unknown as T);
}

// ---- 헤더 빌드 (refresh 호출에는 Authorization 금지) ----
function buildHeaders(path: string): HeadersInit {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  const isRefresh = path.startsWith(REFRESH_PATH);
  const t = useAuth.getState().accessToken; // Zustand에서 토큰 읽기
  if (t && !isRefresh) headers.Authorization = `Bearer ${t}`;
  return headers;
}

// ---- 동시 401 폭주 방지: 리프레시 1회만 수행 ----
let inflightRefresh: Promise<boolean> | null = null;

async function refreshOnce(): Promise<boolean> {
  if (!inflightRefresh) {
    inflightRefresh = (async () => {
      try {
        const res = await fetch(`${API_BASE}${REFRESH_PATH}`, {
          method: 'POST',
          credentials: 'include', // ★ refresh 쿠키 동봉
          headers: { 'Content-Type': 'application/json' },
        });
        if (!res.ok) {
          useAuth.getState().setAccessToken(null);
          useAuth.getState().setUser(null);
          return false;
        }
        const data = (await res.json()) as { accessToken?: string };
        if (data?.accessToken) {
          useAuth.getState().setAccessToken(data.accessToken);
          return true;
        }
        useAuth.getState().setAccessToken(null);
        useAuth.getState().setUser(null);
        return false;
      } catch {
        useAuth.getState().setAccessToken(null);
        useAuth.getState().setUser(null);
        return false;
      } finally {
        // 다음 갱신을 위해 해제
        setTimeout(() => (inflightRefresh = null), 0);
      }
    })();
  }
  return inflightRefresh;
}

// ---- 요청 래퍼 (401 시 1회 자동 리프레시 후 재시도) ----
async function request<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const url = `${API_BASE}${path}`;
  const res = await fetch(url, {
    credentials: 'include', // 모든 요청에 쿠키 포함(특히 refresh용)
    ...init,
    headers: { ...buildHeaders(path), ...(init.headers || {}) },
  });

  if (res.status === 401 && retry && !path.startsWith(REFRESH_PATH)) {
    const ok = await refreshOnce();
    if (ok) {
      const res2 = await fetch(url, {
        credentials: 'include',
        ...init,
        headers: { ...buildHeaders(path), ...(init.headers || {}) },
      });
      return handle<T>(res2);
    }
  }
  return handle<T>(res);
}

// ---- 공개 API ----
export const api = {
  get: <T>(path: string) => request<T>(path, { method: 'GET' }),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'POST', body: body != null ? JSON.stringify(body) : undefined }),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PUT', body: body != null ? JSON.stringify(body) : undefined }),
  patch: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: 'PATCH', body: body != null ? JSON.stringify(body) : undefined }),
  del: <T>(path: string) => request<T>(path, { method: 'DELETE' }),

  /** 서버 로그아웃 호출 + 스토어 정리(useAuth.logout 사용 권장) */
  logout: async () => {
    try {
      await fetch(`${API_BASE}${LOGOUT_PATH}`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
      });
    } finally {
      useAuth.getState().setAccessToken(null);
      useAuth.getState().setUser(null);
    }
  },
};
