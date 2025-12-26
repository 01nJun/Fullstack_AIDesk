import { useState } from "react";
import useCustomLogin from "../../hooks/useCustomLogin";
import KakaoLoginComponent from "./KakaoLoginComponent";

const initState = {
  email: "",
  pw: "",
};

const LoginComponent = () => {
  const [loginParam, setLoginParam] = useState({ ...initState });
  const { doLogin, moveToPath } = useCustomLogin();

  const handleChange = (e) => {
    loginParam[e.target.name] = e.target.value;
    setLoginParam({ ...loginParam });
  };

  const handleClickLogin = (e) => {
    doLogin(loginParam).then((data) => {
      if (data.error) {
        if (data.error === "PENDING_APPROVAL") alert("현재 승인 대기 상태입니다.\n관리자 승인 후 로그인해주세요.");
        else if (data.error === "DELETED_ACCOUNT") alert("탈퇴 또는 삭제된 계정입니다.");
        else if (data.error === "BAD_CREDENTIALS") alert("아이디 또는 비밀번호가 일치하지 않습니다.");
        else alert("로그인 실패. 다시 시도해주세요.");
      } else {
        moveToPath("/");
      }
    });
  };

  const handleClickJoin = () => {
    moveToPath("/member/join");
  };

  return (
    <div className="bg-white p-10 rounded-[40px] shadow-2xl border border-gray-100">
      <div className="flex flex-col items-center mb-10">
        <h1 className="text-5xl font-black italic tracking-tighter text-gray-900 border-b-8 border-blue-500 pb-2">
          LOGIN
        </h1>
        <p className="text-gray-400 font-bold mt-4 uppercase tracking-widest text-[10px]">Access your ticket system</p>
      </div>

      <div className="space-y-6">
        <div>
          <label className="block text-[11px] font-black text-gray-400 uppercase tracking-[0.2em] mb-2 ml-2">Email Address</label>
          <input
            className="w-full p-4 rounded-2xl border-2 border-gray-100 bg-gray-50 font-bold focus:border-blue-500 focus:ring-4 focus:ring-blue-100 outline-none transition-all shadow-inner"
            name="email" type="text" value={loginParam.email} onChange={handleChange} placeholder="example@domain.com"
          />
        </div>

        <div>
          <label className="block text-[11px] font-black text-gray-400 uppercase tracking-[0.2em] mb-2 ml-2">Password</label>
          <input
            className="w-full p-4 rounded-2xl border-2 border-gray-100 bg-gray-50 font-bold focus:border-blue-500 focus:ring-4 focus:ring-blue-100 outline-none transition-all shadow-inner"
            name="pw" type="password" value={loginParam.pw} onChange={handleChange} placeholder="••••••••"
          />
        </div>

        <div className="flex gap-4 pt-4">
          <button
            className="flex-1 bg-gray-900 text-white p-5 rounded-3xl font-black text-lg hover:bg-blue-600 hover:scale-[1.02] transition-all shadow-lg active:scale-95"
            onClick={handleClickLogin}
          >
            LOGIN
          </button>
          <button
            className="flex-1 bg-white text-gray-900 border-2 border-gray-900 p-5 rounded-3xl font-black text-lg hover:bg-gray-50 hover:scale-[1.02] transition-all shadow-lg active:scale-95"
            onClick={handleClickJoin}
          >
            JOIN
          </button>
        </div>
      </div>

      <div className="relative my-10">
        <div className="absolute inset-0 flex items-center"><div className="w-full border-t-2 border-gray-100"></div></div>
        <div className="relative flex justify-center text-[10px] font-black uppercase"><span className="bg-white px-4 text-gray-300 tracking-[0.3em]">Social Access</span></div>
      </div>

      <KakaoLoginComponent />
    </div>
  );
};

export default LoginComponent;