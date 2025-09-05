import { lazy, Suspense } from "react";
import { createBrowserRouter } from "react-router";
import Layout from "../common/Layout";
import LoadingPage from "../common/LoadingPage";

const Loading = lazy(() => import("../common/LoadingPage"));
const Main = lazy(() => import("../pages/MainPage"));
const Test = lazy(() => import("../pages/TestPage"));

const Router = createBrowserRouter([
  {
    path: "/",
    Component: Layout,
    children: [
      {
        index: true,
        element: (
          <Suspense fallback={<Loading />}>
            <Main />
          </Suspense>
        ),
      },
      {
        path: "/test",
        element: (
          <Suspense fallback={<LoadingPage />}>
            <Test />
          </Suspense>
        ),
      },
    ],
  },
]);

export default Router;
