import React from "react";
import { useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import BasicLayout from "../layouts/BasicLayout";

// 가상의 데이터 (추후 API 연동 시 이 구조를 활용)
const DUMMY_TICKETS = [
  { id: 1, title: "로그인 페이지 UI 개선 요청", date: "2024-12-20", status: "진행중", manager: "이영희", priority: "높음" },
  { id: 2, title: "대시보드 차트 추가", date: "2024-12-22", status: "추가 정보 필요", manager: "박민수", priority: "중간" },
  { id: 3, title: "사용자 권한 관리 기능", date: "2024-12-18", status: "검토중", manager: "김철수", priority: "높음" },
];

const DUMMY_NOTICES = [
  { id: 1, title: "디자인 시스템 가이드 v2.0 업데이트", date: "2024-12-15", category: "가이드" },
  { id: 2, title: "UI 컴포넌트 사용법", date: "2024-12-14", category: "FAQ" },
  { id: 3, title: "팀 회의 안건 공유", date: "2024-12-13", category: "공지사항" },
];

const MainPage = () => {
  const loginState = useSelector((state) => state.loginSlice);
  const navigate = useNavigate();

  // 로그인 여부 확인
  const isLoggedIn = !!loginState.email;
  const displayName = loginState.nickname || "사용자";

  // 상태 카운트 (추후 API 연동 대상)
  const statusCounts = {
    important: 0,
    unread: 0,
    pending: 0,
  };

  return (
  <BasicLayout>
    <div className="max-w-7xl mx-auto p-6 space-y-8 bg-gray-50 min-h-screen">
      {/* --- 상단 인사 문구 영역 (컬러 이미지 스타일 반영) --- */}
      <section className="bg-indigo-600 rounded-2xl p-8 text-white shadow-lg relative overflow-hidden">
        <div className="relative z-10">
          <h1 className="text-3xl font-bold mb-2">안녕하세요, {displayName}님</h1>
          <p className="text-indigo-100 opacity-90">AI 챗봇과 대화하며 업무 요청을 정확하게 전달하세요.</p>
          <button className="mt-6 bg-white text-indigo-600 px-6 py-2 rounded-lg font-semibold hover:bg-indigo-50 transition-colors shadow-md">
            + 새 요청 만들기
          </button>
        </div>
        {/* 장식용 원형 배경 */}
        <div className="absolute top-[-20%] right-[-5%] w-64 h-64 bg-indigo-500 rounded-full opacity-20"></div>
      </section>

      {/* --- 상태 카운트 영역 (와이어프레임 구조 + 컬러 디자인 스타일) --- */}
      <section className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <StatusCard title="중요 (찜, Cart)" count={statusCounts.important} color="text-blue-500" icon="⭐" />
        <StatusCard title="읽기 전" count={statusCounts.unread} color="text-green-500" icon="✉️" />
        <StatusCard title="답변 전 (상태 완료 전)" count={statusCounts.pending} color="text-orange-500" icon="💬" />
      </section>

      {/* --- 리스트 영역 (내 티켓 & 최근 공지) --- */}
      <section className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* 내 요청 티켓 */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 flex flex-col">
          <div className="flex justify-between items-center mb-6">
            <div>
              <h2 className="text-xl font-bold text-gray-800">내 요청 티켓</h2>
              <p className="text-sm text-gray-500">최근 생성된 티켓 목록</p>
            </div>
          </div>

          <div className="flex-grow space-y-4">
            {!isLoggedIn ? (
              <p className="text-center py-10 text-gray-400 font-medium">로그인 후 이용 부탁드립니다.</p>
            ) : (
              DUMMY_TICKETS.map((ticket) => (
                <div key={ticket.id} className="p-4 border border-gray-100 rounded-lg hover:bg-gray-50 transition-colors">
                  <div className="flex justify-between items-start mb-2">
                    <h3 className="font-semibold text-gray-800">{ticket.title}</h3>
                    <span className={`text-xs px-2 py-1 rounded ${ticket.priority === '높음' ? 'bg-red-100 text-red-600' : 'bg-gray-100 text-gray-600'}`}>
                      {ticket.priority}
                    </span>
                  </div>
                  <div className="flex text-xs text-gray-500 space-x-4">
                    <span>🕒 {ticket.date}</span>
                    <span>{ticket.status}</span>
                    <span>담당: {ticket.manager}</span>
                  </div>
                </div>
              ))
            )}
          </div>
          <button
            onClick={() => navigate("/tickets/")}
            className="mt-6 text-center text-sm text-gray-500 hover:text-indigo-600 font-medium border-t pt-4"
          >
            전체 티켓 보기
          </button>
        </div>

        {/* 최근 공지 (게시판) */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 flex flex-col">
          <div className="flex justify-between items-center mb-6">
            <div>
              <h2 className="text-xl font-bold text-gray-800">최근 공지</h2>
              <p className="text-sm text-gray-500">최근 등록된 공지사항</p>
            </div>
          </div>

          <div className="flex-grow space-y-4">
            {!isLoggedIn ? (
              <p className="text-center py-10 text-gray-400 font-medium">로그인 후 이용 부탁드립니다.</p>
            ) : (
              DUMMY_NOTICES.map((notice) => (
                <div key={notice.id} className="flex justify-between items-center p-4 border-b border-gray-50 last:border-0">
                  <div>
                    <h3 className="text-sm font-medium text-gray-700">{notice.title}</h3>
                    <span className="text-xs text-gray-400">{notice.date}</span>
                  </div>
                  <span className="text-xs bg-gray-50 text-gray-500 px-2 py-1 rounded border border-gray-200">
                    {notice.category}
                  </span>
                </div>
              ))
            )}
          </div>
          <button
            onClick={() => navigate("/about")} // BasicMenu의 "About" 혹은 "공지사항" 경로
            className="mt-6 text-center text-sm text-gray-500 hover:text-indigo-600 font-medium border-t pt-4"
          >
            전체 공지 보기
          </button>
        </div>
      </section>
    </div>
    </BasicLayout>
  );
};

// 재사용 가능한 상태 카드 컴포넌트
const StatusCard = ({ title, count, color, icon }) => (
  <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 flex justify-between items-center">
    <div>
      <p className="text-sm font-medium text-gray-500 mb-1">{title}</p>
      <p className={`text-3xl font-bold ${color}`}>{count}</p>
    </div>
    <div className="text-2xl opacity-20">{icon}</div>
  </div>
);

export default MainPage;