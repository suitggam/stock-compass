import type { NewsItem } from "../../types/tendency";

type Props = {
  keywords: string[];
  news: NewsItem[];
};

export default function StockHighlights({ keywords, news }: Props) {
  return (
    // 💡 bg-slate-900과 rounded-xl를 적용
    <div className="rounded-xl bg-slate-900 p-5">
      <section className="mb-4">
        {/* 💡 텍스트 색상을 배경에 맞게 변경 */}
        <h3 className="mb-2 text-sm font-semibold text-white">주요 키워드</h3>
        {keywords.length > 0 ? (
          <div className="flex flex-wrap gap-2">
            {keywords.map((word, idx) => (
              <span
                key={`${word}-${idx}`}
                // 💡 배경에 맞게 색상을 변경
                className="rounded-full bg-slate-700 px-2.5 py-1 text-xs font-semibold text-white"
              >
                {word}
              </span>
            ))}
          </div>
        ) : (
          // 💡 텍스트 색상을 배경에 맞게 변경
          <p className="text-xs text-slate-400">표시할 키워드가 없습니다.</p>
        )}
      </section>

      <section>
        {/* 💡 텍스트 색상을 배경에 맞게 변경 */}
        <h3 className="mb-2 text-sm font-semibold text-white">관련 뉴스</h3>
        {news.length > 0 ? (
          <ul className="space-y-2">
            {news.map((item) => (
              <li
                key={item.id}
                // 💡 배경과 어울리도록 색상을 변경
                className="rounded-xl border border-slate-600 bg-slate-800 px-4 py-3"
              >
                {/* 💡 텍스트 색상을 배경에 맞게 변경 */}
                <div className="font-semibold text-white">{item.title}</div>
                {item.summary ? (
                  // 💡 텍스트 색상을 배경에 맞게 변경
                  <p className="mt-1 text-sm text-slate-400">{item.summary}</p>
                ) : null}
              </li>
            ))}
          </ul>
        ) : (
          // 💡 텍스트 색상을 배경에 맞게 변경
          <p className="text-xs text-slate-400">관련 뉴스가 없습니다.</p>
        )}
      </section>
    </div>
  );
}