import { useState, type ChangeEvent } from "react";
import { Link, useNavigate } from "react-router";
import logoImg from "../assets/logo.webp";

export default function PremiumNavyHeader() {
  const [search, setSearch] = useState<string>("");
  const navigate = useNavigate();

  const handleChange = (e: ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setSearch(value);
    console.log("검색어 : ", value);
  };

  const handleSearch = () => {
    if (search.trim()) {
      console.log("검색 실행 : ", search);
      navigate(`/search?query=${encodeURIComponent(search)}`);
    } else {
      alert("검색어를 입력해주세요");
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      handleSearch();
    }
  };

  return (
    <>
      <nav className="bg-slate-800 text-white flex justify-between items-center px-6 py-4">
        {/* 왼쪽: 메뉴들을 가로로 배치 */}
        <div className="flex items-center space-x-6">
          <Link to={"/"}>
            <img src={logoImg} alt="로고이미지" className="h-8 w-auto" />
          </Link>
          <Link
            to={"/game"}
            className="text-slate-300 hover:text-white font-medium transition-colors"
          >
            투자 성향 파악 게임
          </Link>
          <Link
            to={"/stock"}
            className="text-slate-300 hover:text-white font-medium transition-colors"
          >
            모의 투자
          </Link>
          <Link
            to={"/ranking"}
            className="text-slate-300 hover:text-white font-medium transition-colors"
          >
            모의 투자 랭킹
          </Link>
        </div>

        {/* 오른쪽: 검색창과 버튼들을 가로로 배치 */}
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
            className="px-6 py-3 bg-gradient-to-r from-amber-500 to-amber-600 text-slate-900 rounded-xl hover:from-amber-600 hover:to-amber-700 transition-all font-semibold shadow-lg"
          >
            검색
          </button>
          <Link to={"/login"}>
            <button className="px-6 py-3 border border-slate-600 text-slate-300 rounded-xl hover:border-slate-500 hover:text-white transition-colors">
              로그인
            </button>
          </Link>
        </div>
      </nav>
    </>
  );
}
