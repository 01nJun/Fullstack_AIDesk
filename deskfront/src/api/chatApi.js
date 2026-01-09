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
 * @param {Object} params - { content: string, ticketId?: number, messageType?: string, aiEnabled?: boolean }
 */
export const sendMessageRest = async (roomId, { content, ticketId, messageType = "TEXT", aiEnabled = false }) => {
  const res = await jwtAxios.post(`${host}/rooms/${roomId}/messages`, {
    content,
    ticketId,
    messageType,
    aiEnabled,
  });
  return res.data;
};

/**
 * 메시지 전송 + 파일 첨부 (multipart/form-data)
 * - WS는 multipart를 못 보내므로 파일이 있을 때만 사용
 */
export const sendMessageWithFilesRest = async (
  roomId,
  { content, ticketId, messageType = "TEXT", aiEnabled = false, files = [] }
) => {
  const formData = new FormData();

  const messagePayload = {
    content,
    ticketId,
    messageType,
    aiEnabled,
  };

  formData.append("message", new Blob([JSON.stringify(messagePayload)], { type: "application/json" }));

  if (files && files.length > 0) {
    files.forEach((f) => {
      formData.append("files", f);
    });
  }

  const res = await jwtAxios.post(`${host}/rooms/${roomId}/messages/files`, formData, {
    headers: { "Content-Type": "multipart/form-data" },
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
