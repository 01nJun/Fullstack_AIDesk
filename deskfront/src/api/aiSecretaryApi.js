import axios from "axios";
import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./memberApi";

const PYTHON_API_URL = "http://localhost:8000/api/v1";
const AI_API_KEY = "my-super-secret-key-shared-with-java";

const aiClient = axios.create({
  baseURL: PYTHON_API_URL,
  headers: {
    "Content-Type": "application/json",
    "x-api-key": AI_API_KEY,
  },
});

export const aiSecretaryApi = {
  sendMessage: async (payload) => {
    try {
      const response = await aiClient.post("/chat", payload);
      return response.data;
    } catch (error) {
      console.error("AI Chat Error:", error);
      throw error;
    }
  },

  // ✅ [NEW] 회의록 오디오 분석 API
  analyzeMeetingAudio: async (audioFile, conversationId) => {
    try {
      const formData = new FormData();
      formData.append("file", audioFile);
      // 필요하다면 conversation_id도 보낼 수 있음
      formData.append("conversation_id", conversationId);

      const response = await aiClient.post("/analyze-audio", formData, {
        headers: {
          "Content-Type": "multipart/form-data", // 파일 전송 필수 헤더
        },
      });
      return response.data;
    } catch (error) {
      console.error("Audio Analysis Error:", error);
      throw error;
    }
  },

  submitTicket: async (ticketData, files, writerEmail) => {
    try {
      const formData = new FormData();

      // 날짜 포맷팅 (YYYY-MM-DD -> YYYY-MM-DD HH:mm) - 백엔드 타입에 맞추기 위한 전처리
      let finalDeadline = ticketData.deadline;
      if (finalDeadline && finalDeadline.length === 10) {
        finalDeadline += " 09:00";
      }
      // 수신자가 문자열(user1, user2)인 경우 배열로 변환
      let receiverArray = ticketData.receivers;
      if (typeof receiverArray === "string") {
        receiverArray = receiverArray
          .split(",")
          .map((s) => s.trim())
          .filter((s) => s !== "");
      }

      // 백엔드 @RequestPart("ticket")와 매핑될 JSON 데이터
      const ticketPayload = {
        title: ticketData.title,
        content: ticketData.content,
        purpose: ticketData.purpose,
        requirement: ticketData.requirement,
        grade: ticketData.grade,
        deadline: finalDeadline,
        receivers: receiverArray,
      };

      // 중요: JSON을 Blob으로 변환하여 'ticket' 파트에 추가
      // multipart/form-data에서는 문자열, 파일, Blob만 명확하게 인식됨 (JSON 객체를 그대로 넣으면 안됨)
      // JSON을 파일처럼 인식시키기 위해 Blob으로 감싸야하고 하지 않으면 - @RequestPart("ticket") 바인딩 실패 (값이 null로 들어감)
      formData.append(
        "ticket",
        new Blob([JSON.stringify(ticketPayload)], { type: "application/json" })
      );

      // 파일들을 'files' 파트에 추가
      if (files && files.length > 0) {
        files.forEach((file) => {
          formData.append("files", file);
        });
      }

      // 실제 서버 요청
      const response = await jwtAxios.post(
        `${API_SERVER_HOST}/api/tickets`,
        formData,
        {
          params: { writer: writerEmail },
          headers: { "Content-Type": "multipart/form-data" },
        }
      );

      return response.data;
    } catch (error) {
      console.error("API 전송 에러 상세:", error.response || error);
      throw error;
    }
  },
};
