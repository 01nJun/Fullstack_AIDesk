import { useEffect } from "react";
import { useSearchParams } from "react-router-dom";
import { getAccessToken, getMemberWithAccessToken } from "../../api/kakaoApi";
import { login } from "../../slices/loginSlice";
import { useDispatch } from "react-redux";
import useCustomLogin from "../../hooks/useCustomLogin";

const KakaoRedirectPage = () => {
  const [searchParams] = useSearchParams(); // URL 쿼리에서 code 추출

  const { moveToLogin, moveToPath } = useCustomLogin();

  const dispatch = useDispatch();
  const authCode = searchParams.get("code");

  useEffect(() => {
    getAccessToken(authCode).then((accessToken) => {
      // 프론트에서 직접 액세스 토큰 요청
      // console.log(accessToken);

      getMemberWithAccessToken(accessToken).then((memberInfo) => {
        // 백엔드 Spring Controller (SocialController) 호출, claims 반환
        console.log("-------------------");
        console.log(memberInfo);

        // ★ 1. 승인 대기 중 에러 처리
        if (memberInfo.error && memberInfo.error === "PENDING_APPROVAL") {
          alert("현재 승인 대기 상태입니다. 관리자 승인 후 이용 가능합니다.");
          moveToLogin();
          return;
        }

        dispatch(login(memberInfo));

        // ★ 2. 소셜 회원이지만 부서 정보가 없는 경우 (최초 가입)
        if (memberInfo.social && !memberInfo.department) {
          alert("회원가입을 위해 추가 정보(부서 등)를 입력해주세요.");
          moveToPath("/member/modify"); // 수정 페이지로 이동
        } else {
          // 이미 정보가 있고 승인된 경우 (혹은 관리자 등)
          moveToPath("/");
        }
      });
    });
  }, [authCode]);

  return (
    <div>
      {/* <div>Kakao Login Redirect</div>
      <div>{authCode}</div> */}
    </div>
  );
};

export default KakaoRedirectPage;
