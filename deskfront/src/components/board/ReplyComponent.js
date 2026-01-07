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
      parentRno: parentRno
    };

    postReply(replyObj).then(() => {
      setReplyText("");
      setReplyingText("");
      setReplyingTo(null);
      refreshList();
    }).catch(err => alert("등록 권한이 없습니다."));
  };

  const handleClickDelete = (rno) => {
    if (window.confirm("정말로 삭제하시겠습니까?")) {
      deleteReply(rno).then(() => {
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
    <div className="space-y-6">
      {/* 1. 댓글 입력 섹션 */}
      <div className="ui-panel">
        <div className="flex items-center gap-3 mb-6">
            <span className="w-1 h-6 bg-brandNavy rounded-full"></span>
            <h3 className="ui-title">
                댓글 <span className="text-baseMuted">({serverData.totalCount})</span>
            </h3>
        </div>

        <div className="flex gap-3">
          <input
            type="text"
            value={replyText}
            onChange={(e) => setReplyText(e.target.value)}
            placeholder={loginState.nickname ? "매너 있는 댓글을 남겨주세요." : "로그인이 필요합니다."}
            disabled={!loginState.nickname}
            className="ui-input flex-1"
          />
          <button
            onClick={() => handleClickRegister()}
            disabled={!loginState.nickname}
            className="ui-btn-primary"
          >
            등록
          </button>
        </div>
      </div>

      {/* 2. 댓글 리스트 섹션 */}
      <div className="space-y-4">
        {(serverData.dtoList || []).map((reply) => (
          <div
            key={reply.rno}
            className={`transition-all duration-300 ${reply.parentRno ? 'ml-8 lg:ml-12' : ''}`}
          >
            <div className={`ui-card p-4 lg:p-6 ${reply.parentRno ? 'border-l-4 border-l-brandNavy' : ''}`}>

              {/* 댓글 헤더 */}
              <div className="flex justify-between items-center mb-3">
                <div className="flex items-center gap-2">
                    {reply.parentRno && <span className="text-brandNavy font-semibold mr-1">ㄴ</span>}
                    <span className="font-semibold text-baseText text-sm">{reply.replyer}</span>
                </div>
                <span className="text-xs text-baseMuted">{reply.regDate}</span>
              </div>

              {editRno === reply.rno ? (
                <div className="space-y-3">
                  <textarea
                    value={editText}
                    onChange={(e) => setEditText(e.target.value)}
                    className="ui-textarea"
                    rows="2"
                  />
                  <div className="flex justify-end gap-2">
                    <button onClick={() => setEditRno(null)} className="ui-btn-ghost text-xs px-3 py-1.5">취소</button>
                    <button onClick={handleClickModify} className="ui-btn-primary text-xs px-4 py-1.5">수정</button>
                  </div>
                </div>
              ) : (
                <div>
                  <p className="text-baseText font-medium leading-relaxed">
                    {reply.replyText}
                  </p>

                  {/* 댓글 액션 버튼 */}
                  <div className="flex justify-end gap-4 mt-4 pt-4 ui-divider">
                    {!reply.parentRno && loginState.nickname && (
                      <button
                        onClick={() => setReplyingTo(replyingTo === reply.rno ? null : reply.rno)}
                        className="text-xs font-semibold text-brandNavy hover:opacity-80 transition-opacity"
                      >
                        답변
                      </button>
                    )}
                    {canModify(reply.replyer) && (
                      <button
                        onClick={() => {setEditRno(reply.rno); setEditText(reply.replyText);}}
                        className="text-xs font-semibold text-baseMuted hover:text-baseText transition-colors"
                      >
                        수정
                      </button>
                    )}
                    {canDelete(reply.replyer) && (
                      <button
                        onClick={() => handleClickDelete(reply.rno)}
                        className="text-xs font-semibold text-baseMuted hover:text-red-600 transition-colors"
                      >
                        삭제
                      </button>
                    )}
                  </div>

                  {/* 답글 입력창 (답글 모드 활성화 시) */}
                  {replyingTo === reply.rno && (
                    <div className="mt-4 p-4 bg-baseSurface rounded-ui border border-baseBorder">
                        <div className="flex gap-2">
                            <input
                                type="text"
                                value={replyingText}
                                onChange={(e) => setReplyingText(e.target.value)}
                                placeholder="답글을 입력해 주세요..."
                                className="ui-input flex-1 text-sm"
                            />
                            <button
                                onClick={() => handleClickRegister(reply.rno)}
                                className="ui-btn-primary text-xs px-4 py-2"
                            >
                                등록
                            </button>
                        </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default ReplyComponent;