import axios from "axios";
import type { StockInfos } from "../types/StockInfos";

export const API_SERVER_HOST = "http://localhost:8080";
const prefix = `${API_SERVER_HOST}/api/stock`;

export const getStockInfo = async (ticker: string): Promise<StockInfos[]> => {
  const res = await axios.get(`${prefix}/info/${ticker}`);
  return res.data;
};
