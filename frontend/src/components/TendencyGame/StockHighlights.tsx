import type { NewsItem } from "../../types/tendency";

type Props = {
  keywords: string[];
  news: NewsItem[];
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
            {news.map((item) => (
              <li key={item.id} className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-3">
                <div className="font-semibold text-slate-900">{item.title}</div>
                {item.summary ? <p className="mt-1 text-sm text-slate-600">{item.summary}</p> : null}
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
