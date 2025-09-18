import { useState, useMemo, useEffect } from "react";
import { useParams } from "react-router";
import ChartHeader from "../components/Chart/ChartHeader";
import TimeTerm from "../components/Chart/TimeTerm";
import KeywordRank from "../components/KeywordRank";
import NewsCard from "../components/NewsCard";
import ChartMain from "../components/Chart/ChartMain";
import {
  TermText,
  type Term,
  type News,
  type StockInfos,
  mockData2,
  mockData3,
} from "../types/StockInfos";
import { getStockInfo } from "../api/StockInfosApi";

function StockInfoPage() {
  const { ticker } = useParams<{ ticker: string }>();
  const [selectedTerm, setSelectedTerm] = useState<Term>(TermText[0]);
  const [stockData, setStockData] = useState<StockInfos[]>([]);

  const handleSelect = (term: Term) => setSelectedTerm(term);

  // API 호출
  useEffect(() => {
    async function fetchData() {
      const data = await getStockInfo(ticker!);
      setStockData(data);
    }
    fetchData();
  }, [ticker]);

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
      case "3 년":
        c.setFullYear(c.getFullYear() - 3);
        break;
      case "5 년":
        c.setFullYear(c.getFullYear() - 5);
        break;
      default:
        c.setDate(c.getDate() - 7);
    }
    return c;
  }, [selectedTerm, yesterday]);

  const filteredData = useMemo(
    () =>
      stockData.filter(
        (d) => new Date(d.date) >= cutoff && new Date(d.date) <= yesterday
      ),
    [stockData, cutoff, yesterday]
  );

  const latestStock = useMemo(
    () =>
      [...stockData].reverse().find((d) => new Date(d.date) <= yesterday) ??
      null,
    [stockData, yesterday]
  );

  const pastStock = useMemo(() => {
    if (!latestStock) return null;
    return filteredData.length > 0 ? filteredData[0] : latestStock;
  }, [filteredData, latestStock]);

  const pastPrice = pastStock?.endPrice ?? 0;

  const changeRate = pastStock
    ? ((latestStock!.endPrice - pastPrice) / pastPrice) * 10
    : 0;

  const filteredNews: News[] = useMemo(() => {
    return mockData2
      .filter((n) => new Date(n.date) >= cutoff)
      .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
  }, [cutoff]);

  if (!latestStock) return <div>Loading...</div>;

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
                rate={changeRate}
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

export default StockInfoPage;
