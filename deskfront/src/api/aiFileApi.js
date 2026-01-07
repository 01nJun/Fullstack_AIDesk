import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./memberApi";

export const aiFileApi = {
  sendMessage: async (payload) => {
    const response = await jwtAxios.post(
      `${API_SERVER_HOST}/api/ai/file/chat`,
      payload
    );
    return response.data; // AIFileResponseDTO
  },

  viewFileUrl: (uuid) => {
    return `${API_SERVER_HOST}/api/ai/file/view/${uuid}`;
  },

  downloadFile: async (uuid, fileName) => {
    const response = await jwtAxios.get(
      `${API_SERVER_HOST}/api/ai/file/download/${uuid}`,
      { responseType: "blob" }
    );

    const blob = new Blob([response.data]);
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;

    // 다운로드 파일명은 원본 파일명 그대로 사용
    const safeName = fileName || "download";
    link.setAttribute("download", safeName);

    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },
};


