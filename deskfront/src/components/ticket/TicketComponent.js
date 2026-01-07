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
    fetchData();
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
    <div className={`absolute top-full ${alignRight ? 'right-0' : 'left-0'} mt-2 w-44 ui-card py-2 z-50`}>
      {options.map(opt => (
        <div
          key={opt.val}
          className={`px-4 py-2.5 hover:bg-baseSurface cursor-pointer text-xs font-semibold transition-colors ${currentVal === opt.val ? 'text-brandNavy bg-baseSurface' : 'text-baseText'}`}
          onClick={(e) => { e.stopPropagation(); handleColumnFilter(type, opt.val); }}
        >
          {opt.label}
        </div>
      ))}
    </div>
  );

  return (
    <div className="w-full">
      <div className="mb-8">
        <div className="text-xs uppercase tracking-widest text-baseMuted mb-2">TICKET</div>
        <h1 className="ui-title">업무 현황</h1>
      </div>

      <div className="flex flex-col xl:flex-row justify-between items-stretch xl:items-center mb-8 gap-6 ui-card p-6">
        <div className="flex bg-baseSurface p-2 rounded-ui">
          {['ALL', 'RECEIVED', 'SENT'].map((t) => (
            <button
              key={t}
              onClick={() => handleTabChange(t)}
              className={`px-6 py-2.5 rounded-ui font-semibold text-sm transition-all ${
                tab === t ? "bg-baseBg text-brandNavy shadow-chat" : "text-baseMuted hover:text-baseText"
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
            className="ui-select w-36"
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
              className="ui-input"
            />
          </div>

          <button onClick={handleSearch} className="ui-btn-primary">
            검색
          </button>
        </div>
      </div>

      <div className="ui-card overflow-hidden min-h-[600px] flex flex-col">
        <div className="px-6 py-4 bg-baseSurface border-b border-baseBorder flex justify-between items-center">
          <h2 className="text-sm font-semibold text-baseText uppercase tracking-wide">
            {getWorkListTitle()}
          </h2>
          <span className="text-xs text-baseMuted font-medium">
            총 {serverData?.totalCount || 0}개
          </span>
        </div>

        <div className="overflow-x-auto flex-grow">
          <table className="ui-table">
            <thead>
              <tr>
                <th className="w-16 text-center">Pin</th>
                <th className="w-36 text-left relative">
                  <div className="flex items-center cursor-pointer hover:text-brandNavy transition-colors"
                       onClick={() => setOpenDropdown(openDropdown === 'grade' ? null : 'grade')}>
                    중요도 <span className="ml-1 ui-dropdown-arrow">▼</span>
                  </div>
                  {openDropdown === 'grade' && (
                    <HeaderFilterDropdown type="grade" options={[{label:'전체', val:''}, {label:'긴급', val:'URGENT'}, {label:'높음', val:'HIGH'}, {label:'보통', val:'MIDDLE'}, {label:'낮음', val:'LOW'}]} currentVal={activeFilter.grade} />
                  )}
                </th>
                <th className="text-left">제목</th>
                <th className="w-32 text-left">발신자</th>
                <th className="w-32 text-left">수신자</th>
                <th className="w-40 text-center relative">
                  <div className="flex items-center justify-center cursor-pointer hover:text-brandNavy transition-colors"
                       onClick={() => setOpenDropdown(openDropdown === 'sort' ? null : 'sort')}>
                    마감일 <span className="ml-1 ui-dropdown-arrow">▼</span>
                  </div>
                  {openDropdown === 'sort' && (
                    <HeaderFilterDropdown type="sort" options={[{label:'최신순', val:'tno,desc'}, {label:'마감 빠른순', val:'deadline,asc'}, {label:'마감 느린순', val:'deadline,desc'}]} currentVal={activeFilter.sort} />
                  )}
                </th>
                <th className="w-32 text-center relative">
                  <div className="flex items-center justify-center cursor-pointer hover:text-brandNavy transition-colors"
                       onClick={() => setOpenDropdown(openDropdown === 'state' ? null : 'state')}>
                    상태 <span className="ml-1 ui-dropdown-arrow">▼</span>
                  </div>
                  {openDropdown === 'state' && (
                    <HeaderFilterDropdown
                        type="state"
                        alignRight={true}
                        options={[
                            {label:'전체', val:''},
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

            <tbody>
              {loading ? (
                <tr><td colSpan="7" className="p-20 text-center text-baseMuted">로딩 중...</td></tr>
              ) : serverData.dtoList?.length > 0 ? (
                serverData.dtoList.map((ticket) => {
                  const myInfo = ticket.personals?.find(p => p.receiver === currentUserEmail);
                  const isRead = tab === 'RECEIVED' ? ticket.isread : (myInfo ? myInfo.isread : true);
                  const receiverInfo = ticket.personals?.length > 0 ? ticket.personals[0].receiver : ticket.receiver || '미지정';
                  const stateInfo = tab === 'RECEIVED' ? ticket.state : (myInfo ? myInfo.state : (ticket.personals?.[0]?.state || 'NEW'));

                  return (
                    <tr key={ticket.tno || ticket.pno} className="cursor-pointer" onClick={() => openTicketModal(ticket.tno)}>
                      <td className="text-center" onClick={(e) => e.stopPropagation()}>
                        <button onClick={() => togglePin(ticket.tno)} className={`text-xl transition-all hover:scale-125 ${isPinned(ticket.tno) ? 'ui-pin-active' : 'ui-pin-inactive'}`}>
                          {isPinned(ticket.tno) ? '★' : '☆'}
                        </button>
                      </td>
                      <td className="truncate">{getGradeBadge(ticket.grade)}</td>
                      <td>
                        <div className="flex items-center gap-2">
                          {!isRead && <span className="w-2 h-2 bg-brandOrange rounded-full shrink-0"></span>}
                          <span className="font-semibold text-baseText truncate">{ticket.title}</span>
                        </div>
                      </td>
                      <td className="text-baseMuted truncate">{ticket.writer}</td>
                      <td className="text-baseMuted truncate">{receiverInfo}</td>
                      <td className="text-center text-sm ui-deadline truncate">
                        {formatDate(ticket.deadline)}
                      </td>
                      <td className="text-center">
                        <span className="ui-badge ui-text-3xs">
                          {getStateLabel(stateInfo)}
                        </span>
                      </td>
                    </tr>
                  );
                })
              ) : (
                <tr><td colSpan="7" className="p-20 text-center text-baseMuted">데이터가 없습니다.</td></tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="p-6 bg-baseSurface flex justify-center border-t border-baseBorder mt-auto">
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