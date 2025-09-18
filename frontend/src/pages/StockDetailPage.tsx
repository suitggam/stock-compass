import { useState, useMemo } from "react";
import ChartHeader from "../components/Chart/ChartHeader";
import TimeTerm from "../components/Chart/TimeTerm";
import KeywordRank from "../components/KeywordRank";
import NewsCard from "../components/NewsCard";
import {
  mockData,
  mockData2,
  mockData3,
  mockData4,
  TermText,
  type Term,
  type ChartData,
  type News,
} from "../types/StockDetail";
import ChartMain from "../components/Chart/ChartMain";

function StockDetailPage() {
  const [selectedTerm, setSelectedTerm] = useState<Term>(TermText[0]);

  const handleSelect = (term: Term) => {
    setSelectedTerm(term);
  };

  const yesterday = useMemo(() => {
    const d = new Date();
    d.setDate(d.getDate() - 1);
    return d;
  }, []);

  const cutoff = useMemo(() => {
    const c = new Date(yesterday);

    switch (selectedTerm.text) {
      case "1개월":
        c.setMonth(c.getMonth() - 1);
        break;
      case "3개월":
        c.setMonth(c.getMonth() - 3);
        break;
      case "6개월":
        c.setMonth(c.getMonth() - 6);
        break;
      case "1 년":
        c.setFullYear(c.getFullYear() - 1);
        break;
      case "5 년":
        c.setFullYear(c.getFullYear() - 5);
        break;
      default:
        c.setDate(c.getDate() - 7);
    }

    return c;
  }, [selectedTerm, yesterday]);

  const filteredData: ChartData[] = useMemo(() => {
    return mockData4.filter(
      (d) => new Date(d.date) >= cutoff && new Date(d.date) <= yesterday
    );
  }, [cutoff, yesterday]);

  const latestData = useMemo(() => {
    return [...mockData4].reverse().find((d) => new Date(d.date) <= yesterday)!;
  }, [yesterday]);

  const pastData = useMemo(() => {
    const sortedData = [...mockData4].sort(
      (a, b) => new Date(a.date).getTime() - new Date(b.date).getTime()
    );
    return sortedData.find((d) => new Date(d.date) >= cutoff);
  }, [cutoff]);

  const pastPrice = pastData?.endPrice ?? 0;

  const changeRate = pastData
    ? ((latestData.endPrice - pastData.endPrice) / pastData.endPrice) * 100
    : 0;

  const filteredNews: News[] = useMemo(() => {
    return mockData2
      .filter((n) => new Date(n.date) >= cutoff)
      .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
  }, [cutoff]);

  return (
    <div className="min-h-screen bg-gradient-to-br py-10 px-6 from-slate-900 via-slate-800 to-slate-900">
      <div className="max-w-7xl mx-auto space-y-10">
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
          <div className="lg:col-span-3 space-y-6">
            <div className="bg-gradient-to-br from-slate-800 to-slate-700 text-white rounded-2xl shadow-xl p-6 border border-slate-600">
              <ChartHeader
                ticker={mockData.ticker}
                companyName={mockData.companyName}
                endPrice={latestData.endPrice}
                rate={changeRate}
                termText={selectedTerm.text}
                pastPrice={pastPrice}
              />
              <div className="mt-4 ml-3">
                <TimeTerm
                  terms={TermText}
                  selectedTerm={selectedTerm}
                  onSelect={handleSelect}
                />
              </div>
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
              <KeywordRank keywords={mockData3} />
            </div>
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

export default StockDetailPage;
