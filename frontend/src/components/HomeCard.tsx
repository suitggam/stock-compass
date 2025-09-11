import StarGraySvg from "../svg/StarGraySvg";
import type { kospi200_5years } from "../types/Kospi200_5years";

function numberFormat(num: number) {
  return num.toLocaleString();
}

function formatMarketCap(marketCap: number) {
  const inManWon = Math.floor(marketCap / 10000);
  return inManWon.toLocaleString();
}

function HomeCard({
  ticker,
  company_name,
  close_price,
  market_cap,
  volume,
}: kospi200_5years) {
  return (
    <div className="bg-white/20 border-white/30 backdrop-blur-sm rounded-2xl p-4 shadow-lg hover:shadow-xl transition-all duration-300 hover:scale-105 border">
      {/* 헤더 - 회사명과 즐겨찾기 */}
      <div className="flex justify-between items-start mb-3">
        <div className="flex-1">
          <div className="text-white font-semibold text-lg truncate">
            {company_name}
          </div>
          <div className="text-white/70 text-sm font-mono">{ticker}</div>
        </div>
        <button className="cursor-pointer p-1 hover:bg-black/10 rounded-lg transition-colors">
          <StarGraySvg />
        </button>
      </div>

      {/* 산업 분류와 시총 */}
      <div className="flex justify-between items-center mb-3">
        <div className="bg-gradient-to-r from-amber-500 to-amber-600 text-white px-2 py-1 rounded-full text-xs font-medium">
          IT
        </div>
        <div className="text-white/90 text-sm">
          시총{" "}
          <span className="text-white font-medium">
            {formatMarketCap(market_cap)}
          </span>{" "}
          조
        </div>
      </div>

      {/* 주가 정보 */}
      <div className="flex justify-between items-end">
        <div>
          <div className="text-white text-xl font-bold">
            {numberFormat(close_price)}원
          </div>
          <div className="text-green-400 text-sm font-medium">+1.71%</div>
        </div>
        <div className="text-right">
          <div className="text-white/70 text-xs">거래량</div>
          <div className="text-white/90 text-sm font-medium">
            {numberFormat(volume)}
          </div>
        </div>
      </div>
    </div>
  );
}

export default HomeCard;
