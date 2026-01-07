import { useState, useEffect, useCallback, useRef } from "react";

/**
 * 채팅 메시지 무한 스크롤 훅
 * @param {Array} messages - 전체 메시지 배열
 * @param {number} pageSize - 한 번에 로드할 메시지 개수 (기본값: 30)
 * @returns {Object} { visibleMessages, onScroll, scrollToBottom, reset }
 */
const useInfiniteChat = (messages = [], pageSize = 30) => {
  const [visibleCount, setVisibleCount] = useState(pageSize);
  const prevMessagesLengthRef = useRef(messages.length);
  const containerRefRef = useRef(null);

  // visibleMessages: 최신 visibleCount개만 표시
  const visibleMessages = messages.slice(Math.max(0, messages.length - visibleCount));

  // 스크롤 핸들러
  const onScroll = useCallback(
    (e) => {
      const el = e.target;
      if (!el) return;

      // 위로 스크롤하여 이전 메시지 로드
      if (el.scrollTop < 80 && visibleCount < messages.length) {
        const prevHeight = el.scrollHeight;
        const prevScrollTop = el.scrollTop;

        setVisibleCount((prev) => {
          const next = Math.min(messages.length, prev + pageSize);
          // 스크롤 위치 보정
          requestAnimationFrame(() => {
            const newHeight = el.scrollHeight;
            if (newHeight > prevHeight) {
              el.scrollTop = prevScrollTop + (newHeight - prevHeight);
            }
          });
          return next;
        });
      }
    },
    [messages.length, visibleCount, pageSize]
  );

  // 맨 아래로 스크롤
  const scrollToBottom = useCallback(() => {
    const el = containerRefRef.current;
    if (el) {
      el.scrollTop = el.scrollHeight;
    }
  }, []);

  // 초기화 (방 변경 시)
  const reset = useCallback(() => {
    setVisibleCount(pageSize);
    prevMessagesLengthRef.current = 0;
  }, [pageSize]);

  // 초기 로드 시 맨 아래로 스크롤
  useEffect(() => {
    if (messages.length > 0 && prevMessagesLengthRef.current === 0) {
      // 첫 로드 시
      setVisibleCount(Math.min(pageSize, messages.length));
      setTimeout(() => scrollToBottom(), 100);
      prevMessagesLengthRef.current = messages.length;
    }
  }, [messages.length, pageSize, scrollToBottom]);

  // 새 메시지 추가 시 자동으로 맨 아래로 스크롤
  useEffect(() => {
    const prevLength = prevMessagesLengthRef.current;
    const currentLength = messages.length;

    // 새 메시지가 추가된 경우 (맨 뒤에 추가)
    if (currentLength > prevLength && prevLength > 0) {
      // visibleCount도 증가시켜 새 메시지가 보이게 함
      if (visibleCount < currentLength) {
        setVisibleCount(Math.min(currentLength, visibleCount + (currentLength - prevLength)));
      }
      // 맨 아래로 스크롤
      setTimeout(() => scrollToBottom(), 50);
    }

    prevMessagesLengthRef.current = currentLength;
  }, [messages.length, visibleCount, scrollToBottom]);

  // containerRef 설정 함수
  const setContainerRef = useCallback((ref) => {
    containerRefRef.current = ref;
  }, []);

  return {
    visibleMessages,
    onScroll,
    scrollToBottom,
    reset,
    setContainerRef,
  };
};

export default useInfiniteChat;

