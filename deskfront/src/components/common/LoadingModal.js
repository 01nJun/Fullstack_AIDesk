import React from 'react';

const LoadingModal = ({ isOpen, message = "처리 중입니다" }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center">
      {/* 배경 오버레이 */}
      <div className="absolute inset-0 bg-slate-900/40 backdrop-blur-md animate-in fade-in duration-300" />

      {/* 모달 카드 */}
      <div className="relative bg-white/90 backdrop-blur-2xl border border-white/40 rounded-[2.5rem] p-12 shadow-[0_25px_50px_-12px_rgba(0,0,0,0.25)] flex flex-col items-center max-w-xs w-full mx-4 transform animate-in zoom-in-95 duration-300">

        {/* 로딩 애니메이션 영역: 중앙 점 없이 회전 링만 배치 */}
        <div className="relative w-20 h-20 mb-8">
          {/* 바깥쪽 메인 링 (가장자리 그라데이션 효과) */}
          <div className="absolute inset-0 rounded-full border-[3px] border-transparent border-t-blue-600 border-r-blue-600 animate-spin" />

          {/* 안쪽 보조 링 (반대 방향 회전 및 연한 색상) */}
          <div className="absolute inset-2 rounded-full border-[3px] border-transparent border-b-indigo-400 border-l-indigo-400 animate-spin-reverse opacity-60" />

          {/* 가장 바깥쪽 아주 연한 가이드 라인 (원형 윤곽선) */}
          <div className="absolute inset-0 rounded-full border-[3px] border-gray-100" />
        </div>

        {/* 텍스트 영역 */}
        <div className="text-center">
          <h3 className="text-lg font-bold text-gray-800 tracking-tight mb-2">
            {message}
          </h3>
          <div className="flex items-center justify-center space-x-1.5">
            <span className="text-sm font-medium text-gray-400">잠시만 기다려주세요</span>
            {/* 세련된 점 애니메이션 */}
            <div className="flex space-x-1">
              <div className="w-1 h-1 bg-blue-500 rounded-full animate-bounce [animation-delay:-0.3s]"></div>
              <div className="w-1 h-1 bg-blue-500 rounded-full animate-bounce [animation-delay:-0.15s]"></div>
              <div className="w-1 h-1 bg-blue-500 rounded-full animate-bounce"></div>
            </div>
          </div>
        </div>
      </div>

      <style jsx>{`
        /* 역방향 회전 애니메이션 */
        @keyframes spin-reverse {
          from { transform: rotate(360deg); }
          to { transform: rotate(0deg); }
        }
        .animate-spin-reverse {
          animation: spin-reverse 1.5s linear infinite;
        }

        /* 등장 애니메이션 */
        @keyframes fade-in {
          from { opacity: 0; }
          to { opacity: 1; }
        }
        @keyframes zoom-in-95 {
          from { opacity: 0; transform: scale(0.95) translateY(10px); }
          to { opacity: 1; transform: scale(1) translateY(0); }
        }
        .fade-in { animation: fade-in 0.3s ease-out forwards; }
        .zoom-in-95 { animation: zoom-in-95 0.4s cubic-bezier(0.16, 1, 0.3, 1) forwards; }
      `}</style>
    </div>
  );
};

export default LoadingModal;