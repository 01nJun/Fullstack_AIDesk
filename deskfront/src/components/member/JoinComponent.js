import { useState } from "react";
import { joinPost } from "../../api/memberApi";
import useCustomLogin from "../../hooks/useCustomLogin";

const initState = {
  email: "",
  pw: "",
  nickname: "",
  department: "DEVELOPMENT",
};

const JoinComponent = () => {
  const [joinParam, setJoinParam] = useState({ ...initState });
  const { moveToLogin } = useCustomLogin();

  const handleChange = (e) => {
    // 상태 업데이트 방식 개선 (직접 변경 대신 setState 사용)
    setJoinParam({
      ...joinParam,
      [e.target.name]: e.target.value
    });
  };

  const handleClickJoin = (e) => {
    if(e) e.preventDefault();

    if (!joinParam.email || !joinParam.pw || !joinParam.nickname) {
      alert("모든 정보를 입력해주세요.");
      return;
    }
    joinPost(joinParam)
      .then((result) => {
        if (result.result === "success") {
          alert("회원가입이 완료되었습니다.\n관리자 승인 후 로그인이 가능합니다.");
          moveToLogin();
        }
      })
      .catch((err) => {
          const errorCode = err?.response?.data?.code;

          if (errorCode === "DELETED_ACCOUNT") {
            alert("이미 존재하는 이메일 입니다");
          } else {
            alert("회원가입 실패. 다시 시도해주세요.");
          }
      });
  };

  return (
    <div className="ui-card p-8 lg:p-10">
      <div className="flex flex-col items-center mb-8">
        <div className="text-xs uppercase tracking-widest text-baseMuted mb-2">JOIN</div>
        <h1 className="ui-title">회원가입</h1>
        <p className="text-baseMuted text-xs mt-2">전문 계정을 생성하세요</p>
      </div>

      <form className="space-y-4" onSubmit={handleClickJoin}>
        <div>
          <label className="block text-xs font-semibold text-baseMuted mb-2">이메일</label>
          <input
            className="ui-input"
            name="email" type="text" onChange={handleChange} placeholder="example@domain.com"
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-baseMuted mb-2">비밀번호</label>
          <input
            className="ui-input"
            name="pw" type="password" onChange={handleChange} placeholder="••••••••"
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-baseMuted mb-2">닉네임</label>
          <input
            className="ui-input"
            name="nickname" type="text" onChange={handleChange} placeholder="Your Nickname"
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-baseMuted mb-2">부서</label>
          <select
            name="department"
            value={joinParam.department}
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

        <button
          className="w-full ui-btn-primary py-4 mt-6"
          type="submit"
        >
          계정 생성
        </button>
      </form>
    </div>
  );
};

export default JoinComponent;