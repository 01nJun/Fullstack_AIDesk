import axios from "axios";
const API_SERVER_HOST = process.env.REACT_APP_API_SERVER_HOST;

const rest_api_key = process.env.REACT_APP_KAKAO_REST_API_KEY;
const redirect_uri = process.env.REACT_APP_KAKAO_REDIRECT_URI;

const auth_code_path = `https://kauth.kakao.com/oauth/authorize`;
const access_token_url = `https://kauth.kakao.com/oauth/token`;

// 카카오 로그인 페이지로 이동할 URL 생성, 브라우저에서 해당 URL로 이동하면 카카오 로그인 후 redirect_uri로 인증 코드 전송
export const getKakaoLoginLink = () => {
  const kakaoURL = `${auth_code_path}?client_id=${rest_api_key}&redirect_uri=${redirect_uri}&response_type=code`;

  return kakaoURL;
};

// 카카오 로그인 후 받은 인증 코드(authCode) 를 서버에 보내지 않고 프론트에서 직접 액세스 토큰 요청, 요청 성공 시 access_token 반환
export const getAccessToken = async (authCode) => {
  const header = {
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
  };
  // axios가 JSON으로 보내지 않도록 x-www-form-urlencoded 형식으로 직렬화하기 위해 URLSearchParams 사용
  const params = new URLSearchParams({
    grant_type: "authorization_code",
    client_id: rest_api_key,
    redirect_uri,
    code: authCode,
  });

  const res = await axios.post(access_token_url, params, header);

  const accessToken = res.data.access_token;

  return accessToken;
};
// 백엔드 Spring Controller (SocialController) 호출 - 전달: 카카오 accessToken
export const getMemberWithAccessToken = async (accessToken) => {
  const res = await axios.get(`${API_SERVER_HOST}/api/member/kakao`, {
    headers: { Authorization: `Bearer ${accessToken}` },
    withCredentials: true,
  });

  return res.data; // claims 반환
};
