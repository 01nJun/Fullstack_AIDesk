import React from "react";

/**
 * AI 언어순화 경고 모달 (1단계)
 * - 10초 내 2회 욕설 감지 시 표시
 * - 토글 버튼 위에 배치
 * - 붉은 톤으로 강조
 */
const AiWarningModal = ({ isOpen, onSelectOn, onSelectOff }) => {
  if (!isOpen) return null;

  return (
    <>
      {/* 배경 오버레이 (입력 차단용) */}
      <div className="fixed inset-0 bg-black/30 z-[100]" />
      
      {/* 모달 패널 - 토글 버튼 위에 배치 */}
      <div className="fixed bottom-24 right-8 z-[101] animate-slide-up">
        <div className="bg-white rounded-ui shadow-xl border-2 border-red-400 w-[380px]">
          {/* 헤더 - 붉은 톤 */}
          <div className="bg-gradient-to-r from-red-500 to-orange-500 px-5 py-4 rounded-t-ui">
            <div className="flex items-center gap-2">
              <span className="text-2xl">⚠️</span>
              <h3 className="text-base font-semibold text-white">
                주의
              </h3>
            </div>
          </div>

          {/* 본문 */}
          <div className="p-6">
            <p className="text-baseText font-medium text-sm leading-relaxed mb-6">
              현재 AI 언어 순화 기능이 꺼져있어요.
              <br />
              <span className="text-red-600 font-semibold">지금 이 대화방의 상대를 확인해주세요.</span>
            </p>

            {/* 버튼 영역 */}
            <div className="flex gap-3">
              <button
                onClick={onSelectOff}
                className="flex-1 px-4 py-2.5 rounded-ui font-semibold text-sm border border-baseBorder text-baseText bg-white hover:bg-baseBg transition-all shadow-sm"
              >
                OFF 유지
              </button>
              <button
                onClick={onSelectOn}
                className="flex-1 px-4 py-2.5 rounded-ui font-semibold text-sm bg-gradient-to-r from-red-500 to-orange-500 text-white hover:opacity-90 transition-all shadow-md"
              >
                AI ON으로 전환
              </button>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

export default AiWarningModal;


