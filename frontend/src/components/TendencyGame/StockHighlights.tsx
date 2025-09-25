type Props = {
  keywords: string[];
  news: Array<{ title: string; url: string; date: string }>;
};

export default function StockHighlights({ keywords, news }: Props) {
  return (
    <div className="rounded-2xl bg-white p-5 shadow-sm">
      <section className="mb-4">
        <h3 className="mb-2 text-sm font-semibold text-slate-700">주요 키워드</h3>
        {keywords.length > 0 ? (
          <div className="flex flex-wrap gap-2">
            {keywords.map((word, idx) => (
              <span
                key={`${word}-${idx}`}
                className="rounded-full bg-indigo-50 px-2.5 py-1 text-xs font-semibold text-indigo-700"
              >
                {word}
              </span>
            ))}
          </div>
        ) : (
          <p className="text-xs text-slate-400">표시할 키워드가 없습니다.</p>
        )}
      </section>

      <section>
        <h3 className="mb-2 text-sm font-semibold text-slate-700">관련 뉴스</h3>
        {news.length > 0 ? (
          <ul className="space-y-2">
            {news.map((n, idx) => (
              <li key={`${n.url}-${idx}`} className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3">
                <a href={n.url} target="_blank" rel="noreferrer" className="font-semibold text-slate-900 hover:underline">
                  {n.title}
                </a>
                <div className="mt-1 text-xs text-slate-500">{n.date}</div>
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-xs text-slate-400">관련 뉴스가 없습니다.</p>
        )}
      </section>
    </div>
  );
}
