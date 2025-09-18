// src/components/Layout.tsx
import { Outlet } from 'react-router'; // (RR v7) v6이면 'react-router-dom'
import { useEffect } from 'react';
import { useAuth } from '../stores/auth';
import Footer from './Footer';
import Header from './Header';

function Layout() {
  const bootstrap = useAuth((s) => s.bootstrap);

  // ① 앱 마운트 시 무조건 1회
  useEffect(() => {
    void bootstrap();
  }, [bootstrap]);

  // ② 포커스/가시성 복귀 시 재동기화 (OAuth 복귀 즉시 반영)
  useEffect(() => {
    const run = () => {
      void bootstrap();
    };
    const onVis = () => {
      if (document.visibilityState === 'visible') run();
    };
    window.addEventListener('focus', run);
    document.addEventListener('visibilitychange', onVis);
    return () => {
      window.removeEventListener('focus', run);
      document.removeEventListener('visibilitychange', onVis);
    };
  }, [bootstrap]);

  return (
    <div className="min-h-screen bg-gradient-to-br bg-slate-800">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <Header />
        <div className="pt-5 pb-5">
          <Outlet />
        </div>
        <Footer />
      </div>
    </div>
  );
}

export default Layout;
