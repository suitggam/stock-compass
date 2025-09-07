import React, { useEffect } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { token } from "../api/client"; // token.set/get 를 사용하는 유틸

const OAuthSuccess: React.FC = () => {
  const [qp] = useSearchParams();
  const nav = useNavigate();

  useEffect(() => {
    const access = qp.get("access");

    if (access && access.length > 0) {
      // 1) access 토큰 저장(localStorage 등)
      token.set(access);

      // 2) 쿼리스트링 제거하며 마이페이지로
      nav("/mypage", { replace: true });
    } else {
      // access가 없으면 홈으로
      nav("/", { replace: true });
    }
  }, [qp, nav]);

  return (
    <div className="min-h-screen grid place-items-center text-gray-600">
      로그인 처리중…
    </div>
  );
};

export default OAuthSuccess;
