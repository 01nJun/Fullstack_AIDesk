import { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { getTicketDetailByTno } from "../../api/ticketApi";
import { useTicketActions } from "../../hooks/useTicketActions";
import { getGradeBadge, getStateLabel, formatDate } from "../../util/ticketUtils";
import useCustomPin from "../../hooks/useCustomPin";

// 티켓 상세 조회 모달
const TicketDetailModal = ({ tno, onClose, onDelete }) => {
  // Redux에서 현재 로그인 사용자 정보 가져오기
  const loginState = useSelector((state) => state.loginSlice);
  const currentEmail = loginState.email;

  const {togglePin, isPinned} = useCustomPin();

  const [ticket, setTicket] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isWriter, setIsWriter] = useState(false);
  const [isReceiver, setIsReceiver] = useState(false);

  // 티켓 상세 조회 (tno)
  useEffect(() => {
    if (!tno || !currentEmail) return;

    const fetchTicket = async () => {
      try {
        setLoading(true);
        const result = await getTicketDetailByTno(tno, currentEmail);
        setTicket(result.ticket);
        setIsWriter(result.isWriter);
        setIsReceiver(result.isReceiver);
        setLoading(false);
      } catch (err) {
        console.error("티켓 상세 조회 실패:", err);
        alert("조회에 실패했습니다. 다시 시도해보세요.");
        onClose();
      }
    };

    fetchTicket();
  }, [tno, currentEmail, onClose]);

  // 티켓 액션 커스텀 훅
  const { handleStateChange, handleDelete } = useTicketActions(
    ticket,
    currentEmail,
    isReceiver,
    setTicket,
    onDelete,
    onClose
  );

  // ESC 키로 모달 닫기
  useEffect(() => {
    const handleEsc = (e) => {
      if (e.key === "Escape") {
        onClose();
      }
    };
    window.addEventListener("keydown", handleEsc);
    return () => window.removeEventListener("keydown", handleEsc);
  }, [onClose]);

  // 요구사항 파싱 (줄바꿈으로 구분된 문자열을 배열로)
  const parseRequirements = (requirement) => {
    if (!requirement) return [];
    return requirement
      .split(/\n|<br\s*\/?>/i)
      .map((item) => item.trim())
      .filter((item) => item.length > 0);
  };

  // 타입 판별 (state 값으로 판별)
  const isReceivedType = isReceiver && ticket && ticket.pno !== undefined;
  const isSentType = isWriter && ticket && ticket.personals !== undefined;

  if (loading) {
    return (
      <div
        className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50"
        onClick={onClose}
      >
        <div
          className="bg-white rounded-lg p-8 shadow-xl"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="text-lg">로딩 중...</div>
        </div>
      </div>
    );
  }

  if (!ticket) {
    return null;
  }

  const requirements = parseRequirements(ticket.requirement);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50 p-4"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-lg shadow-xl w-full max-w-6xl max-h-[90vh] overflow-hidden flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200 bg-white">
          <div className="flex items-center flex-1">
            <button
              onClick={onClose}
              className="mr-4 text-gray-600 hover:text-gray-900 text-xl font-bold"
              aria-label="닫기"
            >
              ←
            </button>
            <h2 className="text-2xl font-bold text-gray-800 flex-1">
              {ticket.title}
            </h2>
            {ticket.grade && (
              <div className="ml-3">{getGradeBadge(ticket.grade)}</div>
            )}
            {/* 찜하기 버튼 */}
            <button
              onClick={() => togglePin(ticket.tno)}
              className={`ml-4 text-2xl transition-all hover:scale-125 ${
                isPinned(ticket.tno)
                  ? "text-yellow-500"
                  : "text-gray-300 hover:text-yellow-200"
              }`}
              aria-label={isPinned(ticket.tno) ? "찜 해제" : "찜하기"}
            >
              {isPinned(ticket.tno) ? "★" : "☆"}
            </button>
          </div>
        </div>

        {/* Content - Scrollable */}
        <div className="flex-1 overflow-y-auto p-6">
          <div className="grid grid-cols-3 gap-6">
            {/* 좌측 메인 영역 */}
            <div className="col-span-2 space-y-6">
              {/* 요청 요약 */}
              <div className="bg-gray-50 rounded-lg p-5 border border-gray-200">
                <h3 className="text-lg font-semibold text-gray-800 mb-3">
                  요청 요약
                </h3>
                <p className="text-gray-700 whitespace-pre-wrap">
                  {ticket.content || "내용이 없습니다."}
                </p>
              </div>

              {/* 요청 배경/목적 */}
              <div className="bg-gray-50 rounded-lg p-5 border border-gray-200">
                <h3 className="text-lg font-semibold text-gray-800 mb-3">
                  요청 배경/목적
                </h3>
                <p className="text-gray-700 whitespace-pre-wrap">
                  {ticket.purpose || "내용이 없습니다."}
                </p>
              </div>

              {/* 요구사항 */}
              {requirements.length > 0 && (
                <div className="bg-gray-50 rounded-lg p-5 border border-gray-200">
                  <h3 className="text-lg font-semibold text-gray-800 mb-3">
                    요구사항
                  </h3>
                  <ul className="space-y-2">
                    {requirements.map((req, index) => (
                      <li key={index} className="flex items-start">
                        <span className="bg-blue-100 text-blue-800 px-3 py-1 rounded-full text-sm font-medium mr-3 min-w-[2rem] text-center">
                          {index + 1}
                        </span>
                        <span className="text-gray-700 flex-1 pt-1">
                          {req}
                        </span>
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {/* 수신자 목록 (보낸 티켓만) */}
              {isSentType && ticket.personals && ticket.personals.length > 0 && (
                <div className="bg-gray-50 rounded-lg p-5 border border-gray-200">
                  <h3 className="text-lg font-semibold text-gray-800 mb-3">
                    수신자 목록
                  </h3>
                  <div className="space-y-2">
                    {ticket.personals.map((personal, index) => (
                      <div
                        key={personal.pno || index}
                        className="flex items-center justify-between p-3 bg-white rounded border border-gray-200"
                      >
                        <div className="flex items-center space-x-3">
                          <span className="text-gray-700 font-medium">
                            {personal.receiver || "-"}
                          </span>
                          {personal.isread && (
                            <span className="text-xs bg-green-100 text-green-800 px-2 py-1 rounded">
                              읽음
                            </span>
                          )}
                        </div>
                        <span
                          className={`px-3 py-1 rounded text-sm font-medium ${
                            personal.state === "DONE"
                              ? "bg-green-100 text-green-800"
                              : personal.state === "IN_PROGRESS"
                              ? "bg-blue-100 text-blue-800"
                              : personal.state === "NEED_INFO"
                              ? "bg-yellow-100 text-yellow-800"
                              : "bg-gray-100 text-gray-800"
                          }`}
                        >
                          {getStateLabel(personal.state)}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* 첨부 파일 */}
              <div className="bg-gray-50 rounded-lg p-5 border border-gray-200">
                <h3 className="text-lg font-semibold text-gray-800 mb-3">
                  첨부 파일
                </h3>
                <p className="text-gray-500 text-sm">
                  첨부 파일 기능은 추후 구현 예정입니다.
                </p>
              </div>
            </div>

            {/* 우측 사이드 영역 */}
            <div className="col-span-1 space-y-6">
              {/* 세부 정보 */}
              <div className="bg-gray-50 rounded-lg p-5 border border-gray-200">
                <h3 className="text-lg font-semibold text-gray-800 mb-4">
                  세부 정보
                </h3>
                <div className="space-y-4">
                  <div className="flex items-center">
                    <span className="text-gray-500 w-20 flex-shrink-0">
                      {isReceivedType ? "요청자" : "작성자"}
                    </span>
                    <span className="text-gray-800 font-medium">
                      {ticket.writer || "-"}
                    </span>
                  </div>
                  {isReceivedType && (
                    <div className="flex items-center">
                      <span className="text-gray-500 w-20 flex-shrink-0">
                        담당자
                      </span>
                      <span className="text-gray-800 font-medium">
                        {ticket.receiver || "-"}
                      </span>
                    </div>
                  )}
                  <div className="flex items-center">
                    <span className="text-gray-500 w-20 flex-shrink-0">
                      생성일
                    </span>
                    <span className="text-gray-800 font-medium">
                      {formatDate(ticket.birth)}
                    </span>
                  </div>
                  <div className="flex items-center">
                    <span className="text-gray-500 w-20 flex-shrink-0">
                      마감일
                    </span>
                    <span className="text-gray-800 font-medium">
                      {formatDate(ticket.deadline)}
                    </span>
                  </div>
                  {isSentType && (
                    <div className="flex items-center">
                      <span className="text-gray-500 w-20 flex-shrink-0">
                        수신자 수
                      </span>
                      <span className="text-gray-800 font-medium">
                        {ticket.personals?.length || 0}명
                      </span>
                    </div>
                  )}
                </div>
              </div>

              {/* 상태 (받은 티켓만) */}
              {isReceivedType && (
                <div className="bg-gray-50 rounded-lg p-5 border border-gray-200">
                  <h3 className="text-lg font-semibold text-gray-800 mb-4">
                    상태
                  </h3>
                  <select
                    value={ticket.state || "NEW"}
                    onChange={(e) => handleStateChange(e.target.value)}
                    disabled={!isReceiver}
                    className={`w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                      !isReceiver
                        ? "bg-gray-100 text-gray-500 cursor-not-allowed"
                        : "bg-white text-gray-800 cursor-pointer"
                    }`}
                  >
                    <option value="NEW">신규</option>
                    <option value="IN_PROGRESS">진행 중</option>
                    <option value="NEED_INFO">정보 필요</option>
                    <option value="DONE">완료</option>
                  </select>
                </div>
              )}

              {/* 삭제 버튼 (발신자만) */}
              {isWriter && (
                <div className="pt-4">
                  <button
                    onClick={handleDelete}
                    className="text-red-600 hover:text-red-800 text-sm font-medium underline"
                  >
                    (보낸사람) 삭제
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TicketDetailModal;