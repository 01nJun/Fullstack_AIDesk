import React, { useState, useEffect } from 'react';
import { useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import useCustomPin from '../hooks/useCustomPin';
import { getGradeBadge } from "../util/ticketUtils";
import TicketDetailModal from './ticket/TicketDetailModal';

const PinDrawer = () => {
    const [isOpen, setIsOpen] = useState(false);
    const [selectedTno, setSelectedTno] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);

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
            <div className="fixed top-[100px] right-0 z-[40]">
                <button
                    onClick={() => setIsOpen(true)}
                    className="bg-gray-900 text-white p-4 rounded-l-3xl shadow-2xl hover:bg-blue-600 transition-all flex items-center gap-3 border-y-2 border-l-2 border-blue-500"
                >
                    <span className="font-black italic tracking-tighter">PIN</span>
                    <span className="bg-blue-500 text-white text-xs px-2.5 py-1 rounded-full font-black shadow-lg">
                        {pinItems.length}
                    </span>
                </button>
            </div>

            {/* 슬라이드 드로어 */}
            <div className={`fixed inset-y-0 right-0 w-96 bg-white shadow-[-10px_0_30px_rgba(0,0,0,0.1)] z-[100] transform transition-transform duration-500 ease-in-out ${isOpen ? 'translate-x-0' : 'translate-x-full'} border-l-4 border-gray-900`}>
                <div className="p-6 bg-gray-900 text-white flex justify-between items-center">
                    <h2 className="text-xl font-black italic uppercase tracking-widest">Pinned Works</h2>
                    <button onClick={() => setIsOpen(false)} className="text-3xl font-black hover:text-blue-400 transition-colors">&times;</button>
                </div>

                <div className="p-6 overflow-y-auto h-[calc(100%-80px)] bg-gray-50">
                    {pinItems.length > 0 ? (
                        pinItems.map(item => (
                            <div
                                key={item.tno}
                                onClick={() => openTicketModal(item.tno)}
                                className="mb-4 p-4 bg-white border-2 border-gray-100 rounded-2xl shadow-sm hover:shadow-md hover:border-blue-200 transition-all group relative overflow-hidden cursor-pointer"
                            >
                                <div className="absolute left-0 top-0 h-full w-2 bg-blue-500"></div>
                                <div className="flex justify-between items-start">
                                    <div className="truncate pr-4">
                                        <div className="mb-1">{getGradeBadge ? getGradeBadge(item.grade) : <span className="text-[10px] font-black text-blue-500">{item.grade}</span>}</div>
                                        <div className="text-sm font-black text-gray-800 truncate">{item.title}</div>
                                    </div>
                                    <button
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            togglePin(item.tno);
                                        }}
                                        className="bg-red-50 text-red-500 p-2 rounded-xl hover:bg-red-500 hover:text-white transition-all shadow-sm"
                                    >
                                        <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M6 18L18 6M6 6l12 12" />
                                        </svg>
                                    </button>
                                </div>
                            </div>
                        ))
                    ) : (
                        <div className="h-full flex flex-col items-center justify-center text-gray-300 italic font-black">
                            <span className="text-4xl mb-4">EMPTY</span>
                            <p>No Pinned Items</p>
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