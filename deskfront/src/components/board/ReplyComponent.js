 import { useEffect, useState } from "react";
 import { getReplyList, postReply } from "../../api/replyApi";
 import useCustomLogin from "../../hooks/useCustomLogin";

 const initState = {
   dtoList: [],
   pageRequestDTO: null,
   totalCount: 0,
 };

 const ReplyComponent = ({ bno }) => {
   const [serverData, setServerData] = useState(initState);
   const [replyText, setReplyText] = useState(""); // 입력창 글자 바구니
   const { loginState } = useCustomLogin(); // 로그인 정보 가져오기

   // 1. 댓글 목록 불러오기 함수
   const refreshList = (page = 1) => {
     getReplyList(bno, page).then((data) => {
       setServerData(data);
     });
   };

   // 페이지 처음 열릴 때 댓글 목록 호출
   useEffect(() => {
     refreshList();
   }, [bno]);

   // 2. 댓글 등록 버튼 클릭 시
   const handleClickRegister = () => {
     if (!replyText.trim()) {
       alert("댓글 내용을 입력해주세요.");
       return;
     }

     const replyObj = {
       bno: bno,
       replyText: replyText,
       replyer: loginState.nickname || "."
     };

     postReply(replyObj).then((data) => {
       alert("댓글이 등록되었습니다.");
       setReplyText(""); // 입력창 비우기
       refreshList(); // 목록 새로고침
     });
   };

   return (
     <div className="mt-10 p-6 bg-gray-50 rounded-2xl border border-gray-100">
       <h3 className="text-xl font-bold mb-4 text-gray-800">
         댓글 {serverData.totalCount}개
       </h3>

       {/* 댓글 입력창 */}
       <div className="flex gap-2 mb-8">
         <input
           type="text"
           value={replyText}
           onChange={(e) => setReplyText(e.target.value)}
           placeholder="매너 있는 댓글을 남겨주세요."
           className="flex-1 p-3 border border-gray-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-400"
         />
         <button
           onClick={handleClickRegister}
           className="bg-black text-white px-6 py-2 rounded-xl font-bold hover:bg-gray-800 transition-all"
         >
           등록
         </button>
       </div>

       {/* 댓글 목록 */}
       <div className="space-y-4">
         {serverData.dtoList.length > 0 ? (
           serverData.dtoList.map((reply) => (
             <div
               key={reply.rno}
               className="p-4 bg-white rounded-xl shadow-sm border border-gray-100"
             >
               <div className="flex justify-between items-center mb-2">
                 <span className="font-bold text-gray-700">{reply.replyer}</span>
                 <span className="text-xs text-gray-400">{reply.regDate}</span>
               </div>
               <p className="text-gray-600">{reply.replyText}</p>
             </div>
           ))
         ) : (
           <div className="text-center py-10 text-gray-400"></div>
         )}
       </div>
     </div>
   );
 };

 export default ReplyComponent;
