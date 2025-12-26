import React, { useEffect, useState, useCallback } from "react";
import { getPendingList, getActiveList, putApprove, putSoftDelete } from "../../api/adminApi";
import PageComponent from "../common/PageComponent";

const AdminComponent = () => {
  const [activeTab, setActiveTab] = useState("pending");
  const [memberData, setMemberData] = useState({ dtoList: [], totalCount: 0 });
  const [loading, setLoading] = useState(false);
  const [inputKeyword, setInputKeyword] = useState("");
  const [inputDept, setInputDept] = useState("");
  const [searchParams, setSearchParams] = useState({ page: 1, keyword: "", department: "" });

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
      .catch(() => setLoading(false));
  }, [activeTab, searchParams]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const handleSearch = () => {
    setSearchParams({ page: 1, keyword: inputKeyword, department: inputDept });
  };

  const handleTabChange = (tab) => {
    setActiveTab(tab);
    setInputKeyword("");
    setInputDept("");
    setSearchParams({ page: 1, keyword: "", department: "" });
  };

  return (
    <div className="w-full">
      <h1 className="text-4xl font-black mb-10 text-gray-900 border-b-8 border-blue-500 pb-4 inline-block tracking-tighter">
        관리자 대시보드
      </h1>

      <div className="flex flex-col xl:flex-row justify-between items-stretch xl:items-center mb-8 gap-6 bg-white p-6 rounded-3xl shadow-xl border border-gray-100">
        <div className="flex bg-gray-100 p-2 rounded-2xl shadow-inner">
          <button
            onClick={() => handleTabChange("pending")}
            className={`px-8 py-3 rounded-xl font-black text-sm transition-all ${
              activeTab === "pending" ? "bg-white text-blue-600 shadow-md" : "text-gray-400 hover:text-gray-600"
            }`}
          >
            승인 대기
          </button>
          <button
            onClick={() => handleTabChange("active")}
            className={`px-8 py-3 rounded-xl font-black text-sm transition-all ${
              activeTab === "active" ? "bg-white text-blue-600 shadow-md" : "text-gray-400 hover:text-gray-600"
            }`}
          >
            전체 직원
          </button>
        </div>

        <div className="flex items-center gap-3 flex-grow">
          <select
            value={inputDept}
            onChange={(e) => setInputDept(e.target.value)}
            className="border-2 border-gray-200 p-3 rounded-2xl bg-white font-bold focus:border-blue-500 outline-none w-44 shadow-sm"
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
              placeholder="이름이나 이메일을 입력하세요..."
              value={inputKeyword}
              onChange={(e) => setInputKeyword(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
              className="w-full border-2 border-gray-200 p-3 pl-6 rounded-2xl font-bold focus:border-blue-500 focus:ring-4 focus:ring-blue-100 outline-none transition-all shadow-inner"
            />
          </div>

          <button
            onClick={handleSearch}
            className="bg-gray-900 text-white px-8 py-3 rounded-2xl font-black hover:bg-blue-600 transition-all shadow-lg"
          >
            검색
          </button>
        </div>
      </div>

      <div className="bg-white rounded-3xl shadow-2xl overflow-hidden border border-gray-100 min-h-[600px] flex flex-col">
        <div className="p-6 bg-gray-900 text-white flex justify-between items-center">
          <h2 className="text-xl font-black italic uppercase tracking-wider">
            {activeTab === "pending" ? "Approval Waiting List" : "Employee Management"}
          </h2>
          <span className="bg-blue-500 px-6 py-1 rounded-full text-sm font-black italic">
            TOTAL: {memberData?.totalCount || 0}
          </span>
        </div>

        <div className="overflow-x-auto flex-grow">
          <table className="w-full table-fixed">
            <thead>
              <tr className="bg-gray-50 border-b-2 border-gray-100">
                <th className="p-5 text-left text-xs font-black text-gray-400 uppercase tracking-widest">Email Address</th>
                <th className="p-5 w-48 text-center text-xs font-black text-gray-400 uppercase tracking-widest">Nickname</th>
                <th className="p-5 w-48 text-center text-xs font-black text-gray-400 uppercase tracking-widest">Department</th>
                <th className="p-5 w-64 text-center text-xs font-black text-gray-400 uppercase tracking-widest">Management</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {loading ? (
                <tr><td colSpan="4" className="p-40 text-center font-black text-gray-300 animate-pulse">FETCHING MEMBERS...</td></tr>
              ) : memberData?.dtoList?.length > 0 ? (
                memberData.dtoList.map((member) => (
                  <tr key={member.email} className="hover:bg-blue-50/30 transition-all h-[70px]">
                    <td className="p-5 font-bold text-gray-800 truncate">{member.email}</td>
                    <td className="p-5 text-center font-black text-gray-600">{member.nickname}</td>
                    <td className="p-5 text-center">
                      <span className="px-4 py-2 rounded-xl bg-blue-50 text-blue-600 font-black text-xs">
                        {member.department || "미배정"}
                      </span>
                    </td>
                    <td className="p-5">
                      <div className="flex justify-center gap-3">
                        {activeTab === "pending" && (
                          <button
                            onClick={() => putApprove(member.email).then(() => fetchData())}
                            className="bg-blue-600 text-white px-6 py-2 rounded-xl font-black text-sm hover:bg-blue-700 transition-all shadow-md"
                          >
                            승인
                          </button>
                        )}
                        <button
                          onClick={() => putSoftDelete(member.email).then(() => fetchData())}
                          className="border-2 border-red-100 text-red-500 px-6 py-2 rounded-xl font-black text-sm hover:bg-red-50 transition-all"
                        >
                          삭제
                        </button>
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

        <div className="p-8 bg-gray-50 flex justify-center border-t border-gray-100 mt-auto">
          {memberData?.dtoList?.length > 0 && (
            <PageComponent serverData={memberData} movePage={(p) => setSearchParams(prev => ({ ...prev, page: p.page }))} />
          )}
        </div>
      </div>
    </div>
  );
};

export default AdminComponent;