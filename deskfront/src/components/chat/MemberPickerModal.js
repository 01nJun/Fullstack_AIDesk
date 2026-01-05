import React from "react";

/**
 * 멤버 검색/선택 모달 컴포넌트
 *
 * @param {boolean} open - 모달 열림 여부
 * @param {string} title - 모달 제목
 * @param {boolean} multi - 다중 선택 여부
 * @param {string} keyword - 검색 키워드
 * @param {Function} onChangeKeyword - 검색 키워드 변경 핸들러
 * @param {Array} results - 검색 결과 배열 [{ email: string, nickname?: string, department?: string }]
 * @param {Array} selected - 선택된 이메일 배열
 * @param {Function} onToggle - 선택 토글 핸들러 (email: string) => void
 * @param {boolean} loading - 로딩 상태
 * @param {string|null} error - 에러 메시지
 * @param {Function} onClose - 모달 닫기 핸들러
 * @param {Function} onConfirm - 확인 버튼 핸들러
 * @param {boolean} showGroupName - 그룹 이름 입력 표시 여부
 * @param {string} groupName - 그룹 이름
 * @param {Function} onChangeGroupName - 그룹 이름 변경 핸들러
 * @param {string} selectedDepartment - 선택된 부서
 * @param {Function} onChangeDepartment - 부서 변경 핸들러
 */
const MemberPickerModal = ({
  open,
  title,
  multi = false,
  keyword,
  onChangeKeyword,
  results = [],
  selected = [],
  onToggle,
  loading = false,
  error = null,
  onClose,
  onConfirm,
  showGroupName = false,
  groupName = "",
  onChangeGroupName,
  selectedDepartment = "",
  onChangeDepartment,
}) => {
  // 부서 목록 및 이름 매핑
  const departments = [
    { value: "DEVELOPMENT", label: "💻 개발팀", color: "blue" },
    { value: "SALES", label: "📊 영업팀", color: "green" },
    { value: "HR", label: "👥 인사팀", color: "purple" },
    { value: "DESIGN", label: "🎨 디자인팀", color: "pink" },
    { value: "PLANNING", label: "📝 기획팀", color: "yellow" },
    { value: "FINANCE", label: "💰 재무팀", color: "indigo" },
  ];

  const getDepartmentLabel = (dept) => {
    const deptObj = departments.find((d) => d.value === dept);
    return deptObj ? deptObj.label : dept || "부서 미정";
  };
  if (!open) return null;

  return (
    <div
      className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-[2.5rem] p-8 w-full max-w-md shadow-2xl"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="text-2xl font-black mb-6 text-[#111827] uppercase tracking-tight">
          {title}
        </h2>

        {showGroupName && (
          <div className="mb-6">
            <label className="block text-sm font-bold mb-2 text-gray-700 uppercase tracking-wide">
              그룹 이름
            </label>
            <input
              type="text"
              value={groupName}
              onChange={(e) => onChangeGroupName?.(e.target.value)}
              placeholder="그룹 채팅방 이름을 입력하세요"
              className="w-full px-4 py-3 border-2 border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
            />
          </div>
        )}

        <div className="mb-6">
          <label className="block text-sm font-bold mb-2 text-gray-700 uppercase tracking-wide">
            참여자 선택 (복수 선택 가능)
          </label>

          {/* 부서 카테고리 버튼 */}
          <div className="mb-3">
            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                onClick={() => onChangeDepartment?.("")}
                className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
                  selectedDepartment === ""
                    ? "bg-[#111827] text-white shadow-md"
                    : "bg-gray-100 text-gray-600 hover:bg-gray-200"
                }`}
              >
                전체
              </button>
              {departments.map((dept) => (
                <button
                  key={dept.value}
                  type="button"
                  onClick={() => onChangeDepartment?.(dept.value)}
                  className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
                    selectedDepartment === dept.value
                      ? "bg-[#111827] text-white shadow-md"
                      : "bg-gray-100 text-gray-600 hover:bg-gray-200"
                  }`}
                >
                  {dept.label}
                </button>
              ))}
            </div>
          </div>

          <input
            type="text"
            value={keyword}
            onChange={(e) => onChangeKeyword(e.target.value)}
            placeholder="사용자 검색 (선택사항)"
            className="w-full px-4 py-3 border-2 border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 mb-3"
            disabled={loading}
          />

          {loading && (
            <div className="p-4 text-center text-gray-500 text-sm">검색 중...</div>
          )}

          {error && (
            <div className="p-4 text-center text-red-500 text-sm">{error}</div>
          )}

          {!loading && !error && (
            <div className="max-h-60 overflow-y-auto border-2 border-gray-200 rounded-xl">
              {!selectedDepartment && keyword.trim().length < 2 ? (
                <div className="p-4 text-center text-gray-400 text-sm">
                  부서를 선택하거나 검색어를 입력해주세요.
                </div>
              ) : results.length === 0 ? (
                <div className="p-4 text-center text-gray-500">검색 결과가 없습니다.</div>
              ) : (
                results.map((user) => (
                  <div
                    key={user.email}
                    onClick={() => onToggle(user.email)}
                    className={`p-4 border-b border-gray-100 cursor-pointer transition-colors ${
                      selected.includes(user.email)
                        ? "bg-blue-50 border-blue-200"
                        : "hover:bg-gray-50"
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex-1">
                        <div className="font-semibold text-gray-900">
                          {user.nickname || user.email}
                        </div>
                        <div className="text-sm text-gray-500 mb-1">{user.email}</div>
                        {user.department && (
                          <div className="text-xs text-gray-400">
                            {getDepartmentLabel(user.department)}
                          </div>
                        )}
                      </div>
                      {selected.includes(user.email) && (
                        <span className="text-blue-600 font-bold text-lg">✓</span>
                      )}
                    </div>
                  </div>
                ))
              )}
            </div>
          )}

          {selected.length > 0 && (
            <div className="mt-3 text-sm text-gray-600 font-medium">
              선택된 사용자: {selected.length}명
            </div>
          )}
        </div>

        <div className="flex gap-3">
          <button
            onClick={onClose}
            className="flex-1 bg-gray-100 text-gray-400 px-6 py-3.5 rounded-2xl font-black text-xs uppercase tracking-[0.2em] hover:bg-gray-200 hover:text-gray-600 transition-all duration-300"
          >
            취소
          </button>
          <button
            onClick={onConfirm}
            className="flex-1 bg-[#111827] text-white px-6 py-3.5 rounded-2xl font-black text-xs uppercase tracking-[0.2em] hover:bg-blue-600 hover:-translate-y-1 transition-all duration-300 shadow-xl shadow-gray-200"
          >
            확인
          </button>
        </div>
      </div>
    </div>
  );
};

export default MemberPickerModal;

