import React, { useEffect, useState, useCallback } from 'react';
import { useSelector } from 'react-redux';
import { getFileBoxList, downloadFile, deleteFile } from '../../api/fileApi';
import PageComponent from '../common/PageComponent';
import FilePreview from '../common/FilePreview';
import { formatDate } from '../../util/ticketUtils';

const FileBoxComponent = () => {
    const loginState = useSelector((state) => state.loginSlice);

    // 상태 관리
    const [tab, setTab] = useState('ALL');
    const [serverData, setServerData] = useState({ dtoList: [], totalCount: 0, pageNumList: [] });
    const [page, setPage] = useState(1);
    const [sort, setSort] = useState('createdAt,desc');
    const [keyword, setKeyword] = useState('');
    const [searchTemp, setSearchTemp] = useState('');
    const [loading, setLoading] = useState(false);

    // 중복 파일명 처리 로직
    const processDuplicateNames = (files) => {
        const nameCountMap = {};
        return files.map(file => {
            const originalName = file.fileName;
            const lastDot = originalName.lastIndexOf('.');
            const base = lastDot !== -1 ? originalName.substring(0, lastDot) : originalName;
            const ext = lastDot !== -1 ? originalName.substring(lastDot) : "";

            if (!nameCountMap[originalName]) {
                nameCountMap[originalName] = 1;
                return { ...file, displayName: originalName };
            } else {
                const count = nameCountMap[originalName]++;
                return { ...file, displayName: `${base} (${count})${ext}` };
            }
        });
    };

    const fetchData = useCallback(async () => {
        if (!loginState.email) return;
        setLoading(true);
        try {
            const data = await getFileBoxList(loginState.email, tab, { page, size: 10 }, { keyword, sort });
            if (data && data.dtoList) {
                data.dtoList = processDuplicateNames(data.dtoList);
            }
            setServerData(data || { dtoList: [], totalCount: 0, pageNumList: [] });
        } catch (error) {
            console.error(error);
        } finally {
            setLoading(false);
        }
    }, [loginState.email, page, sort, tab, keyword]);

    useEffect(() => {
        fetchData();
    }, [fetchData]);

    const handleSearch = () => {
        setKeyword(searchTemp);
        setPage(1);
    };

    const handleDeleteFile = async (e, uuid, fileName) => {
        e.stopPropagation();
        if (window.confirm(`'${fileName}' 파일을 영구적으로 삭제하시겠습니까?`)) {
            try {
                await deleteFile(uuid);
                fetchData();
            } catch (error) {
                alert("파일 삭제 실패");
            }
        }
    };
      // 다운로드 확인 창 (displayName을 사용하여 사용자에게 안내)
      const handleDownload = (uuid, fileName, displayName) => {
        if (window.confirm(`'${displayName || fileName}' 파일을 다운로드 하시겠습니까?`)) {
          downloadFile(uuid, fileName); // 실제 API 요청은 원본 fileName으로
        }
      };

    return (
        <div className="w-full">
            {/* 제목: 파일 함 */}
            <div className="mb-8">
                <div className="text-xs uppercase tracking-widest text-baseMuted mb-2">FILE</div>
                <h1 className="ui-title">파일 함</h1>
            </div>

            {/* 필터 및 검색 바  */}
            <div className="flex flex-col gap-4 lg:gap-6 mb-8 ui-card p-4 sm:p-6">

                {/* 탭 버튼 */}
                <div className="flex bg-baseSurface p-1.5 sm:p-2 rounded-ui">
                    {['ALL', 'SENT', 'RECEIVED'].map((t) => (
                        <button
                            key={t}
                            onClick={() => { setTab(t); setPage(1); }}
                            className={`px-3 sm:px-4 lg:px-6 py-2 sm:py-2.5 rounded-ui font-semibold text-xs sm:text-sm transition-all flex-1 ${
                                tab === t ? "bg-baseBg text-brandNavy shadow-ui" : "text-baseMuted hover:text-baseText"
                            }`}
                        >
                            {t === 'ALL' ? '전체' : t === 'SENT' ? '발신' : '수신'}
                        </button>
                    ))}
                </div>

                <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 flex-grow">
                    {/* 정렬 선택 */}
                    <select
                        value={sort}
                        onChange={(e) => { setSort(e.target.value); setPage(1); }}
                        className="ui-select w-full sm:w-36"
                    >
                        <option value="createdAt,desc">최신순</option>
                        <option value="createdAt,asc">오래된순</option>
                    </select>

                    {/* 검색창 */}
                    <div className="relative flex-grow">
                        <input
                            type="text"
                            placeholder="파일명 검색..."
                            value={searchTemp}
                            onChange={(e) => setSearchTemp(e.target.value)}
                            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                            className="ui-input w-full"
                        />
                    </div>

                    {/* 검색 버튼 */}
                    <button
                        onClick={handleSearch}
                        className="ui-btn-primary w-full sm:w-auto whitespace-nowrap"
                    >
                        검색
                    </button>
                </div>
            </div>

            {/* 파일 리스트 박스 */}
            <div className="ui-card p-6 lg:p-10 min-h-[600px] flex flex-col">
                <div className="mb-6 flex justify-between items-center">
                    <h2 className="text-sm font-semibold text-baseText uppercase tracking-wide">
                        {tab === 'SENT' ? '발신 파일' : tab === 'RECEIVED' ? '수신 파일' : '전체 파일'}
                    </h2>
                    <span className="text-xs text-baseMuted font-medium">
                        총 {serverData?.totalCount || 0}개
                    </span>
                </div>

                {loading ? (
                    <div className="flex-1 flex items-center justify-center text-baseMuted">로딩 중...</div>
                ) : serverData?.dtoList?.length > 0 ? (
                    <div className="grid grid-cols-2 md:grid-cols-5 gap-6">
                        {serverData.dtoList.map((file) => (
                            <div key={file.uuid} onClick={() => handleDownload(file.uuid, file.fileName, file.displayName)} className="group cursor-pointer relative">
                                {/* 삭제 버튼 */}
                                <button
                                    onClick={(e) => handleDeleteFile(e, file.uuid, file.displayName)}
                                    className="absolute -top-2 -right-2 z-10 ui-btn-danger w-6 h-6 rounded-full text-xs flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
                                >
                                    &times;
                                </button>

                                <div className="aspect-square bg-baseSurface rounded-ui border border-baseBorder overflow-hidden mb-3 shadow-chat relative group-hover:border-brandNavy transition-all">
                                    <FilePreview file={file} />
                                </div>
                                <div className="px-2">
                                    <div className="text-xs font-semibold truncate text-baseText" title={file.displayName}>
                                        {file.displayName}
                                    </div>
                                    <div className="flex justify-between mt-1 ui-text-2xs text-baseMuted">
                                        <span>{(file.fileSize / 1024).toFixed(1)} KB</span>
                                        <span>{formatDate(file.createdAt)}</span>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                ) : (
                    <div className="flex-1 flex flex-col items-center justify-center text-baseMuted">파일이 없습니다.</div>
                )}

                <div className="mt-8 flex justify-center ui-divider pt-6">
                    {serverData?.pageNumList?.length > 0 && (
                        <PageComponent serverData={serverData} movePage={(p) => setPage(p.page)} />
                    )}
                </div>
            </div>
        </div>
    );
};

export default FileBoxComponent;