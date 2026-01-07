import React, { useState, useEffect, useRef } from 'react';
import { useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import useCustomPin from '../hooks/useCustomPin';
import { getGradeBadge } from "../util/ticketUtils";
import TicketDetailModal from './ticket/TicketDetailModal';

const PinDrawer = () => {
    const [isOpen, setIsOpen] = useState(false);
    const [selectedTno, setSelectedTno] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    // header 높이 (h-16 = 64px)
    const HEADER_HEIGHT = 64;
    
    const [topPosition, setTopPosition] = useState(() => {
        // localStorage에서 저장된 위치 불러오기
        const saved = localStorage.getItem('pinButtonTop');
        const initialTop = saved ? parseInt(saved, 10) : 100;
        // header 아래에 위치하도록 최소값 설정
        return Math.max(HEADER_HEIGHT, initialTop);
    });
    const [isDragging, setIsDragging] = useState(false);
    const dragStartY = useRef(0);
    const dragStartTop = useRef(0);
    const buttonRef = useRef(null);
    const hasDragged = useRef(false);
    const currentTopRef = useRef(topPosition);

    // topPosition이 변경될 때 ref도 업데이트
    useEffect(() => {
        currentTopRef.current = topPosition;
    }, [topPosition]);

    const { pinItems, refreshPins, togglePin } = useCustomPin();
    const loginState = useSelector((state) => state.loginSlice);
    const navigate = useNavigate();

    useEffect(() => {
        // 메인 페이지 등 외부에서 사이드바를 열기 위한 이벤트 리스너 등록
        const handleOpenDrawer = () => setIsOpen(true);
        window.addEventListener('open-pin-drawer', handleOpenDrawer);

        if (!loginState.email && isOpen) {
            alert("로그인이 필요한 서비스입니다.");
            setIsOpen(false);
            navigate("/member/login");
        }
        if (loginState.email) {
            refreshPins();
        }

        return () => window.removeEventListener('open-pin-drawer', handleOpenDrawer);
    }, [loginState.email, refreshPins, isOpen, navigate]);

    // 드래그 핸들러
    const handleMouseDown = (e) => {
        e.preventDefault();
        setIsDragging(true);
        hasDragged.current = false;
        dragStartY.current = e.clientY;
        dragStartTop.current = topPosition;
    };

    useEffect(() => {
        if (!isDragging) return;

        const handleMouseMove = (e) => {
            const deltaY = e.clientY - dragStartY.current;
            
            // 5px 이상 이동했으면 드래그로 간주
            if (Math.abs(deltaY) > 5) {
                hasDragged.current = true;
            }
            
            let newTop = dragStartTop.current + deltaY;

            // 화면 경계 체크 (최소 header 높이, 최대 화면 높이 - 버튼 높이)
            const buttonHeight = buttonRef.current?.offsetHeight || 50;
            const maxTop = window.innerHeight - buttonHeight;
            newTop = Math.max(HEADER_HEIGHT, Math.min(newTop, maxTop));

            currentTopRef.current = newTop;
            setTopPosition(newTop);
        };

        const handleMouseUp = () => {
            const wasDragging = hasDragged.current;
            setIsDragging(false);
            
            // 드래그가 있었던 경우에만 위치 저장
            if (wasDragging) {
                localStorage.setItem('pinButtonTop', currentTopRef.current.toString());
            }
            
            // 다음 드래그를 위해 초기화
            setTimeout(() => {
                hasDragged.current = false;
            }, 0);
        };

        window.addEventListener('mousemove', handleMouseMove);
        window.addEventListener('mouseup', handleMouseUp);

        return () => {
            window.removeEventListener('mousemove', handleMouseMove);
            window.removeEventListener('mouseup', handleMouseUp);
        };
    }, [isDragging, topPosition]);

    if (!loginState.email) return null;

    const openTicketModal = (tno) => {
        if (!tno) return;
        setSelectedTno(tno);
        setIsModalOpen(true);
    };

    const closeTicketModal = () => {
        setIsModalOpen(false);
        setSelectedTno(null);
    };

    const handleDeleted = () => {
        refreshPins();
    };

    return (
        <>
            {/* 찜 버튼 */}
            <div 
                ref={buttonRef}
                className="fixed right-0 z-[40]"
                style={{ top: `${topPosition}px` }}
            >
                <button
                    onMouseDown={handleMouseDown}
                    onClick={(e) => {
                        // 드래그가 없었던 경우에만 클릭 처리
                        if (!hasDragged.current) {
                            setIsOpen(true);
                        }
                    }}
                    className={`bg-brandNavy text-white p-3 rounded-l-ui shadow-lg hover:opacity-90 transition-all flex items-center gap-2 ${isDragging ? 'cursor-grabbing' : 'cursor-grab'}`}
                >
                    <span className="font-semibold text-sm">PIN</span>
                    {pinItems.length > 0 && (
                        <span className="ui-badge-notify text-xs px-2 py-0.5">
                            {pinItems.length}
                        </span>
                    )}
                </button>
            </div>

            {/* 슬라이드 드로어 */}
            <div className={`fixed inset-y-0 right-0 w-80 lg:w-96 bg-baseBg shadow-lg z-[100] transform transition-transform duration-300 ease-in-out ${isOpen ? 'translate-x-0' : 'translate-x-full'} border-l border-baseBorder`}>
                <div className="ui-modal-header">
                    <h2 className="ui-title">고정된 업무</h2>
                    <button onClick={() => setIsOpen(false)} className="text-xl font-bold text-baseMuted hover:text-baseText transition-colors">&times;</button>
                </div>

                <div className="p-4 lg:p-6 overflow-y-auto h-[calc(100%-80px)] bg-baseSurface">
                    {pinItems.length > 0 ? (
                        pinItems.map(item => (
                            <div
                                key={item.tno}
                                onClick={() => openTicketModal(item.tno)}
                                className="mb-3 p-4 ui-card hover:shadow-md transition-all group relative overflow-hidden cursor-pointer"
                            >
                                <div className="absolute left-0 top-0 h-full w-1 bg-brandNavy"></div>
                                <div className="flex justify-between items-start">
                                    <div className="truncate pr-4">
                                        <div className="mb-1">{getGradeBadge ? getGradeBadge(item.grade) : <span className="text-xs font-semibold text-brandNavy">{item.grade}</span>}</div>
                                        <div className="text-sm font-semibold text-baseText truncate">{item.title}</div>
                                    </div>
                                    <button
                                        onClick={(e) => {
                                          e.stopPropagation();
                                          togglePin(item.tno);
                                        }}
                                        className="bg-red-50 text-red-600 p-1.5 rounded-ui hover:bg-red-600 hover:text-white transition-all"
                                    >
                                        <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M6 18L18 6M6 6l12 12" />
                                        </svg>
                                    </button>
                                </div>
                            </div>
                        ))
                    ) : (
                        <div className="h-full flex flex-col items-center justify-center text-baseMuted">
                            <span className="text-2xl mb-2">📌</span>
                            <p className="text-sm">고정된 항목이 없습니다</p>
                        </div>
                    )}
                </div>
            </div>

            {isOpen && <div className="fixed inset-0 bg-black/40 backdrop-blur-sm z-[90] transition-opacity" onClick={() => setIsOpen(false)} />}

            {isModalOpen && selectedTno && (
                <div className="fixed inset-0 z-[110]">
                    <TicketDetailModal
                        tno={selectedTno}
                        onClose={closeTicketModal}
                        onDelete={handleDeleted}
                    />
                </div>
            )}
        </>
    );
};

export default PinDrawer;