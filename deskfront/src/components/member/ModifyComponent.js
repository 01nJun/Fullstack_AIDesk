import { useEffect, useState, useRef } from "react";
import { useSelector } from "react-redux";
import { modifyMember, registerFaceApi, updateFaceStatusApi } from "../../api/memberApi"; // API 추가 확인
import useCustomLogin from "../../hooks/useCustomLogin";
import { validatePassword, getPasswordPolicyText } from "../../util/passwordValidator";
import LoadingModal from "../common/LoadingModal";

const initState = {
  email: "",
  pw: "",
  nickname: "",
  department: "DEVELOPMENT",
  faceEnabled: false,
};

const ModifyComponent = () => {
  const [member, setMember] = useState({ ...initState });
  const [passwordError, setPasswordError] = useState(null);
  const [isRecognizing, setIsRecognizing] = useState(false);
  const loginInfo = useSelector((state) => state.loginSlice);
  const { moveToLogin, doLogout } = useCustomLogin();

  const [showCamera, setShowCamera] = useState(false);
  const videoRef = useRef(null);
  const canvasRef = useRef(null);

  useEffect(() => {
    setMember((prev) => ({
      ...prev,
      email: loginInfo.email,
      pw: "",
      nickname: loginInfo.nickname || "",
      department: loginInfo.department || "DEVELOPMENT",
      faceEnabled: loginInfo.faceEnabled || false,
    }));
  }, [loginInfo]);

  const handleChange = (e) => {
    const newValue = e.target.value;
    setMember({ ...member, [e.target.name]: newValue });

    // 비밀번호 실시간 검증 (입력 중)
    if (e.target.name === "pw") {
      if (newValue && newValue.trim() !== "") {
        const validation = validatePassword(newValue);
        setPasswordError(validation.valid ? null : validation.message);
      } else {
        setPasswordError(null);
      }
    }
  };

  const handleClickModify = (e) => {
    if (e) e.preventDefault();

    if (!member.pw || member.pw.trim() === "") {
      alert("비밀번호를 입력해야 정보 수정이 가능합니다.");
      return;
    }

    // 비밀번호 정책 검증
    const passwordValidation = validatePassword(member.pw);
    if (!passwordValidation.valid) {
      alert(passwordValidation.message);
      return;
    }

    if (!member.nickname) {
      alert("닉네임은 필수 입력 항목입니다.");
      return;
    }
    const memberToSend = { ...member, department: member.department || "DEVELOPMENT" };

    modifyMember(memberToSend)
      .then(async (result) => {
        alert("정보 수정이 완료되었습니다.\n비밀번호가 변경되어 모든 기기에서 로그아웃됩니다.");
        await doLogout();
        moveToLogin();
      })
      .catch((err) => {
        alert("수정 중 오류가 발생했습니다.");
      });
  };

  //  얼굴 인식 사용 여부 토글 로직
  const handleToggleFace = async () => {
    const newStatus = !member.faceEnabled;
    try {
      await updateFaceStatusApi(member.email, newStatus);
      setMember({ ...member, faceEnabled: newStatus });
      alert(newStatus ? "얼굴 로그인이 활성화되었습니다." : "얼굴 로그인이 비활성화되었습니다.");
    } catch (err) {
      alert("상태 변경에 실패했습니다.");
    }
  };

  //  실시간 캠 실행 로직
  const startCamera = async () => {
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

  //  캡처 및 등록 로직
  const captureAndRegister = async () => {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    if (!video || !canvas) return;

    // 로딩 상태 시작
    setIsRecognizing(true);

    const context = canvas.getContext("2d");
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    context.drawImage(video, 0, 0, canvas.width, canvas.height);

    // canvas 이미지를 파일(Blob)로 변환
    canvas.toBlob(async (blob) => {
      const file = new File([blob], "face.jpg", { type: "image/jpeg" });

      try {
        await registerFaceApi(member.email, file); // 백엔드 등록 API 호출
        alert("얼굴 등록이 완료되었습니다!");

        // 스트림 중지 및 카메라 UI 닫기
        const stream = video.srcObject;
        stream.getTracks().forEach(track => track.stop());
        setShowCamera(false);
        setMember({ ...member, faceEnabled: true });
      } catch (err) {
        alert("얼굴 등록 중 오류가 발생했습니다.");
      } finally {
        // 로딩 상태 종료
        setIsRecognizing(false);
      }
    }, "image/jpeg");
  };

  return (
    <div className="ui-card p-8 lg:p-10">
      <div className="flex flex-col items-center mb-8">
        <div className="text-xs uppercase tracking-widest text-baseMuted mb-2">PROFILE</div>
        <h1 className="ui-title">프로필 수정</h1>
        <p className="text-baseMuted text-xs mt-2">개인 정보를 업데이트하세요</p>
      </div>

      <form className="space-y-4" onSubmit={handleClickModify}>
        <div>
          <label className="block text-xs font-semibold text-baseMuted mb-2">이메일 (읽기 전용)</label>
          <input
            className="ui-input bg-baseSurface text-baseMuted"
            name="email" type="text" value={member.email} readOnly
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-baseMuted mb-2">새 비밀번호</label>
          <input
            className={`ui-input ${passwordError ? "border-red-500" : ""}`}
            name="pw" type="password" value={member.pw} onChange={handleChange} placeholder="새 비밀번호를 입력하세요"
          />
          {passwordError && (
            <p className="text-xs text-red-500 mt-1">{passwordError}</p>
          )}
          {!passwordError && member.pw && (
            <p className="text-xs text-green-600 mt-1">✓ 비밀번호 규칙을 만족합니다</p>
          )}
            <p className="text-xs text-baseMuted mt-1">{getPasswordPolicyText()}</p>
        </div>

        <div>
          <label className="block text-xs font-semibold text-baseMuted mb-2">닉네임</label>
          <input
            className="ui-input"
            name="nickname" type="text" value={member.nickname} onChange={handleChange}
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-baseMuted mb-2">부서</label>
          <select
            name="department"
            value={member.department || "DEVELOPMENT"}
            onChange={handleChange}
            className="ui-select"
          >
            <option value="DEVELOPMENT">💻 개발팀 (DEVELOPMENT)</option>
            <option value="SALES">🤝 영업팀 (SALES)</option>
            <option value="HR">👥 인사팀 (HR)</option>
            <option value="DESIGN">🎨 디자인팀 (DESIGN)</option>
            <option value="PLANNING">📝 기획팀 (PLANNING)</option>
            <option value="FINANCE">💰 재무팀 (FINANCE)</option>
          </select>
        </div>

        {/* --- 얼굴 인식 관리 섹션 --- */}
        <div className="mt-6 p-6 bg-baseSurface rounded-ui border-2 border-dashed border-baseBorder">
          <label className="block text-xs font-semibold text-baseMuted mb-4">얼굴 인식 설정</label>

          <div className="flex items-center justify-between mb-4">
            <span className="text-sm font-medium text-baseText">
              얼굴 로그인 사용여부: <span className={member.faceEnabled ? "text-brandNavy" : "text-baseMuted"}>
                {member.faceEnabled ? "ON" : "OFF"}
              </span>
            </span>
            <button
              type="button"
              onClick={handleToggleFace}
              className={`px-4 py-2 rounded-ui font-semibold text-xs transition-all ${
                member.faceEnabled ? "ui-btn-danger" : "ui-btn-primary"
              }`}
            >
              {member.faceEnabled ? "비활성화" : "활성화"}
            </button>
          </div>

          {!showCamera ? (
            <button
              type="button"
              onClick={startCamera}
              className="w-full py-3 ui-btn-secondary"
            >
              {member.faceEnabled ? "얼굴 재등록 (카메라 열기)" : "얼굴 등록 (카메라 열기)"}
            </button>
          ) : (
            <div className="flex flex-col items-center space-y-4">
              <div className="relative w-full aspect-video bg-black rounded-ui overflow-hidden">
                <video ref={videoRef} autoPlay playsInline className="w-full h-full object-cover mirror" />
              </div>
              <div className="flex gap-2 w-full">
                <button
                  type="button"
                  onClick={captureAndRegister}
                  disabled={isRecognizing}
                  className={`flex-1 ui-btn-primary ${isRecognizing ? "opacity-50 cursor-not-allowed" : ""}`}
                >
                  캡처 및 저장
                </button>
                <button
                  type="button"
                  onClick={() => setShowCamera(false)}
                  disabled={isRecognizing}
                  className={`ui-btn-secondary ${isRecognizing ? "opacity-50 cursor-not-allowed" : ""}`}
                >
                  취소
                </button>
              </div>
            </div>
          )}
          {/* 캡처용 캔버스 (숨김) */}
          <canvas ref={canvasRef} className="hidden" />
        </div>

        <button
          className="w-full ui-btn-primary py-4 mt-6"
          type="submit"
        >
          업데이트 및 재승인
        </button>
      </form>

      {/* 얼굴 인식 로딩 모달 */}
      <LoadingModal isOpen={isRecognizing} />
    </div>
  );
};

export default ModifyComponent;