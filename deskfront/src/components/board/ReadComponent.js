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

  const isAdmin =
    loginState.roleNames &&
    (loginState.roleNames.includes("ADMIN"));

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
    <div className="border-2 border-gray-100 mt-10 p-6 bg-white rounded-xl shadow-sm">
      {fetching ? <FetchingModal /> : <></>}

      {/* 1. 상단 헤더 영역 (카테고리, 제목, 작성 정보) */}
      <div className="flex justify-between items-center border-b pb-4 mb-6">
        <div>
          <span className="text-blue-500 font-bold text-sm uppercase tracking-wider">
            {board.category || "카테고리 없음"}
          </span>
          <h2 className="text-3xl font-bold text-gray-800 mt-1">
            {board.title || "제목 없음"}
          </h2>
        </div>
        <div className="text-right text-sm text-gray-400">
          <p>
            작성자:{" "}
            <span className="text-gray-600 font-medium">
              {board.writer || "익명"}
            </span>
          </p>
          <p>작성일: {board.regDate || board.modDate || "날짜 정보 없음"}</p>
        </div>
      </div>

      {/* 2. 게시글 본문 영역 (댓글창 위로 이동) */}
      <div className="min-h-[300px] text-gray-700 leading-relaxed text-lg mb-10 px-2">
        {board.content || "내용이 없습니다."}
      </div>

      {/* 3. 하단 버튼 영역 (목록, 수정) */}
      <div className="flex justify-end space-x-4 border-b pb-10 mb-10">
        <button
          className="bg-gray-100 text-gray-600 px-6 py-2 rounded-lg font-bold hover:bg-gray-200 transition"
          onClick={() => moveToList()}
        >
          목록으로
        </button>

        {isAdmin && (
          <button
            className="bg-black text-white px-6 py-2 rounded-lg font-bold hover:bg-gray-800 transition"
            onClick={() => moveToModify(bno)}
          >
            수정하기
          </button>
        )}
      </div>

      {/* 4. 댓글 영역 (맨 밑으로 배치) */}
      <div className="pt-4">
        <ReplyComponent bno={bno} />
      </div>
    </div>
  );
};

export default ReadComponent;
