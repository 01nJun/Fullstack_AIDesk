import jwtAxios from "../util/jwtUtil"; // 커스텀 axios 임포트
const API_SERVER_HOST = process.env.REACT_APP_API_SERVER_HOST;


// 댓글 관련 API의 기본 경로
const prefix = `${API_SERVER_HOST}/api/replies`;

//  특정 게시물의 댓글 목록 가져오기 (GET)
export const getReplyList = async (bno, page = 1) => {
  // jwtAxios가 요청을 가로채서(Interceptor) 자동으로 토큰을 실어 보냅니다.
  const res = await jwtAxios.get(`${prefix}/list/${bno}`, {
    params: { page },
  });
  return res.data;
};

// 새 댓글 등록하기 (POST)
export const postReply = async (replyObj) => {
  // 토큰 정보가 필요 없으므로 세 번째 인자로 헤더를 넘길 필요가 없습니다.
  const res = await jwtAxios.post(`${prefix}/`, replyObj);
  return res.data;
};

// 댓글 삭제하기 (DELETE)
export const deleteReply = async (rno) => {
  const res = await jwtAxios.delete(`${prefix}/${rno}`);
  return res.data;
};

//  댓글 수정하기 (PUT)
export const putReply = async (replyObj) => {
  // replyObj에 rno가 포함되어 있으므로 이를 활용해 경로를 구성합니다.
  const res = await jwtAxios.put(`${prefix}/${replyObj.rno}`, replyObj);
  return res.data;
};