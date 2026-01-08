import jwtAxios from "../util/jwtUtil";
const API_SERVER_HOST = process.env.REACT_APP_API_SERVER_HOST;


const host = `${API_SERVER_HOST}/api/chat`;

/**
 * 채팅방 목록 조회
 */
export const getChatRooms = async () => {
  const res = await jwtAxios.get(`${host}/rooms`);
  return res.data;
};

/**
 * 그룹 채팅방 생성
 * @param {Object} params - { name: string, participantEmails: string[] }
 */
export const createGroupRoom = async ({ name, participantEmails }) => {
  const res = await jwtAxios.post(`${host}/rooms`, {
    name,
    userIds: participantEmails,
  });
  return res.data;
};

/**
 * 1:1 채팅방 생성 또는 기존 방 반환
 * @param {Object} params - { targetEmail: string }
 */
export const createOrGetDirectRoom = async ({ targetEmail }) => {
  const res = await jwtAxios.post(`${host}/rooms/direct`, {
    targetUserId: targetEmail,
  });
  return res.data;
};

/**
 * 채팅방 상세 정보 조회
 * @param {number} roomId
 */
export const getChatRoom = async (roomId) => {
  const res = await jwtAxios.get(`${host}/rooms/${roomId}`);
  return res.data;
};

/**
 * 채팅방 메시지 목록 조회
 * @param {number} roomId
 * @param {Object} params - { page: number, size: number }
 */
export const getMessages = async (roomId, { page = 1, size = 20 } = {}) => {
  const res = await jwtAxios.get(`${host}/rooms/${roomId}/messages`, {
    params: { page, size },
  });
  return res.data;
};

/**
 * 메시지 전송 (REST API)
 * @param {number} roomId
 * @param {Object} params - { content: string, ticketId?: number, messageType?: string, aiEnabled?: boolean, fileUuids?: string[] }
 */
export const sendMessageRest = async (roomId, { content, ticketId, messageType = "TEXT", aiEnabled = false, fileUuids = [] }) => {
  const res = await jwtAxios.post(`${host}/rooms/${roomId}/messages`, {
    content,
    ticketId,
    messageType,
    aiEnabled,
    fileUuids,
  });
  return res.data;
};

/**
 * 읽음 처리
 * @param {number} roomId
 * @param {Object} params - { messageSeq: number }
 */
export const markRead = async (roomId, { messageSeq }) => {
  const res = await jwtAxios.put(`${host}/rooms/${roomId}/read`, {
    messageSeq,
  });
  return res.data;
};

/**
 * 채팅방 나가기
 * @param {number} roomId
 */
export const leaveRoom = async (roomId) => {
  const res = await jwtAxios.post(`${host}/rooms/${roomId}/leave`);
  return res.data;
};

/**
 * 채팅방 초대
 * @param {number} roomId
 * @param {Object} params - { inviteeEmails: string[] }
 */
export const inviteUsers = async (roomId, { inviteeEmails }) => {
  const res = await jwtAxios.post(`${host}/rooms/${roomId}/invite`, {
    userIds: inviteeEmails,
  });
  return res.data;
};

/**
 * 파일 업로드
 * @param {number} chatRoomId
 * @param {File} file
 */
export const uploadChatFile = async (chatRoomId, file) => {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("chatRoomId", chatRoomId);

  const res = await jwtAxios.post(`${host}/files`, formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });
  return res.data;
};

/**
 * 파일 다운로드 (blob 방식)
 * @param {string} uuid
 * @param {string} fileName
 */
export const downloadChatFile = async (uuid, fileName) => {
  try {
    const res = await jwtAxios.get(`${host}/files/${uuid}/download`, {
      responseType: "blob",
    });

    // Blob을 URL로 변환
    const url = window.URL.createObjectURL(new Blob([res.data]));
    const link = document.createElement("a");
    link.href = url;
    link.setAttribute("download", fileName);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  } catch (err) {
    console.error("파일 다운로드 실패:", err);
    throw err;
  }
};

/**
 * 파일 보기 URL 생성 (이미지 프리뷰용)
 * @param {string} uuid
 */
export const getChatFileViewUrl = (uuid) => {
  return `${process.env.REACT_APP_API_SERVER_HOST}/api/chat/files/${uuid}/view`;
};
