import { Outlet } from 'react-router';
import Footer from './Footer';
import Header from './Header';

function Layout() {
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
