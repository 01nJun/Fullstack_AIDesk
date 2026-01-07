import axios from "axios";
import { getCookie, setCookie } from "./cookieUtil";
import { API_SERVER_HOST } from "../api/memberApi";

const jwtAxios = axios.create();

// Access Token 만료 시 Refresh Token으로 새 JWT 발급
const refreshJWT = async (accessToken, refreshToken) => {
  const host = API_SERVER_HOST;

  const header = { headers: { Authorization: `Bearer ${accessToken}` } };

  const res = await axios.get(
    `${host}/api/member/refresh?refreshToken=${refreshToken}`,
    header
  );

  console.log("----------------------");
  console.log(res.data);

  return res.data;
}; // Map.of("accessToken", newAccessToken, "refreshToken", newRefreshToken); 반환

//before request
const beforeReq = (config) => {
  console.log("before request.............");
  const memberInfo = getCookie("member");
  // 쿠키에서 member 조회, 없으면 로그인 필요 에러 반환
  if (!memberInfo) {
    console.log("Member NOT FOUND");
    return Promise.reject({ response: { data: { error: "REQUIRE_LOGIN" } } });
  }
  // 있으면 Authorization: Bearer accessToken 헤더 추가, 모든 jwtAxios 요청 시 자동 실행
  const { accessToken } = memberInfo;

  // Authorization
  config.headers.Authorization = `Bearer ${accessToken}`;
  return config;
};

//fail request
const requestFail = (err) => {
  console.log("request error............");

  return Promise.reject(err);
};

// 서버에서 ERROR_ACCESS_TOKEN 반환 → 토큰 만료, refreshJWT 호출 → 새로운 JWT 발급, 쿠키 갱신
//before return response
const beforeRes = async (res) => {
  console.log("before return response...........");
  //'ERROR_ACCESS_TOKEN'
  const data = res.data;

  // 백엔드에서 'ERROR_ACCESS_TOKEN' 에러가 왔을 때
  if (data && data.error === "ERROR_ACCESS_TOKEN") {
    // 이전에 이미 재시도한 요청인지 확인 (무한 루프 방지 핵심)
    const originalRequest = res.config;
    if (originalRequest._retry) {
      // 이미 재시도했는데도 실패했다면 더 이상 시도하지 않고 에러 리턴
      return Promise.reject({ response: { data: { error: "Login Failed" } } });
    }

    // 재시도 플래그 설정
    originalRequest._retry = true;

    try {
      const memberCookieValue = getCookie("member");

      // 토큰 갱신 시도
      const result = await refreshJWT(
        memberCookieValue.accessToken,
        memberCookieValue.refreshToken
      );

      // 쿠키 갱신
      memberCookieValue.accessToken = result.accessToken;
      memberCookieValue.refreshToken = result.refreshToken;
      setCookie("member", JSON.stringify(memberCookieValue), 1);

      // 갱신된 토큰으로 원래 요청의 헤더 수정
      originalRequest.headers.Authorization = `Bearer ${result.accessToken}`;

      // 원래 요청 재실행
      return await axios(originalRequest);
    } catch (err) {
      // 토큰 갱신 실패 시 (Refresh Token도 만료됨 등) -> 로그인 페이지로 가도록 유도하거나 에러 전파
      console.log("Refresh Token Expired or Error");
      return Promise.reject(err);
    }
  }

  return res;
};

//fail response
const responseFail = (err) => {
  console.log("response fail error.............");
  return Promise.reject(err);
};

jwtAxios.interceptors.request.use(beforeReq, requestFail);

jwtAxios.interceptors.response.use(beforeRes, responseFail);

export default jwtAxios;