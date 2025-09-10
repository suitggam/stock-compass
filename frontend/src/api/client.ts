// API 래퍼 + 토큰 저장소
const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export const token = {
  get: () => localStorage.getItem("access_token"),
  set: (t: string) => localStorage.setItem("access_token", t),
  clear: () => localStorage.removeItem("access_token"),
};

async function handle<T>(res: Response): Promise<T> {
  if (!res.ok) {
    // 에러 메시지 디버깅용으로 콘솔에 노출
    const msg = await res.text().catch(() => "");
    console.error(`[API ${res.status}] ${res.url} -> ${msg}`);
    throw new Error(msg || String(res.status));
  }
  return (await res.json()) as T;
}

// 👉 path가 /api/auth/refresh일 땐 Authorization 헤더를 아예 제거
function buildHeaders(path: string): HeadersInit {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
  };
  const isRefresh = path.startsWith("/api/auth/refresh");

  const t = token.get();
  if (t && !isRefresh) {
    headers.Authorization = `Bearer ${t}`;
  }
  // 중요한 포인트: 토큰이 없으면 아예 Authorization 키를 추가하지 말 것
  return headers;
}

export const api = {
  get: <T>(path: string) =>
    fetch(`${API_BASE}${path}`, {
      headers: buildHeaders(path),
      credentials: "include",
    }).then(handle<T>),

  post: <T>(path: string, body?: unknown) =>
    fetch(`${API_BASE}${path}`, {
      method: "POST",
      headers: buildHeaders(path),
      credentials: "include",
      body: body ? JSON.stringify(body) : undefined,
    }).then(handle<T>),
};

export { API_BASE };
