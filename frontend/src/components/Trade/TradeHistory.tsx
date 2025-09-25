import type { History } from "../../types/Trade";

interface HistoryCardProps {
  tradeHistory: History[];
}

function TradeHistory({ tradeHistory }: HistoryCardProps) {
  const formatTradeType = (type: "BUY" | "SELL"): string => {
    return type === "BUY" ? "매수" : "매도";
  };
  return (
    <>
      {tradeHistory.map((h, idx) => (
        <div key={idx} className="pb-1">
          <div className=" bg-slate-800 text-white rounded-2xl shadow-lg p-4 border border-slate-700 ">
            <div className="flex justify-between gap-2">
              <div>
                {formatTradeType(h.tradeType)} / {h.price.toLocaleString()} 원
              </div>
              <div className="text-slate-300 whitespace-nowrap">
                {h.createAt.toISOString().split("T")[0]}
              </div>
            </div>
          </div>
        </div>
      ))}
    </>
  );
}

export default TradeHistory;
