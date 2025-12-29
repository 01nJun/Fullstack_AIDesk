import axios from "axios";
import jwtAxios from "../util/jwtUtil";
import { getCookie } from "../util/cookieUtil";

export const API_SERVER_HOST = "http://localhost:8080";
const prefix = `${API_SERVER_HOST}/api/board`;

// 토큰을 안전하게 가져오는 함수
const getAuthHeader = () => {
  const member = getCookie("member");
  if (!member) return {};

  try {
    const memberObj =
      typeof member === "string"
        ? JSON.parse(decodeURIComponent(member))
        : member;

    const token = memberObj.accessToken;

    if (!token) {
      // 토큰이 없는 경우 빈 객체 반환 (로그인 안 된 상태로 요청 시도)
      return {};
    }

    return {
      headers: { Authorization: `Bearer ${token}` },
    };
  } catch (err) {
    console.error("쿠키 파싱 에러:", err);
    return {};
  }
};

export const getList = async (pageParam) => {
  const { page, size, type, keyword, category } = pageParam;

  // [수정됨] getAuthHeader()를 호출하여 헤더 정보를 가져온 뒤, 설정 객체에 병합(...)합니다.
  const authHeader = getAuthHeader();

  const res = await axios.get(`${prefix}/list`, {
    params: {
      page,
      size,
      type: type || "t",
      keyword: keyword || "",
      category: category || "",
    },
    ...authHeader // 여기에 headers: { Authorization: ... } 가 들어갑니다.
  });

  return res.data;
};

export const getOne = async (bno) => {
  // [수정됨] 상세 조회 시에도 헤더 추가
  const authHeader = getAuthHeader();

  const res = await axios.get(`${prefix}/${bno}`, {
    ...authHeader
  });

  return res.data;
};

export const postAdd = async (boardObj) => {
  const res = await axios.post(`${prefix}/`, boardObj, getAuthHeader());
  return res.data;
};

export const putOne = async (bno, boardObj) => {
  const res = await axios.put(`${prefix}/${bno}`, boardObj, getAuthHeader());
  return res.data;
};

export const deleteOne = async (bno) => {
  const res = await axios.delete(`${prefix}/${bno}`, getAuthHeader());
  return res.data;
};

// 최근 공지사항 3개 가져오기
export const getRecentBoards = async () => {
    const res = await jwtAxios.get(`${prefix}/list`, { // 백엔드 list 로직이 PageRequestDTO를 받으므로 파라미터로 전달
        params: { page: 1, size: 3 }                   // 최신순 정렬은 보통 백엔드 기본값이거나 추가 파라미터 필요
    });
    return res.data.dtoList;
};