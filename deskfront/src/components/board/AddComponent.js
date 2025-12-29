import { useState } from "react";
import { postAdd } from "../../api/boardApi";
import useCustomMove from "../../hooks/useCustomMove";
import useCustomLogin from "../../hooks/useCustomLogin";
import FetchingModal from "../common/FetchingModal";

const AddComponent = () => {
  const [board, setBoard] = useState({
    title: "",
    content: "",
    category: "공지사항",
  });

  const [fetching, setFetching] = useState(false);
  const { moveToList } = useCustomMove();
  const { loginState } = useCustomLogin();

  const handleChangeBoard = (e) => {
    setBoard({ ...board, [e.target.name]: e.target.value });
  };

  const handleClickAdd = () => {
    const boardObj = {
      ...board,
      writer: loginState.nickname || loginState.email,
    };

    setFetching(true);
    postAdd(boardObj)
      .then((data) => {
        setFetching(false);
        alert("게시물이 작성되었습니다.");
        moveToList({ page: 1 });
      })
      .catch((err) => {
        setFetching(false);
        alert("등록 오류가 발생했습니다.");
      });
  };

  return (
    <div className="max-w-7xl mx-auto p-8 space-y-10 bg-gray-50/30 min-h-screen">
      {fetching && <FetchingModal />}

      {/* 1. 상단 타이틀 섹션 (다른 페이지와 통일) */}
      <div className="relative inline-block mb-2">
        <span className="text-blue-600 font-black text-xs uppercase tracking-widest mb-3 block italic">
            Post New Ticket
        </span>
        <h1 className="text-4xl font-black text-[#111827] mb-4 tracking-tighter uppercase">
            게시글 작성
        </h1>
        <div className="h-1.5 w-full bg-blue-600 rounded-full shadow-[0_2px_10px_rgba(37,99,235,0.3)]"></div>
      </div>

      {/* 2. 작성 폼 카드 (티켓 발행 인터페이스) */}
      <div className="bg-white rounded-[2.5rem] overflow-hidden shadow-[0_25px_60px_rgba(0,0,0,0.05)] border border-gray-100">

        {/* 상단 다크 네이비 바 */}
        <div className="bg-[#1a1f2c] px-10 py-5 flex justify-between items-center border-b border-gray-800">
          <h2 className="text-white font-black italic tracking-widest text-xs uppercase opacity-80">
            Creator Interface
          </h2>
          <div className="flex gap-2.5">
            <div className="w-3 h-3 rounded-full bg-[#ff5f56]"></div>
            <div className="w-3 h-3 rounded-full bg-[#ffbd2e]"></div>
            <div className="w-3 h-3 rounded-full bg-[#27c93f]"></div>
          </div>
        </div>

        {/* 입력 영역 */}
        <div className="p-12 space-y-8 bg-gradient-to-b from-white to-gray-50/30">

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            {/* 카테고리 선택 */}
            <div>
              <label className="text-[11px] font-black text-gray-400 uppercase tracking-[0.2em] mb-3 block ml-1">
                Select Category
              </label>
              <select
                name="category"
                value={board.category}
                onChange={handleChangeBoard}
                className="w-full p-4 bg-gray-50 border-2 border-transparent rounded-2xl focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-50 transition-all outline-none font-bold text-gray-700 appearance-none cursor-pointer"
              >
                <option value="공지사항">공지사항</option>
                <option value="가이드">가이드</option>
                <option value="FAQ">FAQ</option>
              </select>
            </div>

            {/* 작성자 자동 표시 (ID) */}
            <div>
              <label className="text-[11px] font-black text-gray-400 uppercase tracking-[0.2em] mb-3 block ml-1">
                Author Identity
              </label>
              <div className="w-full p-4 bg-gray-100 border-2 border-transparent rounded-2xl text-gray-400 font-black italic">
                {loginState.nickname || "Unknown"}
              </div>
            </div>
          </div>

          {/* 제목 입력 */}
          <div>
            <label className="text-[11px] font-black text-gray-400 uppercase tracking-[0.2em] mb-3 block ml-1">
              Ticket Subject
            </label>
            <input
              name="title"
              type="text"
              placeholder="제목을 입력하세요..."
              value={board.title}
              onChange={handleChangeBoard}
              className="w-full p-4 bg-gray-50 border-2 border-transparent rounded-2xl focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-50 transition-all outline-none font-bold text-gray-700 placeholder:text-gray-200"
            />
          </div>

          {/* 내용 입력 */}
          <div>
            <label className="text-[11px] font-black text-gray-400 uppercase tracking-[0.2em] mb-3 block ml-1">
              Detailed Description
            </label>
            <textarea
              name="content"
              rows="12"
              placeholder="내용을 상세히 기술해 주세요."
              value={board.content}
              onChange={handleChangeBoard}
              className="w-full p-5 bg-gray-50 border-2 border-transparent rounded-[2rem] focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-50 transition-all outline-none font-medium text-gray-700 resize-none leading-relaxed placeholder:text-gray-200"
            ></textarea>
          </div>
        </div>

        {/* 푸터 버튼 영역 */}
        <div className="bg-white px-10 py-8 flex justify-end items-center gap-4 border-t border-gray-100/60">
          <button
            onClick={moveToList}
            className="bg-gray-100 text-gray-400 px-10 py-3.5 rounded-2xl font-black text-xs uppercase tracking-[0.2em] hover:bg-gray-200 hover:text-gray-600 transition-all duration-300"
          >
            취소
          </button>

          <button
            onClick={handleClickAdd}
            className="bg-[#111827] text-white px-12 py-3.5 rounded-2xl font-black text-xs uppercase tracking-[0.2em] hover:bg-blue-600 hover:-translate-y-1 transition-all duration-300 shadow-xl shadow-gray-200"
          >
            등록하기
          </button>
        </div>
      </div>
    </div>
  );
};

export default AddComponent;