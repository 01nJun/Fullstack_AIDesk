import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./memberApi";

const host = `${API_SERVER_HOST}/api/admin`;

// 1. 승인 대기 목록
export const getPendingList = async (pageParam) => {
  //pageParam 안에는 page, size, keyword, department가 들어있음
  const res = await jwtAxios.get(`${host}/pending`, { params: pageParam });
  return res.data;
};

// 2. 전체 직원 목록
export const getActiveList = async (pageParam) => {
  const res = await jwtAxios.get(`${host}/active`, { params: pageParam });
  return res.data;
};

// 3. 승인 처리
export const putApprove = async (email) => {
  const res = await jwtAxios.put(`${host}/approve/${email}`);
  return res.data;
};

// 4. 삭제 처리
export const putSoftDelete = async (email) => {
  const res = await jwtAxios.put(`${host}/delete/${email}`);
  return res.data;
};
