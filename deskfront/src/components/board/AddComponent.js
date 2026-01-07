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
    <div className="ui-container py-8 space-y-8 bg-baseBg min-h-screen">
      {fetching && <FetchingModal />}

      {/* 1. 상단 타이틀 섹션 */}
      <div className="mb-8">
        <div className="text-xs uppercase tracking-widest text-baseMuted mb-2">BOARD</div>
        <h1 className="ui-title">게시글 작성</h1>
      </div>

      {/* 2. 작성 폼 카드 */}
      <div className="ui-card overflow-hidden">

        {/* 헤더 */}
        <div className="px-6 py-4 bg-baseSurface border-b border-baseBorder">
          <h2 className="text-sm font-semibold text-baseText uppercase tracking-wide">
            게시글 작성
          </h2>
        </div>

        {/* 입력 영역 */}
        <div className="p-6 lg:p-12 space-y-6">

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* 카테고리 선택 */}
            <div>
              <label className="block text-xs font-semibold text-baseMuted mb-2">
                카테고리
              </label>
              <select
                name="category"
                value={board.category}
                onChange={handleChangeBoard}
                className="ui-select"
              >
                <option value="공지사항">공지사항</option>
                <option value="가이드">가이드</option>
                <option value="FAQ">FAQ</option>
              </select>
            </div>

            {/* 작성자 자동 표시 */}
            <div>
              <label className="block text-xs font-semibold text-baseMuted mb-2">
                작성자
              </label>
              <div className="ui-input bg-baseSurface text-baseMuted">
                {loginState.nickname || "알 수 없음"}
              </div>
            </div>
          </div>

          {/* 제목 입력 */}
          <div>
            <label className="block text-xs font-semibold text-baseMuted mb-2">
              제목
            </label>
            <input
              name="title"
              type="text"
              placeholder="제목을 입력하세요..."
              value={board.title}
              onChange={handleChangeBoard}
              className="ui-input"
            />
          </div>

          {/* 내용 입력 */}
          <div>
            <label className="block text-xs font-semibold text-baseMuted mb-2">
              내용
            </label>
            <textarea
              name="content"
              rows="12"
              placeholder="내용을 상세히 기술해 주세요."
              value={board.content}
              onChange={handleChangeBoard}
              className="ui-textarea"
            ></textarea>
          </div>
        </div>

        {/* 푸터 버튼 영역 */}
        <div className="px-6 lg:px-10 py-6 lg:py-8 bg-baseSurface flex justify-end items-center gap-3 border-t border-baseBorder">
          <button
            onClick={moveToList}
            className="ui-btn-secondary"
          >
            취소
          </button>

          <button
            onClick={handleClickAdd}
            className="ui-btn-primary"
          >
            등록하기
          </button>
        </div>
      </div>
    </div>
  );
};

export default AddComponent;