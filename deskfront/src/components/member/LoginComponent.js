import { useState, useRef } from "react";
import { useDispatch } from "react-redux"; // useDispatch 추가
import { login } from "../../slices/loginSlice"; // login 액션 임포트
import useCustomLogin from "../../hooks/useCustomLogin";
import KakaoLoginComponent from "./KakaoLoginComponent";
import { loginFace } from "../../api/memberApi";

const initState = { email: "", pw: "" };

const LoginComponent = () => {
  const [loginParam, setLoginParam] = useState({ ...initState });
  const { doLogin, moveToPath } = useCustomLogin();
  const dispatch = useDispatch(); // 리덕스 디스패치 생성

  // 얼굴 인식 관련 상태
  const [showCamera, setShowCamera] = useState(false);
  const videoRef = useRef(null);
  const canvasRef = useRef(null);

  const handleChange = (e) => {
    setLoginParam({ ...loginParam, [e.target.name]: e.target.value });
  };

  const startFaceLoginCamera = async () => {
    setShowCamera(true);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: true });
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
      }
    } catch (err) {
      alert("카메라를 켤 수 없습니다. 권한을 확인해주세요.");
      setShowCamera(false);
    }
  };

  const captureAndLogin = () => {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    if (!video || !canvas) return;

    const context = canvas.getContext("2d");
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    context.drawImage(video, 0, 0, canvas.width, canvas.height);

    canvas.toBlob(async (blob) => {
      const file = new File([blob], "login_face.jpg", { type: "image/jpeg" });
      try {
        const data = await loginFace(file); // 얼굴 로그인 API 호출 (백엔드에서 MemberDTO 반환)

        if (data && data.error) {
          handleError(data.error);
        } else {
          dispatch(login(data));

          stopCamera();
          moveToPath("/"); // 메인 페이지로 이동
        }
      } catch (err) {
        handleError(err.response?.data?.error || "인증 오류");
      }
    }, "image/jpeg");
  };

  const stopCamera = () => {
    if (videoRef.current && videoRef.current.srcObject) {
      const stream = videoRef.current.srcObject;
      stream.getTracks().forEach(track => track.stop());
    }
    setShowCamera(false);
  };

  const handleError = (error) => {
    if (error === "PENDING_APPROVAL") alert("현재 승인 대기 상태입니다.");
    else if (error === "DELETED_ACCOUNT") alert("탈퇴된 계정입니다.");
    else if (error === "FACE_LOGIN_DISABLED") alert("얼굴 로그인이 비활성화되어 있습니다. 일반 로그인 후 마이페이지에서 활성화해주세요.");
    else if (error === "FACE_NOT_RECOGNIZED") alert("등록된 얼굴을 찾을 수 없습니다.");
    else if (error === "BAD_CREDENTIALS") alert("아이디 또는 비밀번호가 틀립니다.");
    else alert("로그인 실패: " + error);
  };

  const handleClickLogin = (e) => {
    if (e) e.preventDefault();
    doLogin(loginParam).then((data) => {
      if (data.error) handleError(data.error);
      else moveToPath("/");
    });
  };

  return (
    <div className="ui-card p-8 lg:p-10">
      <div className="flex flex-col items-center mb-8">
        <div className="text-xs uppercase tracking-widest text-baseMuted mb-2">LOGIN</div>
        <h1 className="ui-title">로그인</h1>
      </div>

      <form className="space-y-4" onSubmit={handleClickLogin}>
        <div>
          <label className="block text-xs font-semibold text-baseMuted mb-2">이메일</label>
          <input className="ui-input" name="email" type="text" value={loginParam.email} onChange={handleChange} />
        </div>
        <div>
          <label className="block text-xs font-semibold text-baseMuted mb-2">비밀번호</label>
          <input className="ui-input" name="pw" type="password" value={loginParam.pw} onChange={handleChange} />
        </div>

        <div className="flex flex-col gap-3 pt-4">
          <div className="flex gap-3">
            <button type="submit" className="flex-1 ui-btn-primary">로그인</button>
            <button type="button" className="flex-1 ui-btn-secondary" onClick={() => moveToPath("/member/join")}>회원가입</button>
          </div>

          {!showCamera ? (
            <button
              type="button"
              onClick={startFaceLoginCamera}
              className="w-full ui-btn-secondary py-3"
            >
              📷 얼굴 인식 로그인
            </button>
          ) : (
            <div className="flex flex-col items-center space-y-4 p-4 bg-baseSurface rounded-ui border-2 border-dashed border-baseBorder">
              <div className="relative w-full aspect-square max-w-[240px] bg-black rounded-full overflow-hidden border-4 border-baseBg">
                <video ref={videoRef} autoPlay playsInline className="w-full h-full object-cover" />
              </div>
              <p className="text-xs font-medium text-baseMuted">인식 버튼을 눌러주세요</p>
              <div className="flex gap-2 w-full">
                <button type="button" onClick={captureAndLogin} className="flex-1 ui-btn-primary text-xs py-2.5">인식하기</button>
                <button type="button" onClick={stopCamera} className="ui-btn-secondary text-xs py-2.5">취소</button>
              </div>
            </div>
          )}
          <canvas ref={canvasRef} className="hidden" />
        </div>
      </form>

      <div className="relative my-8">
        <div className="absolute inset-0 flex items-center"><div className="w-full border-t border-baseBorder"></div></div>
        <div className="relative flex justify-center text-xs font-semibold"><span className="bg-baseBg px-4 text-baseMuted">소셜 로그인</span></div>
      </div>
      <KakaoLoginComponent />
    </div>
  );
};

export default LoginComponent;