import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import type { ChartData } from "../../types/StockDetail";
import type { Payload } from "recharts/types/component/DefaultTooltipContent";

interface ChartProps {
  data: ChartData[];
  term: string;
}

function ChartMain({ data, term }: ChartProps) {
  type CustomPayload = {
    value: number;
    dataKey: keyof ChartData;
  };

  function numberFormat(num: number) {
    return num.toLocaleString();
  }

  const CustomTooltip = ({
    active,
    payload,
    label,
  }: {
    active?: boolean;
    payload?: Payload<number, string>[];
    label?: string;
  }) => {
    if (active && payload && payload.length > 0) {
      const p = payload[0] as CustomPayload;
      return (
        <div className="bg-slate-800 text-white p-3 rounded-lg shadow-lg border border-slate-600">
          <p className="text-base">
            날짜: <span className="font-bold text-amber-300">{label}</span>
          </p>
          <p className="text-base">
            종가:{" "}
            <span className="font-bold text-amber-300">
              {numberFormat(p.value)}
            </span>
          </p>
        </div>
      );
    }
    return null;
  };

  const latestDate = data.length
    ? new Date(data[data.length - 1].date)
    : new Date();
  const filteredData = data.filter((d) => new Date(d.date) <= latestDate);

  // 기간별 X축 interval 설정
  let xInterval: number | "preserveStartEnd" = 0;
  switch (term) {
    case "1주":
      xInterval = 0; // 하루 단위
      break;
    case "1개월":
      xInterval = 6; // 1주 단위
      break;
    case "3개월":
    case "6개월":
      xInterval = 29; // 1개월 단위
      break;
    case "1 년":
      xInterval = 89; // 3개월 단위
      break;
    case "5 년":
      xInterval = 364; // 1년 단위
      break;
    default:
      xInterval = 0;
  }

  return (
    <div className="w-full h-96">
      <div className="w-full h-96 bg-slate-900 rounded-xl p-2">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={filteredData}>
            <XAxis
              dataKey="date"
              stroke="#cbd5e1"
              interval={xInterval}
              tickFormatter={(date, index) => {
                const d = new Date(date);
                const yyyy = d.getFullYear();
                const mm = String(d.getMonth() + 1).padStart(2, "0");
                const dd = String(d.getDate()).padStart(2, "0");
                if (index === filteredData.length - 1) return "";
                return `${yyyy}-${mm}-${dd}`;
              }}
              tick={{ fontSize: 12 }}
            />

            <YAxis
              stroke="#cbd5e1"
              width={60} // 기본보다 넉넉하게 확보
            />
            <Tooltip content={<CustomTooltip />} />
            <Line
              type="monotone"
              dataKey="endPrice"
              stroke="#fbbf24"
              strokeWidth={2}
              dot={{ r: 4, stroke: "#fbbf24", fill: "#fbbf24" }}
              activeDot={{ r: 6, stroke: "#f59e0b", fill: "#fbbf24" }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}

export default ChartMain;
