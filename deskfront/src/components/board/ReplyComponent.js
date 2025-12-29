import { useEffect, useState } from "react";
import { getReplyList, postReply, deleteReply, putReply } from "../../api/replyApi";
import useCustomLogin from "../../hooks/useCustomLogin";

const initState = {
  dtoList: [],
  pageRequestDTO: null,
  totalCount: 0,
};

const ReplyComponent = ({ bno }) => {
  const [serverData, setServerData] = useState(initState);
  const [replyText, setReplyText] = useState("");
  const [editRno, setEditRno] = useState(null);
  const [editText, setEditText] = useState("");

  //  답글 달기 상태 관리 (어떤 rno에 답글을 다는지)
  const [replyingTo, setReplyingTo] = useState(null);
  const [replyingText, setReplyingText] = useState("");

  const { loginState } = useCustomLogin();

  const canModify = (replyer) => loginState.nickname === replyer;
  const canDelete = (replyer) => {
    const isAdmin = loginState.roleNames?.includes("ADMIN");
    const isOwner = loginState.nickname === replyer;
    return isAdmin || isOwner;
  };

  const refreshList = (page = 1) => {
    getReplyList(bno, page).then((data) => {
      setServerData(data);
    });
  };

  useEffect(() => {
    refreshList();
  }, [bno]);

  //  등록 로직 통합 (일반 댓글 & 대댓글)
  const handleClickRegister = (parentRno = null) => {
    const text = parentRno ? replyingText : replyText;

    if (!text.trim()) {
      alert("내용을 입력해주세요.");
      return;
    }

    const replyObj = {
      bno: bno,
      replyText: text,
      replyer: loginState.nickname || ".",
      parentRno: parentRno // 백엔드로 부모 번호 전송
    };

    postReply(replyObj).then(() => {
      alert("등록되었습니다.");
      setReplyText("");
      setReplyingText(""); // 대댓글 입력창 초기화
      setReplyingTo(null);  // 대댓글 모드 종료
      refreshList();
    }).catch(err => alert("등록 권한이 없습니다."));
  };

  const handleClickDelete = (rno) => {
    if (window.confirm("정말로 삭제하시겠습니까?")) {
      deleteReply(rno).then(() => {
        alert("삭제되었습니다.");
        refreshList();
      });
    }
  };

  const handleClickModify = () => {
    if (!editText.trim()) return;
    putReply({ rno: editRno, replyText: editText }).then(() => {
      setEditRno(null);
      refreshList();
    });
  };

  return (
    <div className="mt-10 p-6 bg-gray-50 rounded-2xl border border-gray-100">
      <h3 className="text-xl font-bold mb-4 text-gray-800">댓글 {serverData.totalCount}개</h3>

      {/* 메인 댓글 입력창 */}
      <div className="flex gap-2 mb-8">
        <input
          type="text"
          value={replyText}
          onChange={(e) => setReplyText(e.target.value)}
          placeholder={loginState.nickname ? "매너 있는 댓글을 남겨주세요." : "로그인이 필요합니다."}
          disabled={!loginState.nickname}
          className="flex-1 p-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-400 disabled:bg-gray-200"
        />
        <button onClick={() => handleClickRegister()} disabled={!loginState.nickname} className="bg-black text-white px-6 py-2 rounded-xl font-bold hover:bg-gray-800">등록</button>
      </div>

      {/* 댓글 리스트 */}
      <div className="space-y-4">
        {serverData.dtoList.map((reply) => (
          <div
            key={reply.rno}
            //  대댓글인 경우 왼쪽 여백(들여쓰기) 적용
            className={`p-4 bg-white rounded-xl shadow-sm border border-gray-100 ${reply.parentRno ? 'ml-12 border-l-4 border-l-blue-200' : ''}`}
          >
            <div className="flex justify-between items-center mb-2">
              <span className="font-bold text-gray-700">
                {reply.parentRno && <span className="text-blue-500 mr-2">ㄴ</span>}
                {reply.replyer}
              </span>
              <span className="text-xs text-gray-400">{reply.regDate}</span>
            </div>

            {editRno === reply.rno ? (
              <div>
                <textarea value={editText} onChange={(e) => setEditText(e.target.value)} className="w-full p-3 border border-blue-200 rounded-xl" rows="2" />
                <div className="flex justify-end gap-2 mt-2">
                  <button onClick={() => setEditRno(null)} className="text-sm text-gray-500">취소</button>
                  <button onClick={handleClickModify} className="px-3 py-1 bg-blue-500 text-white rounded-lg">저장</button>
                </div>
              </div>
            ) : (
              <div>
                <p className="text-gray-600 leading-relaxed">{reply.replyText}</p>

                <div className="flex justify-end gap-3 mt-3 border-t pt-2">
                  {/* 답글 버튼 (부모 댓글에만 표시하거나 전체 표시 가능) */}
                  {!reply.parentRno && loginState.nickname && (
                    <button onClick={() => setReplyingTo(replyingTo === reply.rno ? null : reply.rno)} className="text-xs text-blue-500 font-bold">답글</button>
                  )}
                  {canModify(reply.replyer) && <button onClick={() => {setEditRno(reply.rno); setEditText(reply.replyText);}} className="text-xs text-gray-400">수정</button>}
                  {canDelete(reply.replyer) && <button onClick={() => handleClickDelete(reply.rno)} className="text-xs text-gray-400">삭제</button>}
                </div>

                {/* : 답글 입력창 (답글 버튼 클릭 시 해당 댓글 바로 밑에 등장) */}
                {replyingTo === reply.rno && (
                  <div className="mt-4 p-3 bg-blue-50 rounded-xl border border-blue-100">
                    <input
                      type="text"
                      value={replyingText}
                      onChange={(e) => setReplyingText(e.target.value)}
                      placeholder="답글을 입력하세요..."
                      className="w-full p-2 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-300"
                    />
                    <div className="flex justify-end gap-2 mt-2">
                      <button onClick={() => setReplyingTo(null)} className="text-xs text-gray-500">취소</button>
                      <button onClick={() => handleClickRegister(reply.rno)} className="text-xs bg-blue-500 text-white px-3 py-1 rounded-lg font-bold">답글 등록</button>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

export default ReplyComponent;