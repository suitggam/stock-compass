import { useEffect, useState } from "react";
import { API_BASE, api, token } from "../api/client";
import { useNavigate } from "react-router"; // ✅ dom 아님
import NicknameDialog from "../components/NicknameDialog";

type User = {
  userNo: number;
  nickname: string;
  socialEmail: string;
  createdAt: string;
  totalReward: number;
  cash: number;
};

export default function LoginPage() {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [openNickDialog, setOpenNickDialog] = useState(false);
  const nav = useNavigate();

  useEffect(() => {
    (async () => {
      try {
        if (!token.get()) {
          try {
            const res = await api.post<{ accessToken: string }>(
              "/api/auth/refresh",
              {}
            );
            if (res?.accessToken) token.set(res.accessToken);
          } catch (e) {
            console.debug("[refresh] skip:", e);
          }
        }
        if (token.get()) {
          const me = await api.get<User>("/api/users/me");
          setUser(me);
          if (!me.nickname || /^user(_|\d|$)/i.test(me.nickname))
            setOpenNickDialog(true);
        } else {
          setUser(null);
        }
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const goGoogle = () =>
    (window.location.href = `${API_BASE}/users/auth/google`);
  const goKakao = () => (window.location.href = `${API_BASE}/users/auth/kakao`);

  const logout = async () => {
    try {
      await api.post<void>("/api/auth/logout", {});
    } catch (e) {
      console.debug("[logout] ignored:", e);
    } finally {
      token.clear();
      setUser(null);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        불러오는 중…
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col items-center justify-center gap-6">
      <h1 className="text-3xl font-bold">메인페이지</h1>

      {!user ? (
        <div className="flex gap-3">
          <button
            onClick={goGoogle}
            className="px-4 py-2 rounded bg-blue-600 text-white"
          >
            구글 로그인
          </button>
          <button
            onClick={goKakao}
            className="px-4 py-2 rounded bg-yellow-400 text-black"
          >
            카카오 로그인
          </button>
        </div>
      ) : (
        <div className="flex flex-col items-center gap-4">
          <div>
            안녕하세요, <b>{user.nickname}</b> 님!
          </div>
          <div className="flex gap-3">
            <button
              onClick={() => nav("/mypage")}
              className="px-4 py-2 rounded bg-gray-200"
            >
              마이페이지
            </button>
            <button
              onClick={() => setOpenNickDialog(true)}
              className="px-4 py-2 rounded bg-emerald-200"
            >
              닉네임 변경
            </button>
            <button onClick={logout} className="px-4 py-2 rounded bg-rose-200">
              로그아웃
            </button>
          </div>
        </div>
      )}

      <NicknameDialog
        open={openNickDialog}
        initialNickname={user?.nickname}
        onClose={() => setOpenNickDialog(false)}
        onSaved={(newNick) =>
          setUser((u) => (u ? { ...u, nickname: newNick } : u))
        }
      />
    </div>
  );
}
