import { Outlet } from "react-router";
import Header from "./header";
import Footer from "./Footer";

function Layout() {
  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <Header />
      <Outlet />
      <Footer />
    </div>
  );
}

export default Layout;
