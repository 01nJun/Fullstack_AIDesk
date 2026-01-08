import React, { useEffect, useRef, useState } from "react";

const TicketConfirmModal = ({ isOpen, onConfirm, onCancel }) => {
  const modalRef = useRef(null);
  const confirmButtonRef = useRef(null);
  const [shouldAnimate, setShouldAnimate] = useState(false);

  // ESC 키로 닫기, 엔터 키로 확인
  useEffect(() => {
    if (!isOpen) {
      setShouldAnimate(false);
      return;
    }

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        onCancel();
      } else if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        onConfirm();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    
    // 모달이 열릴 때 약간의 지연 후 애니메이션 적용
    // 먼저 초기 위치에 설정한 후, 다음 프레임에서 애니메이션 시작
    setShouldAnimate(false);
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        setShouldAnimate(true);
      });
    });

    // 포커스를 확인 버튼으로 이동
    setTimeout(() => {
      if (confirmButtonRef.current) {
        confirmButtonRef.current.focus();
      }
    }, 100);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, onConfirm, onCancel]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[1999]">
      {/* 배경 오버레이 */}
      <div
        className="absolute inset-0 bg-black/20 transition-opacity duration-200 ease-out"
        onClick={onCancel}
        style={{ opacity: shouldAnimate ? 1 : 0 }}
      />
      
      {/* 우측 슬라이드 인 모달 */}
      <div className="absolute right-0 top-0 h-full flex items-center">
        <div
          ref={modalRef}
          className="bg-baseBg rounded-l-ui shadow-lg border-l-4 border-brandNavy w-[400px] max-h-[90vh] transform transition-transform duration-200 ease-out"
          style={{ 
            transform: shouldAnimate ? 'translateX(0)' : 'translateX(100%)',
            willChange: 'transform'
          }}
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
                ref={confirmButtonRef}
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

