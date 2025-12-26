import { useEffect, useState, useCallback } from "react";
import { getPendingList, getActiveList, putApprove, putSoftDelete } from "../../api/adminApi";
import PageComponent from "../common/PageComponent";

const AdminComponent = () => {
  const [activeTab, setActiveTab] = useState("pending");
  const [memberData, setMemberData] = useState({ dtoList: [], totalCount: 0 });
  const [loading, setLoading] = useState(false);

  // 1. [상태 변경] 사용자가 타이핑하는 값 (입력용 로컬 상태)
  const [inputKeyword, setInputKeyword] = useState("");
  const [inputDept, setInputDept] = useState("");

  // 2. [상태 변경] 실제 서버로 보낼 확정된 검색 조건
  const [searchParams, setSearchParams] = useState({
    page: 1,
    keyword: "",
    department: ""
  });

  // 3. [로직 변경] fetchData는 searchParams에 의존합니다.
  const fetchData = useCallback(() => {
    setLoading(true);

    const params = {
      page: searchParams.page,
      size: 10,
      keyword: searchParams.keyword || null,
      department: searchParams.department || null
    };

    const apiCall = activeTab === "pending" ? getPendingList : getActiveList;

    apiCall(params)
      .then((data) => {
        setMemberData(data || { dtoList: [], totalCount: 0 });
        setLoading(false);
      })
      .catch((err) => {
        console.error("데이터 로드 실패:", err.response || err);
        setLoading(false);
      });
  }, [activeTab, searchParams]); // searchParams가 바뀔 때만 함수 재생성

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // 검색 버튼 클릭 시 실행
  const handleSearch = () => {
    setSearchParams({
      page: 1, // 검색 시 무조건 1페이지부터
      keyword: inputKeyword,
      department: inputDept
    });
  };

  const handleTabChange = (tab) => {
    setActiveTab(tab);
    setInputKeyword("");
    setInputDept("");
    setSearchParams({ page: 1, keyword: "", department: "" });
  };

  const handleApprove = (email) => {
    if (window.confirm(`${email} 회원을 승인하시겠습니까?`)) {
      putApprove(email).then(() => { alert("승인 완료"); fetchData(); });
    }
  };

  const handleDelete = (email) => {
    if (window.confirm(`${email} 회원을 삭제하시겠습니까?`)) {
      putSoftDelete(email).then(() => { alert("삭제 완료"); fetchData(); });
    }
  };

  const movePageHandler = (pageParam) => {
    setSearchParams(prev => ({ ...prev, page: pageParam.page }));
  };

  return (
    <div className="w-full p-8 bg-gray-50 min-h-screen">
      <div className="max-w-7xl mx-auto">
        <h1 className="text-4xl font-black mb-10 text-gray-900 border-b-8 border-blue-500 pb-4 inline-block tracking-tighter">
          관리자 대시보드
        </h1>

        <div className="flex flex-col xl:flex-row justify-between items-stretch xl:items-center mb-8 gap-6 bg-white p-8 rounded-3xl shadow-xl border border-gray-100">
          {/* 탭 버튼 */}
          <div className="flex bg-gray-100 p-2 rounded-2xl shadow-inner">
            <button
              onClick={() => handleTabChange("pending")}
              className={`px-12 py-4 rounded-xl font-black text-xl transition-all ${
                activeTab === "pending" ? "bg-white text-blue-600 shadow-lg" : "text-gray-400 hover:text-gray-600"
              }`}
            >
              승인 대기
            </button>
            <button
              onClick={() => handleTabChange("active")}
              className={`px-12 py-4 rounded-xl font-black text-xl transition-all ${
                activeTab === "active" ? "bg-white text-blue-600 shadow-lg" : "text-gray-400 hover:text-gray-600"
              }`}
            >
              전체 직원
            </button>
          </div>

          {/* 검색 영역 */}
          <div className="flex items-center gap-4 flex-grow">
            <select
              value={inputDept}
              onChange={(e) => setInputDept(e.target.value)} // 입력값만 변경
              className="border-2 border-gray-200 p-4 rounded-2xl bg-white font-bold focus:border-blue-500 outline-none w-56 text-lg"
            >
              <option value="">모든 부서</option>
              <option value="DEVELOPMENT">개발팀</option>
              <option value="SALES">영업팀</option>
              <option value="HR">인사팀</option>
              <option value="DESIGN">디자인팀</option>
              <option value="PLANNING">기획팀</option>
              <option value="FINANCE">재무팀</option>
            </select>

            <div className="relative flex-grow">
              <input
                type="text"
                placeholder="찾으시는 직원의 이름이나 이메일을 입력하세요..."
                value={inputKeyword}
                onChange={(e) => setInputKeyword(e.target.value)} // 입력값만 변경
                onKeyDown={(e) => e.key === 'Enter' && handleSearch()} // 엔터키 지원
                className="w-full border-2 border-gray-200 p-5 pl-8 rounded-2xl font-bold focus:border-blue-500 focus:ring-4 focus:ring-blue-100 outline-none transition-all text-xl shadow-inner"
              />
            </div>

            {/* 검색 버튼 추가 */}
            <button
              onClick={handleSearch}
              className="bg-gray-900 text-white px-10 py-5 rounded-2xl font-black text-xl hover:bg-blue-600 transition-all shadow-lg"
            >
              검색
            </button>
          </div>
        </div>

        {/* --- 리스트 테이블 (기존과 동일) --- */}
        <div className="bg-white rounded-3xl shadow-2xl overflow-hidden border border-gray-100">
          <div className="p-8 bg-gray-900 text-white flex justify-between items-center">
            <h2 className="text-2xl font-black">
              {activeTab === "pending" ? "🚀 승인 대기 회원" : "👥 전체 구성원 리스트"}
            </h2>
            <span className="bg-blue-500 px-6 py-2 rounded-full text-lg font-black italic">
              TOTAL: {memberData?.totalCount || 0}
            </span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full border-collapse">
              <thead>
                <tr className="bg-gray-50 border-b-2 border-gray-100">
                  <th className="p-6 text-left text-sm font-black text-gray-400 uppercase tracking-widest">Email Address</th>
                  <th className="p-6 text-center text-sm font-black text-gray-400 uppercase tracking-widest">Nickname</th>
                  <th className="p-6 text-center text-sm font-black text-gray-400 uppercase tracking-widest">Dept.</th>
                  <th className="p-6 text-center text-sm font-black text-gray-400 uppercase tracking-widest">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {loading ? (
                  <tr><td colSpan="4" className="p-20 text-center font-bold text-gray-400 animate-pulse">데이터를 가져오는 중...</td></tr>
                ) : memberData?.dtoList?.length > 0 ? (
                  memberData.dtoList.map((member) => (
                    <tr key={member.email} className="hover:bg-blue-50/30 transition-all">
                      <td className="p-6 font-bold text-gray-800 text-lg">{member.email}</td>
                      <td className="p-6 text-center font-black text-gray-600">{member.nickname}</td>
                      <td className="p-6 text-center">
                        <span className="px-4 py-2 rounded-xl bg-blue-50 text-blue-600 font-black text-xs">
                          {member.department || "미배정"}
                        </span>
                      </td>
                      <td className="p-6">
                        <div className="flex justify-center gap-4">
                          {activeTab === "pending" && (
                            <button onClick={() => handleApprove(member.email)} className="bg-blue-600 text-white px-8 py-3 rounded-2xl font-black hover:bg-blue-700 shadow-lg shadow-blue-200 transition-all">승인</button>
                          )}
                          <button onClick={() => handleDelete(member.email)} className="bg-white text-red-500 border-2 border-red-100 px-8 py-3 rounded-2xl font-black hover:bg-red-50 transition-all">삭제</button>
                        </div>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr><td colSpan="4" className="p-40 text-center text-gray-300 font-black text-2xl uppercase italic">No Data Found</td></tr>
                )}
              </tbody>
            </table>
          </div>

          {memberData?.dtoList?.length > 0 && (
            <div className="p-10 bg-gray-50 flex justify-center border-t border-gray-100">
              <PageComponent serverData={memberData} movePage={movePageHandler} />
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default AdminComponent;