import { useState, useRef } from "react";
import { useDispatch } from "react-redux"; // useDispatch 추가
import { login } from "../../slices/loginSlice"; // login 액션 임포트
import useCustomLogin from "../../hooks/useCustomLogin";
import KakaoLoginComponent from "./KakaoLoginComponent";
import { loginFace } from "../../api/memberApi";
import LoadingModal from "../common/LoadingModal";

const initState = { email: "", pw: "" };

const LoginComponent = () => {
  const [loginParam, setLoginParam] = useState({ ...initState });
  const { doLogin, moveToPath } = useCustomLogin();
  const dispatch = useDispatch(); // 리덕스 디스패치 생성

  // 얼굴 인식 관련 상태
  const [showCamera, setShowCamera] = useState(false);
  const [isRecognizing, setIsRecognizing] = useState(false);
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

  const captureAndLogin = async () => {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    if (!video || !canvas) return;

    // 로딩 상태 시작
    setIsRecognizing(true);

    const context = canvas.getContext("2d");
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    context.drawImage(video, 0, 0, canvas.width, canvas.height);

    canvas.toBlob(async (blob) => {
      const file = new File([blob], "login_face.jpg", { type: "image/jpeg" });
      try {
        const data = await loginFace(file); // 얼굴 로그인 API 호출 (백엔드에서 MemberDTO 반환)

        if (data && data.error) {
          // 전체 error 객체 전달
          handleError(data);
        } else {
          dispatch(login(data));

          stopCamera();
          moveToPath("/"); // 메인 페이지로 이동
        }
      } catch (err) {
        // 에러 응답 객체 전체 전달
        const errorData = err.response?.data || { error: "인증 오류" };
        handleError(errorData);
      } finally {
        // 로딩 상태 종료
        setIsRecognizing(false);
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

  const handleError = (errorData) => {
    // errorData가 객체인 경우 (새로운 응답 형식)
    let error, message, remainingAttempts, remainingMinutes;
    
    if (typeof errorData === "object" && errorData !== null) {
      error = errorData.error;
      message = errorData.message;
      remainingAttempts = errorData.remainingAttempts;
      remainingMinutes = errorData.remainingMinutes;
    } else {
      // 이전 형식 (문자열만 전달된 경우)
      error = errorData;
    }

    // ACCOUNT_LOCKED인 경우 remainingMinutes를 우선 체크
    if (error === "ACCOUNT_LOCKED") {
      if (remainingMinutes !== undefined && remainingMinutes !== null && remainingMinutes >= 0) {
        alert(`로그인이 잠겨 있습니다.\n남은 시간: ${remainingMinutes}분`);
        return;
      } else if (message) {
        // message에 이미 남은 시간이 포함되어 있을 수 있음
        if (message.includes("남은 시간:")) {
          alert(message);
          return;
        } else {
          // message에서 시간 추출 시도
          const timeMatch = message.match(/남은 시간:\s*(\d+)/);
          if (timeMatch && timeMatch[1]) {
            const extractedMinutes = parseInt(timeMatch[1]);
            alert(`로그인이 잠겨 있습니다.\n남은 시간: ${extractedMinutes}분`);
            return;
          }
        }
        alert(message);
        return;
      } else {
        alert("로그인이 잠겨 있습니다.");
        return;
      }
    }

    // BAD_CREDENTIALS인 경우 remainingAttempts 체크
    if (error === "BAD_CREDENTIALS") {
      if (remainingAttempts !== undefined && remainingAttempts !== null) {
        alert(`아이디 또는 비밀번호가 틀립니다.\n남은 시도 횟수: ${remainingAttempts}회`);
        return;
      } else if (message) {
        alert(message);
        return;
      } else {
        alert("아이디 또는 비밀번호가 틀립니다.");
        return;
      }
    }

    // message가 있으면 우선 사용
    if (message) {
      alert(message);
      return;
    }

    // 기존 에러 코드 처리
    if (error === "PENDING_APPROVAL") {
      alert("현재 승인 대기 상태입니다.");
    } else if (error === "DELETED_ACCOUNT") {
      alert("탈퇴된 계정입니다.");
    } else if (error === "FACE_LOGIN_DISABLED") {
      alert("얼굴 로그인이 비활성화되어 있습니다. 일반 로그인 후 마이페이지에서 활성화해주세요.");
    } else if (error === "FACE_NOT_RECOGNIZED") {
      alert("등록된 얼굴을 찾을 수 없습니다.");
    } else if (error === "ERROR_LOGIN") {
      // ERROR_LOGIN은 기본 에러 - message가 있으면 사용, 없으면 기본 메시지
      alert(message || "로그인에 실패했습니다.");
    } else {
      alert("로그인 실패: " + (error || "알 수 없는 오류"));
    }
  };

  const handleClickLogin = (e) => {
    if (e) e.preventDefault();
    doLogin(loginParam)
      .then((data) => {
        // 성공 또는 에러 응답 모두 여기로 옴
        if (data && data.error) {
          // 전체 error 객체 전달 (message, remainingAttempts, remainingMinutes 등 포함)
          handleError(data);
        } else {
          moveToPath("/");
        }
      })
      .catch((err) => {
        // rejected 상태로 반환된 경우 (rejectWithValue로 전달된 데이터)
        const errorData = (err && typeof err === 'object') ? err : { error: "로그인에 실패했습니다.", message: "로그인에 실패했습니다." };
        handleError(errorData);
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
                <button 
                  type="button" 
                  onClick={captureAndLogin} 
                  disabled={isRecognizing}
                  className={`flex-1 ui-btn-primary text-xs py-2.5 ${isRecognizing ? "opacity-50 cursor-not-allowed" : ""}`}
                >
                  인식하기
                </button>
                <button 
                  type="button" 
                  onClick={stopCamera} 
                  disabled={isRecognizing}
                  className={`ui-btn-secondary text-xs py-2.5 ${isRecognizing ? "opacity-50 cursor-not-allowed" : ""}`}
                >
                  취소
                </button>
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

      {/* 얼굴 인식 로딩 모달 */}
      <LoadingModal isOpen={isRecognizing} />
    </div>
  );
};

export default LoginComponent;