import React from "react";

const TicketConfirmModal = ({ isOpen, onConfirm, onCancel }) => {
  return (
    <div className={`fixed inset-0 z-[1999] pointer-events-none ${!isOpen ? "pointer-events-none" : ""}`}>
      {/* 배경 오버레이 (클릭 시 닫기, blur 제거) */}
      {isOpen && (
        <div
          className="absolute inset-0 bg-transparent transition-opacity duration-300 pointer-events-auto"
          onClick={onCancel}
        />
      )}
      
      {/* 우측 슬라이드 인 모달 */}
      <div className="absolute right-0 top-0 h-full flex items-center pointer-events-auto">
        <div
          className={`bg-white rounded-l-3xl shadow-2xl border-l-4 border-blue-500 w-[400px] max-h-[90vh] transform transition-transform duration-300 ease-out ${
            isOpen ? "translate-x-0" : "translate-x-full"
          }`}
          onClick={(e) => e.stopPropagation()}
        >
          {/* 헤더 */}
          <div className="bg-gradient-to-r from-blue-600 to-blue-700 px-6 py-5 rounded-tl-3xl">
            <div className="flex items-center justify-between">
              <h3 className="text-xl font-black text-white tracking-tight">
                🎫 티켓 생성 확인
              </h3>
              <button
                onClick={onCancel}
                className="text-white/80 hover:text-white transition-colors text-2xl font-bold leading-none"
              >
                &times;
              </button>
            </div>
          </div>

          {/* 본문 */}
          <div className="p-6">
            <p className="text-gray-700 font-semibold text-base leading-relaxed mb-6">
              AI가 티켓 생성 문맥을 감지했습니다.
              <br />
              <span className="text-blue-600 font-bold">티켓 작성 모달을 열까요?</span>
            </p>

            {/* 버튼 영역 */}
            <div className="flex gap-3">
              <button
                onClick={onCancel}
                className="flex-1 bg-gray-100 text-gray-600 py-3 px-4 rounded-xl font-black text-sm uppercase tracking-wide hover:bg-gray-200 transition-all active:scale-95"
              >
                아니오
              </button>
              <button
                onClick={onConfirm}
                className="flex-1 bg-blue-600 text-white py-3 px-4 rounded-xl font-black text-sm uppercase tracking-wide hover:bg-blue-700 transition-all shadow-lg active:scale-95"
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

