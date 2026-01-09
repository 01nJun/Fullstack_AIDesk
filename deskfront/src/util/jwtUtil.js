import axios from "axios";
import { getCookie, setCookie } from "./cookieUtil";
const API_SERVER_HOST = process.env.REACT_APP_API_SERVER_HOST;

const jwtAxios = axios.create({ withCredentials: true });

// Access Token 만료 시 Refresh Token으로 새 JWT 발급
// accessToken이 없어도 Refresh Token만으로 재발급 가능하도록 수정
const refreshJWT = async (accessToken) => {
  const host = API_SERVER_HOST;

  // accessToken이 있으면 Authorization 헤더에 포함, 없으면 빈 헤더 (Refresh Token만 사용)
  const headers = accessToken 
    ? { Authorization: `Bearer ${accessToken}` }
    : {};

  try {
    console.log("Calling refresh endpoint with headers:", headers);
    const res = await axios.get(`${host}/api/member/refresh`, {
      headers,
      withCredentials: true,
    });

    console.log("Refresh endpoint response status:", res.status);
    return res.data;
  } catch (err) {
    console.error("Refresh endpoint error:", err.response?.status, err.response?.data);
    // 에러 응답 처리
    if (err.response && err.response.data && err.response.data.error) {
      throw err;
    }
    // 네트워크 에러 등
    throw err;
  }
}; // Map.of("accessToken", newAccessToken) 반환 (refresh는 HttpOnly 쿠키로만 관리)

