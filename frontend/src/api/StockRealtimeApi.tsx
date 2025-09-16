import axios from "axios";

export const API_SERVER_HOST = "http://localhost:8080";
const prefix = `${API_SERVER_HOST}/api/stock`;

export const getStockRealtime = async () => {
  const res = await axios.get(`${prefix}/realtime`);
  return res.data;
};

export const getEndDay = async () => {
  const res = await axios.get(`${prefix}/endDay`);
  return res.data;
};
