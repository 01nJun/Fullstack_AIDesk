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
    <div className="max-w-7xl mx-auto p-8 space-y-10 bg-gray-50/30 min-h-screen">
      {fetching && <FetchingModal />}

      {/* 1. 최상단 타이틀 섹션 */}
      <div className="relative inline-block">
        <h1 className="text-4xl font-black text-[#111827] mb-2 tracking-tight">게시판 목록</h1>
        <div className="h-1.5 w-full bg-blue-600 rounded-full"></div>
      </div>

      {/* 2. 검색 및 필터 바 (셀렉트 박스 제거 버전) */}
      <div className="bg-white p-5 rounded-[2rem] shadow-[0_8px_30px_rgb(0,0,0,0.04)] flex items-center gap-6 border border-gray-100">

        {/* 카테고리 탭 영역 */}
        <div className="bg-gray-100 p-1.5 rounded-2xl flex gap-1 shrink-0">
          {["전체", "공지사항", "가이드", "FAQ"].map((tab) => (
            <button
              key={tab}
              onClick={() => handleClickCategory(tab)}
              className={`px-6 py-2.5 rounded-xl text-xs font-bold transition-all ${
                category === tab || (tab === "전체" && !category)
                  ? "bg-white text-blue-600 shadow-sm"
                  : "text-gray-400 hover:text-gray-600"
              }`}
            >
              {tab}
            </button>
          ))}
        </div>

        {/* 검색창 영역 (가운데를 꽉 채우도록 설정) */}
        <div className="flex-1 flex gap-3">
          <input
            type="text"
            placeholder="제목을 입력하세요..."
            value={searchStr}
            onChange={(e) => setSearchStr(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSearch()}
            className="flex-1 bg-white border border-gray-200 rounded-xl px-5 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-blue-100 transition-all"
          />
          <button
            onClick={handleSearch}
            className="bg-[#111827] text-white px-10 py-2.5 rounded-xl font-black text-sm hover:bg-gray-800 transition-all shrink-0"
          >
            검색
          </button>
        </div>
      </div>

      {/* 3. 메인 게시판 리스트 카드 */}
      <div className="bg-white rounded-[2rem] overflow-hidden shadow-[0_20px_50px_rgba(0,0,0,0.05)] border border-gray-100">

        {/* 리스트 블랙 헤더 */}
        <div className="bg-[#1a1f2c] px-10 py-5 flex justify-between items-center">
          <h2 className="text-white font-black italic tracking-widest text-lg uppercase">Board List</h2>
          <span className="bg-blue-600 text-white text-[11px] font-black px-5 py-2 rounded-full italic uppercase shadow-lg shadow-blue-900/20">
            Total: {serverData.totalCount}
          </span>
        </div>

        {/* 테이블 본문 */}
        <div className="w-full">
          <table className="w-full text-left">
            <thead>
              <tr className="border-b border-gray-50">
                <th className="px-10 py-6 text-[11px] font-black text-gray-300 uppercase tracking-[0.2em] w-48">Category</th>
                <th className="px-10 py-6 text-[11px] font-black text-gray-300 uppercase tracking-[0.2em]">Subject</th>
                <th className="px-10 py-6 text-[11px] font-black text-gray-300 uppercase tracking-[0.2em] text-center w-40">Writer</th>
                <th className="px-10 py-6 text-[11px] font-black text-gray-300 uppercase tracking-[0.2em] text-right w-44">Date</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-50">
              {serverData.dtoList.length > 0 ? (
                serverData.dtoList.map((board) => (
                  <tr
                    key={board.bno}
                    onClick={() => moveToRead(board.bno)}
                    className="hover:bg-gray-50/80 transition-all cursor-pointer group"
                  >
                    <td className="px-10 py-6">
                      <span className={`text-[10px] font-black px-3 py-1.5 rounded-lg uppercase tracking-tighter ${
                        board.category === '공지사항' ? 'text-red-400 bg-red-50' :
                        board.category === '가이드' ? 'text-blue-400 bg-blue-50' : 'text-gray-400 bg-gray-50'
                      }`}>
                        {board.category || "General"}
                      </span>
                    </td>
                    <td className="px-10 py-6">
                      <div className="flex items-center gap-3">
                        <span className="text-sm font-black text-gray-800 group-hover:text-blue-600 transition-colors">
                          {board.title}
                        </span>
                        {board.replyCount > 0 && (
                          <span className="bg-gray-100 text-gray-400 text-[10px] px-2 py-0.5 rounded font-black">
                            {board.replyCount}
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="px-10 py-6 text-center text-sm font-bold text-gray-600 italic">
                      {board.writer}
                    </td>
                    <td className="px-10 py-6 text-right text-xs font-black text-gray-300 italic tracking-tighter">
                      {board.regDate ? board.regDate.split(' ')[0] : ''}
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="4" className="py-40 text-center">
                    <p className="text-4xl font-black text-gray-100 italic tracking-[0.3em] uppercase opacity-50">No Data Found</p>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* 4. 페이지네이션 */}
      <div className="flex justify-center mt-12 mb-10">
        <PageComponent serverData={serverData} movePage={moveToList} />
      </div>

      {/* 5. 글쓰기 버튼 (이미지의 오른쪽 하단 파란색 버튼) */}
      {isAdmin && (
        <button
          onClick={moveToAdd}
          // flex items-center justify-center: 중앙 정렬
          // leading-none: 행간 제거
          // pb-1: 시각적 중심을 맞추기 위한 미세 조정 (글자가 위로 쏠리는 현상 방지)
          className="fixed bottom-10 right-10 w-16 h-16 bg-blue-600 text-white rounded-full shadow-2xl shadow-blue-500/50 hover:scale-110 hover:bg-blue-700 transition-all duration-300 flex items-center justify-center text-4xl font-light z-50 pb-1"
        >
          +
        </button>
      )}
    </div>
  );
};

export default ListComponent;