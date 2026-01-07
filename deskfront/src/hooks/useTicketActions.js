import { useState } from "react";
import { changeTicketState, deleteTicket } from "../api/ticketApi";

// 티켓 액션(수정, 삭제)를 관리
export const useTicketActions = (
  ticket,
  currentEmail,
  isReceiver,
  setTicket,
  onDelete,
  onClose
) => {
  // 티켓 상태 변경 핸들러 (받은 티켓만)
  const handleStateChange = async (newState) => {
    if (!ticket || !currentEmail || !isReceiver || !ticket.pno) return;

    try {
      const updatedTicket = await changeTicketState(
        ticket.pno,
        currentEmail,
        newState
      );
      setTicket(updatedTicket);
    } catch (err) {
      console.error("상태 변경 실패:", err);
      alert("상태 변경에 실패했습니다.");
    }
  };

  // 티켓 삭제 핸들러 (보낸 티켓만)
  const handleDelete = async () => {
    if (!ticket || !currentEmail) return;

    if (!window.confirm("정말 이 티켓을 삭제하시겠습니까?")) {
      return;
    }

    try {
      await deleteTicket(ticket.tno, currentEmail);
      alert("티켓이 삭제되었습니다.");
      onDelete?.(); // 상위 컴포넌트에 삭제 완료 알림
      onClose(); // 모달 닫기
    } catch (err) {
      console.error("삭제 실패:", err);
      alert("삭제에 실패했습니다.");
    }
  };

  return { handleStateChange, handleDelete };
};

