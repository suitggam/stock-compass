// API 래퍼 + 토큰 저장소
const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export const token = {
  get: () => localStorage.getItem('access_token'),
  set: (t: string) => localStorage.setItem('access_token', t),
  clear: () => localStorage.removeItem('access_token'),
};

async function handle<T>(res: Response): Promise<T> {
  if (!res.ok) throw new Error(await res.text());
  return (await res.json()) as T;
}

export const api = {
  get: <T>(path: string) =>
    fetch(`${API_BASE}${path}`, {
      headers: { Authorization: token.get() ? `Bearer ${token.get()}` : '' },
      credentials: 'include',
    }).then(handle<T>),

  post: <T>(path: string, body?: unknown) =>
    fetch(`${API_BASE}${path}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: token.get() ? `Bearer ${token.get()}` : '',
      },
      credentials: 'include',
      body: body ? JSON.stringify(body) : undefined,
    }).then(handle<T>),
};

export { API_BASE };
