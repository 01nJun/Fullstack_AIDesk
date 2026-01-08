// import axios from "axios"; // 기존 axios 제거
import jwtAxios from "../util/jwtUtil"; // 프로젝트의 jwtAxios 경로로 수정하세요

const API_BASE_URL = "http://localhost:8080/api/stt";

export const sttApi = {
  uploadAudio: async (file) => {
    const formData = new FormData();
    formData.append("file", file);

    try {
      // jwtAxios를 사용하면 Authorization 헤더가 자동으로 붙습니다.
      const response = await jwtAxios.post(`${API_BASE_URL}/upload`, formData, {
        headers: {
          "Content-Type": "multipart/form-data", // 파일 전송 설정
        },
      });
      return response.data;
    } catch (error) {
      console.error("STT API Error:", error);
      throw error;
    }
  },
};

