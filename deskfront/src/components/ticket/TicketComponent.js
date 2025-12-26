import React, { useEffect, useState, useCallback } from 'react';
import { useSelector } from 'react-redux';
import { getSentTickets, getReceivedTickets, getAllTickets } from '../../api/ticketApi';
import PageComponent from '../common/PageComponent';
import useCustomPin from '../../hooks/useCustomPin';
import { getGradeBadge, getStateLabel, formatDate } from '../../util/ticketUtils';
import TicketDetailModal from './TicketDetailModal';

const TicketComponent = () => {
  const loginState = useSelector((state) => state.loginSlice);
  const currentUserEmail = loginState.email;

  const [tab, setTab] = useState('ALL');
  const [serverData, setServerData] = useState({ dtoList: [], totalCount: 0 });
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);

  const [inputKeyword, setInputKeyword] = useState("");
  const [inputGrade, setInputGrade] = useState("");
  const [inputSort, setInputSort] = useState("tno,desc");
  const [activeFilter, setActiveFilter] = useState({ keyword: "", grade: "", sort: "tno,desc" });

  const [selectedTno, setSelectedTno] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const { togglePin, isPinned } = useCustomPin();

  const fetchData = useCallback(async () => {
    if (!currentUserEmail) return;
    setLoading(true);
    const pageParam = { page, size: 10, sort: activeFilter.sort };
    const filterParam = { keyword: activeFilter.keyword, grade: activeFilter.grade === "" ? null : activeFilter.grade };

    try {
      let result;
      if (tab === 'SENT') result = await getSentTickets(currentUserEmail, pageParam, filterParam);
      else if (tab === 'RECEIVED') result = await getReceivedTickets(currentUserEmail, pageParam, filterParam);
      else result = await getAllTickets(currentUserEmail, pageParam, filterParam);
      setServerData(result || { dtoList: [], totalCount: 0 });
    } catch (error) {
      console.error("데이터 로드 실패:", error);
    } finally {
      setLoading(false);
    }
  }, [tab, page, activeFilter, currentUserEmail]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const handleSearch = () => {
    setPage(1);
    setActiveFilter({ keyword: inputKeyword, grade: inputGrade, sort: inputSort });
  };

  const handleTabChange = (newTab) => {
    setTab(newTab);
    setPage(1);
    setInputKeyword("");
    setInputGrade("");
    setInputSort("tno,desc");
    setActiveFilter({ keyword: "", grade: "", sort: "tno,desc" });
  };

    // 모달 열기
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

    // 티켓 삭제 후
    const handleDeleted = () => {
    fetchData(); // 데이터 새로고침
    };

  return (
    <div className="w-full">
      <h1 className="text-4xl font-black mb-10 text-gray-900 border-b-8 border-blue-500 pb-4 inline-block tracking-tighter">
        티켓 목록
      </h1>

      <div className="flex flex-col xl:flex-row justify-between items-stretch xl:items-center mb-8 gap-6 bg-white p-6 rounded-3xl shadow-xl border border-gray-100">
        <div className="flex bg-gray-100 p-2 rounded-2xl shadow-inner">
          {['ALL', 'RECEIVED', 'SENT'].map((t) => (
            <button
              key={t}
              onClick={() => handleTabChange(t)}
              className={`px-6 py-3 rounded-xl font-black text-sm transition-all ${
                tab === t ? "bg-white text-blue-600 shadow-md" : "text-gray-400 hover:text-gray-600"
              }`}
            >
              {t === 'ALL' ? '전체 티켓' : t === 'RECEIVED' ? '받은 티켓' : '보낸 티켓'}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-3 flex-grow">
          <select
            value={inputGrade}
            onChange={(e) => setInputGrade(e.target.value)}
            className="border-2 border-gray-200 p-3 rounded-2xl bg-white font-bold focus:border-blue-500 outline-none w-32 shadow-sm"
          >
            <option value="">중요도</option>
            <option value="URGENT">🚨 긴급</option>
            <option value="HIGH">HIGH</option>
            <option value="MIDDLE">MIDDLE</option>
            <option value="LOW">LOW</option>
          </select>

          <select
            value={inputSort}
            onChange={(e) => setInputSort(e.target.value)}
            className="border-2 border-gray-200 p-3 rounded-2xl bg-white font-bold focus:border-blue-500 outline-none w-44 shadow-sm"
          >
            <option value="tno,desc">최신 등록순</option>
            <option value="deadline,asc">마감 빠른순</option>
            <option value="deadline,desc">마감 느린순</option>
          </select>

          <div className="relative flex-grow">
            <input
              type="text"
              placeholder="제목, 본문 또는 요청자를 입력하세요..."
              value={inputKeyword}
              onChange={(e) => setInputKeyword(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
              className="w-full border-2 border-gray-200 p-3 pl-6 rounded-2xl font-bold focus:border-blue-500 focus:ring-4 focus:ring-blue-100 outline-none transition-all shadow-inner"
            />
          </div>

          <button
            onClick={handleSearch}
            className="bg-gray-900 text-white px-8 py-3 rounded-2xl font-black hover:bg-blue-600 transition-all shadow-lg"
          >
            검색
          </button>
        </div>
      </div>

      <div className="bg-white rounded-3xl shadow-2xl overflow-hidden border border-gray-100 min-h-[600px] flex flex-col">
        <div className="p-6 bg-gray-900 text-white flex justify-between items-center">
          <h2 className="text-xl font-black italic uppercase tracking-wider">Ticket List</h2>
          <span className="bg-blue-500 px-6 py-1 rounded-full text-sm font-black italic">
            TOTAL: {serverData?.totalCount || 0}
          </span>
        </div>

        <div className="overflow-x-auto flex-grow">
          <table className="w-full table-fixed">
            <thead>
              <tr className="bg-gray-50 border-b-2 border-gray-100">
                <th className="p-5 w-16 text-center text-xs font-black text-gray-400 uppercase tracking-widest">Pin</th>
                <th className="p-5 w-32 text-left text-xs font-black text-gray-400 uppercase tracking-widest">Grade</th>
                <th className="p-5 text-left text-xs font-black text-gray-400 uppercase tracking-widest">Subject</th>
                <th className="p-5 w-32 text-left text-xs font-black text-gray-400 uppercase tracking-widest">Sender</th>
                <th className="p-5 w-32 text-left text-xs font-black text-gray-400 uppercase tracking-widest">Receiver</th>
                <th className="p-5 w-32 text-center text-xs font-black text-gray-400 uppercase tracking-widest">Deadline</th>
                <th className="p-5 w-28 text-center text-xs font-black text-gray-400 uppercase tracking-widest">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {loading ? (
                <tr><td colSpan="7" className="p-40 text-center font-black text-gray-300 animate-pulse uppercase">Loading Tickets...</td></tr>
              ) : serverData.dtoList?.length > 0 ? (
                serverData.dtoList.map((ticket) => {
                  const receiverInfo = ticket.personals?.length > 0 ? ticket.personals[0].receiver : ticket.receiver || '미지정';
                  const stateInfo = ticket.personals?.length > 0 ? ticket.personals[0].state : ticket.state || 'NEW';
                  return (
                    <tr 
                      key={ticket.tno || ticket.pno} 
                      className="hover:bg-blue-50/30 transition-all h-[65px] cursor-pointer"
                      onClick={() => openTicketModal(ticket.tno)}
                    >
                      <td className="p-4 text-center" onClick={(e) => e.stopPropagation()}>
                        <button onClick={() => togglePin(ticket.tno)} className={`text-2xl transition-all hover:scale-125 ${isPinned(ticket.tno) ? 'text-yellow-500' : 'text-gray-200'}`}>
                          {isPinned(ticket.tno) ? '★' : '☆'}
                        </button>
                      </td>
                      <td className="p-4 truncate">{getGradeBadge(ticket.grade)}</td>
                      <td className="p-4 font-bold text-gray-800 truncate">{ticket.title}</td>
                      <td className="p-4 text-gray-500 font-medium truncate">{ticket.writer}</td>
                      <td className="p-4 text-gray-500 font-medium truncate">{receiverInfo}</td>
                      <td className="p-4 text-center font-black text-red-500 tracking-tighter truncate">
                        {ticket.deadline ? formatDate(ticket.deadline) : '-'}
                      </td>
                      <td className="p-4 text-center">{getStateLabel(stateInfo)}</td>
                    </tr>
                  );
                })
              ) : (
                <tr><td colSpan="7" className="p-40 text-center text-gray-300 font-black text-2xl uppercase italic">No Data Found</td></tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="p-8 bg-gray-50 flex justify-center border-t border-gray-100 mt-auto">
          {serverData?.dtoList?.length > 0 && <PageComponent serverData={serverData} movePage={(p) => setPage(p.page)} />}
        </div>
      </div>

        {/* 티켓 상세 모달 */}
            {isModalOpen && selectedTno && (
            <TicketDetailModal
                tno={selectedTno}
                onClose={closeTicketModal}
                onDelete={handleDeleted}
            />
        )}
    </div>
  );
};

export default TicketComponent;