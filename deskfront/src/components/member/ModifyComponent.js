import { useEffect, useState } from "react";
import { useSelector, useDispatch } from "react-redux";
import { modifyMember } from "../../api/memberApi";
import { logout } from "../../slices/loginSlice";
import useCustomLogin from "../../hooks/useCustomLogin";

const initState = {
  email: "",
  pw: "",
  nickname: "",
  department: "DEVELOPMENT",
};

const ModifyComponent = () => {
  const [member, setMember] = useState({ ...initState });
  const loginInfo = useSelector((state) => state.loginSlice);
  const { moveToLogin } = useCustomLogin();
  const dispatch = useDispatch();

  useEffect(() => {
    setMember((prev) => ({
      ...prev,
      email: loginInfo.email,
      pw: "",
      nickname: loginInfo.nickname || "",
      department: loginInfo.department || "DEVELOPMENT",
    }));
  }, [loginInfo]);

  const handleChange = (e) => {
    setMember({ ...member, [e.target.name]: e.target.value });
  };

  const handleClickModify = (e) => {
    if(e) e.preventDefault();

    if (!member.nickname) {
      alert("닉네임은 필수 입력 항목입니다.");
      return;
    }
    const memberToSend = { ...member, department: member.department || "DEVELOPMENT" };

    modifyMember(memberToSend)
      .then((result) => {
        alert("정보 수정이 완료되었습니다.\n관리자 승인 대기 상태로 전환됩니다.\n승인 후 로그인해주세요.");
        dispatch(logout());
        moveToLogin();
      })
      .catch((err) => {
        alert("수정 중 오류가 발생했습니다.");
      });
  };

  return (
    <div className="bg-white p-10 rounded-[40px] shadow-2xl border border-gray-100">
      <div className="flex flex-col items-center mb-10">
        <h1 className="text-4xl font-black italic tracking-tighter text-gray-900 border-b-8 border-blue-500 pb-2 uppercase">
          Modify Profile
        </h1>
        <p className="text-gray-400 font-bold mt-4 uppercase tracking-widest text-[10px]">Update your personal information</p>
      </div>

      <form className="space-y-5" onSubmit={handleClickModify}>
        <div>
          <label className="block text-[11px] font-black text-gray-400 uppercase tracking-[0.2em] mb-2 ml-2">Email (Read Only)</label>
          <input
            className="w-full p-4 rounded-2xl border-2 border-transparent bg-gray-100 font-bold text-gray-400 outline-none shadow-inner"
            name="email" type="text" value={member.email} readOnly
          />
        </div>

        <div>
          <label className="block text-[11px] font-black text-gray-400 uppercase tracking-[0.2em] mb-2 ml-2">New Password</label>
          <input
            className="w-full p-4 rounded-2xl border-2 border-gray-100 bg-gray-50 font-bold focus:border-blue-500 focus:ring-4 focus:ring-blue-100 outline-none transition-all shadow-inner"
            name="pw" type="password" value={member.pw} onChange={handleChange} placeholder="Enter new password"
          />
          <p className="text-[10px] text-blue-500 font-bold mt-2 ml-2 uppercase italic">* Optional: Set password for direct login</p>
        </div>

        <div>
          <label className="block text-[11px] font-black text-gray-400 uppercase tracking-[0.2em] mb-2 ml-2">Nickname</label>
          <input
            className="w-full p-4 rounded-2xl border-2 border-gray-100 bg-gray-50 font-bold focus:border-blue-500 focus:ring-4 focus:ring-blue-100 outline-none transition-all shadow-inner"
            name="nickname" type="text" value={member.nickname} onChange={handleChange}
          />
        </div>

        <div>
          <label className="block text-[11px] font-black text-gray-400 uppercase tracking-[0.2em] mb-2 ml-2">Department</label>
          <div className="relative">
            <select
              name="department"
              value={member.department || "DEVELOPMENT"}
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
          type="submit"
        >
          UPDATE & RE-APPROVE
        </button>
      </form>
    </div>
  );
};

export default ModifyComponent;