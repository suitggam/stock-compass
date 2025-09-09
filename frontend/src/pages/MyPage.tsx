import React, { useEffect, useState } from 'react';
import { api, token } from '../api/client';
import type { User } from '../types/user';
import { useNavigate } from 'react-router-dom';

const fmtCurrency = (n?: number | null) =>
  n === undefined || n === null ? '-' : new Intl.NumberFormat('ko-KR').format(n) + '원';
const fmtDate = (iso?: string | null) => (iso ? new Date(iso).toISOString().slice(0, 10) : '-');

const MyPage: React.FC = () => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const nav = useNavigate();

  useEffect(() => {
    if (!token.get()) {
      nav('/', { replace: true });
      return;
    }
    api
      .get<User>('/api/users/me')
      .then(setUser)
      .catch(() => nav('/', { replace: true }))
      .finally(() => setLoading(false));
  }, [nav]);

  const name = user?.nickname ?? '사용자';
  const joined = fmtDate(user?.createdAt);
  const cash = fmtCurrency(user?.cash ?? 0);
  const favCount = 0; // 추후 /favorite API 붙이면 교체
  const gameCount = (user?.top1 ?? 0) + (user?.top2 ?? 0) + (user?.top3 ?? 0) + (user?.topten ?? 0);

  if (loading) return <div className="min-h-screen grid place-items-center">불러오는 중…</div>;

  return (
    /* === 여기부터는 네가 준 JSX 틀을 그대로 유지하고 동적 값만 채움 === */
    <div className="w-[1920px] h-[2462px] min-h-[1200px] px-64 bg-gradient-to-br from-indigo-500 to-purple-800 inline-flex flex-col justify-start items-start">
      <div className="w-full h-[2264px] max-w-[1400px] relative">
        <div className="w-[1360px] p-7 left-[20px] top-[20px] absolute bg-white/95 rounded-[20px] shadow-[0px_8px_32px_0px_rgba(0,0,0,0.10)] backdrop-blur-[5px] inline-flex flex-col justify-start items-start gap-2.5">
          <div className="self-stretch pt-[5px] pb-1.5 flex flex-col justify-start items-start">
            <div className="justify-center text-indigo-500 text-4xl font-bold">👤 마이페이지</div>
          </div>
          <div className="self-stretch pt-px pb-[3px] flex flex-col justify-start items-start">
            <div className="self-stretch justify-center text-zinc-800 text-base">
              나만의 투자 프로필과 투자 여정을 관리해보세요
            </div>
          </div>
        </div>

        <div className="w-[1360px] h-[497px] left-[20px] top-[201px] absolute inline-flex justify-center items-start gap-7">
          <div className="flex-1 self-stretch relative bg-white/95 rounded-[20px] shadow-[0px_8px_32px_0px_rgba(0,0,0,0.10)] backdrop-blur-[5px]">
            <div className="w-28 h-28 left-[276px] top-[47.50px] absolute bg-gradient-to-br from-indigo-500 to-purple-800 rounded-[60px] shadow-[0px_8px_25px_0px_rgba(102,126,234,0.30)]" />
            <div className="w-[605px] h-12 left-[33px] top-[202px] absolute">
              <div className="left-[258px] top-[3px] absolute text-center justify-center text-zinc-800 text-3xl font-bold">
                {name}
              </div>
            </div>
            <div className="w-[605px] py-0.5 left-[30px] top-[258px] absolute inline-flex flex-col justify-start items-center gap-1">
              <div className="text-center justify-center text-stone-500 text-lg">
                • 가입일: {joined}
              </div>
            </div>
            <div className="w-[622px] left-[22px] top-[323px] absolute inline-flex justify-center items-start gap-5">
              <Dash title="관심 종목" value={favCount} />
              <Dash title="게임 횟수(랭킹 합)" value={gameCount} />
              <Dash title="나의 잔고" valueStr={cash} />
            </div>
            <div className="w-16 h-8 left-[581px] top-[27px] absolute justify-center text-stone-500 text-base">
              회원탈퇴
            </div>
          </div>

          {/* 오른쪽 카드(시각 요소는 그대로 유지) */}
          <div className="flex-1 self-stretch relative bg-white/95 rounded-[20px] shadow-[0px_8px_32px_0px_rgba(0,0,0,0.10)] backdrop-blur-[5px]">
            <div className="w-[605px] py-0.5 left-[30px] top-[30px] absolute inline-flex justify-start items-center gap-2.5">
              <div className="pb-px inline-flex flex-col justify-start items-start">
                <div className="justify-center text-zinc-800 text-xs font-bold">📊</div>
              </div>
              <div className="justify-center text-zinc-800 text-2xl font-bold">투자 성향 분석</div>
            </div>
            <div className="w-[605px] h-72 left-[30px] top-[87px] absolute inline-flex justify-center items-center">
              <div className="w-64 h-64 relative">
                <div className="w-0 h-48 left-[25px] top-[25px] absolute origin-top-left -rotate-90 outline outline-[50px] outline-offset-[-25px] outline-green-500" />
                <div className="w-0 h-48 left-[25px] top-[25px] absolute origin-top-left -rotate-90 outline outline-[50px] outline-offset-[-25px] outline-sky-500" />
                <div className="w-0 h-48 left-[25px] top-[25px] absolute origin-top-left -rotate-90 outline outline-[50px] outline-offset-[-25px] outline-amber-500" />
              </div>
            </div>
          </div>
        </div>

        {/* 아래의 긴 관심종목/히스토리 섹션은 네 코드 유지 (API 붙이면 값만 교체) */}
      </div>
    </div>
  );
};

const Dash: React.FC<{ title: string; value?: number; valueStr?: string }> = ({
  title,
  value,
  valueStr,
}) => (
  <div className="flex-1 self-stretch p-5 bg-indigo-500/5 rounded-2xl inline-flex flex-col justify-start items-center gap-px">
    <div className="self-stretch pb-px flex flex-col justify-start items-center">
      <div className="self-stretch text-center justify-center text-indigo-500 text-3xl font-bold">
        {valueStr ?? value ?? 0}
      </div>
    </div>
    <div className="inline-flex justify-center items-start">
      <div className="text-center justify-center text-stone-500 text-sm">{title}</div>
    </div>
  </div>
);

export default MyPage;
