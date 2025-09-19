type Item = {
  name: string;
  code: string;
  price: number;
  diff: number;
  rate: number;
};

const DUMMY: Item[] = [
  { name: "삼성전자", code: "005930", price: 71500, diff: 1200, rate: 1.71 },
  { name: "SK하이닉스", code: "000660", price: 89300, diff: -800, rate: -0.89 },
  { name: "네이버", code: "035420", price: 195000, diff: 3500, rate: 1.83 },
  { name: "카카오", code: "035720", price: 48250, diff: 950, rate: 2.01 },
  {
    name: "LG에너지솔루션",
    code: "373220",
    price: 412000,
    diff: -5000,
    rate: -1.2,
  },
  { name: "현대차", code: "005380", price: 189500, diff: 2500, rate: 1.34 },
];

function Card({ item }: { item: Item }) {
  const up = item.diff >= 0;
  return (
    <div className="p-5 rounded-2xl bg-white/85 border border-black/5 shadow-[0_8px_30px_rgba(0,0,0,0.12)] relative">
      <div className="absolute left-0 top-0 w-full h-1 bg-gradient-to-r from-black to-amber-500 rounded-t-2xl" />
      <div className="flex justify-between gap-4">
        <div>
          <div className="text-neutral-900 text-base sm:text-lg font-bold">
            {item.name}
          </div>
          <div className="text-neutral-500 text-xs">{item.code}</div>
        </div>
        <div className="text-right">
          <div className="text-neutral-900 text-base sm:text-lg font-bold">
            {item.price.toLocaleString()}원
          </div>
          <div
            className={`${
              up
                ? "bg-emerald-100 text-emerald-700"
                : "bg-rose-100 text-rose-600"
            } mt-1 px-2 py-1 rounded-md text-xs sm:text-sm`}
          >
            {up ? "+" : ""}
            {item.diff.toLocaleString()} ({up ? "+" : ""}
            {item.rate}%)
          </div>
        </div>
      </div>
      <div className="grid grid-cols-2 gap-2 mt-3">
        <button className="px-3 py-2 rounded-lg text-xs text-neutral-900 border border-neutral-200 hover:bg-neutral-50">
          📈 차트보기
        </button>
        <button className="px-3 py-2 rounded-lg text-xs text-white bg-neutral-900 hover:bg-black">
          ❌ 삭제
        </button>
      </div>
    </div>
  );
}

export default function Watchlist() {
  return (
    <section className="w-full bg-white/80 backdrop-blur-xl rounded-2xl shadow-[0_8px_40px_rgba(0,0,0,0.16)] p-6 border border-black/5 relative">
      <div className="absolute inset-x-0 top-0 h-1 rounded-t-2xl bg-gradient-to-r from-amber-500 to-black" />
      <div className="flex items-center gap-2 mb-5">
        <div className="text-sm font-bold text-white/80">⭐</div>
        <h3 className="text-xl sm:text-2xl font-extrabold text-neutral-900">
          관심 종목
        </h3>
      </div>

      {/* ✅ 반응형: 1열 → sm:2열 → lg:3열 → xl:4열 */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        {DUMMY.map((item) => (
          <Card key={item.code} item={item} />
        ))}
      </div>
    </section>
  );
}
