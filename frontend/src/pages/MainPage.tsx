import { API_BASE } from "../api/client";

function MainPage() {
  const goGoogle = () =>
    (window.location.href = `${API_BASE}/users/auth/google`);
  const goKakao = () => (window.location.href = `${API_BASE}/users/auth/kakao`);

  return (
    <div className="min-h-screen flex flex-col items-center justify-center gap-6 bg-gradient-to-br from-indigo-500 to-purple-800">
      <h1 className="text-white text-3xl font-bold">메인페이지입니다.</h1>
      <div className="flex gap-3">
        <button
          onClick={goGoogle}
          className="px-5 py-3 rounded-xl bg-white text-gray-800 font-semibold shadow"
        >
          🔵 구글로 로그인
        </button>
        <button
          onClick={goKakao}
          className="px-5 py-3 rounded-xl bg-yellow-300 text-black font-semibold shadow"
        >
          🟡 카카오로 로그인
        </button>
      </div>
    </div>
  );
}
export default MainPage;
