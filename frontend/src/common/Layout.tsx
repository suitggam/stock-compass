import { Outlet } from 'react-router';
import { useEffect } from 'react';
import { useAuth } from '../stores/auth';
import Footer from './Footer';
import Header from './Header';

function Layout() {
  const { loading, bootstrap } = useAuth();

  // 앱 진입/리다이렉트 직후 1회: refresh → me
  useEffect(() => {
    if (loading) void bootstrap();
  }, [loading, bootstrap]);

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
