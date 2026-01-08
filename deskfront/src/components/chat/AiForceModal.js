import React from "react";

/**
 * AI 언어순화 강제 활성화 모달 (2단계)
 * - 1단계에서 OFF 선택 후 또 욕설 사용 시 표시
 * - 토글 버튼 위에 배치
 * - 부드러운 어조로 안내
 */
const AiForceModal = ({ isOpen, onConfirm }) => {
  if (!isOpen) return null;

  return (
    <>
      {/* 배경 오버레이 */}
      <div className="fixed inset-0 bg-black/30 z-[100]" />
      
      {/* 모달 패널 - 토글 버튼 위에 배치 */}
      <div className="fixed bottom-24 right-8 z-[101] animate-slide-up">
        <div className="bg-white rounded-ui shadow-xl border-2 border-orange-400 w-[380px]">
          {/* 헤더 - 부드러운 오렌지 톤 */}
          <div className="bg-gradient-to-r from-orange-400 to-amber-400 px-5 py-4 rounded-t-ui">
            <div className="flex items-center gap-2">
              <span className="text-2xl">💬</span>
              <h3 className="text-base font-semibold text-white">
                안내
              </h3>
            </div>
          </div>

          {/* 본문 */}
          <div className="p-6">
            <p className="text-baseText font-medium text-sm leading-relaxed mb-6">
              사내에서는 서로 존중하는 언어를 사용하도록 해요.
              <br />
              <span className="text-orange-600 font-semibold">잠시 동안 AI 언어 순화 기능이 자동으로 켜집니다.</span>
            </p>

            {/* 버튼 영역 */}
            <div className="flex">
              <button
                onClick={onConfirm}
                className="w-full px-4 py-2.5 rounded-ui font-semibold text-sm bg-gradient-to-r from-orange-400 to-amber-400 text-white hover:opacity-90 transition-all shadow-md"
              >
                확인
              </button>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};

export default AiForceModal;


