import { useEffect, useState } from "react";
import { getList } from "../../api/boardApi";
import useCustomMove from "../../hooks/useCustomMove";
import FetchingModal from "../common/FetchingModal";
import PageComponent from "../common/PageComponent";
import useCustomLogin from "../../hooks/useCustomLogin";

const initState = {
  dtoList: [],
  pageNumList: [],
  pageRequestDTO: null,
  prev: false,
  next: false,
  totalCount: 0,
  prevPage: 0,
  nextPage: 0,
  totalPage: 0,
  current: 0,
};

const ListComponent = () => {
  const { exceptionHandle, loginState } = useCustomLogin();
  const {
    page, size, refresh, keyword, type, category,
    moveToList, moveToRead, moveToAdd,
  } = useCustomMove();

  const [serverData, setServerData] = useState(initState);
  const [fetching, setFetching] = useState(false);
  const [searchStr, setSearchStr] = useState(keyword || "");

  const isAdmin = loginState?.roleNames?.includes("ADMIN");

  useEffect(() => {
    setFetching(true);
    getList({ page, size, keyword, type, category })
      .then((data) => {
        if (data) setServerData(data);
        setFetching(false);
      })
      .catch((err) => {
        setFetching(false);
        exceptionHandle(err);
      });
  }, [page, size, refresh, keyword, type, category]);

  const handleSearch = () => {
    moveToList({ page: 1, keyword: searchStr, type: "t" });
  };


  const handleClickCategory = (categoryName) => {
    // '전체'를 누르면 category를 없애고, 나머지는 그 이름으로 이동
    const selectCategory = categoryName === "전체" ? "" : categoryName;
    moveToList({ page: 1, category: selectCategory });
  };

  return (
    <div className="w-full space-y-8">
      {fetching && <FetchingModal />}

      {/* 1. 최상단 타이틀 섹션 */}
      <div>
        <div className="text-xs uppercase tracking-widest text-baseMuted mb-2">BOARD</div>
        <h1 className="ui-title">게시판 목록</h1>
      </div>

      {/* 2. 검색 및 필터 바 */}
      <div className="flex flex-col gap-4 lg:gap-6 mb-8 ui-card p-4 sm:p-6">

        {/* 카테고리 탭 영역 */}
        <div className="flex bg-baseSurface p-1.5 sm:p-2 rounded-ui">
          {["전체", "공지사항", "가이드", "FAQ"].map((tab) => (
            <button
              key={tab}
              onClick={() => handleClickCategory(tab)}
              className={`px-3 sm:px-4 lg:px-6 py-2 sm:py-2.5 rounded-ui font-semibold text-xs sm:text-sm transition-all flex-1 ${
                category === tab || (tab === "전체" && !category)
                  ? "bg-baseBg text-brandNavy shadow-ui"
                  : "text-baseMuted hover:text-baseText"
              }`}
            >
              {tab}
            </button>
          ))}
        </div>

        {/* 검색창 영역 */}
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 flex-grow">
          <div className="relative flex-grow">
            <input
              type="text"
              placeholder="제목을 입력하세요..."
              value={searchStr}
              onChange={(e) => setSearchStr(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleSearch()}
              className="ui-input w-full"
            />
          </div>
          <button
            onClick={handleSearch}
            className="ui-btn-primary w-full sm:w-auto whitespace-nowrap"
          >
            검색
          </button>
        </div>
      </div>

      {/* 3. 메인 게시판 리스트 카드 */}
      <div className="ui-card overflow-hidden">

        {/* 리스트 헤더 */}
        <div className="px-4 sm:px-6 py-3 sm:py-4 bg-baseSurface border-b border-baseBorder flex justify-between items-center">
          <h2 className="text-xs sm:text-sm font-semibold text-baseText uppercase tracking-wide">게시판 목록</h2>
          <span className="text-xs text-baseMuted font-medium">
            총 {serverData.totalCount}개
          </span>
        </div>

        {/* 데스크톱 테이블 뷰 */}
        <div className="hidden lg:block w-full">
          <table className="ui-table">
            <thead>
              <tr>
                <th className="w-48">카테고리</th>
                <th>제목</th>
                <th className="text-center w-40">작성자</th>
                <th className="text-right w-44">작성일</th>
              </tr>
            </thead>
            <tbody>
              {serverData.dtoList.length > 0 ? (
                serverData.dtoList.map((board) => (
                  <tr
                    key={board.bno}
                    onClick={() => moveToRead(board.bno)}
                    className="cursor-pointer"
                  >
                    <td>
                      <span className={`ui-text-2xs ${
                        board.category === '공지사항' ? 'ui-badge-category-notice' :
                        board.category === '가이드' ? 'ui-badge-category-guide' : 'ui-badge-category'
                      }`}>
                        {board.category || "일반"}
                      </span>
                    </td>
                    <td>
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-semibold text-baseText">
                          {board.title}
                        </span>
                        {board.replyCount > 0 && (
                          <span className="ui-badge ui-text-2xs">
                            {board.replyCount}
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="text-center text-sm text-baseText">{board.writer}</td>
                    <td className="text-right text-xs text-baseMuted">
                      {board.regDate ? board.regDate.split(' ')[0] : ''}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="4" className="py-20 text-center">
                    <p className="text-baseMuted">데이터가 없습니다.</p>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        {/* 모바일/태블릿 카드 뷰 */}
        <div className="lg:hidden p-4 space-y-3">
          {serverData.dtoList.length > 0 ? (
            serverData.dtoList.map((board) => (
              <div
                key={board.bno}
                onClick={() => moveToRead(board.bno)}
                className="bg-baseBg border border-baseBorder rounded-ui p-4 hover:shadow-md transition-all cursor-pointer"
              >
                <div className="flex items-start justify-between gap-3 mb-2">
                  <div className="flex items-center gap-2 flex-1 min-w-0">
                    <span className={`ui-text-2xs shrink-0 ${
                      board.category === '공지사항' ? 'ui-badge-category-notice' :
                      board.category === '가이드' ? 'ui-badge-category-guide' : 'ui-badge-category'
                    }`}>
                      {board.category || "일반"}
                    </span>
                    {board.replyCount > 0 && (
                      <span className="ui-badge ui-text-2xs shrink-0">
                        {board.replyCount}
                      </span>
                    )}
                  </div>
                </div>
                <h3 className="font-semibold text-base text-baseText mb-2 line-clamp-2">
                  {board.title}
                </h3>
                <div className="flex items-center justify-between text-sm text-baseMuted">
                  <span>{board.writer}</span>
                  <span className="text-xs">
                    {board.regDate ? board.regDate.split(' ')[0] : ''}
                  </span>
                </div>
              </div>
            ))
          ) : (
            <div className="p-20 text-center text-baseMuted">데이터가 없습니다.</div>
          )}
        </div>
      </div>

      {/* 4. 페이지네이션 */}
      <div className="flex justify-center">
        <PageComponent serverData={serverData} movePage={moveToList} />
      </div>

      {/* 5. 글쓰기 버튼 */}
      {isAdmin && (
        <button
          onClick={moveToAdd}
          className="fixed bottom-10 right-10 w-14 h-14 bg-brandNavy text-white rounded-full shadow-lg hover:opacity-90 transition-all flex items-center justify-center text-3xl font-light z-50"
        >
          +
        </button>
      )}
    </div>
  );
};

export default ListComponent;