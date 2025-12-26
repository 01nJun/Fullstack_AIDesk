import React from 'react';

const CommonModal = ({ title, content, callbackFn, closeFn, isOpen }) => {
  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 z-[2000] flex items-center justify-center bg-black/60 backdrop-blur-sm transition-all"
      onClick={closeFn}
    >
      <div
        className="relative bg-white w-full max-w-[550px] rounded-[40px] shadow-[0_20px_50px_rgba(0,0,0,0.3)] overflow-hidden border border-gray-100 transform transition-all animate-in fade-in zoom-in duration-300"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header: 티켓 리스트 헤더 스타일 적용 */}
        <div className="bg-gray-900 p-6 flex justify-between items-center border-b-4 border-blue-500">
          <h2 className="text-xl font-black italic uppercase tracking-[0.2em] text-white">
            {title || 'Notification'}
          </h2>
          <button 
            onClick={closeFn}
            className="text-gray-400 hover:text-white transition-colors text-2xl font-black"
          >
            &times;
          </button>
        </div>

        {/* Body: 강렬한 텍스트 스타일 */}
        <div className="p-12 text-center">
          <div className="text-2xl font-black text-gray-800 tracking-tighter leading-tight whitespace-pre-wrap">
            {content}
          </div>
          {/* 하단 장식선 */}
          <div className="mt-8 flex justify-center">
            <div className="w-16 h-2 bg-blue-500 rounded-full"></div>
          </div>
        </div>

        {/* Footer: 버튼 디자인 통일 */}
        <div className="p-6 bg-gray-50 flex gap-4 justify-center items-center">
          {/* 취소 버튼: 보조 역할 */}
          {closeFn && (
            <button
              className="flex-1 bg-white text-gray-400 border-2 border-gray-200 py-4 rounded-2xl font-black text-lg hover:bg-gray-100 hover:text-gray-600 transition-all active:scale-95 shadow-sm"
              onClick={closeFn}
            >
              CANCEL
            </button>
          )}
          
          {/* 확인 버튼: 강조 역할 */}
          <button
            className="flex-1 bg-blue-600 text-white py-4 rounded-2xl font-black text-lg hover:bg-gray-900 hover:scale-[1.02] transition-all shadow-lg active:scale-95"
            onClick={() => {
              if (callbackFn) callbackFn();
              if (closeFn) closeFn();
            }}
          >
            CONFIRM
          </button>
        </div>

        {/* 데코레이션 요소 (선택 사항): 티켓 느낌을 주기 위한 작은 구멍 디테일 */}
        <div className="absolute top-1/2 -left-4 w-8 h-8 bg-black/60 rounded-full translate-y-[-50%] hidden md:block"></div>
        <div className="absolute top-1/2 -right-4 w-8 h-8 bg-black/60 rounded-full translate-y-[-50%] hidden md:block"></div>
      </div>
    </div>
  );
};

export default CommonModal;