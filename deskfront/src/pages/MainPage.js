import React, { useEffect, useState, useCallback } from "react";
import { useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import BasicLayout from "../layouts/BasicLayout";
import { getRecentReceivedTickets, getReceivedTickets, getSentTickets } from "../api/ticketApi";
import { getRecentBoards } from "../api/boardApi";
import useCustomPin from "../hooks/useCustomPin";
import TicketDetailModal from "../components/ticket/TicketDetailModal";
import { getGradeBadge } from "../util/ticketUtils";

const MainPage = () => {
  const loginState = useSelector((state) => state.loginSlice);
  const navigate = useNavigate();
  const { pinItems } = useCustomPin();

  const [recentTasks, setRecentTasks] = useState([]);
  const [recentBoards, setRecentBoards] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [pendingSentCount, setPendingSentCount] = useState(0);

  const [selectedTno, setSelectedTno] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const isLoggedIn = !!loginState.email;
  const email = loginState.email;
  const displayName = loginState.nickname || "사용자";

  const getCategoryStyle = (category) => {
    switch (category) {
      case "공지사항": return "bg-red-100 text-red-600 border border-red-200";
      case "가이드": return "bg-blue-100 text-blue-600 border border-blue-200";
      case "FAQ": return "bg-green-100 text-green-600 border border-green-200";
      default: return "bg-gray-100 text-gray-500 border border-gray-200";
    }
  };

  const fetchMainData = useCallback(() => {
    getRecentBoards().then(data => setRecentBoards(data || []));
    if (isLoggedIn) {
      getRecentReceivedTickets(email).then(data => setRecentTasks(data || []));
      getReceivedTickets(email, { size: 1 }, { read: false }).then(res => setUnreadCount(res.totalCount || 0));
      getSentTickets(email, { size: 1 }, { state: 'IN_PROGRESS' }).then(res => setPendingSentCount(res.totalCount || 0));
    }
  }, [isLoggedIn, email]);

  useEffect(() => {
    fetchMainData();
  }, [fetchMainData]);

  const moveToListWithFilter = (tab, read = "ALL", state = "") => {
    const params = new URLSearchParams();
    params.set("tab", tab);
    params.set("read", read);
    if (state) params.set("state", state);
    navigate({ pathname: "/tickets/list", search: `?${params.toString()}` });
  };

  const openDetail = (tno) => {
    setSelectedTno(tno);
    setIsModalOpen(true);
  };

  return (
    <BasicLayout>
      {/* py-12와 space-y-8로 '중간' 크기의 여백 확보 */}
      <div className="max-w-7xl mx-auto px-6 py-12 space-y-8 bg-gray-50 min-h-[calc(100vh-80px)]">

        {/* --- 상단 영역 (p-10으로 크기 상향) --- */}
        <section className="bg-indigo-600 rounded-2xl p-10 text-white shadow-lg relative overflow-hidden">
          <div className="relative z-10">
            <h1 className="text-3xl font-bold mb-3">안녕하세요, {displayName}님</h1>
            <p className="text-lg text-indigo-100 opacity-90">AI 챗봇과 대화하며 업무 요청을 정확하게 전달하세요.</p>
            <button
              onClick={() => navigate("/tickets/add")}
              className="mt-6 bg-white text-indigo-600 px-7 py-2.5 rounded-xl font-bold hover:bg-indigo-50 transition-all shadow-md active:scale-95"
            >
              + 새 업무 요청 만들기
            </button>
          </div>
          <div className="absolute top-[-20%] right-[-5%] w-72 h-72 bg-indigo-500 rounded-full opacity-20"></div>
          <div className="absolute bottom-[-15%] left-[30%] w-32 h-32 bg-indigo-400 rounded-full opacity-10"></div>
        </section>

        {/* --- 상태 카드 영역 (p-8, text-4xl로 크기 상향) --- */}
        <section className="grid grid-cols-1 md:grid-cols-3 gap-8">
          <StatusCard
            title="중요 업무 (PIN)"
            count={pinItems.length}
            color="text-blue-500"
            icon="⭐"
            onClick={() => window.dispatchEvent(new Event('open-pin-drawer'))}
          />
          <StatusCard
            title="읽지 않은 업무 (받은함)"
            count={unreadCount}
            color="text-green-500"
            icon="✉️"
            onClick={() => moveToListWithFilter('RECEIVED', 'UNREAD')}
          />
          <StatusCard
            title="진행 중인 업무 (전체)"
            count={pendingSentCount}
            color="text-orange-500"
            icon="💬"
            onClick={() => moveToListWithFilter('ALL', 'ALL', 'IN_PROGRESS')}
          />
        </section>

        {/* --- 하단 리스트 영역 (min-h-[440px]로 밸런스 조정) --- */}
        <section className="grid grid-cols-1 lg:grid-cols-2 gap-8">

          {/* 최근 받은 업무 */}
          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8 flex flex-col min-h-[440px]">
            <h2 className="text-xl font-bold text-gray-800 mb-8 border-l-4 border-indigo-500 pl-4">최근 받은 업무</h2>
            <div className="flex-grow space-y-4">
              {!isLoggedIn ? (
                <p className="text-center py-14 text-gray-400 font-medium">로그인이 필요합니다.</p>
              ) : recentTasks.length > 0 ? (
                recentTasks.map((task) => (
                  <div
                    key={task.tno}
                    onClick={() => openDetail(task.tno)}
                    className="p-5 border border-gray-50 rounded-xl hover:bg-gray-50 transition-all cursor-pointer flex justify-between items-center"
                  >
                    <div className="truncate pr-4">
                      <h3 className="text-lg font-semibold text-gray-800 truncate">{task.title}</h3>
                      <div className="flex text-sm text-gray-500 space-x-3 mt-1.5">
                        <span>🕒 {task.birth}</span>
                        <span>발신자: {task.writer}</span>
                      </div>
                    </div>
                    <div className="shrink-0">
                        {getGradeBadge(task.grade)}
                    </div>
                  </div>
                ))
              ) : (
                <p className="text-center py-14 text-gray-400 font-medium">받은 업무가 없습니다.</p>
              )}
            </div>
            <button
              onClick={() => moveToListWithFilter('RECEIVED')}
              className="mt-8 text-center text-sm text-gray-500 hover:text-indigo-600 font-bold border-t pt-5"
            >
              전체 업무 보기
            </button>
          </div>

          {/* 최근 공지사항 */}
          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-8 flex flex-col min-h-[440px]">
            <h2 className="text-xl font-bold text-gray-800 mb-8 border-l-4 border-indigo-500 pl-4">최근 공지</h2>
            <div className="flex-grow space-y-4">
              {recentBoards.length > 0 ? (
                recentBoards.map((board) => (
                  <div
                    key={board.bno}
                    onClick={() => navigate(`/board/read/${board.bno}`)}
                    className="flex justify-between items-center p-5 border-b border-gray-50 last:border-0 hover:bg-gray-50 rounded-xl transition-all cursor-pointer"
                  >
                    <div className="truncate pr-4">
                      <h3 className="text-base font-medium text-gray-700 truncate">{board.title}</h3>
                      <span className="text-sm text-gray-400">{board.regDate}</span>
                    </div>
                    <span className={`shrink-0 text-[11px] px-3 py-1 rounded-full border font-bold ${getCategoryStyle(board.category)}`}>
                      {board.category || "일반"}
                    </span>
                  </div>
                ))
              ) : (
                <p className="text-center py-14 text-gray-400 font-medium">등록된 공지가 없습니다.</p>
              )}
            </div>
            <button
              onClick={() => navigate("/board/list")}
              className="mt-8 text-center text-sm text-gray-500 hover:text-indigo-600 font-bold border-t pt-5"
            >
              전체 공지 보기
            </button>
          </div>
        </section>
      </div>

      {isModalOpen && selectedTno && (
        <TicketDetailModal
          tno={selectedTno}
          onClose={() => setIsModalOpen(false)}
          onDelete={() => { fetchMainData(); setIsModalOpen(false); }}
        />
      )}
    </BasicLayout>
  );
};

// 상태 카드 컴포넌트
const StatusCard = ({ title, count, color, icon, onClick }) => (
  <div onClick={onClick} className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100 flex justify-between items-center cursor-pointer hover:shadow-md transition-all">
    <div>
      <p className="text-sm font-medium text-gray-500 mb-1">{title}</p>
      <p className={`text-4xl font-extrabold ${color}`}>{count}</p>
    </div>
    <div className="text-3xl opacity-20">{icon}</div>
  </div>
);

export default MainPage;