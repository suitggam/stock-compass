import { useEffect, useState } from "react";
import HomeCard from "../components/HomeCard";
import type {
  WebSocketRealtime,
  BackendRealtime,
  EndDay,
} from "../types/StockRealtime";
import { getStockRealtime, getEndDay } from "../api/StockRealtimeAPi";

export default function HomePage() {
  const [wsStocks, setWsStocks] = useState<WebSocketRealtime[]>([]);
  const [backendStocks, setBackendStocks] = useState<BackendRealtime[]>([]);
  const [endDayStocks, setEndDayStocks] = useState<EndDay[]>([]);
  const [isMarketOpen, setIsMarketOpen] = useState(true);

  // 0️⃣ 장 시간 확인 (09:00 ~ 15:30 사이)
  useEffect(() => {
    const checkMarketOpen = () => {
      const now = new Date();
      const hours = now.getHours();
      const minutes = now.getMinutes();
      const open = hours > 9 || (hours === 9 && minutes >= 0);
      const close = hours < 15 || (hours === 15 && minutes <= 30);
      setIsMarketOpen(open && close);
    };

    checkMarketOpen();
    const interval = setInterval(checkMarketOpen, 60 * 1000); // 1분마다 확인
    return () => clearInterval(interval);
  }, []);

  // 1️⃣ 장 상태에 따라 데이터 가져오기
  useEffect(() => {
    if (isMarketOpen) {
      // 장이 열리면 실시간 WS + Backend 정보
      getStockRealtime()
        .then(setBackendStocks)
        .catch((err) => console.error("❌ 백엔드 API 에러:", err));
    } else {
      // 장이 닫히면 마감 데이터
      getEndDay()
        .then(setEndDayStocks)
        .catch((err) => console.error("❌ EndDay API 에러:", err));
    }
  }, [isMarketOpen]);

  // 2️⃣ WebSocket 연결 (장 열려있을 때만)
  useEffect(() => {
    if (!isMarketOpen) return;

    const ws = new WebSocket("ws://localhost:8765");

    ws.onopen = () => console.log("✅ WS 연결 성공");

    ws.onmessage = (event) => {
      try {
        const data: WebSocketRealtime[] = JSON.parse(event.data);
        setWsStocks(data);
      } catch (err) {
        console.error("❌ WS 데이터 파싱 오류:", err, event.data);
      }
    };

    ws.onclose = () => console.log("❌ WS 연결 종료");
    ws.onerror = (err) => console.error("❌ WS 에러", err);

    return () => ws.close();
  }, [isMarketOpen]);

  // 3️⃣ 화면에 보여줄 데이터 결정 (중복 제거 + 전체 ticker 표시)
  const displayStocks: EndDay[] = (() => {
    if (!isMarketOpen) return endDayStocks;

    // backendStocks를 Map으로 변환 (ticker -> BackendRealtime)
    const backendMap = new Map(backendStocks.map((b) => [b.ticker, b]));

    // 모든 ticker를 포함하도록 wsStocks + backendStocks 병합
    const allTickers = Array.from(
      new Set([
        ...wsStocks.map((w) => w.ticker),
        ...backendStocks.map((b) => b.ticker),
      ])
    );

    return allTickers.map((ticker) => {
      const wsItem = wsStocks.find((w) => w.ticker === ticker);
      const backendItem = backendMap.get(ticker);

      return {
        ticker,
        companyName: wsItem?.companyName ?? "알수없음",
        endPrice: Number(wsItem?.price ?? 0),
        rate: wsItem?.rate ?? 0,
        volume: backendItem?.volume ?? 0,
        marketCap: backendItem?.marketCap ?? 0,
        categoryName: backendItem?.categoryName ?? "알수없음",
      };
    });
  })();

  return (
    <div className="grid grid-cols-3 gap-4 p-4">
      {displayStocks.map((stock) => (
        <HomeCard
          key={stock.ticker}
          ticker={stock.ticker}
          companyName={stock.companyName}
          price={stock.endPrice}
          rate={stock.rate}
          volume={stock.volume}
          marketCap={stock.marketCap}
          categoryName={stock.categoryName}
        />
      ))}
    </div>
  );
}
