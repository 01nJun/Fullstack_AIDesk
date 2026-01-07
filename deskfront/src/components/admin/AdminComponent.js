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
      <div className="mb-8">
        <div className="text-xs uppercase tracking-widest text-baseMuted mb-2">ADMIN</div>
        <h1 className="ui-title">
          관리자 대시보드
        </h1>
      </div>

      <div className="flex flex-col gap-4 lg:gap-6 mb-8 ui-card p-4 sm:p-6">
        <div className="flex bg-baseSurface p-1.5 sm:p-2 rounded-ui">
          <button
            onClick={() => handleTabChange("pending")}
            className={`px-3 sm:px-4 lg:px-6 py-2 sm:py-2.5 rounded-ui font-semibold text-xs sm:text-sm transition-all flex-1 ${
              activeTab === "pending" ? "bg-baseBg text-brandNavy shadow-ui" : "text-baseMuted hover:text-baseText"
            }`}
          >
            승인 대기
          </button>
          <button
            onClick={() => handleTabChange("active")}
            className={`px-3 sm:px-4 lg:px-6 py-2 sm:py-2.5 rounded-ui font-semibold text-xs sm:text-sm transition-all flex-1 ${
              activeTab === "active" ? "bg-baseBg text-brandNavy shadow-ui" : "text-baseMuted hover:text-baseText"
            }`}
          >
            전체 직원
          </button>
        </div>

        {/* 검색 영역을 form으로 변경: 엔터 시 handleSearch 실행 */}
        <form
          className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 flex-grow"
          onSubmit={(e) => {
            e.preventDefault();
            handleSearch();
          }}
        >
          <select
            value={inputDept}
            onChange={(e) => setInputDept(e.target.value)}
            className="ui-select w-full sm:w-44"
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
              className="ui-input w-full"
            />
          </div>

          <button
            type="submit"
            className="ui-btn-primary w-full sm:w-auto whitespace-nowrap"
          >
            검색
          </button>
        </form>
      </div>

      <div className="ui-card overflow-hidden min-h-[600px] flex flex-col">
        <div className="px-4 sm:px-6 py-3 sm:py-4 bg-baseSurface border-b border-baseBorder flex justify-between items-center">
          <h2 className="text-xs sm:text-sm font-semibold text-baseText uppercase tracking-wide">
            {activeTab === "pending" ? "승인 대기 목록" : "직원 관리"}
          </h2>
          <span className="text-xs text-baseMuted font-medium">
            총 {memberData?.totalCount || 0}명
          </span>
        </div>

        {/* 데스크톱 테이블 뷰 */}
        <div className="hidden lg:block overflow-x-auto flex-grow">
          <table className="ui-table">
            <thead>
              <tr>
                <th className="text-left">이메일</th>
                <th className="w-48 text-center">닉네임</th>
                <th className="w-48 text-center">부서</th>
                <th className="w-64 text-center">관리</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr><td colSpan="4" className="p-20 text-center text-baseMuted">로딩 중...</td></tr>
              ) : memberData?.dtoList?.length > 0 ? (
                memberData.dtoList.map((member) => (
                  <tr key={member.email}>
                    <td className="font-semibold text-baseText truncate">{member.email}</td>
                    <td className="text-center text-baseText">{member.nickname}</td>
                    <td className="text-center">
                      <span className="ui-badge">
                        {member.department || "미배정"}
                      </span>
                    </td>
                    <td>
                      <div className="flex justify-center gap-2">
                        {activeTab === "pending" && (
                          <button
                            onClick={() => putApprove(member.email).then(() => fetchData())}
                            className="ui-btn-primary text-xs px-4 py-1.5"
                          >
                            승인
                          </button>
                        )}
                        <button
                          onClick={() => putSoftDelete(member.email).then(() => fetchData())}
                          className="ui-btn-danger text-xs px-4 py-1.5"
                        >
                          삭제
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr><td colSpan="4" className="p-20 text-center text-baseMuted">데이터가 없습니다.</td></tr>
              )}
            </tbody>
          </table>
        </div>

        {/* 모바일/태블릿 카드 뷰 */}
        <div className="lg:hidden flex-grow p-4 space-y-3">
          {loading ? (
            <div className="p-20 text-center text-baseMuted">로딩 중...</div>
          ) : memberData?.dtoList?.length > 0 ? (
            memberData.dtoList.map((member) => (
              <div
                key={member.email}
                className="bg-baseBg border border-baseBorder rounded-ui p-4"
              >
                <div className="mb-3">
                  <div className="font-semibold text-base text-baseText mb-1 truncate">{member.email}</div>
                  <div className="flex items-center gap-2 mb-2">
                    <span className="text-sm text-baseMuted">닉네임:</span>
                    <span className="text-sm font-medium text-baseText">{member.nickname}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="text-sm text-baseMuted">부서:</span>
                    <span className="ui-badge">
                      {member.department || "미배정"}
                    </span>
                  </div>
                </div>
                <div className="flex gap-2 pt-3 border-t border-baseBorder">
                  {activeTab === "pending" && (
                    <button
                      onClick={() => putApprove(member.email).then(() => fetchData())}
                      className="ui-btn-primary text-xs px-4 py-2 flex-1"
                    >
                      승인
                    </button>
                  )}
                  <button
                    onClick={() => putSoftDelete(member.email).then(() => fetchData())}
                    className="ui-btn-danger text-xs px-4 py-2 flex-1"
                  >
                    삭제
                  </button>
                </div>
              </div>
            ))
          ) : (
            <div className="p-20 text-center text-baseMuted">데이터가 없습니다.</div>
          )}
        </div>

        <div className="p-4 sm:p-6 bg-baseSurface flex justify-center border-t border-baseBorder mt-auto">
          {memberData?.dtoList?.length > 0 && (
            <PageComponent serverData={memberData} movePage={(p) => setSearchParams(prev => ({ ...prev, page: p.page }))} />
          )}
        </div>
      </div>
    </div>
  );
};

export default AdminComponent;