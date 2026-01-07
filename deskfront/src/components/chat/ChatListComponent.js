import React, { useMemo, useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useSelector } from "react-redux";
import MemberPickerModal from "./MemberPickerModal";
import { searchMembers } from "../../api/memberApi";
import { getChatRooms, createGroupRoom, createOrGetDirectRoom } from "../../api/chatApi";

const ChatListComponent = ({ currentUserId: propCurrentUserId }) => {
  const navigate = useNavigate();
  const loginInfo = useSelector((state) => state.loginSlice);
  const currentUserId = loginInfo?.email || propCurrentUserId || "";

  // 채팅방 목록 상태
  const [chatRooms, setChatRooms] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // 모달 관련 상태
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedUsers, setSelectedUsers] = useState([]);
  const [searchKeyword, setSearchKeyword] = useState("");
  const [selectedDepartment, setSelectedDepartment] = useState("");
  const [searchResults, setSearchResults] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [searchError, setSearchError] = useState(null);
  const [userInfoMap, setUserInfoMap] = useState({}); // email -> {nickname, department}
  const [groupName, setGroupName] = useState(""); // 그룹 채팅방 이름

  // 채팅방 목록 로드
  useEffect(() => {
    loadChatRooms();
  }, []);

  const loadChatRooms = async () => {
    setLoading(true);
    setError(null);
    try {
      const rooms = await getChatRooms();

      // 인증 에러 처리
      if (rooms && typeof rooms === 'object' && rooms.error === "ERROR_ACCESS_TOKEN") {
        setError("인증이 만료되었습니다. 다시 로그인해주세요.");
        setChatRooms([]);
        return;
      }

      // 백엔드 응답을 프론트엔드 형식으로 변환
      const transformedRooms = rooms.map((room) => {
        // participants에서 현재 사용자 제외한 상대방 찾기
        const otherParticipants = room.participants?.filter(
          (p) => p.userId !== currentUserId
        ) || [];

        return {
          id: room.id,
          isGroup: room.roomType === "GROUP",
          name: room.name,
          participants: room.participants?.map((p) => p.userId) || [],
          participantInfo: room.participants?.map((p) => ({
            email: p.userId,
            nickname: p.nickname || p.userId,
          })) || [],
          lastMessage: room.lastMsgContent
            ? {
                content: room.lastMsgContent,
                createdAt: room.lastMsgAt,
              }
            : null,
          unreadCount: room.unreadCount || 0,
          // 1:1 채팅용
          user1Id: !otherParticipants.length ? currentUserId : currentUserId,
          user2Id: otherParticipants.length > 0 ? otherParticipants[0].userId : null,
        };
      });
      setChatRooms(transformedRooms);
    } catch (err) {
      console.error("채팅방 목록 로드 실패:", err);
      setError("채팅방 목록을 불러오는데 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const handleCreateChat = () => {
    setShowCreateModal(true);
    setSelectedUsers([]);
    setSearchKeyword("");
    setSelectedDepartment("");
    setGroupName("");
  };

  // 멤버 검색 API 호출 (디바운싱 적용) - 검색어 또는 부서 선택 시 검색
  useEffect(() => {
    // 검색어가 2글자 미만이고 부서도 선택 안 했으면 검색 안 함
    if (searchKeyword.trim().length < 2 && !selectedDepartment) {
      setSearchResults([]);
      setSearchError(null);
      return;
    }

    const timeoutId = setTimeout(() => {
      handleSearchMembers(searchKeyword, selectedDepartment);
    }, 300); // 300ms 디바운싱

    return () => clearTimeout(timeoutId);
  }, [searchKeyword, selectedDepartment]);

  const handleSearchMembers = async (keyword, department) => {
    setSearchLoading(true);
    setSearchError(null);
    try {
      const data = await searchMembers(keyword || null, 1, 20, department || null);
      // 현재 사용자 제외 및 DTO 변환
      const filtered = data.dtoList
        .filter((m) => m.email !== (loginInfo?.email || currentUserId))
        .map((m) => ({
          email: m.email,
          nickname: m.nickname || m.email,
          department: m.department || null,
        }));
      setSearchResults(filtered);

      // userInfoMap 업데이트
      const newMap = {};
      filtered.forEach(user => {
        newMap[user.email] = { nickname: user.nickname, department: user.department };
      });
      setUserInfoMap(prev => ({ ...prev, ...newMap }));
    } catch (err) {
      console.error("멤버 검색 실패:", err);
      setSearchError("멤버 검색에 실패했습니다.");
      setSearchResults([]);
    } finally {
      setSearchLoading(false);
    }
  };

  const availableUsers = useMemo(() => {
    return searchResults;
  }, [searchResults]);

  const toggleUserSelection = (email) => {
    setSelectedUsers((prev) =>
      prev.includes(email) ? prev.filter((id) => id !== email) : [...prev, email]
    );
  };

  const getChatRoomName = (room) => {
    if (room.isGroup) {
      if (room.participantInfo && room.participantInfo.length > 0) {
        const names = room.participantInfo.map(p => p.nickname);
        if (names.length <= 2) {
          return names.join(", ");
        }
        return `${names.slice(0, 2).join(", ")} 외 ${names.length - 2}명`;
      }
      return "그룹 채팅";
    } else {
      // DIRECT 방: 현재 사용자를 제외한 상대방 찾기
      if (room.participantInfo && room.participantInfo.length > 0) {
        const otherParticipant = room.participantInfo.find(
          (p) => p.email !== currentUserId
        );
        
        if (otherParticipant) {
          return otherParticipant.nickname;
        }
      }
      const otherUser = room.user1Id === currentUserId ? room.user2Id : room.user1Id;
      return otherUser || "알 수 없음";
    }
  };

  const formatLastMessage = (message) => {
    if (!message) return "메시지가 없습니다.";
    return message.content.length > 30 ? message.content.slice(0, 30) + "..." : message.content;
  };

  // 채팅방 생성
  const handleCreateRoom = async () => {
    if (selectedUsers.length === 0) {
      return alert("최소 1명 이상의 참여자를 선택해주세요.");
    }

    try {
      let newRoom;

      if (selectedUsers.length === 1) {
        // 1:1 채팅방 생성
        newRoom = await createOrGetDirectRoom({ targetEmail: selectedUsers[0] });
      } else {
        // 그룹 채팅방 생성
        if (!groupName.trim()) {
          return alert("그룹 채팅방 이름을 입력해주세요.");
        }
        newRoom = await createGroupRoom({
          name: groupName.trim(),
          participantEmails: selectedUsers,
        });
      }

      setShowCreateModal(false);
      setSelectedUsers([]);
      setGroupName("");

      // 목록 새로고침
      await loadChatRooms();

      // 생성된 방으로 이동
      navigate(`/chat/${newRoom.id}`);
    } catch (err) {
      console.error("채팅방 생성 실패:", err);
      alert("채팅방 생성에 실패했습니다.");
    }
  };

  return (
    <>
      <div className="w-full">
        {/* 상단 타이틀 */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
          <div>
            <div className="text-xs uppercase tracking-widest text-chatMuted mb-2">CHAT</div>
            <h1 className="ui-title">
              채팅방 목록
            </h1>
          </div>
          <button
            onClick={handleCreateChat}
            className="bg-chatNavy text-white px-6 py-2.5 rounded-chat font-semibold text-sm hover:opacity-90 transition-all shadow-chat focus:outline-none focus:ring-2 focus:ring-chatNavy focus:ring-offset-2"
          >
            새 채팅
          </button>
        </div>

        {/* 목록 카드 */}
        <div className="bg-chatBg rounded-chatLg shadow-chatMd overflow-hidden border border-chatBorder min-h-[600px] flex flex-col">
          <div className="px-6 py-4 bg-chatSurface border-b border-chatBorder flex justify-between items-center">
            <h2 className="text-sm font-semibold text-chatText uppercase tracking-wide">
              채팅방 목록
            </h2>
            <span className="text-xs text-chatMuted font-medium">
              총 {chatRooms.length}개
            </span>
          </div>

          <div className="flex-grow divide-y divide-chatBorder">
            {loading ? (
              <div className="p-20 text-center text-chatMuted">
                <p className="text-base">로딩 중...</p>
              </div>
            ) : error ? (
              <div className="p-20 text-center text-chatMuted">
                <p className="text-base">{error}</p>
              </div>
            ) : chatRooms.length === 0 ? (
              <div className="p-20 text-center text-chatMuted">
                <p className="text-lg font-medium mb-2">채팅방이 없습니다</p>
                <p className="text-sm">새 채팅을 시작해보세요.</p>
              </div>
            ) : (
              chatRooms.map((room) => (
                <div
                  key={room.id}
                  onClick={() => navigate(`/chat/${room.id}`)}
                  className="relative flex items-center justify-between px-6 py-4 hover:bg-chatSurface transition-colors cursor-pointer group"
                >
                  {/* 선택 인디케이터 (좌측) */}
                  <div className="absolute left-0 top-0 bottom-0 w-0 group-hover:w-0.5 bg-chatNavy transition-all"></div>
                  
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1.5">
                      <h3 className="font-semibold text-base text-chatText truncate">
                        {getChatRoomName(room)}
                      </h3>
                      {room.isGroup && (
                        <span className="px-2 py-0.5 rounded ui-text-2xs font-medium bg-chatSurface text-chatMuted uppercase shrink-0">
                          그룹
                        </span>
                      )}
                    </div>
                    {room.lastMessage && (
                      <>
                        <p className="text-sm text-chatMuted mb-1 truncate">
                          {formatLastMessage(room.lastMessage)}
                        </p>
                        <p className="text-xs text-chatMuted">
                          {new Date(room.lastMessage.createdAt).toLocaleString("ko-KR", {
                            month: "short",
                            day: "numeric",
                            hour: "2-digit",
                            minute: "2-digit",
                          })}
                        </p>
                      </>
                    )}
                  </div>
                  {room.unreadCount > 0 && (
                    <span className="bg-chatOrange text-white text-xs font-semibold px-2 py-1 rounded-full shrink-0 ml-4 min-w-[20px] text-center">
                      {room.unreadCount}
                    </span>
                  )}
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* 생성 모달 */}
      <MemberPickerModal
        open={showCreateModal}
        title="새 채팅 생성"
        multi={true}
        keyword={searchKeyword}
        onChangeKeyword={setSearchKeyword}
        results={availableUsers}
        selected={selectedUsers}
        onToggle={toggleUserSelection}
        loading={searchLoading}
        error={searchError}
        onClose={() => {
          setShowCreateModal(false);
          setSearchKeyword("");
          setSelectedUsers([]);
          setSelectedDepartment("");
        }}
        selectedDepartment={selectedDepartment}
        onChangeDepartment={setSelectedDepartment}
        onConfirm={handleCreateRoom}
        showGroupName={selectedUsers.length > 1}
        groupName={groupName}
        onChangeGroupName={setGroupName}
      />
    </>
  );
};

export default ChatListComponent;