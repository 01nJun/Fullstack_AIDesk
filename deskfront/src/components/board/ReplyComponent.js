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
    <div className="space-y-8">
      {/* 1. 댓글 입력 섹션 - 화이트 카드 스타일 */}
      <div className="bg-white p-8 rounded-[2rem] border border-gray-100 shadow-[0_10px_30px_rgba(0,0,0,0.03)]">
        <div className="flex items-center gap-3 mb-6">
            <span className="w-2 h-6 bg-blue-600 rounded-full"></span>
            <h3 className="text-xl font-black text-gray-900 tracking-tighter italic uppercase">
                Comments <span className="text-blue-600">[{serverData.totalCount}]</span>
            </h3>
        </div>

        <div className="flex gap-3">
          <input
            type="text"
            value={replyText}
            onChange={(e) => setReplyText(e.target.value)}
            placeholder={loginState.nickname ? "매너 있는 댓글을 남겨주세요." : "로그인이 필요합니다."}
            disabled={!loginState.nickname}
            className="flex-1 p-4 bg-gray-50 border-2 border-transparent rounded-2xl focus:bg-white focus:border-blue-500 focus:ring-4 focus:ring-blue-50 transition-all outline-none font-medium placeholder:text-gray-300"
          />
          <button
            onClick={() => handleClickRegister()}
            disabled={!loginState.nickname}
            className="bg-[#111827] text-white px-8 py-4 rounded-2xl font-black text-sm uppercase tracking-widest hover:bg-blue-600 disabled:bg-gray-200 transition-all shadow-lg shadow-gray-200 hover:-translate-y-1"
          >
            등록
          </button>
        </div>
      </div>

      {/* 2. 댓글 리스트 섹션 */}
      <div className="space-y-4">
        {serverData.dtoList.map((reply) => (
          <div
            key={reply.rno}
            className={`transition-all duration-300 ${reply.parentRno ? 'ml-12' : ''}`}
          >
            <div className={`p-6 bg-white rounded-[1.8rem] border border-gray-100 shadow-sm hover:shadow-md transition-all ${reply.parentRno ? 'border-l-4 border-l-blue-500' : ''}`}>

              {/* 댓글 헤더 */}
              <div className="flex justify-between items-center mb-4">
                <div className="flex items-center gap-2">
                    {reply.parentRno && <span className="text-blue-500 font-black mr-1 text-lg">ㄴ</span>}
                    <span className="font-black text-gray-900 text-sm tracking-tight">{reply.replyer}</span>
                </div>
                <span className="text-[10px] font-bold text-gray-300 italic uppercase tracking-widest">{reply.regDate}</span>
              </div>

              {editRno === reply.rno ? (
                <div className="space-y-3">
                  <textarea
                    value={editText}
                    onChange={(e) => setEditText(e.target.value)}
                    className="w-full p-4 bg-blue-50 border-2 border-blue-100 rounded-2xl focus:outline-none font-medium text-gray-700"
                    rows="2"
                  />
                  <div className="flex justify-end gap-2">
                    <button onClick={() => setEditRno(null)} className="px-4 py-2 text-xs font-bold text-gray-400 uppercase tracking-widest">Cancel</button>
                    <button onClick={handleClickModify} className="px-5 py-2 bg-blue-600 text-white rounded-xl text-xs font-black uppercase tracking-widest shadow-lg shadow-blue-100">Update</button>
                  </div>
                </div>
              ) : (
                <div>
                  <p className="text-gray-600 font-medium leading-relaxed px-1">
                    {reply.replyText}
                  </p>

                  {/* 댓글 액션 버튼 */}
                  <div className="flex justify-end gap-5 mt-5 pt-4 border-t border-gray-50">
                    {!reply.parentRno && loginState.nickname && (
                      <button
                        onClick={() => setReplyingTo(replyingTo === reply.rno ? null : reply.rno)}
                        className="text-[11px] font-black text-blue-500 uppercase tracking-widest hover:scale-110 transition-transform"
                      >
                        답변
                      </button>
                    )}
                    {canModify(reply.replyer) && (
                      <button
                        onClick={() => {setEditRno(reply.rno); setEditText(reply.replyText);}}
                        className="text-[11px] font-black text-gray-300 uppercase tracking-widest hover:text-gray-900 transition-colors"
                      >
                        수정
                      </button>
                    )}
                    {canDelete(reply.replyer) && (
                      <button
                        onClick={() => handleClickDelete(reply.rno)}
                        className="text-[11px] font-black text-gray-300 uppercase tracking-widest hover:text-red-500 transition-colors"
                      >
                        삭제
                      </button>
                    )}
                  </div>

                  {/* 답글 입력창 (답글 모드 활성화 시) */}
                  {replyingTo === reply.rno && (
                    <div className="mt-6 p-6 bg-gray-50 rounded-[1.5rem] border border-gray-100 animate-fadeIn">
                        <div className="flex gap-3">
                            <input
                                type="text"
                                value={replyingText}
                                onChange={(e) => setReplyingText(e.target.value)}
                                placeholder="답글을 입력해 주세요..."
                                className="flex-1 p-3 bg-white border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-400 outline-none text-sm font-medium"
                            />
                            <button
                                onClick={() => handleClickRegister(reply.rno)}
                                className="bg-blue-600 text-white px-6 py-2 rounded-xl font-black text-xs uppercase tracking-widest shadow-lg shadow-blue-100 hover:bg-blue-700 transition-all"
                            >
                                Post Reply
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