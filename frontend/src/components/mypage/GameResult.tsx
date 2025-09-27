import type { MyPageData } from "../../types/MyPageData";
import { Link } from "react-router";

type Props = {
  data?: MyPageData["gameResult"] | null;
};

type PairKey = "I_E" | "S_N" | "F_T" | "P_J";

export default function GameResult({ data }: Props) {
  if (!data) {
    return (
      <section className="w-full bg-slate-700 backdrop-blur-xl rounded-2xl shadow-lg p-6 border border-slate-600 relative">
        <div className="text-center space-y-4">
          <h2 className="text-2xl font-extrabold text-white mb-2">
            투자 성격 유형 진단 결과
          </h2>

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

  const clamp = (n: number) => Math.max(0, Math.min(100, Math.round(n)));

  const labels = {
    I: "반응형",
    E: "마이웨이형",
    S: "신중형",
    N: "무심형",
    F: "단타형",
    T: "장타형",
    P: "재능형",
    J: "노력형",
  } as const;

  const rows: Array<{
    key: PairKey;
    leftCode: keyof typeof labels;
    rightCode: keyof typeof labels;
    leftPct: number;
    rightPct: number;
    gradient: string;
  }> = [
    {
      key: "I_E",
      leftCode: "I",
      rightCode: "E",
      leftPct: clamp(Number(data.tendencyI) || 0),
      rightPct: clamp(Number(data.tendencyE) || 0),
      gradient: "from-rose-400 to-rose-500",
    },
    {
      key: "S_N",
      leftCode: "S",
      rightCode: "N",
      leftPct: clamp(Number(data.tendencyS) || 0),
      rightPct: clamp(Number(data.tendencyN) || 0),
      gradient: "from-emerald-400 to-teal-500",
    },
    {
      key: "F_T",
      leftCode: "F",
      rightCode: "T",
      leftPct: clamp(Number(data.tendencyF) || 0),
      rightPct: clamp(Number(data.tendencyT) || 0),
      gradient: "from-lime-400 to-green-500",
    },
    {
      key: "P_J",
      leftCode: "P",
      rightCode: "J",
      leftPct: clamp(Number(data.tendencyP) || 0),
      rightPct: clamp(Number(data.tendencyJ) || 0),
      gradient: "from-pink-400 to-rose-400",
    },
  ];

  const typeTag = (data.tendencyResult || "").toUpperCase();
  const briefDesc = makeBriefDesc(typeTag);

  return (
    <section className="w-full bg-slate-700 backdrop-blur-xl rounded-2xl shadow-lg p-6 border border-slate-600 relative">
      <div className="space-y-6">
        <header className="text-center">
          <h2 className="text-2xl font-extrabold text-white">
            투자 성격 유형 진단 결과
          </h2>
        </header>

        {/* 결과 요약 */}
        <div className="p-4 bg-gradient-to-r from-amber-500/20 to-amber-600/20 rounded-xl border border-amber-400/30">
          <div className="flex items-center justify-between flex-wrap gap-3">
            <div>
              <div className="text-3xl font-extrabold text-amber-300">
                {typeTag || "결과 미정"}
              </div>
              <p className="text-slate-200/90 mt-1">{briefDesc}</p>
            </div>

            {/* 태그형 배지들 */}
            <div className="flex flex-wrap gap-2">
              {Array.from(typeTag).map((c) => (
                <span
                  key={c}
                  className="px-3 py-1 rounded-full bg-amber-500/20 text-amber-200 border border-amber-300/30 text-sm font-semibold"
                >
                  {c} · {labels[c as keyof typeof labels]}
                </span>
              ))}
            </div>
          </div>
        </div>

        {/* 막대 차트 */}
        <div className="bg-slate-800/40 rounded-xl p-4 border border-slate-600/60">
          <ul className="space-y-5">
            {rows.map((r) => (
              <li key={r.key} className="space-y-2">
                {/* 상단 레이블 줄 */}
                <div className="flex justify-between text-xs text-slate-300">
                  <div className="flex items-center gap-2">
                    <span className="font-bold">{r.leftCode}</span>
                    <span className="opacity-80">{labels[r.leftCode]}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="opacity-80 text-right">
                      {labels[r.rightCode]}
                    </span>
                    <span className="font-bold">{r.rightCode}</span>
                  </div>
                </div>

                {/* 프로그레스 바 */}
                <div className="relative h-4 w-full bg-slate-200/20 rounded-full overflow-hidden">
                  <div
                    className={`absolute left-0 top-0 h-full bg-gradient-to-r ${r.gradient}`}
                    style={{ width: `${r.leftPct}%` }}
                  />
                </div>

                {/* 하단 퍼센트 줄 */}
                <div className="flex justify-between text-xs text-slate-300">
                  <span>{r.leftPct}%</span>
                  <span>{r.rightPct}%</span>
                </div>
              </li>
            ))}
          </ul>
        </div>

        {/* 하단: 마지막 진단 일자 */}
        <div className="flex justify-center">
          <div className="text-slate-400 text-sm flex items-center gap-2">
            <svg
              width="16"
              height="16"
              viewBox="0 0 24 24"
              fill="none"
              className="text-slate-400"
            >
              <path
                d="M7 2v2M17 2v2M4 8h16M5 12h14M5 16h10M4 6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6Z"
                stroke="currentColor"
                strokeWidth="1.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
            <span>
              마지막 진단 일자&nbsp;·&nbsp;{formatDateTime(data.createdAt)}
            </span>
          </div>
        </div>
      </div>
    </section>
  );
}

function formatDateTime(isoLike: string) {
  if (!isoLike) return "";
  try {
    const d = new Date(isoLike);
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, "0");
    const dd = String(d.getDate()).padStart(2, "0");
    const hh = String(d.getHours()).padStart(2, "0");
    const mi = String(d.getMinutes()).padStart(2, "0");
    return `${yyyy}-${mm}-${dd} ${hh}:${mi}`;
  } catch {
    return isoLike;
  }
}

// 결과 설명여기에 하드코딩 박자 ㅋㅋㅋ
function makeBriefDesc(type: string) {
  const t = type.toUpperCase();
  if (!t) return "진단 결과를 확인했습니다.";
  const map: Record<string, string> = {
    ISTJ: "신중하고 체계적인 분석형 투자 성향",
    ISFJ: "책임감 있고 안정 지향의 수호자형 성향",
    INFJ: "통찰로 큰 흐름을 보는 이상가형 성향",
    INTJ: "장기 관점의 전략가형 투자 성향",
    ISTP: "데이터 기반의 실용적·기술가형 성향",
    ISFP: "감각에 강하고 유연한 실전가형 성향",
    INFP: "가치와 스토리를 중시하는 탐색가형 성향",
    INTP: "가설·데이터 중심의 탐구형 투자 성향",
    ESTP: "기회 포착에 능한 민첩한 실전형 성향",
    ESFP: "현장 감각이 뛰어난 실전형 성향",
    ENFP: "직관과 트렌드에 민감한 기회 포착형",
    ENTP: "아이디어와 변동성에 강한 도전형",
    ESTJ: "원칙과 규율을 중시하는 관리자형",
    ESFJ: "안정과 합의를 중시하는 조정가형 성향",
    ENFJ: "조직력과 리더십이 돋보이는 촉진자형 성향",
    ENTJ: "결단력 있는 리더형 투자 성향",
  };
  return map[t] ?? "당신의 투자 성향을 한눈에 보여주는 결과입니다.";
}
