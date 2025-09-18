import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import type { User } from '../types/user';

type State = {
  accessToken: string | null;
  user: User | null;
  loading: boolean;
};

type Actions = {
  setAccessToken: (t: string | null) => void;
  setUser: (u: User | null) => void;
  bootstrap: () => Promise<void>;
  logout: () => Promise<void>;
};

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

// 중복 호출/1회 보장 플래그(타입 노출 없음)
let __authReady = false;
let __authInflight: Promise<void> | null = null;

export const useAuth = create<State & Actions>()(
  persist(
    (set, get) => ({
      accessToken: null,
      user: null,
      loading: true,

      setAccessToken: (t) => set({ accessToken: t }),
      setUser: (u) => set({ user: u }),

      // 앱 진입/리다이렉트 복귀 시 1회 실행 (내부에서 중복 방지)
      bootstrap: async () => {
        if (__authReady) return;
        if (__authInflight) return __authInflight;

        const job = (async () => {
          set({ loading: true });
          try {
            // 1) 토큰 없으면 refresh 시도
            if (!get().accessToken) {
              const res = await fetch(`${API_BASE}/users/auth/refresh`, {
                method: 'POST',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
              });
              if (res.ok) {
                const data = (await res.json()) as { accessToken?: string };
                if (data?.accessToken) set({ accessToken: data.accessToken });
              } else {
                set({ accessToken: null, user: null });
              }
            }

            // 2) 토큰이 있으면 me 조회
            if (get().accessToken) {
              const meRes = await fetch(`${API_BASE}/users/login-user`, {
                method: 'GET',
                credentials: 'include',
                headers: {
                  'Content-Type': 'application/json',
                  Authorization: `Bearer ${get().accessToken}`,
                },
              });
              if (meRes.ok) {
                const me = (await meRes.json()) as User;
                set({ user: me });
              } else {
                set({ accessToken: null, user: null });
              }
            }
          } finally {
            set({ loading: false });
            __authReady = true;
            __authInflight = null;
          }
        })();

        __authInflight = job;
        return job;
      },

      logout: async () => {
        try {
          await fetch(`${API_BASE}/users/logout`, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
          });
        } finally {
          set({ accessToken: null, user: null });
          __authReady = false;
        }
      },
    }),
    {
      name: 'auth-store',
      storage: createJSONStorage(() => sessionStorage),
    },
  ),
);
