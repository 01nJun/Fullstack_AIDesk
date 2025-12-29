import { useEffect, useState } from "react";
import { getOne } from "../../api/boardApi";
import useCustomMove from "../../hooks/useCustomMove";
import FetchingModal from "../common/FetchingModal";
import { useSelector } from "react-redux";
import ReplyComponent from "./ReplyComponent";

const initState = {
  bno: 0,
  title: "",
  content: "",
  writer: "",
  category: "",
  regDate: "",
  modDate: "",
};

const ReadComponent = ({ bno }) => {
  const [board, setBoard] = useState(initState);
  const [fetching, setFetching] = useState(false);
  const { moveToList, moveToModify } = useCustomMove();

  const loginState = useSelector((state) => state.loginSlice);

  // 관리자이거나 본인인 경우 수정 가능하도록 로직 보완
  const canModify = loginState.roleNames?.includes("ADMIN") || loginState.nickname === board.writer;

  useEffect(() => {
    setFetching(true);
    getOne(bno)
      .then((data) => {
        setBoard(data);
        setFetching(false);
      })
      .catch((err) => {
        console.error("데이터 로딩 에러:", err);
        setFetching(false);
      });
  }, [bno]);

  return (
    <div className="max-w-7xl mx-auto p-8 space-y-10 bg-gray-50/30 min-h-screen">
      {fetching && <FetchingModal />}

      {/* 1. 상단 타이틀 섹션 (티켓 목록 헤더 스타일) */}
      <div className="flex justify-between items-end mb-4">
        <div className="relative inline-block">
          <span className="text-blue-600 font-black text-xs uppercase tracking-widest mb-3 block italic">
            {board.category || "General Board"}
          </span>
          <h1 className="text-4xl font-black text-[#111827] mb-4 tracking-tighter uppercase">
            {board.title || "No Title"}
          </h1>
          {/* 타이틀 아래의 굵은 블루 라인 */}
          <div className="h-1.5 w-full bg-blue-600 rounded-full shadow-[0_2px_10px_rgba(37,99,235,0.3)]"></div>
        </div>

        {/* 작성 정보 섹션 */}
        <div className="text-right space-y-1 pb-2">
          <div className="text-sm font-black text-gray-900 italic">
            WRITER. <span className="text-blue-600 underline decoration-2 underline-offset-4">{board.writer || "Unknown"}</span>
          </div>
          <div className="text-[11px] font-bold text-gray-300 italic uppercase tracking-widest">
            REG.DATE: {board.regDate || board.modDate}
          </div>
        </div>
      </div>

      {/* 2. 메인 콘텐츠 카드 (티켓 상세 박스 스타일) */}
      <div className="bg-white rounded-[2.5rem] overflow-hidden shadow-[0_25px_60px_rgba(0,0,0,0.05)] border border-gray-100 transition-all hover:shadow-[0_25px_60px_rgba(0,0,0,0.08)]">
        {/* 카드 상단 다크 네이비 바 */}
        <div className="bg-[#1a1f2c] px-10 py-5 flex justify-between items-center border-b border-gray-800">
          <h2 className="text-white font-black italic tracking-widest text-xs uppercase opacity-80">Article Detail View</h2>
          <div className="flex gap-2.5">
            <div className="w-3 h-3 rounded-full bg-[#ff5f56]"></div>
            <div className="w-3 h-3 rounded-full bg-[#ffbd2e]"></div>
            <div className="w-3 h-3 rounded-full bg-[#27c93f]"></div>
          </div>
        </div>

        {/* 본문 텍스트 영역 */}
        <div className="p-12 min-h-[450px] bg-gradient-to-b from-white to-gray-50/30">
          <div className="text-gray-700 leading-[1.8] text-lg font-medium whitespace-pre-wrap break-all">
            {board.content || "No Content available."}
          </div>
        </div>

        {/* 푸터 버튼 영역 (경계선 강조) */}
        <div className="bg-white px-10 py-8 flex justify-end gap-4 border-t border-gray-100/60">
          <button
            onClick={moveToList}
            className="bg-gray-100 text-gray-400 px-10 py-3.5 rounded-2xl font-black text-xs uppercase tracking-[0.2em] hover:bg-gray-200 hover:text-gray-600 transition-all duration-300"
          >
            목록으로
          </button>

          {canModify && (
            <button
              onClick={() => moveToModify(bno)}
              className="bg-[#111827] text-white px-10 py-3.5 rounded-2xl font-black text-xs uppercase tracking-[0.2em] hover:bg-blue-600 hover:-translate-y-1 transition-all duration-300 shadow-xl shadow-gray-200"
            >
              수정하기
            </button>
          )}
        </div>
      </div>

      {/* 3. 댓글 영역 */}
      <div className="mt-16 bg-white rounded-[2.5rem] p-10 border border-gray-100 shadow-sm">
        <ReplyComponent bno={bno} />
      </div>
    </div>
  );
};

export default ReadComponent;