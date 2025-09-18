import { useEffect, useState } from "react";
import HomeCard from "../components/HomeCard";
import type {
  WebSocketRealtime,
  BackendRealtime,
  EndDay,
  PageResponseDto,
} from "../types/StockRealtime";
import {
  getStockRealtimeWithPage,
  getEndDayWithPage,
} from "../api/StockRealtimeApi";
import { Link } from "react-router";

export default function HomePage() {
  const [backendStocks, setBackendStocks] = useState<BackendRealtime[]>([]);
  const [wsStocks, setWsStocks] = useState<Map<string, WebSocketRealtime>>(
    new Map()
  );
  const [endDayStocks, setEndDayStocks] = useState<EndDay[]>([]);
  const [isMarketOpen, setIsMarketOpen] = useState(true);

  // 페이지네이션 상태
  const [page, setPage] = useState(1);
  const [size] = useState(21);
  const [totalPages, setTotalPages] = useState(1);

  // 0️⃣ 장 시간 확인 (09:00 ~ 15:30)
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
    const interval = setInterval(checkMarketOpen, 60 * 1000);
    return () => clearInterval(interval);
  }, []);

  // 1️⃣ 1분 단위로 백엔드 전체 데이터 가져오기
  useEffect(() => {
    const fetchBackend = async () => {
      try {
        if (isMarketOpen) {
          const response: PageResponseDto<BackendRealtime> =
            await getStockRealtimeWithPage(1, 1000); // 전체
          setBackendStocks(response.dtoList);
          setTotalPages(Math.ceil(response.dtoList.length / size));
        } else {
          const response: PageResponseDto<EndDay> = await getEndDayWithPage(
            page,
            size
          );
          setEndDayStocks(response.dtoList);
          setTotalPages(response.totalPage || 1);
        }
      } catch (err) {
        console.error("❌ API 에러:", err);
      }
    };

    fetchBackend();
    const interval = setInterval(fetchBackend, 60 * 1000); // 1분 단위 새로고침
    return () => clearInterval(interval);
  }, [isMarketOpen, page, size]);

  // 2️⃣ WebSocket 연결 (장 열려있을 때만)
  useEffect(() => {
    if (!isMarketOpen) return;

    const ws = new WebSocket("ws://localhost:8765");

    ws.onopen = () => console.log("✅ WS 연결 성공");

    ws.onmessage = (event) => {
      try {
        const data: WebSocketRealtime[] = JSON.parse(event.data);
        setWsStocks((prev) => {
          const updated = new Map(prev);
          data.forEach((d) => {
            updated.set(d.ticker, {
              ticker: d.ticker,
              price: d.price,
              rate: d.rate,
            });
          });
          return updated;
        });
      } catch (err) {
        console.error("❌ WS 데이터 파싱 오류:", err, event.data);
      }
    };

    ws.onclose = () => console.log("❌ WS 연결 종료");
    ws.onerror = (err) => console.error("❌ WS 에러", err);

    return () => ws.close();
  }, [isMarketOpen]);

  // 3️⃣ 화면에 보여줄 데이터 결정
  const displayStocks: (BackendRealtime & {
    endPrice?: number;
    rate?: number;
  })[] = (() => {
    if (!isMarketOpen) return endDayStocks;

    const startIdx = (page - 1) * size;
    const endIdx = startIdx + size;
    return backendStocks.slice(startIdx, endIdx).map((b) => {
      const wsItem = wsStocks.get(b.ticker);
      return {
        ...b,
        endPrice: wsItem ? Number(wsItem.price) : 0,
        rate: wsItem ? wsItem.rate : 0,
      };
    });
  })();

  return (
    <div>
      <div className="grid grid-cols-3 gap-4 p-4">
        {displayStocks.map((stock) => (
          <Link to={`stock/${stock.ticker}`}>
            <HomeCard
              key={stock.ticker}
              ticker={stock.ticker}
              companyName={stock.companyName}
              price={stock.endPrice ?? 0}
              rate={stock.rate ?? 0}
              volume={stock.volume}
              marketCap={stock.marketCap}
              categoryName={stock.categoryName}
            />
          </Link>
        ))}
      </div>

      {/* 4️⃣ 페이지네이션 UI */}
      <div className="pagination flex justify-center gap-2 mt-4 flex-wrap">
        <button
          disabled={page === 1}
          className="cursor-pointer px-3 py-1 border rounded bg-gray-200 hover:bg-gray-300 disabled:opacity-50"
          onClick={() => setPage((p) => Math.max(p - 1, 1))}
        >
          Prev
        </button>

        {Array.from({ length: totalPages }, (_, i) => i + 1).map((p) => (
          <button
            key={p}
            className={`cursor-pointer px-3 py-1 border rounded ${
              p === page
                ? "bg-blue-500 text-white"
                : "bg-gray-200 hover:bg-gray-300"
            }`}
            onClick={() => setPage(p)}
          >
            {p}
          </button>
        ))}

        <button
          disabled={page === totalPages}
          className="cursor-pointer px-3 py-1 border rounded bg-gray-200 hover:bg-gray-300 disabled:opacity-50"
          onClick={() => setPage((p) => Math.min(p + 1, totalPages))}
        >
          Next
        </button>
      </div>
    </div>
  );
}
