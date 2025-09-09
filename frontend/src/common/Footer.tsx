export default function Footer() {
  return (
    <footer className="bg-slate-800 text-white mt-auto">
      <div className="max-w-7xl mx-auto px-6 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          {/* 서비스 소개 */}
          <div className="col-span-1 md:col-span-2">
            <div className="flex items-center space-x-3 mb-4">
              <div className="w-8 h-8 bg-gradient-to-br from-amber-400 to-amber-600 rounded-lg flex items-center justify-center">
                <span className="text-slate-900 font-bold">₩</span>
              </div>
              <h3 className="text-xl font-bold">TetoBoys</h3>
            </div>
            <p className="text-slate-300 mb-4 leading-relaxed">
              안전하고 재미있는 모의 투자 게임으로 투자 실력을 키워보세요.
            </p>
            <p className="text-slate-300 mb-4 leading-relaxed">
              실제 주식 데이터를 기반으로 한 리얼한 투자 경험을 제공합니다.
            </p>
          </div>

          {/* 정보 메뉴 */}
          <div>
            <h4 className="font-semibold text-white mb-4">정보</h4>
            <ul className="space-y-2 text-slate-300">
              <li>
                <p className="transition-colors">팀 이름 : 테토보이즈</p>
              </li>
              <li>
                <p className="transition-colors">팀장 : 김종재</p>
              </li>
              <li>
                <p className=" transition-colors whitespace-nowrap">
                  팀원 : 지성현, 이상용, 김대정, 장동현, 정연수
                </p>
              </li>
            </ul>
          </div>
        </div>

        {/* 투자 경고 문구 */}
        <div className="mt-6 p-4 bg-slate-700 rounded-lg border-l-4 border-amber-500">
          <p className="text-sm text-slate-300">
            ⚠️{" "}
            <span className="font-medium text-amber-400">투자 유의사항 : </span>
            본 서비스는 모의 투자 게임으로, 실제 투자와는 다를 수 있습니다. 실제
            투자 시에는 원금 손실 위험이 있으니 신중하게 결정하시기 바랍니다.
          </p>
        </div>
      </div>
    </footer>
  );
}
