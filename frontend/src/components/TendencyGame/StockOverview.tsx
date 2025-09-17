import { Line } from "react-chartjs-2";
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Tooltip,
  Legend,
} from "chart.js";
import type { ChartOptions } from "chart.js";
import type { ChartLineData } from "../../types/tendency";

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend);

const formatNumber = (n: number) => new Intl.NumberFormat("ko-KR").format(Math.round(n));
const formatCurrency = (n: number) => `${formatNumber(n)}원`;
const formatDiff = (change: number, rate: number) => {
  const positive = change >= 0;
  return `${positive ? "+" : ""}${formatNumber(change)}원 (${positive ? "+" : ""}${rate.toFixed(2)}%)`;
};

type Props = {
  companyName: string;
  ticker?: string;
  currentWeek: string;
  nextWeek: string;
  price: number;
  change: number;
  rate: number;
  chartData: ChartLineData;
  chartOptions?: ChartOptions<'line'>;
  rangeFrom?: string;
};

export default function StockOverview({
  companyName,
  ticker,
  currentWeek,
  nextWeek,
  price,
  change,
  rate,
  chartData,
  chartOptions,
  rangeFrom,
}: Props) {
  const diffText = formatDiff(change, rate);
  const positive = change >= 0;

  return (
    <div className="space-y-4 rounded-2xl bg-white p-5 shadow-sm">
      <header className="flex items-start justify-between">
        <div className="flex items-center gap-2">
          <h2 className="text-2xl font-bold text-slate-900">{companyName}</h2>
          {ticker ? (
            <span className="rounded-full bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-600">
              {ticker}
            </span>
          ) : null}
        </div>
        <div className="mt-1 text-sm text-slate-500">{currentWeek}</div>
      </header>

      <section>
        <div className="text-3xl font-extrabold text-slate-900">{formatCurrency(price)}</div>
        <div className={`mt-1 text-sm font-semibold ${positive ? "text-emerald-600" : "text-rose-600"}`}>
          {diffText}
        </div>
      </section>

      <div className="rounded-xl border border-slate-200 bg-white p-2">
        <Line
          data={{ labels: chartData.labels, datasets: chartData.datasets as any }}
          options={{ responsive: true, maintainAspectRatio: false, ...chartOptions }}
          height={220}
        />
      </div>

      <footer className="flex items-center justify-between text-xs text-slate-500">
        <span>
          {rangeFrom ? `범위: ${rangeFrom} ~ ${currentWeek}` : `현재: ${currentWeek}`}
        </span>
        <span>다음: {nextWeek}</span>
      </footer>
    </div>
  );
}
