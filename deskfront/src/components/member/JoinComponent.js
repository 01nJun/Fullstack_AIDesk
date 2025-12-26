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
    joinParam[e.target.name] = e.target.value;
    setJoinParam({ ...joinParam });
  };

  const handleClickJoin = () => {
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
        alert("회원가입 실패. 다시 시도해주세요.");
      });
  };

  return (
    <div className="bg-white p-10 rounded-[40px] shadow-2xl border border-gray-100">
      <div className="flex flex-col items-center mb-10">
        <h1 className="text-4xl font-black italic tracking-tighter text-gray-900 border-b-8 border-blue-500 pb-2 uppercase">
          Join Account
        </h1>
        <p className="text-gray-400 font-bold mt-4 uppercase tracking-widest text-[10px]">Create your professional account</p>
      </div>

      <div className="space-y-5">
        <div>
          <label className="block text-[11px] font-black text-gray-400 uppercase tracking-[0.2em] mb-2 ml-2">Email Address</label>
          <input
            className="w-full p-4 rounded-2xl border-2 border-gray-100 bg-gray-50 font-bold focus:border-blue-500 focus:ring-4 focus:ring-blue-100 outline-none transition-all shadow-inner"
            name="email" type="text" onChange={handleChange} placeholder="example@domain.com"
          />
        </div>

        <div>
          <label className="block text-[11px] font-black text-gray-400 uppercase tracking-[0.2em] mb-2 ml-2">Password</label>
          <input
            className="w-full p-4 rounded-2xl border-2 border-gray-100 bg-gray-50 font-bold focus:border-blue-500 focus:ring-4 focus:ring-blue-100 outline-none transition-all shadow-inner"
            name="pw" type="password" onChange={handleChange} placeholder="••••••••"
          />
        </div>

        <div>
          <label className="block text-[11px] font-black text-gray-400 uppercase tracking-[0.2em] mb-2 ml-2">Nickname</label>
          <input
            className="w-full p-4 rounded-2xl border-2 border-gray-100 bg-gray-50 font-bold focus:border-blue-500 focus:ring-4 focus:ring-blue-100 outline-none transition-all shadow-inner"
            name="nickname" type="text" onChange={handleChange} placeholder="Your Nickname"
          />
        </div>

        <div>
          <label className="block text-[11px] font-black text-gray-400 uppercase tracking-[0.2em] mb-2 ml-2">Department</label>
          <div className="relative">
            <select
              name="department"
              value={joinParam.department}
              onChange={handleChange}
              className="w-full p-4 rounded-2xl border-2 border-gray-100 bg-gray-50 font-black text-gray-700 focus:border-blue-500 outline-none transition-all shadow-sm appearance-none"
            >
              <option value="DEVELOPMENT">💻 개발팀 (DEVELOPMENT)</option>
              <option value="SALES">🤝 영업팀 (SALES)</option>
              <option value="HR">👥 인사팀 (HR)</option>
              <option value="DESIGN">🎨 디자인팀 (DESIGN)</option>
              <option value="PLANNING">📝 기획팀 (PLANNING)</option>
              <option value="FINANCE">💰 재무팀 (FINANCE)</option>
            </select>
            <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-4 text-gray-400">
              ▼
            </div>
          </div>
        </div>

        <button
          className="w-full bg-blue-600 text-white p-5 rounded-3xl font-black text-xl hover:bg-gray-900 hover:scale-[1.02] transition-all shadow-xl mt-6 active:scale-95"
          type="button"
          onClick={handleClickJoin}
        >
          CREATE ACCOUNT
        </button>
      </div>
    </div>
  );
};

export default JoinComponent;