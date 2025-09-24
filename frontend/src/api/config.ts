export type AppRuntimeConfig = {
  API_BASE_URL?: string;
};

// 전역 window에 타입 선언
declare global {
  interface Window {
    __APP_CONFIG__?: AppRuntimeConfig;
  }
}

export const API_BASE: string =
  (typeof window !== 'undefined' ? window.__APP_CONFIG__?.API_BASE_URL : undefined) ??
  import.meta.env.VITE_API_BASE_URL ??
  '/api';
