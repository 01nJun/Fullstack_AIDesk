import axios from "axios";
import jwtAxios from "../util/jwtUtil";

const API_SERVER_HOST = process.env.REACT_APP_API_SERVER_HOST;


const host = `${API_SERVER_HOST}/api/member`;

// /login API를 호출해서 로그인 처리, 성공 시 서버에서 보내주는 JWT 토큰, 사용자 정보를 받음
// 서버에서는 컨트롤러 없이도 Spring Security Filter Chain이 요청을 가로챔(config.loginPage()루트와 동일) - loadUserByUsername 실행
export const loginPost = async (loginParam) => {
  const header = { headers: { "Content-Type": "x-www-form-urlencoded" } };

  const form = new FormData();
  form.append("username", loginParam.email);
  form.append("password", loginParam.pw);

  const res = await axios.post(`${host}/login`, form, header);

  return res.data;
};

export const modifyMember = async (member) => {
  const res = await jwtAxios.put(`${host}/modify`, member);

  return res.data;
};

export const joinPost = async (joinParam) => {
  const header = { headers: { "Content-Type": "application/json" } };

  // JSON 형태로 전송
  const res = await axios.post(`${host}/join`, joinParam, header);

  return res.data;
};

// 얼굴 등록 (PostgreSQL 저장 + MariaDB 상태 변경)
export const registerFaceApi = async (email, file) => {
    const formData = new FormData();
    formData.append("email", email);
    formData.append("faceFile", file);

    const res = await axios.post(`${host}/face-register`, formData, {
        headers: { "Content-Type": "multipart/form-data" }
    });
    return res.data;
};

// 얼굴 로그인 활성화/비활성화 토글
export const updateFaceStatusApi = async (email, status) => {
    const res = await axios.put(`${host}/update-face-status`, { email, status });
    return res.data;
};
// 얼굴 로그인 요청 (FaceAuthenticationFilter가 가로채는 URL)
export const loginFace = async (file) => {
    const formData = new FormData();
    formData.append("faceFile", file);
    const res = await axios.post(`${host}/login/face`, formData, {
        headers: { "Content-Type": "multipart/form-data" }
    });
    return res.data;
}
// 멤버 검색 (일반 사용자용)
export const searchMembers = async (keyword, page = 1, size = 20, department = null) => {
  const params = {
    page,
    size,
    keyword: keyword || null,
    department: department || null
  };

  const res = await jwtAxios.get(`${host}/search`, { params });
  return res.data;
};

// 담당자 정보 조회 (email로 부서, 닉네임 조회)
export const getMemberInfo = async (email) => {
  const res = await jwtAxios.get(`${host}/info/${email}`);
  return res.data;
};