import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./memberApi";

export const aiSecretaryApi = {
  /**
   * [AI 채팅 및 분석 요청]
   * Java Backend Proxy (/api/ai/ticket/chat)로 요청을 보냅니다.
   */
  sendMessage: async (payload) => {
    try {
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
   * [AI 요약 요청]
   * Java Backend (/api/ai/summary)로 텍스트/파일을 보내 요약 데이터를 받습니다.
   */
  getSummary: async (ticketData, file) => {
    try {
      const formData = new FormData();
      if (file) {
        formData.append("file", file);
      }

      const data = {
        title: ticketData.title || "",
        shortSummary: ticketData.content || "", // content -> shortSummary
        overview: ticketData.purpose || "",    // purpose -> overview
        details: ticketData.requirement || "", // requirement -> details
        attendees: ticketData.receivers || [], // receivers -> attendees
        deadline: ticketData.deadline || null,
      };

       formData.append(
              "data",
              new Blob([JSON.stringify(data)], { type: "application/json" })
            );

      const response = await jwtAxios.post(
        `${API_SERVER_HOST}/api/ai/summary`,
        formData,
        {
          headers: {
            "Content-Type": "multipart/form-data",
          },
        }
      );
      return response.data; // MeetingMinutesDto 반환
    } catch (error) {
      console.error("AI Summary Error:", error);
      throw error;
    }
  },

  /**
   * [요약 데이터로 PDF 생성]
   * Java Backend (/api/ai/summary-pdf)로 JSON 데이터를 보내 PDF 바이너리를 받습니다.
   */
  downloadSummaryPdf: async (summaryData) => {
    try {
      const response = await jwtAxios.post(
        `${API_SERVER_HOST}/api/ai/summary-pdf`,
        summaryData,
        {
          responseType: "arraybuffer", // PDF 바이너리 받기 필수
        }
      );
      return response.data;
    } catch (error) {
      console.error("PDF Download Error (JSON):", error);
      throw error;
    }
  },

  /**
   * [기존 방식 PDF 생성] (Fallback용)
   */
  downloadPdf: async (ticketData, file) => {
    try {
      const formData = new FormData();
      if (file) {
        formData.append("file", file);
      }

      const data = {
              title: ticketData.title || "",
              shortSummary: ticketData.content || "",
              overview: ticketData.purpose || "",
              details: ticketData.requirement || "",
              attendees: ticketData.receivers || [],
              deadline: ticketData.deadline || null,
            };

      formData.append(
            "data",
            new Blob([JSON.stringify(data)], { type: "application/json" })
          );

      const response = await jwtAxios.post(
        `${API_SERVER_HOST}/api/ai/summarize-report`,
        formData,
        {
          headers: {
            "Content-Type": "multipart/form-data",
          },
          responseType: "arraybuffer",
        }
      );
      return response.data;
    } catch (error) {
      console.error("PDF Download Error (FormData):", error);
      throw error;
    }
  },

  /**
   * [티켓 최종 전송]
   */
  submitTicket: async (ticketData, files, writerEmail) => {
    if (!ticketData.receivers || ticketData.receivers.length === 0) {
      console.error("Validation Failed: No receivers assigned.");
      throw new Error(
        "담당자가 지정되지 않았습니다. AI 대화를 통해 담당자를 확정해 주세요."
      );
    }

    try {
      let finalDeadline = ticketData.deadline;
      if (finalDeadline && finalDeadline.length === 10) {
        finalDeadline += " 09:00";
      }

      const formData = new FormData();

      const ticketPayload = {
        title: ticketData.title,
        content: ticketData.content,
        purpose: ticketData.purpose,
        requirement: ticketData.requirement,
        grade: ticketData.grade,
        deadline: finalDeadline,
        receivers: ticketData.receivers,
      };

      const jsonBlob = new Blob([JSON.stringify(ticketPayload)], {
        type: "application/json",
      });
      formData.append("ticket", jsonBlob);

      if (files && files.length > 0) {
        files.forEach((file) => {
          formData.append("files", file);
        });
      }

      const response = await jwtAxios.post(
        `${API_SERVER_HOST}/api/tickets`,
        formData,
        {
          params: { writer: writerEmail },
          headers: {
            "Content-Type": "multipart/form-data",
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
   */
  checkHealth: async () => {
    return true;
  },
};
