import axios from "axios";
import type {
  BackendRealtime,
  EndDay,
  PageResponseDto,
} from "../types/StockRealtime";

export const API_SERVER_HOST = "http://localhost:8080";
const prefix = `${API_SERVER_HOST}/api/stock`;

export const getStockRealtimeWithPage = async (
  page: number,
  size: number
): Promise<PageResponseDto<BackendRealtime>> => {
  const res = await axios.get(`${prefix}/realtime?page=${page}&size=${size}`);
  return res.data;
};
export const getEndDayWithPage = async (
  page: number,
  size: number
): Promise<PageResponseDto<EndDay>> => {
  const res = await axios.get(`${prefix}/endDay?page=${page}&size=${size}`);
  return res.data;
};
