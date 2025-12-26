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
        // ★ 데이터를 불러올 때 서버가 꺼져있을 경우 처리
        console.error("데이터 로딩 에러:", err);
        alert("게시글 정보를 불러올 수 없습니다. 서버 연결을 확인하세요.");
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

    const boardParam = {
      bno: board.bno,
      title: board.title,
      content: board.content,
      writer: board.writer,
      category: board.category,
    };

    putOne(bno, boardParam)
      .then((data) => {
        // data?.error 처럼 옵셔널 체이닝을 쓰면 더 안전합니다.
        if (data?.error) {
          alert("수정 실패: 관리자 권한이 필요합니다.");
          setFetching(false);
          return;
        }

        setFetching(false);
        alert("성공적으로 수정되었습니다.");
        moveToRead(bno);
      })
      .catch((err) => {
        setFetching(false);

        // ★ [핵심 수정] 서버가 꺼져있으면 err.response 자체가 없습니다.
        // err.response가 있을 때만 status를 확인하도록 수정했습니다.
        if (err.response) {
          if (err.response.status === 401) {
            alert("인증에 실패했습니다. 다시 로그인해 주세요.");
          } else if (err.response.status === 403) {
            alert("실제 권한이 없습니다(관리자만 가능).");
          } else {
            alert("서버 에러가 발생했습니다: " + err.response.status);
          }
        } else {
          // 서버가 꺼져있거나 네트워크 연결이 안 된 경우(ERR_CONNECTION_REFUSED 등)
          alert("서버와 연결할 수 없습니다. 백엔드 서버가 작동 중인지 확인하세요.");
        }
      });
  };

  const handleClickDelete = () => {
    if (window.confirm("정말로 삭제하시겠습니까?")) {
      setFetching(true);
      deleteOne(bno)
        .then((data) => { // 여기
          if (data?.error) {
            alert("삭제 실패: 관리자 권한이 필요합니다.");
            setFetching(false);
            return;
          }
          setFetching(false);
          alert("삭제되었습니다.");
          moveToList();
        })
        .catch((err) => {
          setFetching(false);
          // ★ 삭제 시에도 네트워크 에러 처리 추가
          if (!err.response) {
            alert("서버와 연결할 수 없어 삭제에 실패했습니다.");
          } else {
            alert("삭제 실패: 관리자 권한이 필요합니다.");
          }
        });
    }
  };

  return (
    <div className="border-2 border-gray-100 mt-10 p-6 bg-white rounded-xl shadow-sm">
      {fetching ? <FetchingModal /> : <></>}

      <div className="mb-4">
        <label className="block text-sm font-bold text-gray-700 mb-2">
          카테고리
        </label>
        <select
          name="category"
          value={board.category}
          onChange={handleChangeBoard}
          className="w-full p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
        >
          <option value="공지사항">공지사항</option>
          <option value="가이드">가이드</option>
          <option value="FAQ">FAQ</option>
        </select>
      </div>

      <div className="mb-4">
        <label className="block text-sm font-bold text-gray-700 mb-2">
          제목
        </label>
        <input
          name="title"
          type="text"
          value={board.title}
          onChange={handleChangeBoard}
          className="w-full p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
        />
      </div>

      <div className="mb-4">
        <label className="block text-sm font-bold text-gray-400 mb-2">
          작성자
        </label>
        <input
          type="text"
          value={board.writer}
          readOnly
          className="w-full p-3 border rounded-lg bg-gray-50 text-gray-400 cursor-not-allowed"
        />
      </div>

      <div className="mb-6">
        <label className="block text-sm font-bold text-gray-700 mb-2">
          내용
        </label>
        <textarea
          name="content"
          rows="10"
          value={board.content}
          onChange={handleChangeBoard}
          className="w-full p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400 resize-none"
        ></textarea>
      </div>

      <div className="flex justify-end space-x-4">
        <button
          className="bg-red-500 text-white px-6 py-2 rounded-lg font-bold hover:bg-red-600"
          onClick={handleClickDelete}
        >
          삭제하기
        </button>
        <button
          className="bg-gray-100 text-gray-600 px-6 py-2 rounded-lg font-bold"
          onClick={() => moveToRead(bno)}
        >
          취소
        </button>
        <button
          className="bg-black text-white px-6 py-2 rounded-lg font-bold"
          onClick={handleClickModify}
        >
          저장하기
        </button>
      </div>
    </div>
  );
};

export default ModifyComponent;