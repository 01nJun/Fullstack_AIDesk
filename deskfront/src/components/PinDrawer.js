import React, { useState, useEffect } from 'react';
import useCustomPin from '../hooks/useCustomPin';
import { getGradeBadge} from "../util/ticketUtils";
import TicketDetailModal from './ticket/TicketDetailModal';

const PinDrawer = () => {
    const [isOpen, setIsOpen] = useState(false);
    const [selectedTno, setSelectedTno] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const { pinItems, refreshPins, togglePin } = useCustomPin();

    useEffect(() => {
        refreshPins();
    }, [refreshPins]);

    // 아이템 클릭 시 모달 열기
    const openTicketModal = (tno) => {
        if (!tno) return;
        setSelectedTno(tno);
        setIsModalOpen(true);
    };

    // 모달 닫기
    const closeTicketModal = () => {
        setIsModalOpen(false);
        setSelectedTno(null);
    };

    // 티켓 삭제 후 핸들러 (모달에서 삭제 시 호출)
    const handleDeleted = () => {
        refreshPins();
    };

    return (
        <>
            {/* 찜 버튼: 헤더 바로 아래 고정 (헤더 높이 64px 기준) */}
            <div className="fixed top-[64px] right-0 z-[40]">
                <button onClick={() => setIsOpen(true)} className="bg-blue-600 text-white p-3 rounded-l-lg shadow-xl hover:bg-blue-700 transition-all flex items-center gap-2">
                    <span className="font-bold">PIN</span>
                    <span className="bg-white text-blue-600 text-xs px-2 py-0.5 rounded-full font-black">{pinItems.length}</span>
                </button>
            </div>

            {/* 슬라이드 드로어 */}
            <div className={`fixed inset-y-0 right-0 w-80 bg-white shadow-2xl z-[100] transform transition-transform duration-300 ${isOpen ? 'translate-x-0' : 'translate-x-full'}`}>
                <div className="p-4 border-b flex justify-between items-center bg-gray-50">
                    <h2 className="text-lg font-bold">찜한 티켓 목록</h2>
                    <button onClick={() => setIsOpen(false)} className="text-2xl">&times;</button>
                </div>
                <div className="p-4 overflow-y-auto h-full">
                    {pinItems.map(item => (
                        <div
                            key={item.tno}
                            className="mb-3 p-3 border rounded-lg flex justify-between items-center group cursor-pointer hover:bg-gray-50"
                            onClick={() => openTicketModal(item.tno)}
                        >
                            <div className="truncate w-40">
                                {getGradeBadge(item.grade)}
                                <div className="text-sm font-medium truncate">{item.title}</div>
                            </div>
                            <button
                                onClick={(e) => {
                                    e.stopPropagation(); // 이벤트 버블링 방지
                                    togglePin(item.tno);
                                }}
                                className="text-xs text-red-400 hover:underline"
                            >
                                삭제
                            </button>
                        </div>
                    ))}
                </div>
            </div>
            {isOpen && <div className="fixed inset-0 bg-black/20 z-[90]" onClick={() => setIsOpen(false)} />}

            {/* 티켓 상세 모달 */}
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