import { createBrowserRouter } from 'react-router-dom';
import { Suspense, lazy } from 'react';
import Layout from '../common/Layout';
import LoadingPage from '../common/LoadingPage';

const Main = lazy(() => import('../pages/MainPage'));
const Test = lazy(() => import('../pages/TestPage'));
const OAuthSuccess = lazy(() => import('../pages/OAuthSuccess'));
const MyPage = lazy(() => import('../pages/MyPage'));
const OAuthFail = lazy(() => import('../pages/OAuthFail')); // ★ 추가

const Router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />, // ✅
    children: [
      {
        index: true,
        element: (
          <Suspense fallback={<LoadingPage />}>
            <Main />
          </Suspense>
        ),
      },
      {
        path: 'test',
        element: (
          <Suspense fallback={<LoadingPage />}>
            <Test />
          </Suspense>
        ),
      },
      {
        path: 'oauth/success',
        element: (
          <Suspense fallback={<LoadingPage />}>
            <OAuthSuccess />
          </Suspense>
        ),
      },
      {
        path: 'oauth/fail', // ★ 추가
        element: (
          <Suspense fallback={<LoadingPage />}>
            <OAuthFail />
          </Suspense>
        ),
      },
      {
        path: 'mypage',
        element: (
          <Suspense fallback={<LoadingPage />}>
            <MyPage />
          </Suspense>
        ),
      },
    ],
  },
]);

export default Router;
