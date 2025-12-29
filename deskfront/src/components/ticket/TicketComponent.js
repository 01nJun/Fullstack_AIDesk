import React, { useEffect, useState, useCallback } from 'react';
import { useSelector } from 'react-redux';
import { useSearchParams } from 'react-router-dom';
import { getSentTickets, getReceivedTickets, getAllTickets } from '../../api/ticketApi';
import PageComponent from '../common/PageComponent';
import useCustomPin from '../../hooks/useCustomPin';
import { getGradeBadge, getStateLabel, formatDate } from '../../util/ticketUtils';
import TicketDetailModal from './TicketDetailModal';

const TicketComponent = () => {
  const loginState = useSelector((state) => state.loginSlice);
  const currentUserEmail = loginState.email;
  const [searchParams, setSearchParams] = useSearchParams();

  // 1. URL 파라미터에서 초기값 추출
  const initialTab = searchParams.get('tab') || 'ALL';
  const initialRead = searchParams.get('read') || 'ALL';
  const initialState = searchParams.get('state') || '';
  const initialKeyword = searchParams.get('keyword') || '';

  const [tab, setTab] = useState(initialTab);
  const [serverData, setServerData] = useState({ dtoList: [], totalCount: 0 });
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);

  // 검색바 UI용 상태
  const [inputKeyword, setInputKeyword] = useState(initialKeyword);
  const [inputRead, setInputRead] = useState(initialRead);

  // 실제 API 호출에 사용되는 필터 상태
  const [activeFilter, setActiveFilter] = useState({
    keyword: initialKeyword,
    read: initialRead,
    grade: "",
    state: initialState,
    sort: "tno,desc"
  });

  // URL 파라미터가 변경될 때 컴포넌트 상태를 동기화 (MainPage에서 이동해올 때 작동)
  useEffect(() => {
    const t = searchParams.get('tab') || 'ALL';
    const r = searchParams.get('read') || 'ALL';
    const s = searchParams.get('state') || '';
    const k = searchParams.get('keyword') || '';

    setTab(t);
    setInputRead(r);
    setInputKeyword(k);
    setActiveFilter(prev => ({
      ...prev,
      read: r,
      state: s,
      keyword: k
    }));
    setPage(1);
  }, [searchParams]);

  const [openDropdown, setOpenDropdown] = useState(null);
  const [selectedTno, setSelectedTno] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const { togglePin, isPinned } = useCustomPin();

  const fetchData = useCallback(async () => {
    if (!currentUserEmail) return;
    setLoading(true);

    const pageParam = { page, size: 10, sort: activeFilter.sort };
    const filterParam = {
      keyword: activeFilter.keyword || null,
      grade: activeFilter.grade || null,
      read: activeFilter.read === "UNREAD" ? false : (activeFilter.read === "READ" ? true : null),
      state: activeFilter.state || null
    };

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
    setActiveFilter(prev => ({ ...prev, keyword: inputKeyword, read: inputRead }));
    // URL도 현재 필터 상태로 업데이트 (선택 사항)
    setSearchParams({ tab, read: inputRead, keyword: inputKeyword, state: activeFilter.state });
  };

  const handleColumnFilter = (key, val) => {
    setPage(1);
    setActiveFilter(prev => ({ ...prev, [key]: val }));
    setOpenDropdown(null);
  };

  const handleTabChange = (newTab) => {
    setTab(newTab);
    setPage(1);
    setInputKeyword("");
    setInputRead("ALL");
    setActiveFilter({ keyword: "", read: "ALL", grade: "", state: "", sort: "tno,desc" });
    setSearchParams({ tab: newTab }); // URL 초기화
  };

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
    fetchData();
  };

  const getWorkListTitle = () => {
    if (tab === 'RECEIVED') return "RECEIVED WORK LIST";
    if (tab === 'SENT') return "SENT WORK LIST";
    return "ALL WORK LIST";
  };

  const HeaderFilterDropdown = ({ type, options, currentVal, alignRight = false }) => (
    <div className={`absolute top-full ${alignRight ? 'right-0' : 'left-0'} mt-2 w-44 bg-white rounded-2xl shadow-2xl border border-gray-100 py-2 z-50`}>
      {options.map(opt => (
        <div
          key={opt.val}
          className={`px-5 py-3 hover:bg-blue-50 cursor-pointer text-xs font-bold transition-colors ${currentVal === opt.val ? 'text-blue-600 bg-blue-50/50' : 'text-gray-600'}`}
          onClick={(e) => { e.stopPropagation(); handleColumnFilter(type, opt.val); }}
        >
          {opt.label}
        </div>
      ))}
    </div>
  );

  return (
    <div className="w-full">
      <h1 className="text-4xl font-extrabold mb-10 text-gray-900 border-b-8 border-blue-500 pb-4 inline-block tracking-normal">
        업무 현황
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
              {t === 'ALL' ? '전체 업무' : t === 'RECEIVED' ? '받은 업무' : '보낸 업무'}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-3 flex-grow">
          <select
            value={inputRead}
            onChange={(e) => setInputRead(e.target.value)}
            className="border-2 border-gray-200 p-3 rounded-2xl bg-white font-bold focus:border-blue-500 outline-none w-36 shadow-sm"
          >
            <option value="ALL">전체 상태</option>
            <option value="READ">읽음</option>
            <option value="UNREAD">안읽음</option>
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

          <button onClick={handleSearch} className="bg-gray-900 text-white px-8 py-3 rounded-2xl font-black hover:bg-blue-600 transition-all shadow-lg">
            검색
          </button>
        </div>
      </div>

      <div className="bg-white rounded-3xl shadow-2xl overflow-hidden border border-gray-100 min-h-[600px] flex flex-col">
        <div className="p-6 bg-gray-900 text-white flex justify-between items-center">
          <h2 className="text-xl font-black italic uppercase tracking-wider">
            {getWorkListTitle()}
          </h2>
          <span className="bg-blue-500 px-6 py-1 rounded-full text-sm font-black italic">
            TOTAL: {serverData?.totalCount || 0}
          </span>
        </div>

        <div className="overflow-x-auto flex-grow">
          <table className="w-full table-fixed">
            <thead>
              <tr className="bg-gray-50 border-b-2 border-gray-100">
                <th className="p-5 w-16 text-center text-xs font-black text-gray-400 uppercase tracking-widest">Pin</th>
                <th className="p-5 w-36 text-left relative">
                  <div className="flex items-center cursor-pointer text-xs font-black text-gray-400 uppercase tracking-widest hover:text-blue-500 transition-colors"
                       onClick={() => setOpenDropdown(openDropdown === 'grade' ? null : 'grade')}>
                    Grade <span className="ml-2 text-[10px]">▼</span>
                  </div>
                  {openDropdown === 'grade' && (
                    <HeaderFilterDropdown type="grade" options={[{label:'ALL', val:''}, {label:'URGENT', val:'URGENT'}, {label:'HIGH', val:'HIGH'}, {label:'MIDDLE', val:'MIDDLE'}, {label:'LOW', val:'LOW'}]} currentVal={activeFilter.grade} />
                  )}
                </th>
                <th className="p-5 text-left text-xs font-black text-gray-400 uppercase tracking-widest">Subject</th>
                <th className="p-5 w-32 text-left text-xs font-black text-gray-400 uppercase tracking-widest">Sender</th>
                <th className="p-5 w-32 text-left text-xs font-black text-gray-400 uppercase tracking-widest">Receiver</th>
                <th className="p-5 w-40 text-center relative">
                  <div className="flex items-center justify-center cursor-pointer text-xs font-black text-gray-400 uppercase tracking-widest hover:text-blue-500 transition-colors"
                       onClick={() => setOpenDropdown(openDropdown === 'sort' ? null : 'sort')}>
                    Deadline <span className="ml-2 text-[10px]">▼</span>
                  </div>
                  {openDropdown === 'sort' && (
                    <HeaderFilterDropdown type="sort" options={[{label:'최신순', val:'tno,desc'}, {label:'마감 빠른순', val:'deadline,asc'}, {label:'마감 느린순', val:'deadline,desc'}]} currentVal={activeFilter.sort} />
                  )}
                </th>
                <th className="p-5 w-32 text-center relative">
                  <div className="flex items-center justify-center cursor-pointer text-xs font-black text-gray-400 uppercase tracking-widest hover:text-blue-500 transition-colors"
                       onClick={() => setOpenDropdown(openDropdown === 'state' ? null : 'state')}>
                    Status <span className="ml-2 text-[10px]">▼</span>
                  </div>
                  {openDropdown === 'state' && (
                    <HeaderFilterDropdown
                        type="state"
                        alignRight={true}
                        options={[
                            {label:'ALL', val:''},
                            {label:'신규', val:'NEW'},
                            {label:'진행 중', val:'IN_PROGRESS'},
                            {label:'정보 필요', val:'NEED_INFO'},
                            {label:'완료', val:'DONE'}
                        ]}
                        currentVal={activeFilter.state}
                    />
                  )}
                </th>
              </tr>
            </thead>

            <tbody className="divide-y divide-gray-100">
              {loading ? (
                <tr><td colSpan="7" className="p-40 text-center font-black text-gray-300 animate-pulse uppercase">Loading Work...</td></tr>
              ) : serverData.dtoList?.length > 0 ? (
                serverData.dtoList.map((ticket) => {
                  const myInfo = ticket.personals?.find(p => p.receiver === currentUserEmail);
                  const isRead = tab === 'RECEIVED' ? ticket.isread : (myInfo ? myInfo.isread : true);
                  const receiverInfo = ticket.personals?.length > 0 ? ticket.personals[0].receiver : ticket.receiver || '미지정';
                  const stateInfo = tab === 'RECEIVED' ? ticket.state : (myInfo ? myInfo.state : (ticket.personals?.[0]?.state || 'NEW'));

                  return (
                    <tr key={ticket.tno || ticket.pno} className="hover:bg-blue-50/30 transition-all h-[65px] cursor-pointer" onClick={() => openTicketModal(ticket.tno)}>
                      <td className="p-4 text-center" onClick={(e) => e.stopPropagation()}>
                        <button onClick={() => togglePin(ticket.tno)} className={`text-2xl transition-all hover:scale-125 ${isPinned(ticket.tno) ? 'text-yellow-500' : 'text-gray-200'}`}>
                          {isPinned(ticket.tno) ? '★' : '☆'}
                        </button>
                      </td>
                      <td className="p-4 truncate">{getGradeBadge(ticket.grade)}</td>
                      <td className="p-4">
                        <div className="flex items-center gap-2">
                          {!isRead && <div className="w-2 h-2 bg-blue-500 rounded-full shrink-0 shadow-[0_0_8px_rgba(59,130,246,0.5)]"></div>}
                          <span className="font-bold text-gray-800 truncate">{ticket.title}</span>
                        </div>
                      </td>
                      <td className="p-4 text-gray-500 font-medium truncate">{ticket.writer}</td>
                      <td className="p-4 text-gray-500 font-medium truncate">{receiverInfo}</td>
                      <td className="p-4 text-center font-black text-red-500 tracking-tighter truncate">
                        {formatDate(ticket.deadline)}
                      </td>
                      <td className="p-4 text-center">
                        <span className="px-3 py-1 rounded-xl text-[11px] font-black bg-gray-100 text-gray-700 uppercase">
                          {getStateLabel(stateInfo)}
                        </span>
                      </td>
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

      {isModalOpen && selectedTno && (
        <TicketDetailModal tno={selectedTno} onClose={closeTicketModal} onDelete={handleDeleted} />
      )}
    </div>
  );
};

export default TicketComponent;