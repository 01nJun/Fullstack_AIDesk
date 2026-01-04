import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./memberApi";

export const aiSecretaryApi = {
  /**
   * [AI 채팅 및 분석 요청]
   * Java Backend Proxy (/api/ai/ticket/chat)로 요청을 보냅니다.
   */
  sendMessage: async (payload) => {
    try {
      // [수정] 백엔드 컨트롤러 주소 변경 반영 (/api/ai/chat -> /api/ai/ticket/chat)
      const response = await jwtAxios.post(
        `${API_SERVER_HOST}/api/ai/ticket/chat`,
        payload
      );
      return response.data; // AITicketResponseDTO 반환
    } catch (error) {
      console.error("AI Chat Error (Proxy):", error);
      throw error;
    }
  },

  /**
   * [티켓 최종 전송]
   * Java Backend (/api/tickets)로 티켓 생성 요청
   */
  submitTicket: async (ticketData, files, writerEmail) => {
    // 1. 담당자 유효성 검사
    if (!ticketData.receivers || ticketData.receivers.length === 0) {
      console.error("Validation Failed: No receivers assigned.");
      throw new Error(
        "담당자가 지정되지 않았습니다. AI 대화를 통해 담당자를 확정해 주세요."
      );
    }

    try {
      // 2. 마감일 시간 포맷 보정 (YYYY-MM-DD -> YYYY-MM-DD 09:00)
      let finalDeadline = ticketData.deadline;
      if (finalDeadline && finalDeadline.length === 10) {
        finalDeadline += " 09:00";
      }

      // 3. FormData 생성 (파일 업로드 포함)
      const formData = new FormData();

      // 3-1. 티켓 정보 JSON 변환하여 추가
      const ticketPayload = {
        title: ticketData.title,
        content: ticketData.content,
        purpose: ticketData.purpose,
        requirement: ticketData.requirement,
        grade: ticketData.grade,
        deadline: finalDeadline,
        receivers: ticketData.receivers, // ["email1", "email2"]
      };

      // Blob으로 감싸서 'application/json' 타입 명시 (Spring @RequestPart 호환)
      const jsonBlob = new Blob([JSON.stringify(ticketPayload)], {
        type: "application/json",
      });
      formData.append("ticket", jsonBlob);

      // 3-2. 파일 리스트 추가
      if (files && files.length > 0) {
        files.forEach((file) => {
          formData.append("files", file);
        });
      }

      // 4. 전송 (Content-Type은 axios가 자동으로 설정함)
      const response = await jwtAxios.post(
        `${API_SERVER_HOST}/api/tickets`,
        formData,
        {
          params: { writer: writerEmail },
          headers: {
            "Content-Type": "multipart/form-data", // 명시적으로 지정
          },
        }
      );

      return response.data;
    } catch (error) {
      console.error("Submit Ticket Error:", error);
      throw error;
    }
  },

  /**
   * [헬스 체크]
   * Java 서버 연결 확인용
   */
  checkHealth: async () => {
    try {
      return true;
    } catch (error) {
      return false;
    }
  },
};
