import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import type { User } from '../types/user'; // 또는 '@/types/user'

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

export const useAuth = create<State & Actions>()(
  persist(
    (set, get) => ({
      accessToken: null,
      user: null,
      loading: true,

      setAccessToken: (t) => set({ accessToken: t }),
      setUser: (u) => set({ user: u }),

      bootstrap: async () => {
        try {
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
        }
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
        }
      },
    }),
    {
      name: 'auth-store',
      storage: createJSONStorage(() => sessionStorage),
    },
  ),
);
