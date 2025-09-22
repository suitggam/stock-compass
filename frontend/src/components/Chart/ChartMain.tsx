// src/components/Chart/ChartMain.tsx
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import type { Payload } from "recharts/types/component/DefaultTooltipContent";
import type { StockInfos } from "../../types/StockInfos";

interface ChartProps {
  data: StockInfos[];
  term: string;
}

function ChartMain({ data, term }: ChartProps) {
  // 숫자 포맷
  function numberFormat(num: number) {
    return num.toLocaleString();
  }

  // chart용 데이터: dateString 추가
  const chartData = data.map((d) => ({
    ...d,
    dateString: d.date, // yyyy-MM-dd 문자열 그대로 사용
  }));

  // Custom Tooltip
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
      const p = payload[0];
      return (
        <div className="bg-slate-800 text-white p-3 rounded-lg shadow-lg border border-slate-600">
          <p className="text-base">
            날짜: <span className="font-bold text-amber-300">{label}</span>
          </p>
          <p className="text-base">
            종가:{" "}
            <span className="font-bold text-amber-300">
              {numberFormat(p.value ?? 0)} {/* undefined 처리 */}
            </span>
          </p>
        </div>
      );
    }
    return null;
  };

  if (!chartData || chartData.length === 0) return null;

  // 최신 데이터 찾기 (문자열 비교)
  const latestData = chartData.reduce((prev, curr) => {
    return curr.dateString > prev.dateString ? curr : prev;
  });

  const latestDateString = latestData.dateString;

  // 날짜 순 정렬
  const sortedData = [...chartData].sort((a, b) =>
    a.dateString > b.dateString ? 1 : -1
  );

  // 최신 날짜까지 필터
  const filteredData = sortedData.filter(
    (d) => d.dateString <= latestDateString
  );

  // X축 interval 설정
  let xInterval: number | "preserveStartEnd" = 0;
  if (term === "사용자 지정") {
    const len = filteredData.length;
    if (len <= 7) xInterval = 0;
    else if (len <= 30) xInterval = 6;
    else if (len <= 180) xInterval = 29;
    else if (len <= 365) xInterval = 89;
    else if (len <= 365 * 3) xInterval = 179;
    else if (len <= 365 * 5) xInterval = 364;
    else xInterval = Math.floor(len / 4);
  } else {
    switch (term) {
      case "1주":
        xInterval = 0;
        break;
      case "1개월":
        xInterval = 6;
        break;
      case "6개월":
        xInterval = 29;
        break;
      case "1 년":
        xInterval = 89;
        break;
      case "3 년":
        xInterval = 179;
        break;
      case "5 년":
        xInterval = 364;
        break;
      default:
        xInterval = 0;
    }
  }

  return (
    <div className="w-full h-96">
      <div className="w-full h-96 bg-slate-900 rounded-xl p-2">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={filteredData}>
            <XAxis
              dataKey="dateString"
              stroke="#cbd5e1"
              interval={xInterval}
              tick={{ fontSize: 12 }}
            />
            <YAxis stroke="#cbd5e1" width={60} />
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
