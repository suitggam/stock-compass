// src/components/mypage/PersonalityResult.tsx
import type { MyPageData } from '../../types/MyPageData';

type Props = {
  data?: MyPageData['personality'] | null;
};

export default function PersonalityResult({ data }: Props) {
  if (!data) {
    return (
      <section className="w-full bg-white/80 backdrop-blur-xl rounded-2xl shadow-[0_20px_60px_rgba(0,0,0,0.08)] p-6 border border-black/5 relative">
        <div className="absolute inset-x-0 top-0 h-1 rounded-t-2xl bg-gradient-to-r from-amber-500 via-amber-400 to-black" />
        <h2 className="text-center text-2xl font-extrabold text-neutral-900 mb-2">
          투자 성격 유형 진단 결과
        </h2>
        <p className="text-center text-sm text-neutral-500">아직 결과가 없습니다.</p>
      </section>
    );
  }

  // 간단 버전: 타입/요약만 표시 (세부 막대그래프 등은 나중에 점수 받아서 그릴 때 복원)
  return (
    <section className="w-full bg-white/80 backdrop-blur-xl rounded-2xl shadow-[0_20px_60px_rgba(0,0,0,0.08)] p-6 border border-black/5 relative">
      <div className="absolute inset-x-0 top-0 h-1 rounded-t-2xl bg-gradient-to-r from-amber-500 via-amber-400 to-black" />
      <h2 className="text-center text-2xl font-extrabold text-neutral-900 mb-2">
        투자 성격 유형 진단 결과
      </h2>
      <div className="text-center space-y-1.5">
        <div className="text-3xl font-extrabold text-amber-600">{data.type}</div>
        <p className="text-neutral-600">{data.summary}</p>
      </div>
    </section>
  );
}
