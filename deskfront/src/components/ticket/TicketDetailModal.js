import { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { getTicketDetailByTno } from "../../api/ticketApi";
import { downloadFile } from "../../api/fileApi";
import { useTicketActions } from "../../hooks/useTicketActions";
import { getGradeBadge, getStateLabel, formatDate } from "../../util/ticketUtils";
import FilePreview from "../common/FilePreview";
import useCustomPin from "../../hooks/useCustomPin";

const TicketDetailModal = ({ tno, onClose, onDelete }) => {
  const loginState = useSelector((state) => state.loginSlice);
  const currentEmail = loginState.email;
  const { togglePin, isPinned } = useCustomPin();

  const [ticket, setTicket] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isWriter, setIsWriter] = useState(false);
  const [isReceiver, setIsReceiver] = useState(false);

  // TicketDetailModal.js 내부의 useEffect 수정
  useEffect(() => {
    if (!tno || !currentEmail) return;

    const fetchTicket = async () => {
      try {
        setLoading(true);
        const result = await getTicketDetailByTno(tno, currentEmail);

        if (result.ticket && result.ticket.files) {
          const nameMap = {};
          result.ticket.files = result.ticket.files.map((f) => {
            // [체크!] 수신 데이터에 uuid와 fileName이 있는지 확인하기 위한 로그
            // console.log("파일 정보 확인:", f);

            const name = f.fileName || "unknown"; // fileName이 없을 경우 대비

            // 중복 파일명 처리 로직은 유지하되, 원본 객체의 모든 속성(...f)을 반드시 유지해야 함
            let finalDisplayName = name;
            if (!nameMap[name]) {
              nameMap[name] = 1;
              finalDisplayName = name;
            } else {
              const count = nameMap[name]++;
              const dot = name.lastIndexOf(".");
              const b = dot !== -1 ? name.substring(0, dot) : name;
              const e = dot !== -1 ? name.substring(dot) : "";
              finalDisplayName = `${b} (${count})${e}`;
            }

            return {
              ...f, // uuid, image(추가한다면) 등 기존 속성 전체를 그대로 복사
              displayName: finalDisplayName
            };
          });
        }

        setTicket(result.ticket);
        setIsWriter(result.isWriter);
        setIsReceiver(result.isReceiver);
        setLoading(false);
      } catch (err) {
        console.error("티켓 상세 로드 실패:", err);
        const errorMessage = err.response?.data?.message || err.message || "조회 실패";
        alert(errorMessage);
        onClose();
      }
    };
    fetchTicket();
  }, [tno, currentEmail, onClose]);

  const { handleStateChange, handleDelete } = useTicketActions(
    ticket,
    currentEmail,
    isReceiver,
    setTicket,
    onDelete,
    onClose
  );

  // 보낸 티켓: personals 배열이 있는 경우
  const isSentType = isWriter && ticket && ticket.personals !== undefined;
  // 받은 티켓: isReceiver가 true인 경우 (pno는 상태 변경 API 호출 시 필요하지만, UI 표시는 isReceiver만으로 판단)
  const isReceivedType = isReceiver && ticket;

  // 다운로드 확인 창 (displayName을 사용하여 사용자에게 안내)
  const handleDownload = (uuid, fileName, displayName) => {
    if (window.confirm(`'${displayName || fileName}' 파일을 다운로드 하시겠습니까?`)) {
      downloadFile(uuid, fileName); // 실제 API 요청은 원본 fileName으로
    }
  };

  if (loading || !ticket)
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50 text-white font-black">
        로딩 중...
      </div>
    );

  return (
    <div className="ui-modal-overlay p-4" onClick={onClose}>
      <div className="ui-modal-panel w-full max-w-[80%] max-h-[90vh] overflow-hidden flex flex-col" onClick={(e) => e.stopPropagation()}>
        <div className="ui-modal-header flex items-center justify-between">
          <div className="flex items-center flex-1 gap-3">
            <button onClick={onClose} className="text-baseMuted hover:text-baseText text-xl font-bold transition-all">←</button>
            <h2 className="ui-title flex-1 truncate">{ticket.title}</h2>
            {ticket.grade && <div>{getGradeBadge(ticket.grade)}</div>}
            <button onClick={() => togglePin(ticket.tno)} className={`text-xl transition-all ${isPinned(ticket.tno) ? "ui-pin-active" : "ui-pin-inactive"}`}>
              {isPinned(ticket.tno) ? "★" : "☆"}
            </button>
          </div>
        </div>

        <div className="ui-modal-body flex-1 overflow-y-auto">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div className="lg:col-span-2 space-y-6">
              <div className="ui-panel">
                <h3 className="ui-title mb-4">요청 요약</h3>
                <p className="text-baseText whitespace-pre-wrap leading-relaxed">{ticket.content}</p>
              </div>

              {ticket.purpose && (
                <div className="ui-panel">
                  <h3 className="ui-title mb-4">목적</h3>
                  <p className="text-baseText whitespace-pre-wrap leading-relaxed">{ticket.purpose}</p>
                </div>
              )}

              {ticket.requirement && (
                <div className="ui-panel">
                  <h3 className="ui-title mb-4">요구사항</h3>
                  <p className="text-baseText whitespace-pre-wrap leading-relaxed">{ticket.requirement}</p>
                </div>
              )}

              <div className="ui-panel">
                <h3 className="ui-title mb-4">첨부 파일 ({ticket.files?.length || 0})</h3>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {ticket.files?.map((file) => (
                    <div
                      key={file.uuid}
                      onClick={() => handleDownload(file.uuid, file.fileName, file.displayName)}
                      className="flex items-center p-3 ui-card cursor-pointer hover:bg-baseSurface transition-all"
                    >
                      <div className="w-10 h-10 rounded-ui overflow-hidden flex-shrink-0 mr-3">
                        <FilePreview file={file} />
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="text-xs font-semibold truncate" title={file.displayName}>
                          {file.displayName}
                        </div>
                        <div className="ui-text-2xs text-baseMuted">
                          {(file.fileSize / 1024).toFixed(1)} KB
                        </div>
                      </div>
                      <span className="text-brandNavy ml-2">↓</span>
                    </div>
                  ))}
                </div>
              </div>

              {isSentType && ticket.personals && (
                <div className="ui-panel">
                  <h3 className="ui-title mb-4">수신자 상태</h3>
                  <div className="space-y-2">
                    {ticket.personals.map((p, idx) => (
                      <div key={idx} className="flex justify-between items-center p-3 ui-card">
                        <span className="font-semibold text-baseText">{p.receiver}</span>
                        <span className="ui-badge text-xs">{getStateLabel(p.state)}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <div className="lg:col-span-1 space-y-6">
              <div className="ui-panel">
                <h3 className="ui-title mb-4">세부 정보</h3>
                <div className="space-y-3 text-sm">
                  <div className="flex justify-between"><span className="text-baseMuted">작성자</span><span className="text-baseText font-semibold">{ticket.writer}</span></div>
                  <div className="flex justify-between"><span className="text-baseMuted">생성일</span><span className="text-baseText font-semibold">{formatDate(ticket.birth)}</span></div>
                  <div className="flex justify-between"><span className="text-baseMuted">마감일</span><span className="ui-deadline">{formatDate(ticket.deadline)}</span></div>
                </div>
              </div>

              {isReceivedType && (
                <div className="ui-panel">
                  <h3 className="ui-title mb-4">상태 변경</h3>
                  <select
                    value={ticket.state || "NEW"}
                    onChange={(e) => handleStateChange(e.target.value)}
                    className="ui-select"
                  >
                    <option value="NEW">{getStateLabel("NEW")}</option>
                    <option value="IN_PROGRESS">{getStateLabel("IN_PROGRESS")}</option>
                    <option value="NEED_INFO">{getStateLabel("NEED_INFO")}</option>
                    <option value="DONE">{getStateLabel("DONE")}</option>
                  </select>
                </div>
              )}
              {isWriter && <button onClick={handleDelete} className="ui-btn-danger w-full">삭제</button>}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TicketDetailModal;