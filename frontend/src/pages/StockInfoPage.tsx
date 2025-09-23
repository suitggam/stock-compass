// src/pages/StockInfoPage.tsx
import { useState, useMemo, useEffect } from "react";
import { useParams } from "react-router";
import ChartHeader from "../components/Chart/ChartHeader";
import TimeTerm from "../components/Chart/TimeTerm";
import KeywordRank from "../components/KeywordRank";
import NewsCard from "../components/NewsCard";
import ChartMain from "../components/Chart/ChartMain";
import DateModal from "./DateModal";
import ChartNews from "../components/Chart/ChartNews";
import {
  TermText,
  type Term,
  type News,
  type StockInfos,
  type Keyword,
} from "../types/StockInfos";
import {
  extractKeywords,
  getStockInfo as fetchStockInfo,
} from "../api/StockInfosApi";

function StockInfoPage() {
  const { ticker } = useParams<{ ticker: string }>();
  const [selectedTerm, setSelectedTerm] = useState<Term>(TermText[0]);
  const [stockData, setStockData] = useState<StockInfos[]>([]);
  const [customModalOpen, setCustomModalOpen] = useState(false);
  const [customStartDate, setCustomStartDate] = useState<Date | null>(null);
  const [customEndDate, setCustomEndDate] = useState<Date | null>(null);
  const [keywords, setKeywords] = useState<Keyword[]>([]);
  const [news, setNews] = useState<News[]>([]);

  // 오늘 기준 startDate / endDate 계산
  const { startDate, endDate } = useMemo(() => {
    const today = new Date();
    let start: Date;
    let end: Date = today;

    if (
      selectedTerm.text === "사용자 지정" &&
      customStartDate &&
      customEndDate
    ) {
      start = customStartDate;
      end = customEndDate;
    } else {
      start = new Date(today);
      switch (selectedTerm.text) {
        case "1주":
          start.setDate(start.getDate() - 7);
          break;
        case "1개월":
          start.setMonth(start.getMonth() - 1);
          break;
        case "3개월":
          start.setMonth(start.getMonth() - 3);
          break;
        case "6개월":
          start.setMonth(start.getMonth() - 6);
          break;
        case "1 년":
          start.setFullYear(start.getFullYear() - 1);
          break;
        case "3 년":
          start.setFullYear(start.getFullYear() - 3);
          break;
        case "5 년":
          start.setFullYear(start.getFullYear() - 5);
          break;
        default:
          start.setDate(start.getDate() - 7);
      }
    }

    return { startDate: start, endDate: end };
  }, [selectedTerm, customStartDate, customEndDate]);

  // 주식 데이터 불러오기
  useEffect(() => {
    if (!ticker) return;
    (async () => {
      const data = await fetchStockInfo(ticker);
      console.log("백엔드 데이터:", data);
      setStockData(data); // 문자열 날짜 그대로
    })();
  }, [ticker]);

  // 최신 데이터 찾기
  const latestStock = useMemo(() => {
    if (!stockData.length) return null;
    return stockData.reduce((prev, curr) =>
      new Date(curr.date) > new Date(prev.date) ? curr : prev
    );
  }, [stockData]);

  // 기간 필터링 (Date 객체 기준)
  const filteredData = useMemo(() => {
    if (!startDate || !endDate) return [];
    return stockData
      .filter((d) => {
        const dDate = new Date(d.date);
        return dDate >= startDate && dDate <= endDate;
      })
      .sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());
  }, [stockData, startDate, endDate]);

  // 키워드/뉴스 가져오기
  useEffect(() => {
    if (!latestStock || !startDate || !endDate) return;
    (async () => {
      const { keywords, news } = await extractKeywords(
        latestStock.ticker,
        latestStock.companyName,
        startDate.toISOString().slice(0, 10), // yyyy-MM-dd
        endDate.toISOString().slice(0, 10)
      );
      setKeywords(keywords);
      setNews(news);
    })();
  }, [latestStock, startDate, endDate]);

  const filteredNews = useMemo(() => {
    return news
      .filter((n) => {
        const nDate = new Date(n.date);
        return nDate >= startDate && nDate <= endDate;
      })
      .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
  }, [news, startDate, endDate]);

  const handleSelect = (term: Term) => {
    setSelectedTerm(term);
    if (term.text === "사용자 지정") setCustomModalOpen(true);
  };

  if (!latestStock) return <div>Loading...</div>;

  // 과거 가격: 기간 시작일 데이터
  const pastPrice = filteredData[0]?.endPrice ?? latestStock.endPrice;

  return (
    <div className="min-h-screen bg-gradient-to-br py-10 px-6 from-slate-900 via-slate-800 to-slate-900">
      <div className="max-w-7xl mx-auto space-y-10">
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
          <div className="lg:col-span-3 space-y-6">
            <div className="bg-gradient-to-br from-slate-800 to-slate-700 text-white rounded-2xl shadow-xl p-6 border border-slate-600">
              <ChartHeader
                ticker={latestStock.ticker}
                companyName={latestStock.companyName}
                endPrice={latestStock.endPrice}
                rate={((latestStock.endPrice - pastPrice) / pastPrice) * 100}
                termText={selectedTerm.text}
                pastPrice={pastPrice}
                date={latestStock.date}
              />

              <div className="mt-4 ml-3">
                <TimeTerm
                  terms={TermText}
                  selectedTerm={selectedTerm}
                  onSelect={handleSelect}
                />
              </div>

              <DateModal
                isOpen={customModalOpen}
                onClose={() => setCustomModalOpen(false)}
                onConfirm={(start, end) => {
                  setCustomStartDate(start);
                  setCustomEndDate(end);
                }}
              />

              <div className="mt-4">
                <ChartMain term={selectedTerm.text} data={filteredData} />
              </div>
            </div>
          </div>

          <div className="lg:col-span-1">
            <div className="bg-gradient-to-br from-slate-800 to-slate-700 rounded-2xl shadow-lg p-5 border border-slate-600">
              <h3 className="text-lg font-bold text-amber-400 mb-4">
                키워드 랭킹
              </h3>
              <KeywordRank keywords={keywords} />
            </div>
          </div>
        </div>

        <div className="bg-gradient-to-br from-slate-800 to-slate-700 rounded-2xl shadow-xl p-6 border border-slate-600">
          <h2 className="font-bold mb-6 text-white text-2xl flex items-center gap-2">
            뉴스 요약
          </h2>
          <div className="grid">
            <ChartNews />
          </div>
        </div>

        <div className="bg-gradient-to-br from-slate-800 to-slate-700 rounded-2xl shadow-xl p-6 border border-slate-600">
          <h2 className="font-bold mb-6 text-white text-2xl flex items-center gap-2">
            관련 뉴스
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {filteredNews.slice(0, 10).map((n, index) => (
              <NewsCard key={index} news={n} />
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

export default StockInfoPage;