//before request - async로 변경하여 accessToken이 없을 때 Refresh Token으로 재발급 시도
const beforeReq = async (config) => {
  console.log("before request.............");
  const memberInfoRaw = getCookie("member");
  // 쿠키에서 member 조회, 없으면 로그인 필요 에러 반환
  if (!memberInfoRaw) {
    console.log("Member NOT FOUND");
    return Promise.reject({ response: { data: { error: "REQUIRE_LOGIN" } } });
  }
  
  // 쿠키는 JSON 문자열로 저장되므로 파싱 필요
  let memberInfo;
  try {
    memberInfo = typeof memberInfoRaw === 'string' ? JSON.parse(memberInfoRaw) : memberInfoRaw;
  } catch (e) {
    console.log("Failed to parse member cookie:", e);
    return Promise.reject({ response: { data: { error: "REQUIRE_LOGIN" } } });
  }
  
  let { accessToken } = memberInfo || {};
  
  // accessToken이 없으면 Refresh Token으로 재발급 시도
  if (!accessToken) {
    console.log("accessToken is missing in member cookie, attempting refresh...");
    try {
      // Refresh Token만으로 Access Token 재발급 시도 (accessToken을 null로 전달)
      // 단, 이 경우 요청 자체를 보류하고 나중에 response interceptor에서 처리하도록 함
      // beforeReq는 동기적으로 실행되어야 하므로 여기서는 그냥 진행
      // accessToken이 없으면 빈 Authorization 헤더로 보내고, 서버에서 ERROR_ACCESS_TOKEN을 반환하면
      // responseFail에서 처리하도록 함
      console.log("Access token missing, will try refresh in response interceptor");
      // 여기서는 일단 요청을 진행시키되, 서버에서 에러가 나면 responseFail에서 처리
    } catch (refreshErr) {
      console.log("Failed to prepare refresh:", refreshErr);
      return Promise.reject({ response: { data: { error: "REQUIRE_LOGIN" } } });
    }
  }
  
//  console.log("accessToken:", accessToken);

  // Authorization 헤더 설정
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
    // 이전에 이미 재시도한 요청인지 확인 (무한 루프 방지)
    const originalRequest = res.config;
    if (originalRequest._retry) {
      // 이미 재시도했는데도 실패했다면 더 이상 시도하지 않고 에러 리턴
      return Promise.reject({ response: { data: { error: "Login Failed" } } });
    }

    // 재시도 플래그 설정
    originalRequest._retry = true;

    try {
      const memberCookieValueRaw = getCookie("member");
      
      if (!memberCookieValueRaw) {
        return Promise.reject({ response: { data: { error: "REQUIRE_LOGIN" } } });
      }
      
      // 쿠키 파싱
      let memberCookieValue;
      try {
        memberCookieValue = typeof memberCookieValueRaw === 'string' ? JSON.parse(memberCookieValueRaw) : memberCookieValueRaw;
      } catch (e) {
        return Promise.reject({ response: { data: { error: "REQUIRE_LOGIN" } } });
      }

      if (!memberCookieValue || !memberCookieValue.accessToken) {
        return Promise.reject({ response: { data: { error: "REQUIRE_LOGIN" } } });
      }

      // 토큰 갱신 시도
      const result = await refreshJWT(memberCookieValue.accessToken);

      // 쿠키 갱신
      memberCookieValue.accessToken = result.accessToken;
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
const responseFail = async (err) => {
  // 에러 응답 처리
  console.log("response fail error.............");
  if (err.response && err.response.data) {
    const errorData = err.response.data;
    const errorType = errorData.error;
    const originalRequest = err.config;
    
    // Refresh Token 관련 에러는 로그인 필요로 처리
    if (errorType === "UNKNOWN_REFRESH" || errorType === "NULL_REFRESH" || 
        errorType === "REFRESH_REPLAY_DETECTED" || errorType === "REFRESH_TAMPERED" ||
        errorType === "REFRESH_DEVICE_MISMATCH" || errorType === "REFRESH_IP_MISMATCH" ||
        errorType === "REFRESH_BINDING_MISMATCH" || errorType === "INVALID_REFRESH_CLAIMS") {
      return Promise.reject({ response: { data: { error: "REQUIRE_LOGIN" } } });
    }
    
    // ERROR_ACCESS_TOKEN, Expired, MalFormed 등의 JWT 에러 처리
    if (errorType === "ERROR_ACCESS_TOKEN" || errorType === "Expired" || errorType === "MalFormed" || errorType === "Invalid") {
      // 무한 루프 방지
      if (originalRequest && originalRequest._retry) {
        return Promise.reject({ response: { data: { error: "REQUIRE_LOGIN" } } });
      }
      
      if (originalRequest) {
        originalRequest._retry = true;
        
        try {
          const memberCookieValueRaw = getCookie("member");
          
          if (!memberCookieValueRaw) {
            return Promise.reject({ response: { data: { error: "REQUIRE_LOGIN" } } });
          }
          
          // 쿠키 파싱
          let memberCookieValue;
          try {
            memberCookieValue = typeof memberCookieValueRaw === 'string' ? JSON.parse(memberCookieValueRaw) : memberCookieValueRaw;
          } catch (e) {
            return Promise.reject({ response: { data: { error: "REQUIRE_LOGIN" } } });
          }
          
          // accessToken이 없어도 Refresh Token만으로 재발급 시도
          // accessToken이 없거나 만료된 경우 null을 전달하여 Refresh Token만으로 재발급
          const currentAccessToken = memberCookieValue?.accessToken || null;
          console.log("Attempting token refresh, current accessToken:", currentAccessToken ? "exists" : "missing");
          
          // 토큰 갱신 시도 - accessToken이 만료되었거나 없으면 null 전달 (Refresh Token만 사용)
          const result = await refreshJWT(null); // Refresh Token으로만 재발급 시도
          
          console.log("Token refresh result:", result);
          
          if (result && result.accessToken) {
            // 쿠키 갱신
            if (!memberCookieValue) {
              memberCookieValue = {};
            }
            memberCookieValue.accessToken = result.accessToken;
            setCookie("member", JSON.stringify(memberCookieValue), 1);
            
            // 갱신된 토큰으로 원래 요청의 헤더 수정
            originalRequest.headers.Authorization = `Bearer ${result.accessToken}`;
            
            // 원래 요청 재실행
            return await axios(originalRequest);
          } else {
            return Promise.reject({ response: { data: { error: "REQUIRE_LOGIN" } } });
          }
        } catch (refreshErr) {
          console.error("Refresh Token 갱신 실패:", refreshErr.response?.data || refreshErr.message);
          // Refresh Token 갱신 실패 - 로그인 필요
          return Promise.reject({ response: { data: { error: "REQUIRE_LOGIN" } } });
        }
      }
    }
  }
  
  return Promise.reject(err);
};

jwtAxios.interceptors.request.use(beforeReq, requestFail);

jwtAxios.interceptors.response.use(beforeRes, responseFail);

export default jwtAxios;