type Season = {
  title: string;
  range: string;
  change: number;
  rate: number;
  total: number;
};

const DUMMY: Season[] = [
  {
    title: "시즌 11 • 2025년 9월 1주차",
    range: "2025.09.01 ~ 진행중",
    change: 44510,
    rate: 6.34,
    total: 10263456,
  },
  {
    title: "시즌 10 • 2025년 8월 4주차",
    range: "2025.08.25 ~ 2025.08.29",
    change: 12100,
    rate: 1.34,
    total: 10218946,
  },
  {
    title: "시즌 9 • 2025년 8월 3주차",
    range: "2025.08.18 ~ 2025.08.22",
    change: -15078,
    rate: -4.2,
    total: 10206846,
  },
  {
    title: "시즌 8 • 2025년 8월 2주차",
    range: "2025.08.11 ~ 2025.08.15",
    change: 23825,
    rate: 3.54,
    total: 10221924,
  },
  {
    title: "시즌 7 • 2025년 8월 1주차",
    range: "2025.08.04 ~ 2025.08.08",
    change: 86087,
    rate: 11.18,
    total: 10198099,
  },
];

export default function MockInvestmentHistory() {
  return (
    <section className="w-full bg-white/80 backdrop-blur-xl rounded-2xl shadow-[0_8px_40px_rgba(0,0,0,0.16)] p-6 border border-black/5 relative">
      <div className="absolute inset-x-0 top-0 h-1 rounded-t-2xl bg-gradient-to-r from-black via-neutral-800 to-amber-500" />
      <div className="flex items-center gap-2 mb-5">
        <div className="text-xs font-bold text-white/80">🎮</div>
        <h3 className="text-xl sm:text-2xl font-extrabold text-neutral-900">
          모의 투자 히스토리
        </h3>
      </div>

      {/* ✅ KPI: 모바일 2열 → md:4열 */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
        <div className="rounded-2xl p-5 text-center bg-amber-400/10 border border-amber-500/20">
          <div className="text-amber-600 text-xl sm:text-2xl font-bold">
            {DUMMY.length}
          </div>
          <div className="text-neutral-500 text-xs">총 게임</div>
        </div>
        <div className="rounded-2xl p-5 text-center bg-amber-400/10 border border-amber-500/20">
          <div className="text-amber-600 text-xl sm:text-2xl font-bold">
            {(10263456).toLocaleString()}원
          </div>
          <div className="text-neutral-500 text-xs">보유 자산</div>
        </div>
        <div className="rounded-2xl p-5 text-center bg-amber-400/10 border border-amber-500/20">
          <div className="text-amber-600 text-xl sm:text-2xl font-bold">
            2.63%
          </div>
          <div className="text-neutral-500 text-xs">총 수익률</div>
        </div>
        <div className="rounded-2xl p-5 text-center bg-amber-400/10 border border-amber-500/20">
          <div className="text-amber-600 text-xl sm:text-2xl font-bold">
            69등
          </div>
          <div className="text-neutral-500 text-xs">현재 자산 등수</div>
        </div>
      </div>

      {/* ✅ 리스트: 모바일에서 카드가 단 줄에 꽉 차고, 내용이 줄바꿈되도록 */}
      <div className="flex flex-col gap-3.5">
        {DUMMY.map((s, i) => {
          const up = s.change >= 0;
          return (
            <div
              key={i}
              className="p-5 rounded-2xl flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 bg-neutral-50/70 border border-neutral-200/70"
            >
              <div className="min-w-0">
                <div className="text-neutral-900 text-[15px] font-semibold truncate">
                  🎮 {s.title}
                </div>
                <div className="text-neutral-500 text-sm">{s.range}</div>
              </div>
              <div className="flex items-center gap-3 sm:gap-6">
                <div
                  className={`${
                    up
                      ? "bg-emerald-100 text-emerald-700"
                      : "bg-rose-100 text-rose-600"
                  } px-4 py-2 rounded-[10px] text-sm font-bold text-center`}
                >
                  {up ? "+" : ""}
                  {s.change.toLocaleString()} ({up ? "+" : ""}
                  {s.rate}%)
                </div>
                <div className="text-right text-neutral-900 text-sm font-bold whitespace-nowrap">
                  {s.total.toLocaleString()}원
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* 페이징 더미 */}
      <div className="mt-6 flex items-center justify-center gap-2">
        <button
          className="w-8 h-8 opacity-50 bg-neutral-300 rounded"
          aria-label="prev"
        />
        <button className="w-8 h-8 bg-white rounded outline outline-1 outline-amber-500 text-amber-600 font-bold">
          1
        </button>
        <button className="w-8 h-8 bg-white rounded outline outline-1 outline-neutral-200 font-bold">
          2
        </button>
        <button className="w-8 h-8 bg-white rounded outline outline-1 outline-neutral-200 font-bold">
          ...
        </button>
        <button className="w-8 h-8 bg-white rounded outline outline-1 outline-neutral-200 font-bold">
          9
        </button>
        <button className="w-8 h-8 bg-white rounded outline outline-1 outline-neutral-200 font-bold">
          10
        </button>
        <button
          className="w-8 h-8 bg-white rounded outline outline-1 outline-neutral-200"
          aria-label="next"
        />
      </div>
    </section>
  );
}
