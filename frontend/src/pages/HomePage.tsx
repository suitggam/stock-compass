import { useEffect, useState } from "react";
import HomeCard from "../components/HomeCard";
import type { Kospi200_Realtime } from "../types/Kospi200_Realtime";

export default function HomePage() {
  const [stocks, setStocks] = useState<Kospi200_Realtime[]>([]);

  useEffect(() => {
    const ws = new WebSocket("ws://localhost:8765");

    ws.onopen = () => console.log("✅ WS 연결 성공");

    ws.onmessage = (event) => {
      try {
        const data: Kospi200_Realtime[] = JSON.parse(event.data);
        console.log("📡 실시간 데이터:", data); // 콘솔로 확인
        setStocks(data); // 화면 업데이트
      } catch (err) {
        console.error("❌ WS 데이터 파싱 오류:", err, event.data);
      }
    };

    ws.onclose = () => console.log("❌ WS 연결 종료");
    ws.onerror = (err) => console.error("❌ WS 에러", err);

    return () => ws.close();
  }, []);

  return (
    <div className="grid grid-cols-3 gap-4 p-4">
      {stocks.map((stock) => (
        <HomeCard
          key={stock.ticker}
          ticker={stock.ticker}
          company_name={stock.company_name}
          price={stock.price ?? 0} // undefined 방지
          rate={stock.rate ?? 0} // undefined 방지
        />
      ))}
    </div>
  );
}
