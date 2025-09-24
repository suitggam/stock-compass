// src/api/StockInfosApi.tsx
import axios from "axios";
import type {
  ExtractKeywordsResponse,
  Keyword,
  News,
  StockInfos,
} from "../types/StockInfos";
import { useAuth } from "../stores/auth";

export const API_SERVER_HOST =
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";
const prefix = `${API_SERVER_HOST}/api/stock`;

// 🔹 키워드 추출 API (백엔드 호출)
// 🔹 키워드 추출 API (백엔드 호출) - 토큰 추가 ✅
export const extractKeywords = async (
  ticker: string,
  companyName: string,
  startDate: string,
  endDate: string
): Promise<{ keywords: Keyword[]; news: News[] }> => {
  try {
    const accessToken = useAuth.getState().accessToken;

    const payload = {
      companyName,
      startDate,
      endDate,
      topKeywords: 10,
      useAiFilter: true,
    };

    // 토큰이 있는 경우에만 헤더에 포함
    const headers = accessToken
      ? {
          Authorization: `Bearer ${accessToken}`,
        }
      : {};

    const url = `${prefix}/extract-keywords/${ticker}`;
    console.log("Sending POST to backend /extract-keywords:", url, payload);
    console.log(
      "[extractKeywords] Using accessToken:",
      accessToken ? "present" : "none"
    );

    const res = await axios.post<ExtractKeywordsResponse>(url, payload, {
      headers, // ✅ 헤더 추가!
    });
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

// 🔹 주식 정보 API - 토큰 추가 ✅
export const getStockInfo = async (ticker: string): Promise<StockInfos[]> => {
  const accessToken = useAuth.getState().accessToken;

  // 토큰이 있는 경우에만 헤더에 포함
  const headers = accessToken
    ? {
        Authorization: `Bearer ${accessToken}`,
      }
    : {};

  console.log(
    "[getStockInfo] Using accessToken:",
    accessToken ? "present" : "none"
  );

  const res = await axios.get<StockInfos[]>(`${prefix}/info/${ticker}`, {
    headers,
  });
  return res.data;
};

// src/api/FavoriteApi.ts
export async function toggleFavorite(ticker: string): Promise<boolean> {
  const accessToken = useAuth.getState().accessToken;
  try {
    console.log("[toggleFavorite] Sending request for ticker:", ticker);
    const res = await axios.post(
      `${API_SERVER_HOST}/api/stock/favorites/toggle`,
      null,
      {
        params: { ticker },
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      }
    );
    console.log("[toggleFavorite] Response:", res.data);
    return res.data;
  } catch (err) {
    console.error("[toggleFavorite] Error:", err);
    throw err;
  }
}

export async function fetchFavorite(ticker: string): Promise<boolean> {
  const accessToken = useAuth.getState().accessToken; // store에서 토큰 가져오기
  if (!accessToken) throw new Error("No access token available");

  console.log("[fetchFavorite] Using accessToken:", accessToken);

  const res = await axios.get(
    `${API_SERVER_HOST}/api/stock/favorites/${ticker}`,
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    }
  );

  console.log("[fetchFavorite] Response status:", res.status);
  console.log("[fetchFavorite] Data:", res.data);

  return res.data.isFavorite;
}
