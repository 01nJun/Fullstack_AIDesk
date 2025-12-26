 import axios from "axios";
 import { getCookie } from "../util/cookieUtil";

 export const API_SERVER_HOST = "http://localhost:8080";
 const prefix = `${API_SERVER_HOST}/api/replies`;

 // [공통] 쿠키에서 토큰을 꺼내 Authorization 헤더를 만드는 함수
 const getAuthHeader = () => {
   const member = getCookie("member");
   if (!member) return {};

   let memberObj = member;
   if (typeof member === "string") {
     try {
       memberObj = JSON.parse(decodeURIComponent(member));
     } catch (err) {
       return {};
     }
   }

   const token = memberObj.accessToken;
   if (!token) return {};

   return {
     headers: { Authorization: `Bearer ${token}` },
   };
 };

 // 1. 특정 게시물의 댓글 목록 가져오기 (GET)
 export const getReplyList = async (bno, page = 1) => {
   // Config와 Filter에서 허용했으므로 비로그인 시에도 작동합니다.
   const res = await axios.get(`${prefix}/list/${bno}`, {
     params: { page },
     ...getAuthHeader(), // 로그인 상태라면 토큰을 실어 보냄 (선택 사항)
   });
   return res.data;
 };

 // 2. 새 댓글 등록하기 (POST)
 export const postReply = async (replyObj) => {
   // axios.post(url, data, config) -> 헤더는 반드시 세 번째 인자에 들어가야 합니다.
   const header = getAuthHeader();
   const res = await axios.post(`${prefix}/`, replyObj, header);
   return res.data;
 };

 // 3. 댓글 삭제하기 (DELETE) - 추가 구현 시 참고
 export const deleteReply = async (rno) => {
   const res = await axios.delete(`${prefix}/${rno}`, getAuthHeader());
   return res.data;
 };

 // 4. 댓글 수정하기 (PUT) - 추가 구현 시 참고
 export const putReply = async (replyObj) => {
   const res = await axios.put(
     `${prefix}/${replyObj.rno}`,
     replyObj,
     getAuthHeader()
   );
   return res.data;
 };
