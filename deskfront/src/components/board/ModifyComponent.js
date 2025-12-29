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
    <div className="max-w-7xl mx-auto p-8 space-y-10 bg-gray-50/30 min-h-screen">
      {fetching && <FetchingModal />}

      {/* 1. 상단 타이틀 섹션 */}
      <div className="relative inline-block mb-2">
        <span className="text-blue-600 font-black text-xs uppercase tracking-widest mb-3 block italic">
            Management System
        </span>
        <h1 className="text-4xl font-black text-[#111827] mb-4 tracking-tighter uppercase">
            게시글 수정
        </h1>
        <div className="h-1.5 w-full bg-blue-600 rounded-full shadow-[0_2px_10px_rgba(37,99,235,0.3)]"></div>
      </div>

      {/* 2. 수정 폼 카드 (티켓 상세 박스 스타일) */}
      <div className="bg-white rounded-[2.5rem] overflow-hidden shadow-[0_25px_60px_rgba(0,0,0,0.05)] border border-gray-100">

        {/* 상단 다크 네이비 바 */}
        <div className="bg-[#1a1f2c] px-10 py-5 flex justify-between items-center border-b border-gray-800">
          <h2 className="text-white font-black italic tracking-widest text-xs uppercase opacity-80">
            Modifier Interface
          </h2>
          <div className="flex gap-2.5">
            <div className="w-3 h-3 rounded-full bg-[#ff5f56]"></div>
            <div className="w-3 h-3 rounded-full bg-[#ffbd2e]"></div>
            <div className="w-3 h-3 rounded-full bg-[#27c93f]"></div>
          </div>
        </div>

        {/* 입력 폼 영역 */}
        <div className="p-12 space-y-8 bg-gradient-to-b from-white to-gray-50/30">

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            {/* 카테고리 선택 */}
            <div>
              <label className="text-[11px] font-black text-gray-400 uppercase tracking-[0.2em] mb-3 block ml-1">
                Category Selection
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

            {/* 작성자 (Read Only) */}
            <div>
              <label className="text-[11px] font-black text-gray-400 uppercase tracking-[0.2em] mb-3 block ml-1">
                Writer (Identity)
              </label>
              <input
                type="text"
                value={board.writer}
                readOnly
                className="w-full p-4 bg-gray-100 border-2 border-transparent rounded-2xl text-gray-400 font-black italic cursor-not-allowed outline-none"
              />
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
              value={board.title}
              onChange={handleChangeBoard}
              className="w-full p-4 bg-gray-50 border-2 border-transparent rounded-2xl focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-50 transition-all outline-none font-bold text-gray-700"
            />
          </div>

          {/* 내용 입력 */}
          <div>
            <label className="text-[11px] font-black text-gray-400 uppercase tracking-[0.2em] mb-3 block ml-1">
              Content Description
            </label>
            <textarea
              name="content"
              rows="10"
              value={board.content}
              onChange={handleChangeBoard}
              className="w-full p-5 bg-gray-50 border-2 border-transparent rounded-[2rem] focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-50 transition-all outline-none font-medium text-gray-700 resize-none leading-relaxed"
            ></textarea>
          </div>
        </div>

        {/* 푸터 버튼 영역 */}
        <div className="bg-white px-10 py-8 flex justify-end items-center gap-4 border-t border-gray-100/60">
          <button
            onClick={handleClickDelete}
            className="bg-[#ef4444] text-white px-8 py-3.5 rounded-2xl font-black text-xs uppercase tracking-[0.2em] hover:bg-red-600 hover:-translate-y-1 transition-all duration-300 shadow-lg shadow-red-100"
          >
            삭제하기
          </button>

          <div className="flex-1"></div> {/* 간격 벌리기 */}

          <button
            onClick={() => moveToRead(bno)}
            className="bg-gray-100 text-gray-400 px-8 py-3.5 rounded-2xl font-black text-xs uppercase tracking-[0.2em] hover:bg-gray-200 hover:text-gray-600 transition-all duration-300"
          >
            취소
          </button>

          <button
            onClick={handleClickModify}
            className="bg-[#111827] text-white px-10 py-3.5 rounded-2xl font-black text-xs uppercase tracking-[0.2em] hover:bg-blue-600 hover:-translate-y-1 transition-all duration-300 shadow-xl shadow-gray-200"
          >
            저장하기
          </button>
        </div>
      </div>
    </div>
  );
};

export default ModifyComponent;