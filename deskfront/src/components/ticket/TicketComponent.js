import React from 'react';
import PageComponent from '../common/PageComponent';
import useCustomPin from '../../hooks/useCustomPin'; // 커스텀 훅 임포트
import { getGradeBadge, getStateLabel, formatDate } from "../../util/ticketUtils";

const TicketComponent = ({ ticketList, serverData, movePage, onRowClick}) => {

  // 찜 기능에 필요한 상태와 함수 가져오기
    const { togglePin, isPinned } = useCustomPin();

    if (!ticketList || ticketList.length === 0) {
        return <div className="text-center py-20 bg-white rounded-2xl border-2 border-dashed text-gray-400">조회된 티켓 데이터가 없습니다.</div>;
    }

    return (
        <div className="bg-white shadow-2xl rounded-2xl overflow-hidden border">
            <table className="w-full text-left border-collapse">
                <thead className="bg-gray-800 text-white text-xs">
                    <tr>
                        <th className="p-4 w-12 text-center">📌</th> {/* 찜 컬럼 헤더 추가 */}
                        <th className="p-4">중요도</th>
                        <th className="p-4">제목</th>
                        <th className="p-4">요청자</th>
                        <th className="p-4">받은 사람</th>
                        <th className="p-4 text-center">마감일</th>
                        <th className="p-4 text-center">진행도</th>
                    </tr>
                </thead>
                <tbody className="text-sm divide-y">
                    {ticketList.map((ticket) => {
                        const receiverInfo = ticket.personals && ticket.personals.length > 0
                            ? ticket.personals[0].receiver
                            : ticket.receiver || '미지정';

                        const stateInfo = ticket.personals && ticket.personals.length > 0
                            ? ticket.personals[0].state
                            : ticket.state || 'NEW';

                       // 현재 티켓이 찜 상태인지 확인
                       const pinned = isPinned(ticket.tno);

                       return (
                           <tr
                                key={ticket.tno || ticket.pno}
                                className="hover:bg-gray-50 transition-colors"
                                onClick={() => onRowClick?.(ticket.tno)}
                                >
                               {/*  찜 버튼 셀 추가 */}
                               <td className="p-4 text-center">
                                    <button
                                      onClick={(e) => {
                                        e.stopPropagation(); // 이벤트 버블링 방지
                                        togglePin(ticket.tno);
                                      }}
                                      className={`text-xl transition-all hover:scale-125 ${pinned ? 'text-yellow-500' : 'text-gray-300 hover:text-yellow-200'}`}
                                    >
                                      {pinned ? '★' : '☆'}
                                    </button>
                               </td>
                                <td className="p-4">{getGradeBadge(ticket.grade)}</td>
                                <td className="p-4 font-bold text-gray-800">{ticket.title}</td>
                                <td className="p-4 text-gray-500">{ticket.writer}</td>
                                <td className="p-4 text-gray-500">{receiverInfo}</td>
                                <td className="p-4 text-center font-mono text-red-500 font-semibold">
                                    {ticket.deadline ? formatDate(ticket.deadline): '기한없음'}
                                </td>
                                <td className="p-4 text-center">{getStateLabel(stateInfo)}</td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>

            {/* 공통 페이징 컴포넌트 사용 */}
            <div className="p-4 bg-white border-t">
                <PageComponent serverData={serverData} movePage={movePage} />
            </div>
        </div>
    );
};

export default TicketComponent;