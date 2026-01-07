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
    <div className="ui-container py-8 space-y-8 bg-baseBg min-h-screen">
      {fetching && <FetchingModal />}

      {/* 1. 상단 타이틀 섹션 */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4 mb-6">
        <div>
          <div className="text-xs uppercase tracking-widest text-baseMuted mb-2">
            {board.category || "일반 게시판"}
          </div>
          <h1 className="ui-title">
            {board.title || "제목 없음"}
          </h1>
        </div>

        {/* 작성 정보 섹션 */}
        <div className="text-right space-y-1">
          <div className="text-sm font-semibold text-baseText">
            작성자: <span className="text-brandNavy">{board.writer || "알 수 없음"}</span>
          </div>
          <div className="text-xs text-baseMuted">
            작성일: {board.regDate || board.modDate}
          </div>
        </div>
      </div>

      {/* 2. 메인 콘텐츠 카드 */}
      <div className="ui-card overflow-hidden">
        {/* 카드 헤더 */}
        <div className="px-6 py-4 bg-baseSurface border-b border-baseBorder flex justify-between items-center">
          <h2 className="text-sm font-semibold text-baseText uppercase tracking-wide">게시글 상세</h2>
        </div>

        {/* 본문 텍스트 영역 */}
        <div className="p-6 lg:p-12 min-h-[450px] bg-baseBg">
          <div className="text-baseText leading-relaxed text-base whitespace-pre-wrap break-words">
            {board.content || "내용이 없습니다."}
          </div>
        </div>

        {/* 푸터 버튼 영역 */}
        <div className="px-6 lg:px-10 py-6 lg:py-8 bg-baseSurface flex justify-end gap-3 border-t border-baseBorder">
          <button
            onClick={moveToList}
            className="ui-btn-secondary"
          >
            목록으로
          </button>

          {canModify && (
            <button
              onClick={() => moveToModify(bno)}
              className="ui-btn-primary"
            >
              수정하기
            </button>
          )}
        </div>
      </div>

      {/* 3. 댓글 영역 */}
      <div className="ui-card p-6 lg:p-10">
        <ReplyComponent bno={bno} />
      </div>
    </div>
  );
};

export default ReadComponent;