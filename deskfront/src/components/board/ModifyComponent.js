import { useEffect, useState } from "react";
import { getOne, putOne, deleteOne } from "../../api/boardApi";
import useCustomMove from "../../hooks/useCustomMove";
import FetchingModal from "../common/FetchingModal";

const initState = {
  bno: 0,
  title: "",
  content: "",
  writer: "",
  category: "",
};

const ModifyComponent = ({ bno }) => {
  const [board, setBoard] = useState(initState);
  const [fetching, setFetching] = useState(false);
  const { moveToList, moveToRead } = useCustomMove();

  useEffect(() => {
    setFetching(true);
    getOne(bno)
      .then((data) => {
        setBoard(data);
        setFetching(false);
      })
      .catch((err) => {
        setFetching(false);
        console.error("데이터 로딩 에러:", err);
        alert("게시글 정보를 불러올 수 없습니다.");
      });
  }, [bno]);

  const handleChangeBoard = (e) => {
    setBoard({
      ...board,
      [e.target.name]: e.target.value,
    });
  };

  const handleClickModify = () => {
    setFetching(true);
    const boardParam = { ...board };

    putOne(bno, boardParam)
      .then((data) => {
        if (data?.error) {
          alert("수정 실패: 권한이 없습니다.");
          setFetching(false);
          return;
        }
        setFetching(false);
        alert("성공적으로 수정되었습니다.");
        moveToRead(bno);
      })
      .catch((err) => {
        setFetching(false);
        alert("서버 통신 에러가 발생했습니다.");
      });
  };

  const handleClickDelete = () => {
    if (window.confirm("정말로 삭제하시겠습니까?")) {
      setFetching(true);
      deleteOne(bno)
        .then((data) => {
          if (data?.error) {
            alert("삭제 실패: 권한이 없습니다.");
            setFetching(false);
            return;
          }
          setFetching(false);
          alert("삭제되었습니다.");
          moveToList();
        })
        .catch((err) => {
          setFetching(false);
          alert("삭제 처리 중 에러가 발생했습니다.");
        });
    }
  };

  return (
    <div className="ui-container py-8 space-y-8 bg-baseBg min-h-screen">
      {fetching && <FetchingModal />}

      {/* 1. 상단 타이틀 섹션 */}
      <div className="mb-8">
        <div className="text-xs uppercase tracking-widest text-baseMuted mb-2">BOARD</div>
        <h1 className="ui-title">게시글 수정</h1>
      </div>

      {/* 2. 수정 폼 카드 */}
      <div className="ui-card overflow-hidden">

        {/* 헤더 */}
        <div className="px-6 py-4 bg-baseSurface border-b border-baseBorder">
          <h2 className="text-sm font-semibold text-baseText uppercase tracking-wide">
            게시글 수정
          </h2>
        </div>

        {/* 입력 폼 영역 */}
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

            {/* 작성자 (Read Only) */}
            <div>
              <label className="block text-xs font-semibold text-baseMuted mb-2">
                작성자
              </label>
              <input
                type="text"
                value={board.writer}
                readOnly
                className="ui-input bg-baseSurface text-baseMuted"
              />
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
              rows="10"
              value={board.content}
              onChange={handleChangeBoard}
              className="ui-textarea"
            ></textarea>
          </div>
        </div>

        {/* 푸터 버튼 영역 */}
        <div className="px-6 lg:px-10 py-6 lg:py-8 bg-baseSurface flex justify-end items-center gap-3 border-t border-baseBorder">
          <button
            onClick={handleClickDelete}
            className="ui-btn-danger"
          >
            삭제하기
          </button>

          <button
            onClick={() => moveToRead(bno)}
            className="ui-btn-secondary"
          >
            취소
          </button>

          <button
            onClick={handleClickModify}
            className="ui-btn-primary"
          >
            저장하기
          </button>
        </div>
      </div>
    </div>
  );
};

export default ModifyComponent;