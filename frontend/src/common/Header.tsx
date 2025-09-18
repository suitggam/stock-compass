import { useState, type ChangeEvent } from 'react';
import { Link, useNavigate } from 'react-router';
import { useAuth } from '../stores/auth';
import logoImg from '../assets/logo.webp';

function getInitials(name: string) {
  // 한글/영문 혼용도 무난하게 앞 2글자
  const trimmed = (name || '').trim();
  if (!trimmed) return '?';
  // 공백 분리 후 첫 글자 조합
  const parts = trimmed.split(/\s+/);
  if (parts.length >= 2) {
    return (parts[0][0] + parts[1][0]).toUpperCase();
  }
  return trimmed.slice(0, 2).toUpperCase();
}

export default function Header() {
  const [search, setSearch] = useState('');
  const navigate = useNavigate();
  const { user, logout } = useAuth(); // 🔸 로그인 상태/유저 정보/로그아웃

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setSearch(value);
    // console.log('검색어 : ', value);
  };

  const handleSearch = () => {
    if (search.trim()) {
      navigate(`/search?query=${encodeURIComponent(search)}`);
    } else {
      alert('검색어를 입력해주세요');
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') handleSearch();
  };

  const goLogin = () => navigate('/login');
  const goMyPage = () => navigate('/mypage');

  return (
    <nav className="bg-slate-800 text-white flex justify-between items-center px-6 py-4">
      {/* 왼쪽: 로고 + 메뉴 */}
      <div className="flex items-center space-x-6">
        <Link to="/">
          <img src={logoImg} alt="로고이미지" className="h-8 w-auto" />
        </Link>
        <Link to="/game" className="text-slate-300 hover:text-white font-bold transition-colors">
          투자 성향 파악 게임
        </Link>
        <Link to="/stock" className="text-slate-300 hover:text-white font-bold transition-colors">
          모의 투자
        </Link>
        <Link to="/ranking" className="text-slate-300 hover:text-white font-bold transition-colors">
          모의 투자 랭킹
        </Link>
      </div>

      {/* 오른쪽: 검색 + 유저영역 */}
      <div className="flex items-center space-x-4">
        <input
          className="w-72 px-4 py-3 bg-slate-700 border border-slate-600 rounded-xl text-white placeholder-slate-400 focus:ring-2 focus:ring-amber-500 focus:border-amber-500 transition-all"
          placeholder="기업명 또는 종목코드로 검색"
          value={search}
          onChange={handleChange}
          onKeyDown={handleKeyPress}
        />
        <button
          onClick={handleSearch}
          className="px-6 py-3 bg-gradient-to-r from-amber-500 to-amber-600 text-slate-900 rounded-xl hover:from-amber-600 hover:to-amber-700 transition-all font-bold shadow-lg"
        >
          검색
        </button>

        {/* 🔸 로그인 상태에 따라 분기 */}
        {!user ? (
          <button
            onClick={goLogin}
            className="px-6 py-3 border border-slate-600 text-slate-300 rounded-xl hover:border-slate-500 hover:text-white transition-colors font-bold"
          >
            로그인
          </button>
        ) : (
          <div className="flex items-center gap-3">
            {/* 아바타 */}
            {user.avatarUrl ? (
              <img
                onClick={goMyPage}
                src={user.avatarUrl}
                alt="프로필"
                className="h-10 w-10 rounded-full object-cover border border-slate-600"
                referrerPolicy="no-referrer" // 구글 이미지 등 CORS 이슈 완화
              />
            ) : (
              <div className="h-10 w-10 rounded-full bg-amber-500/90 text-slate-900 flex items-center justify-center font-extrabold">
                {getInitials(user.nickname)}
              </div>
            )}

            {/* 닉네임 */}
            <span className="hidden sm:inline text-slate-200 font-medium">{user.nickname}</span>

            {/* 로그아웃 */}
            <button
              onClick={() => logout()}
              className="px-4 py-2 text-sm rounded-lg border border-slate-600 hover:bg-slate-700/60 transition-colors"
            >
              로그아웃
            </button>
          </div>
        )}
      </div>
    </nav>
  );
}
