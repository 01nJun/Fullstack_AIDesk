import React, { useEffect, useState, useCallback } from "react";
import { useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import BasicLayout from "../layouts/BasicLayout";
import { getRecentReceivedTickets, getReceivedTickets, getSentTickets } from "../api/ticketApi";
import { getRecentBoards } from "../api/boardApi";
import { getChatRooms } from "../api/chatApi";
import useCustomPin from "../hooks/useCustomPin";
import TicketDetailModal from "../components/ticket/TicketDetailModal";
import { getGradeBadge } from "../util/ticketUtils";
import AIAssistantModal from "../components/menu/AIAssistantModal";

const MainPage = () => {
  const loginState = useSelector((state) => state.loginSlice);
  const navigate = useNavigate();
  const { pinItems } = useCustomPin();

  const [recentTasks, setRecentTasks] = useState([]);
  const [recentBoards, setRecentBoards] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [unreadChatCount, setUnreadChatCount] = useState(0);

  const [selectedTno, setSelectedTno] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isAIWidgetOpen, setIsAIWidgetOpen] = useState(false); // 요청서 모달 상태 추가

  const isLoggedIn = !!loginState.email;
  const email = loginState.email;
  const displayName = loginState.nickname || "사용자";

  const getCategoryStyle = (category) => {
    switch (category) {
      case "공지사항": return "ui-badge-category-notice";
      case "가이드": return "ui-badge-category-guide";
      case "FAQ": return "ui-badge-category-faq";
      default: return "ui-badge-category";
    }
  };

  const fetchMainData = useCallback(() => {
    if (!isLoggedIn) {
      setRecentTasks([]);
      setRecentBoards([]);
      setUnreadCount(0);
      setUnreadChatCount(0);
      return;
    }

    getRecentBoards().then(data => setRecentBoards(data || [])).catch(() => {});
    getRecentReceivedTickets(email).then(data => setRecentTasks(data || [])).catch(() => setRecentTasks([]));
    getReceivedTickets(email, { size: 1 }, { read: false }).then(res => setUnreadCount(res.totalCount || 0)).catch(() => setUnreadCount(0));
    
    // 채팅방 목록 조회 후 unreadCount 합산
    getChatRooms()
      .then(rooms => {
        if (Array.isArray(rooms)) {
          const totalUnread = rooms.reduce((sum, room) => sum + (room.unreadCount || 0), 0);
          setUnreadChatCount(totalUnread);
        } else {
          setUnreadChatCount(0);
        }
      })
      .catch(() => setUnreadChatCount(0));
  }, [isLoggedIn, email]);

  useEffect(() => {
    fetchMainData();
  }, [fetchMainData]);

  const moveToListWithFilter = (tab, read = "ALL", state = "") => {
    if (!isLoggedIn) {
      alert("로그인이 필요한 서비스입니다.");
      navigate("/member/login");
      return;
    }
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

  // 3D Parallax Effect
  useEffect(() => {
    const container = document.getElementById('hero-section');
    const scene = document.querySelector('.scene-content');
    
    if (!container || !scene) return;

    const handleMouseMove = (e) => {
      if (window.matchMedia("(min-width: 768px)").matches) {
        const x = e.clientX;
        const y = e.clientY;
        
        const centerX = window.innerWidth / 2;
        const centerY = window.innerHeight / 2;
        
        const percentX = (x - centerX) / centerX;
        const percentY = (y - centerY) / centerY;

        // Parallax intensity
        const rotateY = percentX * 5; 
        const rotateX = percentY * -5; 
        const translateX = percentX * 10;
        const translateY = percentY * 10;

        scene.style.transform = `
          rotateX(${rotateX}deg) 
          rotateY(${rotateY}deg)
          translateX(${translateX}px)
          translateY(${translateY}px)
        `;
      }
    };

    const handleMouseLeave = () => {
      if (scene) {
        scene.style.transform = `rotateX(0) rotateY(0) translateX(0) translateY(0)`;
      }
    };

    container.addEventListener('mousemove', handleMouseMove);
    container.addEventListener('mouseleave', handleMouseLeave);

    return () => {
      container.removeEventListener('mousemove', handleMouseMove);
      container.removeEventListener('mouseleave', handleMouseLeave);
    };
  }, []);

  return (
    <BasicLayout>
      {/* 요청서 모달 추가 */}
      {isAIWidgetOpen && <AIAssistantModal onClose={() => setIsAIWidgetOpen(false)} />}

      <div className="bg-slate-50 min-h-screen flex flex-col">
        {/* Hero Section - 데스크톱만 표시 */}
        <section id="hero-section" className="hidden lg:block relative w-full bg-[#05060a] text-white overflow-hidden shadow-xl z-0 min-h-[550px] md:min-h-[450px] transition-all">
          {/* 1. Vivid Background Grid */}
          <div className="absolute inset-0 bg-grid pointer-events-none"></div>

          {/* 2. Sharp Flowing Lines (SVG) */}
          <div className="flow-lines-container">
            <svg width="100%" height="100%" preserveAspectRatio="none">
              <defs>
                {/* Brand Blue Gradient Stroke */}
                <linearGradient id="gradient-stroke" x1="0%" y1="0%" x2="100%" y2="0%">
                  <stop offset="0%" style={{stopColor: '#1f3a68', stopOpacity: 0}}></stop>
                  <stop offset="20%" style={{stopColor: '#3b82f6', stopOpacity: 0.8}}></stop>
                  <stop offset="50%" style={{stopColor: '#60a5fa', stopOpacity: 1}}></stop>
                  <stop offset="80%" style={{stopColor: '#3b82f6', stopOpacity: 0.8}}></stop>
                  <stop offset="100%" style={{stopColor: '#1f3a68', stopOpacity: 0}}></stop>
                </linearGradient>
                
                {/* Brand Orange Gradient Stroke */}
                <linearGradient id="gradient-stroke-orange" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" style={{stopColor: '#ff8a2a', stopOpacity: 0}}></stop>
                  <stop offset="50%" style={{stopColor: '#ff8a2a', stopOpacity: 1}}></stop>
                  <stop offset="100%" style={{stopColor: '#ff8a2a', stopOpacity: 0}}></stop>
                </linearGradient>
              </defs>

              {/* Flow Path 1: Top Curve */}
              <path className="flow-path" d="M-100,150 C300,150 400,350 800,350 S1300,100 2000,200"></path>
              
              {/* Flow Path 2: Bottom Wave */}
              <path className="flow-path" d="M-100,650 C500,650 600,450 1000,450 S1600,750 2000,550" style={{animationDuration: '20s', strokeWidth: '2px'}}></path>
              
              {/* Flow Path 3: Reverse Orange (Subtle) */}
              <path className="flow-path-reverse" d="M2000,100 C1500,100 1400,400 900,400 S400,100 -100,300"></path>
              
              {/* Flow Path 4: Reverse Orange (Dynamic) */}
              <path className="flow-path-reverse" d="M2000,800 C1600,800 1500,500 1000,500 S400,900 -100,700" style={{animationDuration: '18s'}}></path>
              
              {/* Extra vertical connection hints */}
              <path className="flow-path" d="M400,-100 Q400,400 800,1000" style={{strokeOpacity: 0.3, strokeDasharray: '5, 15', strokeWidth: '1px'}}></path>
            </svg>
          </div>

          {/* Ambient Glows */}
          <div className="glow-blob bg-[#1f3a68] w-[800px] h-[800px] -top-[20%] -left-[10%] animate-pulse" style={{animationDuration: '6s'}}></div>
          <div className="glow-blob bg-[#ff8a2a] w-[600px] h-[600px] -bottom-[20%] -right-[10%] animate-pulse" style={{animationDuration: '8s'}}></div>

          {/* Content Grid */}
          <div className="relative w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 grid lg:grid-cols-2 gap-16 items-center z-20 min-h-[550px] md:min-h-[650px] py-16">
            
            {/* Left: Text Content */}
            <div className="text-center lg:text-left space-y-8 pointer-events-none lg:pointer-events-auto relative">
              {isLoggedIn && (
                <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-[#1f3a68]/40 border border-[#1f3a68] text-xs font-semibold text-blue-100 mb-2 backdrop-blur-sm">
                  <span className="relative flex h-2 w-2">
                    <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-[#ff8a2a] opacity-75"></span>
                    <span className="relative inline-flex rounded-full h-2 w-2 bg-[#ff8a2a]"></span>
                  </span>
                  AI Workflow Engine Active
                </div>
              )}
              
              <h1 className="text-5xl lg:text-6xl font-extrabold tracking-tight leading-[1.15] drop-shadow-2xl">
                {isLoggedIn ? (
                  <>
                    안녕하세요, {displayName}님 <br />
                    <span className="text-5xl text-gradient-brand">오늘의 업무를 시작해볼까요?</span>
                  </>
                ) : (
                  <>
                    로그인이 필요합니다
                  </>
                )}
              </h1>
              
              <p className="text-lg text-gray-300 max-w-xl mx-auto lg:mx-0 leading-relaxed font-light">
                AI 챗봇과 대화하며 복잡한 업무 요청서를 자동으로 생성하세요. <br className="hidden lg:block" />
                {isLoggedIn && (
                  <>
                     {displayName}님이 고정해두신 중요한 업무가 <strong className="text-[#ff8a2a] font-semibold">{pinItems.length}건</strong> 있습니다.
                  </>
                )}
              </p>

              {/* Interactive AI Input Area */}
              <div className="flex flex-col sm:flex-row gap-4 justify-center lg:justify-start pt-4">
                <button
                  onClick={() => isLoggedIn ? setIsAIWidgetOpen(true) : navigate("/member/login")}
                  className="group px-8 py-3.5 rounded-md bg-[#1f3a68] hover:bg-[#2c4c82] border border-[#ff8a2a]/50 text-white font-semibold shadow-[0_0_20px_rgba(31,58,104,0.4)] transition-all transform hover:scale-105 active:scale-95 flex items-center justify-center gap-2"
                >
                  <span>{isLoggedIn ? "새 업무 요청서 만들기" : "로그인 하러 가기"}</span>
                  <span className="material-symbols-outlined text-sm text-[#ff8a2a] group-hover:translate-x-1 transition-transform">arrow_forward</span>
                </button>
              </div>
            </div>

            {/* Right: 3D Visualization */}
            <div id="scene" className="h-[550px] w-full flex items-center justify-center scene-container relative pointer-events-none lg:pointer-events-auto">
              <div className="scene-content relative w-full h-full flex items-center justify-center" style={{transform: 'rotateX(0deg) rotateY(0deg) translateX(0px) translateY(0px)'}}>
                
                {/* 1. Center Core: 업무 히스토리 (Hub) */}
                <div className="glass-card absolute w-72 h-72 rounded-[2rem] flex flex-col items-center justify-center z-10 animate-float-slow transform rotate-6 border-[#ff8a2a]/30">
                  <div className="absolute inset-0 rounded-[2rem] border border-[#1f3a68] animate-pulse"></div>
                  <div className="w-24 h-24 bg-gradient-to-tr from-[#1f3a68] to-[#0f172a] rounded-3xl flex items-center justify-center mb-5 shadow-2xl shadow-black/50 relative border border-white/10">
                    <span className="material-symbols-outlined text-5xl text-white">hub</span>
                    {/* Connection dots */}
                    <div className="absolute -top-1.5 -right-1.5 w-3 h-3 bg-[#ff8a2a] rounded-full shadow-[0_0_10px_#ff8a2a]"></div>
                    <div className="absolute -bottom-1.5 -left-1.5 w-2 h-2 bg-blue-400 rounded-full"></div>
                  </div>
                  <h3 className="font-bold text-xl text-white tracking-wide">Work History</h3>
                  <div className="flex items-center gap-2 text-xs text-gray-300 mt-2 bg-[#1f3a68]/60 px-3 py-1 rounded-full border border-[#1f3a68] shadow-lg">
                    <span className="w-1.5 h-1.5 rounded-full bg-[#ff8a2a] animate-pulse"></span>
                    Syncing Context...
                  </div>
                  {/* Flow Diagram */}
                  <div className="w-48 mt-5 relative h-8">
                    <div className="absolute top-1/2 left-0 w-full h-0.5 bg-white/10 -translate-y-1/2"></div>
                    <div className="absolute top-1/2 left-[20%] w-2 h-2 bg-gray-500 rounded-full -translate-y-1/2 -translate-x-1/2"></div>
                    <div className="absolute top-1/2 left-[50%] w-3 h-3 bg-[#1f3a68] border border-[#ff8a2a] rounded-full -translate-y-1/2 -translate-x-1/2 z-10 box-content"></div>
                    <div className="absolute top-1/2 left-[80%] w-2 h-2 bg-white rounded-full -translate-y-1/2 -translate-x-1/2 shadow-[0_0_8px_white]"></div>
                  </div>
                </div>

                {/* 2. Top Right: 업무 요청 (Ticket) */}
                <div className="glass-card absolute top-[12%] right-[2%] w-56 p-5 rounded-2xl animate-float-delayed z-20 border-[#ff8a2a]/20 hover:border-[#ff8a2a] transition-colors duration-300">
                  <div className="flex items-center justify-between mb-3">
                    <div className="w-10 h-10 rounded-lg bg-[#1f3a68] flex items-center justify-center text-[#ff8a2a] border border-[#ff8a2a]/20">
                      <span className="material-symbols-outlined">description</span>
                    </div>
                    <span className="text-[10px] font-bold bg-[#ff8a2a]/10 text-[#ff8a2a] px-2 py-1 rounded border border-[#ff8a2a]/20">NEW TICKET</span>
                  </div>
                  <div className="text-xs text-gray-400 mb-1 uppercase tracking-wide">Request detected</div>
                  <div className="font-bold text-white text-base leading-tight">"Bug fix in Login"</div>
                  <div className="mt-4 flex items-center gap-2 text-[11px] text-gray-400 bg-black/20 p-2 rounded-lg">
                    <span className="material-symbols-outlined text-sm text-[#ff8a2a]">arrow_right_alt</span>
                    <span>Auto-created Ticket #1042</span>
                  </div>
                </div>

                {/* 3. Bottom Left: 회의록 (Recap) */}
                <div className="glass-card absolute bottom-[15%] left-[0%] w-64 p-5 rounded-2xl animate-float z-20 border-blue-400/20">
                  <div className="flex items-center gap-3 mb-4">
                    <div className="p-1.5 bg-blue-500/20 rounded-md">
                      <span className="material-symbols-outlined text-blue-300 text-lg">record_voice_over</span>
                    </div>
                    <span className="text-sm font-semibold text-white">Meeting Recap</span>
                  </div>
                  {/* Transcribing Visual */}
                  <div className="space-y-3">
                    <div className="flex gap-1.5 items-center justify-center h-8 bg-black/20 rounded-lg px-2">
                      <div className="w-1 h-3 bg-[#ff8a2a] rounded-full animate-pulse"></div>
                      <div className="w-1 h-5 bg-[#ff8a2a] rounded-full animate-pulse" style={{animationDelay: '0.1s'}}></div>
                      <div className="w-1 h-8 bg-[#ff8a2a] rounded-full animate-pulse" style={{animationDelay: '0.2s'}}></div>
                      <div className="w-1 h-4 bg-[#ff8a2a] rounded-full animate-pulse" style={{animationDelay: '0.3s'}}></div>
                      <div className="w-1 h-6 bg-[#ff8a2a] rounded-full animate-pulse" style={{animationDelay: '0.1s'}}></div>
                      <div className="w-1 h-2 bg-[#ff8a2a] rounded-full animate-pulse"></div>
                    </div>
                    <div className="text-xs text-gray-300 flex items-center gap-2 bg-[#1f3a68]/30 p-2 rounded-lg border border-white/5">
                      <span className="material-symbols-outlined text-sm text-green-400">check_circle</span>
                      <span>Action Items Extracted</span>
                    </div>
                  </div>
                </div>

                {/* 4. Top Left: AI Assistant - Always Ready (Decorative, Non-Clickable) */}
                <div className="glass-card absolute top-[22%] left-[8%] w-auto px-5 py-3 rounded-2xl flex items-center gap-3 animate-float-slow z-0 border-[#1f3a68] shadow-[0_0_30px_rgba(31,58,104,0.3)] pointer-events-none">
                  <span className="material-symbols-outlined text-blue-200 text-2xl">mail</span>
                  <div className="text-left">
                    <div className="text-xs font-bold text-white">AI Assistant</div>
                    <div className="text-[10px] text-blue-200/70">Always Ready</div>
                  </div>
                </div>

                {/* 5. Bottom Right: 팀 협업 (Team Status) */}
                <div className="glass-card absolute bottom-[28%] right-[10%] w-auto pl-2 pr-5 py-2 rounded-full flex items-center gap-3 animate-float z-30 border-[#ff8a2a]/30 bg-[#05060a]/80">
                  <div className="relative">
                    <div className="w-10 h-10 rounded-full bg-gradient-to-br from-[#1f3a68] to-gray-900 flex items-center justify-center text-xs font-bold text-white border border-white/10">
                      <span className="material-symbols-outlined text-sm">groups</span>
                    </div>
                    <div className="absolute -top-0.5 -right-0.5 w-3.5 h-3.5 bg-[#05060a] rounded-full flex items-center justify-center">
                      <div className="w-2 h-2 bg-[#ff8a2a] rounded-full animate-ping absolute opacity-75"></div>
                      <div className="w-2 h-2 bg-[#ff8a2a] rounded-full relative"></div>
                    </div>
                  </div>
                  <div className="text-xs">
                    <div className="font-bold text-white">Team Channel</div>
                    <div className="text-blue-200/70">Active members</div>
                  </div>
                </div>

                {/* Orbitals */}
                <div className="absolute top-1/2 left-1/2 w-[500px] h-[500px] border border-[#1f3a68]/40 rounded-full -translate-x-1/2 -translate-y-1/2 z-[-1]" style={{transform: 'translate(-50%, -50%) rotateX(60deg)'}}></div>
                <div className="absolute top-1/2 left-1/2 w-[800px] h-[800px] border border-white/5 rounded-full -translate-x-1/2 -translate-y-1/2 z-[-1] animate-[spin_60s_linear_infinite]"></div>
              </div>
            </div>
          </div>
        </section>

        {/* Main Content Area */}
        <main className="flex-grow max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 lg:-mt-16 w-full space-y-10 relative z-20">
          {/* Stats Overview Cards - 모든 화면에서 가로 배치 */}
          <section className="grid grid-cols-3 gap-3 sm:gap-4 lg:gap-6">
            <StatusCard
              title="중요 업무 (PIN)"
              count={isLoggedIn ? pinItems.length : 0}
              color="text-brandNavy"
              icon="star"
              onClick={() => isLoggedIn && window.dispatchEvent(new Event('open-pin-drawer'))}
            />
            <StatusCard
              title="읽지 않은 업무"
              count={unreadCount}
              color="text-brandNavy"
              icon="mail"
              onClick={() => moveToListWithFilter('RECEIVED', 'UNREAD')}
              hasBadge={unreadCount > 0}
            />
            <StatusCard
              title="내가 읽지 않은 채팅"
              count={unreadChatCount}
              color="text-brandNavy"
              icon="chat"
              onClick={() => isLoggedIn ? navigate("/chat") : navigate("/member/login")}
              hasBadge={unreadChatCount > 0}
            />
          </section>

          {/* Two Column Layout */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
            {/* Left: Recent Tasks */}
            <div className="bg-white rounded-md shadow-sm border border-slate-200 flex flex-col h-full">
              <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-white rounded-t-2xl sticky top-0 z-10">
                <div className="flex items-center gap-3">
                  <div className="w-1.5 h-6 bg-brandNavy rounded-full"></div>
                  <h2 className="text-xl font-bold text-slate-800">최근 받은 업무</h2>
                </div>
                <button 
                  onClick={() => moveToListWithFilter('RECEIVED')}
                  className="text-xs font-semibold text-slate-500 hover:text-brandNavy transition-colors flex items-center bg-slate-50 hover:bg-blue-50 px-3 py-1.5 rounded-lg"
                >
                  전체보기 <span className="material-symbols-outlined text-sm ml-0.5">chevron_right</span>
                </button>
              </div>
              <div className="p-3 flex-1">
                {!isLoggedIn ? (
                  <div className="flex flex-col items-center justify-center h-full py-14">
                    <p className="text-slate-500 font-medium mb-4">로그인 후 이용 가능합니다.</p>
                  </div>
                ) : recentTasks.length > 0 ? (
                  recentTasks.map((task, index) => (
                    <div 
                      key={task.tno} 
                      onClick={() => openDetail(task.tno)} 
                      className="group relative flex items-center gap-5 p-5 hover:bg-slate-50 rounded-xl transition-all cursor-pointer border-b border-slate-50 last:border-0 mb-1"
                    >
                      <div className="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-12 bg-brandOrange rounded-r opacity-0 group-hover:opacity-100 transition-opacity"></div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2 mb-1.5">
                          <span className="text-base font-bold text-slate-800 truncate group-hover:text-brandNavy transition-colors">{task.title}</span>
                          {index === 0 && unreadCount > 0 && (
                            <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-orange-100 text-brandOrange border border-orange-200 uppercase tracking-wide">New</span>
                          )}
                        </div>
                        <div className="flex items-center gap-4 text-xs text-slate-500">
                          <span className="flex items-center gap-1.5">
                            <span className="material-symbols-outlined text-[16px] text-slate-400">schedule</span> 
                            {task.birth}
                          </span>
                          <span className="flex items-center gap-1.5">
                            <span className="material-symbols-outlined text-[16px] text-slate-400">account_circle</span> 
                            {task.writer}
                          </span>
                        </div>
                      </div>
                      <div className="shrink-0">{getGradeBadge(task.grade)}</div>
                    </div>
                  ))
                ) : (
                  <div className="p-4 flex flex-col items-center justify-center h-28 text-slate-300 border-2 border-dashed border-slate-100 rounded-md m-2 bg-slate-50/50">
                    <span className="material-symbols-outlined mb-1">check_circle</span>
                    <span className="text-xs font-medium">받은 업무가 없습니다</span>
                  </div>
                )}
              </div>
            </div>

            {/* Right: Notices */}
            <div className="bg-white rounded-md shadow-sm border border-slate-200 flex flex-col h-full">
              <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-white rounded-t-2xl sticky top-0 z-10">
                <div className="flex items-center gap-3">
                  <div className="w-1.5 h-6 bg-brandNavy rounded-full"></div>
                  <h2 className="text-xl font-bold text-slate-800">최근 공지</h2>
                </div>
                <button 
                  onClick={() => navigate("/board/list")}
                  className="text-xs font-semibold text-slate-500 hover:text-brandNavy transition-colors flex items-center bg-slate-50 hover:bg-blue-50 px-3 py-1.5 rounded-lg"
                >
                  전체보기 <span className="material-symbols-outlined text-sm ml-0.5">chevron_right</span>
                </button>
              </div>
              <div className="p-3 flex-1">
                {!isLoggedIn ? (
                  <div className="flex flex-col items-center justify-center h-full py-14">
                    <p className="text-slate-500 font-medium mb-4">로그인 후 이용 가능합니다.</p>
                  </div>
                ) : recentBoards.length > 0 ? (
                  recentBoards.map((board) => (
                    <div 
                      key={board.bno} 
                      onClick={() => navigate(`/board/read/${board.bno}`)} 
                      className="group flex items-start gap-4 p-5 hover:bg-slate-50 rounded-md transition-all cursor-pointer border-b border-slate-50 last:border-0 mb-1"
                    >
                      <div className={`mt-0.5 min-w-[40px] h-[40px] rounded-full flex items-center justify-center border group-hover:scale-110 transition-transform ${
                        board.category === '공지사항' ? 'bg-red-50 text-red-500 border-red-100' :
                        board.category === '가이드' ? 'bg-blue-50 text-blue-600 border-blue-100' :
                        board.category === 'FAQ' ? 'bg-green-50 text-green-600 border-green-100' :
                        'bg-slate-50 text-slate-600 border-slate-100'
                      }`}>
                        <span className="material-symbols-outlined text-[20px]">
                          {board.category === '공지사항' ? 'campaign' : 
                           board.category === '가이드' ? 'menu_book' :
                           board.category === 'FAQ' ? 'help' : 'info'}
                        </span>
                      </div>
                      <div className="flex-1">
                        <div className="flex justify-between items-start mb-1">
                          <h3 className="text-sm font-bold text-slate-700 group-hover:text-brandNavy transition-colors leading-snug">{board.title}</h3>
                          <span className={`ml-2 px-2 py-0.5 rounded-full text-[10px] font-bold whitespace-nowrap ${
                            board.category === '공지사항' ? 'bg-red-100 text-red-600' :
                            board.category === '가이드' ? 'bg-blue-100 text-blue-600' :
                            board.category === 'FAQ' ? 'bg-green-100 text-green-700' :
                            'bg-slate-100 text-slate-500'
                          }`}>
                            {board.category || "일반"}
                          </span>
                        </div>
                        <p className="text-xs text-slate-500 mt-1 line-clamp-1 leading-relaxed">{board.content || ''}</p>
                        <span className="text-[11px] text-slate-400 mt-2 block font-medium">{board.regDate}</span>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="p-4 flex flex-col items-center justify-center h-28 text-slate-300 border-2 border-dashed border-slate-100 rounded-xl m-2 bg-slate-50/50">
                    <span className="material-symbols-outlined mb-1">check_circle</span>
                    <span className="text-xs font-medium">새로운 공지사항이 없습니다</span>
                  </div>
                )}
              </div>
            </div>
          </div>
        </main>
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

const StatusCard = ({ title, count, color, icon, onClick, hasBadge }) => (
  <div 
    onClick={onClick} 
    className="bg-white rounded-md p-3 sm:p-4 lg:p-6 shadow-lg border border-slate-100 hover:shadow-xl hover:-translate-y-1 transition-all duration-300 group cursor-pointer relative overflow-hidden"
  >
    <div className="flex justify-between items-start mb-2 sm:mb-3 lg:mb-4">
      <div className="flex-1 min-w-0">
        <p className="text-[10px] sm:text-xs lg:text-sm font-medium text-slate-500 mb-1 truncate">{title}</p>
        {hasBadge ? (
          <div className="flex items-center gap-1 sm:gap-2">
            <h3 className="text-2xl sm:text-3xl lg:text-4xl font-bold text-slate-800 tracking-tight">{count}</h3>
            <span className="absolute top-2 sm:top-3 lg:top-5 right-2 sm:right-3 lg:right-5 flex h-2 w-2 sm:h-2.5 sm:w-2.5 lg:h-3 lg:w-3">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-brandOrange opacity-75"></span>
              <span className="relative inline-flex rounded-full h-full w-full bg-brandOrange"></span>
            </span>
          </div>
        ) : (
          <h3 className="text-2xl sm:text-3xl lg:text-4xl font-bold text-slate-800 tracking-tight">{count}</h3>
        )}
      </div>
      <div className={`p-1.5 sm:p-2 lg:p-3 rounded-lg sm:rounded-xl group-hover:bg-brandNavy group-hover:text-white transition-colors shrink-0 ${
        icon === 'star' ? 'bg-blue-50 text-brandNavy' :
        icon === 'mail' ? 'bg-orange-50 text-brandOrange' :
        icon === 'chat' ? 'bg-purple-50 text-purple-600' :
        'bg-slate-50 text-slate-600'
      }`}>
        <span className="material-symbols-outlined text-base sm:text-xl lg:text-2xl">{icon}</span>
      </div>
    </div>
    {hasBadge && icon === 'mail' && (
      <p className="text-[9px] sm:text-[10px] lg:text-xs text-slate-400 font-medium hidden sm:block">최근 1시간 내 수신</p>
    )}
    {hasBadge && icon === 'chat' && (
      <p className="text-[9px] sm:text-[10px] lg:text-xs text-slate-400 font-medium hidden sm:block">읽지 않은 메시지</p>
    )}
    {!hasBadge && icon === 'star' && (
      <p className="text-[9px] sm:text-[10px] lg:text-xs text-slate-400 font-medium hidden sm:block">마감 기한 임박 업무 포함</p>
    )}
  </div>
);

export default MainPage;