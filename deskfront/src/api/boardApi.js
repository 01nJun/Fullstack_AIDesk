import jwtAxios from "../util/jwtUtil"; // 커스텀 axios 임포트
import { API_SERVER_HOST } from "./memberApi";

const prefix = `${API_SERVER_HOST}/api/board`;

// 1. 목록 가져오기
export const getList = async (pageParam) => {
  const { page, size, type, keyword, category } = pageParam;

  // jwtAxios가 알아서 헤더에 토큰을 넣어줍니다.
  const res = await jwtAxios.get(`${prefix}/list`, {
    params: {
      page,
      size,
      type: type || "t",
      keyword: keyword || "",
      category: category || "",
    }
  });

  return res.data;
};

//  상세 조회
export const getOne = async (bno) => {
  const res = await jwtAxios.get(`${prefix}/${bno}`);
  return res.data;
};

// 등록
export const postAdd = async (boardObj) => {
  const res = await jwtAxios.post(`${prefix}/`, boardObj);
  return res.data;
};

// 수정
export const putOne = async (bno, boardObj) => {
  const res = await jwtAxios.put(`${prefix}/${bno}`, boardObj);
  return res.data;
};

// 삭제
export const deleteOne = async (bno) => {
  const res = await jwtAxios.delete(`${prefix}/${bno}`);
  return res.data;
};