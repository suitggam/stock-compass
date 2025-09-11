// api/client.ts
// API 래퍼 + 토큰 저장소 (localStorage 유지, 동시 401 처리, 자동 갱신)
export const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

/** 메모리 캐시 + localStorage 동기화 */
const mem = { access: null as string | null };

export const token = {
  get: () => (mem.access ??= localStorage.getItem('access_token')),
  set: (t: string) => {
    mem.access = t;
    localStorage.setItem('access_token', t);
  },
  clear: () => {
    mem.access = null;
    localStorage.removeItem('access_token');
  },
};

async function handle<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const msg = await res.text().catch(() => '');
    console.error(`[API ${res.status}] ${res.url} -> ${msg}`);
    throw new Error(msg || String(res.status));
  }
  // 204 등 본문 없는 경우 대비
  const text = await res.text();
  return text ? (JSON.parse(text) as T) : (undefined as unknown as T);
}

/** /api/auth/refresh 호출 시 Authorization 헤더 금지 */
function buildHeaders(path: string): HeadersInit {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  const isRefresh = path.startsWith('/api/auth/refresh');
  const t = token.get();
  if (t && !isRefresh) headers.Authorization = `Bearer ${t}`;
  return headers;
}

/** 동시 401 폭주 방지: 한 번만 refresh 날리고 모두가 기다리게 함 */
let inflightRefresh: Promise<boolean> | null = null;

async function refreshOnce(): Promise<boolean> {
  if (!inflightRefresh) {
    inflightRefresh = (async () => {
      try {
        const res = await fetch(`${API_BASE}/api/auth/refresh`, {
          method: 'POST',
          credentials: 'include', // ★ RT 쿠키 동봉
          headers: { 'Content-Type': 'application/json' },
        });
        if (!res.ok) {
          token.clear();
          return false;
        }
        const data = (await res.json()) as { accessToken?: string };
        if (data?.accessToken) {
          token.set(data.accessToken);
          return true;
        }
        token.clear();
        return false;
      } catch {
        token.clear();
        return false;
      } finally {
        // 다음 갱신을 위해 해제
        setTimeout(() => (inflightRefresh = null), 0);
      }
    })();
  }
  return inflightRefresh;
}

async function request<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const url = `${API_BASE}${path}`;
  const res = await fetch(url, {
    credentials: 'include', // 모든 요청 쿠키 포함(특히 refresh용)
    ...init,
    headers: { ...buildHeaders(path), ...(init.headers || {}) },
  });

  // 401이면 한 번만 /refresh 시도 후 재시도
  if (res.status === 401 && retry && !path.startsWith('/api/auth/refresh')) {
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

export const api = {
  get: <T>(path: string) => request<T>(path, { method: 'GET' }),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, {
      method: 'POST',
      body: body != null ? JSON.stringify(body) : undefined,
    }),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, {
      method: 'PUT',
      body: body != null ? JSON.stringify(body) : undefined,
    }),
  patch: <T>(path: string, body?: unknown) =>
    request<T>(path, {
      method: 'PATCH',
      body: body != null ? JSON.stringify(body) : undefined,
    }),
  del: <T>(path: string) => request<T>(path, { method: 'DELETE' }),
  /** 서버 로그아웃 호출 + 로컬 토큰 파기 */
  logout: async () => {
    try {
      await fetch(`${API_BASE}/api/auth/logout`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
      });
    } finally {
      token.clear();
    }
  },
};
