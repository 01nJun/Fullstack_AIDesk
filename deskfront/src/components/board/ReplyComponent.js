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

  const { loginState } = useCustomLogin();



  // 수정 권한: 작성자 본인만 가능 (관리자도 남의 글은 수정 불가)
  const canModify = (replyer) => {
    return loginState.nickname === replyer;
  };

  // 삭제 권한: 작성자 본인 또는 관리자(ADMIN) 가능
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

  const handleClickRegister = () => {
    if (!replyText.trim()) {
      alert("댓글 내용을 입력해주세요.");
      return;
    }
    const replyObj = {
      bno: bno,
      replyText: replyText,
      replyer: loginState.nickname || "익명"
    };
    postReply(replyObj).then(() => {
      alert("댓글이 등록되었습니다.");
      setReplyText("");
      refreshList();
    }).catch(err => alert("등록 권한이 없습니다."));
  };

  const handleClickDelete = (rno) => {
    if (window.confirm("정말로 이 댓글을 삭제하시겠습니까?")) {
      deleteReply(rno).then(() => {
        alert("삭제되었습니다.");
        refreshList();
      }).catch(err => alert("삭제 권한이 없습니다."));
    }
  };

  const enterEditMode = (reply) => {
    setEditRno(reply.rno);
    setEditText(reply.replyText);
  };

  const handleClickModify = () => {
    if (!editText.trim()) {
      alert("수정할 내용을 입력해주세요.");
      return;
    }
    const replyObj = { rno: editRno, replyText: editText };

    putReply(replyObj).then(() => {
      alert("댓글이 수정되었습니다.");
      setEditRno(null);
      refreshList();
    }).catch(err => {
      // ★ 여기가 중요: 백엔드에서 에러를 던지면 여기서 잡습니다.
      alert("수정 실패: 본인만 수정할 수 있습니다.");
    });
  };

  return (
    <div className="mt-10 p-6 bg-gray-50 rounded-2xl border border-gray-100">
      <h3 className="text-xl font-bold mb-4 text-gray-800">
        댓글 {serverData.totalCount}개
      </h3>

      <div className="flex gap-2 mb-8">
        <input
          type="text"
          value={replyText}
          onChange={(e) => setReplyText(e.target.value)}
          placeholder={loginState.nickname ? "매너 있는 댓글을 남겨주세요." : "로그인이 필요합니다."}
          disabled={!loginState.nickname}
          className="flex-1 p-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-400 disabled:bg-gray-200"
        />
        <button
          onClick={handleClickRegister}
          disabled={!loginState.nickname}
          className="bg-black text-white px-6 py-2 rounded-xl font-bold hover:bg-gray-800 transition-all disabled:bg-gray-400"
        >
          등록
        </button>
      </div>

      <div className="space-y-4">
        {serverData.dtoList.length > 0 ? (
          serverData.dtoList.map((reply) => (
            <div key={reply.rno} className="p-4 bg-white rounded-xl shadow-sm border border-gray-100">
              <div className="flex justify-between items-center mb-2">
                <span className="font-bold text-gray-700">{reply.replyer}</span>
                <span className="text-xs text-gray-400">{reply.regDate}</span>
              </div>

              {editRno === reply.rno ? (
                <div>
                  <textarea
                    value={editText}
                    onChange={(e) => setEditText(e.target.value)}
                    className="w-full p-3 border border-blue-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-400"
                    rows="2"
                  />
                  <div className="flex justify-end gap-2 mt-2">
                    <button onClick={() => setEditRno(null)} className="px-3 py-1 text-sm text-gray-500">취소</button>
                    <button onClick={handleClickModify} className="px-3 py-1 text-sm bg-blue-500 text-white rounded-lg font-bold">저장</button>
                  </div>
                </div>
              ) : (
                <div>
                  <p className="text-gray-600 leading-relaxed">{reply.replyText}</p>

                  <div className="flex justify-end gap-3 mt-3 border-t pt-2 border-gray-50">
                    {/* [수정] 버튼: 오직 본인에게만 보여줌 */}
                    {canModify(reply.replyer) && (
                      <button
                        onClick={() => enterEditMode(reply)}
                        className="text-xs text-gray-400 hover:text-blue-500 transition-colors"
                      >
                        수정
                      </button>
                    )}

                    {/* [삭제] 버튼: 본인 또는 관리자에게 보여줌 */}
                    {canDelete(reply.replyer) && (
                      <button
                        onClick={() => handleClickDelete(reply.rno)}
                        className="text-xs text-gray-400 hover:text-red-500 transition-colors"
                      >
                        삭제
                      </button>
                    )}
                  </div>
                </div>
              )}
            </div>
          ))
        ) : (
          <div className="text-center py-10 text-gray-400 font-medium">
          </div>
        )}
      </div>
    </div>
  );
};

export default ReplyComponent;