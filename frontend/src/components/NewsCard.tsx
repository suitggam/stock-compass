// NewsCard.tsx
import { Link } from "react-router";
import type { News } from "../types/StockDetail";

interface NewsCardProps {
  news: News;
}

function NewsCard({ news }: NewsCardProps) {
  return (
    <div className="bg-slate-800 text-white rounded-2xl shadow-lg p-4 border border-slate-700 hover:bg-slate-700 transition">
      <Link to={news.url} target="_blank" rel="noopener noreferrer">
        <div className="flex justify-between gap-2">
          <div className="font-semibold hover:underline">{news.title}</div>
          <div className="text-slate-300 whitespace-nowrap">{news.date}</div>
        </div>
      </Link>
    </div>
  );
}

export default NewsCard;
