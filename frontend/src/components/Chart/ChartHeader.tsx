import { CalendarDays } from "lucide-react";
import type { StockDetail } from "../../types/StockDetail";

interface ChartHeaderProps extends StockDetail {
  termText: string;
  pastPrice: number;
}

function ChartHeader({
  ticker,
  companyName,
  endPrice,
  rate,
  termText,
  pastPrice,
}: ChartHeaderProps) {
  const today = new Date();
  const formattedDate = `${today.getFullYear()}년 ${
    today.getMonth() + 1
  }월 ${today.getDate()}일`;

  // 가격 색상
  const priceColor =
    rate > 0 ? "text-red-400" : rate < 0 ? "text-green-400" : "text-slate-300";

  const arrow = rate > 0 ? "▼" : rate < 0 ? "▲" : "-";

  return (
    <div className="bg-gradient-to-r from-slate-900 via-slate-800 to-slate-900 text-white px-6 py-5 rounded-2xl shadow-lg border border-slate-700">
      {/* 상단: 회사명 + 티커 + 날짜 */}
      <div className="flex justify-between items-center">
        <div className="flex items-center gap-4">
          <h1 className="text-2xl font-extrabold tracking-tight text-white">
            {companyName}
          </h1>
          <span className="px-3 py-1 text-sm font-semibold text-amber-400 bg-slate-700 rounded-full border border-amber-500 shadow-md">
            {ticker}
          </span>
        </div>

        <div className="flex items-center gap-2 text-sm text-slate-400">
          <CalendarDays className="w-4 h-4 text-slate-500" />
          <span>{formattedDate}</span>
        </div>
      </div>

      {/* 하단: 종가 + 전 대비 */}
      <div className="mt-4 flex items-baseline gap-3">
        <span
          className={`text-3xl font-extrabold tracking-tight drop-shadow-md ${priceColor}`}
        >
          {endPrice.toLocaleString()} 원
        </span>
        <span className={`text-sm font-semibold ${priceColor}`}>
          {arrow} {Math.abs(rate).toFixed(2)} %
        </span>
        <span className="text-sm text-slate-400">
          ({termText} 전 대비: {pastPrice.toLocaleString()} 원)
        </span>
      </div>
    </div>
  );
}

export default ChartHeader;
