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
      className="ui-modal-overlay"
      onClick={onClose}
    >
      <div
        className="ui-modal-panel max-w-md"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="ui-modal-header">
          <h2 className="ui-title">{title}</h2>
          <button
            onClick={onClose}
            className="text-baseMuted hover:text-baseText text-xl font-bold leading-none w-8 h-8 flex items-center justify-center"
            aria-label="닫기"
          >
            &times;
          </button>
        </div>

        <div className="ui-modal-body space-y-4">
          {showGroupName && (
            <div>
              <label className="block text-xs font-semibold text-baseMuted mb-2">
                그룹 이름
              </label>
              <input
                type="text"
                value={groupName}
                onChange={(e) => onChangeGroupName?.(e.target.value)}
                placeholder="그룹 채팅방 이름을 입력하세요"
                className="ui-input"
              />
            </div>
          )}

          <div>
            <label className="block text-xs font-semibold text-baseMuted mb-2">
              참여자 선택 (복수 선택 가능)
            </label>

            {/* 부서 카테고리 버튼 */}
            <div className="mb-3">
              <div className="flex flex-wrap gap-2">
                <button
                  type="button"
                  onClick={() => onChangeDepartment?.("")}
                  className={`px-3 py-1.5 rounded-ui text-xs font-semibold transition-all ${
                    selectedDepartment === ""
                      ? "bg-brandNavy text-white shadow-chat"
                      : "bg-baseSurface text-baseMuted hover:text-baseText"
                  }`}
                >
                  전체
                </button>
                {departments.map((dept) => (
                  <button
                    key={dept.value}
                    type="button"
                    onClick={() => onChangeDepartment?.(dept.value)}
                    className={`px-3 py-1.5 rounded-ui text-xs font-semibold transition-all ${
                      selectedDepartment === dept.value
                        ? "bg-brandNavy text-white shadow-chat"
                        : "bg-baseSurface text-baseMuted hover:text-baseText"
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
              className="ui-input mb-3"
              disabled={loading}
            />

            {loading && (
              <div className="p-4 text-center text-baseMuted text-sm">검색 중...</div>
            )}

            {error && (
              <div className="p-4 text-center text-red-600 text-sm">{error}</div>
            )}

            {!loading && !error && (
              <div className="max-h-60 overflow-y-auto ui-card border-2">
                {!selectedDepartment && keyword.trim().length < 2 ? (
                  <div className="p-4 text-center text-baseMuted text-sm">
                    부서를 선택하거나 검색어를 입력해주세요.
                  </div>
                ) : results.length === 0 ? (
                  <div className="p-4 text-center text-baseMuted">검색 결과가 없습니다.</div>
                ) : (
                  results.map((user) => (
                    <div
                      key={user.email}
                      onClick={() => onToggle(user.email)}
                      className={`p-4 border-b border-baseBorder cursor-pointer transition-colors ${
                        selected.includes(user.email)
                          ? "bg-baseSurface border-brandNavy"
                          : "hover:bg-baseSurface"
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex-1">
                          <div className="font-semibold text-baseText">
                            {user.nickname || user.email}
                          </div>
                          <div className="text-sm text-baseMuted mb-1">{user.email}</div>
                          {user.department && (
                            <div className="text-xs text-baseMuted">
                              {getDepartmentLabel(user.department)}
                            </div>
                          )}
                        </div>
                        {selected.includes(user.email) && (
                          <span className="text-brandNavy font-bold text-lg">✓</span>
                        )}
                      </div>
                    </div>
                  ))
                )}
              </div>
            )}

            {selected.length > 0 && (
              <div className="mt-3 text-sm text-baseMuted font-medium">
                선택된 사용자: {selected.length}명
              </div>
            )}
          </div>
        </div>

        <div className="ui-modal-footer">
          <button
            onClick={onClose}
            className="ui-btn-secondary flex-1"
          >
            취소
          </button>
          <button
            onClick={onConfirm}
            className="ui-btn-primary flex-1"
          >
            확인
          </button>
        </div>
      </div>
    </div>
  );
};

export default MemberPickerModal;

