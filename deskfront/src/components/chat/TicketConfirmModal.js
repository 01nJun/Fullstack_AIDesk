import React from "react";

const TicketConfirmModal = ({ isOpen, onConfirm, onCancel }) => {
  // 모달이 닫혀있으면 아예 렌더링하지 않음
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[1999]">
      {/* 배경 오버레이 */}
      <div
        className="absolute inset-0 bg-transparent transition-opacity duration-300"
        onClick={onCancel}
      />
      
      {/* 우측 슬라이드 인 모달 */}
      <div className="absolute right-0 top-0 h-full flex items-center">
        <div
          className={`bg-baseBg rounded-l-ui shadow-lg border-l-4 border-brandNavy w-[400px] max-h-[90vh] transform transition-transform duration-300 ease-out ${
            isOpen ? "translate-x-0" : "translate-x-full"
          }`}
          onClick={(e) => e.stopPropagation()}
        >
          {/* 헤더 */}
          <div className="bg-brandNavy px-6 py-4 rounded-tl-ui">
            <div className="flex items-center justify-between">
              <h3 className="text-base font-semibold text-white">
                🎫 티켓 생성 확인
              </h3>
              <button
                onClick={onCancel}
                className="text-white/80 hover:text-white transition-colors text-xl font-bold leading-none"
              >
                &times;
              </button>
            </div>
          </div>

          {/* 본문 */}
          <div className="p-6">
            <p className="text-baseText font-medium text-sm leading-relaxed mb-6">
              AI가 티켓 생성 문맥을 감지했습니다.
              <br />
              <span className="text-brandNavy font-semibold">티켓 작성 모달을 열까요?</span>
            </p>

            {/* 버튼 영역 */}
            <div className="flex gap-3">
              <button
                onClick={onCancel}
                className="ui-btn-secondary flex-1"
              >
                아니오
              </button>
              <button
                onClick={onConfirm}
                className="ui-btn-primary flex-1"
              >
                예
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TicketConfirmModal;

