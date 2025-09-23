// src/api/StockInfosApi.tsx
import axios from "axios";
import type {
  ExtractKeywordsResponse,
  Keyword,
  News,
  StockInfos,
} from "../types/StockInfos";

export const API_SERVER_HOST =
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";
const prefix = `${API_SERVER_HOST}/api/stock`;

// 🔹 응답 타입 정의

// 🔹 키워드 추출 API (백엔드 호출)
export const extractKeywords = async (
  ticker: string,
  companyName: string,
  startDate: string,
  endDate: string
): Promise<{ keywords: Keyword[]; news: News[] }> => {
  try {
    const payload = {
      companyName,
      startDate,
      endDate,
      topKeywords: 10,
      useAiFilter: true,
    };

    const url = `${prefix}/extract-keywords/${ticker}`;
    console.log("Sending POST to backend /extract-keywords:", url, payload);

    const res = await axios.post<ExtractKeywordsResponse>(url, payload);
    console.log("Received response:", res.data);

    // keywords 변환
    const keywords = Object.entries(res.data.keywords || {}).map(
      ([keyword, count]) => ({ keyword, count })
    );

    // 뉴스 변환 (title, date, url만 가져오기)
    const news = (res.data.topNewsArticles ?? []).map((n) => ({
      title: n.title,
      date: n.date,
      url: n.url,
    }));

    return { keywords, news };
  } catch (err) {
    console.error("extractKeywords error:", err);
    return { keywords: [], news: [] };
  }
};

// 🔹 기존 주식 정보 API
export const getStockInfo = async (ticker: string): Promise<StockInfos[]> => {
  const res = await axios.get<StockInfos[]>(`${prefix}/info/${ticker}`);
  return res.data; // 그대로 반환
};
