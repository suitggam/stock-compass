import { useState } from "react";
import { useTrade } from "../../hooks/useTrade";
import { sellStock } from "../../api/TradeApi";
import type { UserAsset } from "../../types/Trade";

interface TradeCardProps {
  ticker: string; // 종목 코드 추가
  userTrade: UserAsset;
  stockPrice: number;
  onTrade?: (type: "BUY" | "SELL", amount: number) => void; // 로컬 상태 업데이트용
  onTradeSuccess?: () => void; // 거래 성공 시 추가 처리
}

function TradeCard({
  ticker,
  userTrade,
  stockPrice,
  onTrade,
  onTradeSuccess,
}: TradeCardProps) {
  const [amount, setAmount] = useState<number>(0);
  const { loading, error, buyStock, clearError } = useTrade();

  const handleBuy = async () => {
    if (amount <= 0) {
      alert("수량을 입력해주세요.");
      return;
    }

    const totalCost = amount * stockPrice;
    if (totalCost > userTrade.cash) {
      alert("보유 현금이 부족합니다.");
      return;
    }

    try {
      clearError();

      // 실제 API 호출
      const result = await buyStock({
        ticker,
        price: stockPrice,
        volume: amount,
      });

      if (result) {
        // 성공 시
        alert(
          `매수 완료!\n종목: ${ticker}\n수량: ${amount}주\n총 금액: ${result.totalPrice.toLocaleString()}원`
        );

        // 로컬 상태 업데이트 (기존 로직 유지)
        if (onTrade) onTrade("BUY", totalCost);

        // 추가 처리 (예: 거래 내역 새로고침)
        if (onTradeSuccess) onTradeSuccess();

        // 수량 초기화
        setAmount(0);
      }
    } catch (err) {
      console.error("매수 실패:", err);
      alert("매수 주문 처리 중 오류가 발생했습니다.");
    }
  };

  const handleSell = async () => {
    if (amount <= 0) {
      alert("수량을 입력해주세요.");
      return;
    }

    const totalValue = amount * stockPrice;
    if (totalValue > userTrade.haveStock) {
      alert("보유 주식이 부족합니다.");
      return;
    }

    try {
      clearError();

      // 실제 API 호출
      const result = await sellStock({
        ticker,
        price: stockPrice,
        volume: amount,
      });

      if (result) {
        // 성공 시
        alert(
          `매도 완료!\n종목: ${ticker}\n수량: ${amount}주\n총 금액: ${result.totalPrice.toLocaleString()}원`
        );

        // 로컬 상태 업데이트 (기존 로직 유지)
        if (onTrade) onTrade("SELL", totalValue);

        // 추가 처리
        if (onTradeSuccess) onTradeSuccess();

        // 수량 초기화
        setAmount(0);
      }
    } catch (err) {
      console.error("매도 실패:", err);
      alert("매도 주문 처리 중 오류가 발생했습니다.");
    }
  };

  const increase = () => setAmount((prev) => prev + 1);
  const decrease = () => setAmount((prev) => (prev > 0 ? prev - 1 : 0));

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    if (/^\d*$/.test(value)) {
      setAmount(Number(value));
    }
  };

  const inputColor = amount === 0 ? "text-slate-400" : "text-white";

  return (
    <div className="bg-slate-800 text-white rounded-2xl shadow-lg p-6 space-y-6">
      {/* 에러 메시지 */}
      {error && (
        <div className="bg-red-500/20 border border-red-500 rounded-lg p-3 text-red-300 text-sm">
          {error}
        </div>
      )}

      {/* 자산 정보 */}
      <div className="space-y-3">
        <div className="flex justify-between border-b border-slate-700 pb-2">
          <span>총 자산</span>
          <span className="font-semibold">
            {userTrade.originalMoney.toLocaleString()} 원
          </span>
        </div>
        <div className="flex justify-between border-b border-slate-700 pb-2">
          <span>보유 현금</span>
          <span className="font-semibold">
            {userTrade.cash.toLocaleString()} 원
          </span>
        </div>
        <div className="flex justify-between border-b border-slate-700 pb-2">
          <span>보유 주식</span>
          <span className="font-semibold">
            {userTrade.haveStock.toLocaleString()} 원
          </span>
        </div>
        <div className="flex justify-between">
          <span>전체 손익률</span>
          <span
            className={`font-semibold ${
              (userTrade.cash + userTrade.haveStock - userTrade.originalMoney) /
                userTrade.originalMoney >=
              0
                ? "text-green-400"
                : "text-red-400"
            }`}
          >
            {(userTrade.cash + userTrade.haveStock - userTrade.originalMoney) /
              userTrade.originalMoney >
            0
              ? "+"
              : ""}
            {(
              (userTrade.cash + userTrade.haveStock - userTrade.originalMoney) /
              userTrade.originalMoney
            ).toFixed(2)}{" "}
            %
          </span>
        </div>
      </div>

      {/* 투자 수량 조절 + 버튼 */}
      <div className="space-y-3">
        <label className="block text-sm text-slate-300">투자 수량</label>
        <div className="flex items-center gap-2">
          <input
            type="text"
            className={`w-32 text-center ${inputColor} bg-slate-700 rounded py-1 px-2 border border-slate-600 focus:border-blue-500 focus:outline-none`}
            value={amount}
            onChange={handleChange}
            disabled={loading}
            placeholder="0"
          />
          <button
            className="w-10 bg-green-500 px-3 py-1 rounded text-white font-semibold hover:bg-green-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
            onClick={increase}
            disabled={loading}
          >
            +
          </button>
          <button
            className="w-10 bg-red-500 px-3 py-1 rounded text-white font-semibold hover:bg-red-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
            onClick={decrease}
            disabled={loading}
          >
            -
          </button>
        </div>

        {/* 예상 금액 표시 */}
        <div className="text-sm text-slate-300 text-center bg-slate-700 rounded py-2">
          거래금액: {(amount * stockPrice).toLocaleString()}원
        </div>

        <div className="flex gap-3">
          <button
            className="flex-1 bg-green-500 px-4 py-2 rounded-lg text-white font-semibold hover:bg-green-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
            onClick={handleBuy}
            disabled={loading || amount <= 0}
          >
            {loading ? "처리중..." : "매수"}
          </button>
          <button
            className="flex-1 bg-red-500 px-4 py-2 rounded-lg text-white font-semibold hover:bg-red-600 transition disabled:opacity-50 disabled:cursor-not-allowed"
            onClick={handleSell} // 매도 버튼 활성화
            disabled={loading || amount <= 0}
          >
            {loading ? "처리중..." : "매도"}
          </button>
        </div>
      </div>
    </div>
  );
}

export default TradeCard;
