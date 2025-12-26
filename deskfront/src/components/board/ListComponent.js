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

  // 1. [수정] useCustomMove에서 'category'를 추가로 가져옵니다.
  const {
    page,
    size,
    refresh,
    keyword,
    type,
    category, // <-- 이거 추가!
    moveToList,
    moveToRead,
    moveToAdd,
  } = useCustomMove();

  const [serverData, setServerData] = useState(initState);
  const [fetching, setFetching] = useState(false);

  const [searchStr, setSearchStr] = useState(keyword || "");

  const isAdmin = loginState?.roleNames?.includes("ADMIN");

  useEffect(() => {
    setFetching(true);

    // 2. [수정] getList를 호출할 때 'category'도 함께 보냅니다.
    getList({ page, size, keyword, type, category })
      .then((data) => {
        if (data) {
          setServerData(data);
        }
        setFetching(false);
      })
      .catch((err) => {
        setFetching(false);
        exceptionHandle(err);
      });
    // 3. [수정] 감시 목록(dependency)에 category를 넣어야 버튼 누를 때 화면이 바뀝니다.
  }, [page, size, refresh, keyword, type, category]);

  useEffect(() => {
    setSearchStr(keyword || "");
  }, [keyword]);

  const handleSearch = () => {
    moveToList({ page: 1, keyword: searchStr, type: "t" });
  };

  // 4. [추가] 카테고리 버튼을 눌렀을 때 실행될 함수
  const handleClickCategory = (categoryName) => {
    // '전체'를 누르면 category를 없애고, 나머지는 그 이름으로 이동
    const selectCategory = categoryName === "전체" ? "" : categoryName;
    moveToList({ page: 1, category: selectCategory });
  };

  const getCategoryStyle = (category) => {
    switch (category) {
      case "공지사항":
        return "bg-red-100 text-red-600 border border-red-200";
      case "가이드":
        return "bg-blue-100 text-blue-600 border border-blue-200";
      case "FAQ":
        return "bg-green-100 text-green-600 border border-green-200";
      default:
        return "bg-gray-100 text-gray-500 border border-gray-200";
    }
  };

  return (
    <div className="p-6 w-full bg-white rounded-lg shadow-sm">
      {fetching ? <FetchingModal /> : <></>}

      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-extrabold text-gray-800">게시판</h1>
          <p className="text-gray-500 text-sm mt-1">
            팀 공지사항, 가이드, FAQ를 확인하세요
          </p>
        </div>

        {isAdmin && (
          <button
            onClick={moveToAdd}
            className="bg-blue-600 text-white px-5 py-2.5 rounded-xl flex items-center shadow-md hover:bg-blue-700 transition-all font-bold"
          >
            <span className="mr-2 text-xl">+</span> 새 글 작성
          </button>
        )}
      </div>

      <div className="mb-6">
        <div className="relative">
          <input
            type="text"
            placeholder="게시글 검색..."
            value={searchStr}
            onChange={(e) => setSearchStr(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSearch()}
            className="w-full p-4 pl-12 bg-gray-50 border border-gray-200 rounded-2xl mb-4 focus:outline-none focus:ring-2 focus:ring-blue-400 focus:bg-white transition-all"
          />
          <span
            className="absolute left-4 top-4 text-gray-400 cursor-pointer"
            onClick={handleSearch}
          >
            🔍
          </span>
        </div>

        {/* 5. [수정] 카테고리 버튼 영역 */}
        <div className="flex gap-2">
          {["전체", "공지사항", "가이드", "FAQ"].map((tab) => (
            <button
              key={tab}
              // 클릭 시 handleClickCategory 함수 실행!
              onClick={() => handleClickCategory(tab)}
              className={`px-5 py-2 rounded-full text-sm font-bold transition-all ${
                // 현재 선택된 카테고리(category)와 버튼 이름(tab)이 같으면 검은색으로 표시
                category === tab || (tab === "전체" && !category)
                  ? "bg-gray-800 text-white"
                  : "bg-gray-100 text-gray-500 hover:bg-gray-200"
              }`}
            >
              {tab}
            </button>
          ))}
        </div>
      </div>

      {/* ... 테이블 영역 생략 (기존과 동일) ... */}
      <div className="overflow-hidden rounded-xl border border-gray-100 shadow-sm">
        <table className="w-full text-left">
          <thead className="bg-gray-50 text-gray-500 text-sm uppercase">
            <tr>
              <th className="px-6 py-4 font-bold">제목</th>
              <th className="px-6 py-4 font-bold text-center w-32">카테고리</th>
              <th className="px-6 py-4 font-bold w-32">작성자</th>
              <th className="px-6 py-4 font-bold w-40">작성일</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {serverData?.dtoList?.length > 0 ? (
              serverData.dtoList.map((board) => (
                <tr
                  key={board.bno}
                  className="hover:bg-blue-50/50 cursor-pointer transition-colors group"
                  onClick={() => moveToRead(board.bno)}
                >
                  <td className="px-6 py-5 font-bold text-gray-700 group-hover:text-blue-600 transition-colors">
                    {board.title}

                    {board.replyCount > 0 && (
                      <span className="ml-2 text-blue-400 font-medium text-sm">
                        ({board.replyCount})
                      </span>
                    )}
                  </td>
                  <td className="px-6 py-5 text-center">
                    <span
                      className={`px-3 py-1.5 rounded-lg text-xs font-black ${getCategoryStyle(
                        board.category
                      )}`}
                    >
                      {board.category || "일반"}
                    </span>
                  </td>
                  <td className="px-6 py-5 text-gray-600 font-medium">
                    {board.writer}
                  </td>
                  <td className="px-6 py-5 text-gray-400 text-sm">
                    {board.regDate || board.modDate}
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td
                  colSpan="4"
                  className="py-20 text-center text-gray-400 font-bold"
                >
                  게시글이 존재하지 않습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="mt-8 flex justify-center">
        <PageComponent serverData={serverData} movePage={moveToList} />
      </div>
    </div>
  );
};

export default ListComponent;
