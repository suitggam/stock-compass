import type { MyPageData } from '../../types/MyPageData';
import { Link } from 'react-router';

type Props = {
  data?: MyPageData['personality'] | null;
};

export default function PersonalityResult({ data }: Props) {
  if (!data) {
    return (
      <section className="w-full bg-slate-700 backdrop-blur-xl rounded-2xl shadow-lg p-6 border border-slate-600 relative">
        <div className="text-center space-y-4">
          <h2 className="text-2xl font-extrabold text-white mb-2">투자 성격 유형 진단 결과</h2>

          {/* 빈 상태 아이콘 */}
          <div className="flex justify-center mb-4">
            <div className="w-16 h-16 rounded-full bg-slate-600 flex items-center justify-center">
              <svg
                width="32"
                height="32"
                viewBox="0 0 24 24"
                fill="none"
                className="text-slate-400"
              >
                <path
                  d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 15h2v2h-2v-2zm0-8h2v6h-2V9z"
                  fill="currentColor"
                />
              </svg>
            </div>
          </div>

          <p className="text-slate-400 mb-6">아직 진단을 받지 않으셨습니다.</p>

          <Link
            to="/game"
            className="px-6 py-3 bg-gradient-to-r from-amber-500 to-amber-600 text-white font-semibold rounded-lg hover:from-amber-600 hover:to-amber-700 transition-all shadow-lg"
          >
            성향 진단 시작하기
          </Link>
        </div>
      </section>
    );
  }

  // 간단 버전: 타입/요약만 표시 (세부 막대그래프 등은 나중에 점수 받아서 그릴 때 복원)
  return (
    <section className="w-full bg-slate-700 backdrop-blur-xl rounded-2xl shadow-lg p-6 border border-slate-600 relative">
      <div className="absolute inset-x-0 top-0 h-1 rounded-t-2xl bg-gradient-to-r from-amber-500 to-amber-600" />

      <div className="text-center space-y-4">
        <h2 className="text-2xl font-extrabold text-white mb-2">투자 성격 유형 진단 결과</h2>

        {/* 결과 표시 */}
        <div className="space-y-4">
          {/* 성향 타입 */}
          <div className="p-4 bg-gradient-to-r from-amber-500/20 to-amber-600/20 rounded-xl border border-amber-400/30">
            <div className="text-3xl font-extrabold text-amber-300 mb-2">{data.type}</div>
            <p className="text-slate-300 leading-relaxed">{data.summary}</p>
          </div>

          {/* 추가 정보가 있다면 표시할 수 있는 공간 */}
          <div className="flex justify-center">
            <button className="text-amber-400 hover:text-amber-300 text-sm font-medium transition-colors">
              상세 결과 보기 →
            </button>
          </div>
        </div>
      </div>
    </section>
  );
}
