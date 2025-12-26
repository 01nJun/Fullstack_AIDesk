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
      writer: loginState.nickname || loginState.email, // 로그인한 정보 사용
    };

    setFetching(true);
    postAdd(boardObj)
      .then((data) => {
        setFetching(false);
        alert("새 글이 등록되었습니다.");
        moveToList({ page: 1 });
      })
      .catch((err) => {
        setFetching(false);
        alert("등록 오류!");
      });
  };

  return (
    <div className="p-6 bg-white rounded-xl shadow-sm border mt-10">
      {fetching && <FetchingModal />}
      <h1 className="text-2xl font-bold mb-6">게시글 작성</h1>
      <div className="space-y-4">
        <div>
          <label className="block font-bold mb-1">카테고리</label>
          <select
            name="category"
            value={board.category}
            onChange={handleChangeBoard}
            className="w-full p-3 border rounded-lg"
          >
            <option value="공지사항">공지사항</option>
            <option value="가이드">가이드</option>
            <option value="FAQ">FAQ</option>
          </select>
        </div>
        <div>
          <label className="block font-bold mb-1">제목</label>
          <input
            name="title"
            type="text"
            value={board.title}
            onChange={handleChangeBoard}
            className="w-full p-3 border rounded-lg"
          />
        </div>
        <div>
          <label className="block font-bold mb-1">내용</label>
          <textarea
            name="content"
            rows="10"
            value={board.content}
            onChange={handleChangeBoard}
            className="w-full p-3 border rounded-lg"
          />
        </div>
      </div>
      <div className="flex justify-end gap-3 mt-8">
        <button
          onClick={moveToList}
          className="px-6 py-2 bg-gray-100 rounded-lg"
        >
          취소
        </button>
        <button
          onClick={handleClickAdd}
          className="px-8 py-2 bg-black text-white rounded-lg font-bold"
        >
          저장하기
        </button>
      </div>
    </div>
  );
};

export default AddComponent;
