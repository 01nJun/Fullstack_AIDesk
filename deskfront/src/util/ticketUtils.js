// 티켓 관련 유틸리티

// 중요도 배지 JSX 반환
export const getGradeBadge = (grade) => {
  const gradeMap = {
    LOW: { text: "낮음", className: "ui-badge-grade-low" },
    MIDDLE: { text: "보통", className: "ui-badge-grade-middle" },
    HIGH: { text: "높음", className: "ui-badge-grade-high" },
    URGENT: { text: "긴급", className: "ui-badge-grade-urgent" },
  };
  const gradeInfo = gradeMap[grade] || gradeMap.MIDDLE;
  return (
    <span className={`${gradeInfo.className} ml-3`}>
      {gradeInfo.text}
    </span>
  );
};

// 상태 코드 한글 라벨 변환
export const getStateLabel = (state) => {
  const stateMap = {
    NEW: "신규",
    IN_PROGRESS: "진행 중",
    NEED_INFO: "정보 필요",
    DONE: "완료",
  };
  return stateMap[state] || state;
};

// 날짜 문자열을 한국어 형식으로 포맷팅
export const formatDate = (dateString) => {
  if (!dateString) return "-";
  try {
    const date = new Date(dateString);
    return date.toLocaleDateString("ko-KR", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    });
  } catch (e) {
    return dateString;
  }
};

