// src/components/mypage/Watchlist.tsx
type FavoriteItem = { itemId: number; name: string };

export default function Watchlist({ items = [] }: { items?: FavoriteItem[] }) {
  const empty = !items || items.length === 0;

  return (
    <section className="w-full bg-white/80 backdrop-blur-xl rounded-2xl shadow-[0_8px_40px_rgba(0,0,0,0.16)] p-6 border border-black/5 relative">
      <div className="absolute inset-x-0 top-0 h-1 rounded-t-2xl bg-gradient-to-r from-amber-500 to-black" />
      <div className="flex items-center gap-2 mb-5">
        <div className="text-sm font-bold text-white/80">⭐</div>
        <h3 className="text-xl sm:text-2xl font-extrabold text-neutral-900">관심 종목</h3>
      </div>

      {empty ? (
        <div className="text-sm text-neutral-500">관심 종목이 없습니다.</div>
      ) : (
        // 1열 → sm:2열 → lg:3열 → xl:4열
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {items.map((it) => (
            <button
              key={it.itemId}
              className="px-4 py-5 rounded-2xl bg-white/85 border border-black/5 shadow-[0_8px_30px_rgba(0,0,0,0.12)] text-left hover:shadow-[0_12px_34px_rgba(0,0,0,0.16)] transition-shadow"
              title={it.name}
              // onClick={() => navigate(`/stock/${it.itemId}`)} // 라우팅 원하면 활성화
            >
              <div className="text-neutral-900 font-bold truncate">{it.name}</div>
              {/* 필요하면 코드/심볼 등 추가 */}
            </button>
          ))}
        </div>
      )}
    </section>
  );
}
