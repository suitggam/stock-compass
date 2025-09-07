import { Outlet } from "react-router-dom";

function Layout() {
  return (
    <>
      <header>헤더입니다.</header>
      <Outlet />
      <footer>푸터입니다.</footer>
    </>
  );
}

export default Layout;
