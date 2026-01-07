import React from 'react';

const CommonModal = ({ title, content, callbackFn, closeFn, isOpen }) => {
  if (!isOpen) return null;

  return (
    <div
      className="ui-modal-overlay"
      onClick={closeFn}
    >
      <div
        className="ui-modal-panel max-w-[550px]"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="ui-modal-header flex justify-between items-center">
          <h2 className="ui-title">
            {title || '알림'}
          </h2>
          <button 
            onClick={closeFn}
            className="text-baseMuted hover:text-baseText transition-colors text-2xl font-bold leading-none w-8 h-8 flex items-center justify-center"
            aria-label="닫기"
          >
            &times;
          </button>
        </div>

        {/* Body */}
        <div className="ui-modal-body text-center">
          <div className="text-base leading-relaxed text-baseText whitespace-pre-wrap">
            {content}
          </div>
        </div>

        {/* Footer */}
        <div className="ui-modal-footer">
          {/* 취소 버튼 */}
          {closeFn && (
            <button
              className="ui-btn-secondary"
              onClick={closeFn}
            >
              취소
            </button>
          )}
          
          {/* 확인 버튼 */}
          <button
            className="ui-btn-primary"
            onClick={() => {
              if (callbackFn) callbackFn();
              if (closeFn) closeFn();
            }}
          >
            확인
          </button>
        </div>
      </div>
    </div>
  );
};

export default CommonModal;