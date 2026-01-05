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
      if (room.participantInfo && room.participantInfo.length > 0) {
        return room.participantInfo[0].nickname;
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
      <div className="max-w-7xl mx-auto p-8 space-y-10 bg-gray-50/30 min-h-screen">
        {/* 상단 타이틀 */}
        <div className="flex justify-between items-end mb-4">
        <div className="relative inline-block">
          <span className="text-blue-600 font-black text-xs uppercase tracking-widest mb-3 block italic">
            Chat Room
          </span>
          <h1 className="text-4xl font-black text-[#111827] mb-4 tracking-tighter uppercase">
            Chat
          </h1>
          <div className="h-1.5 w-full bg-blue-600 rounded-full shadow-[0_2px_10px_rgba(37,99,235,0.3)]"></div>
        </div>

        <div className="flex gap-3 items-center pb-2">
          <button
            onClick={handleCreateChat}
            className="bg-[#111827] text-white px-10 py-3.5 rounded-2xl font-black text-xs uppercase tracking-[0.2em] hover:bg-blue-600 hover:-translate-y-1 transition-all duration-300 shadow-xl shadow-gray-200"
          >
            새 채팅
          </button>
        </div>
      </div>

      {/* 목록 카드 */}
      <div className="bg-white rounded-[2.5rem] overflow-hidden shadow-[0_25px_60px_rgba(0,0,0,0.05)] border border-gray-100">
        <div className="bg-[#1a1f2c] px-10 py-5 flex justify-between items-center border-b border-gray-800">
          <h2 className="text-white font-black italic tracking-widest text-xs uppercase opacity-80">
            Chat Room List
          </h2>
          <div className="flex gap-2.5">
            <div className="w-3 h-3 rounded-full bg-[#ff5f56]"></div>
            <div className="w-3 h-3 rounded-full bg-[#ffbd2e]"></div>
            <div className="w-3 h-3 rounded-full bg-[#27c93f]"></div>
          </div>
        </div>

        <div className="p-12 min-h-[450px] bg-gradient-to-b from-white to-gray-50/30">
          {loading ? (
            <div className="text-center text-gray-500 py-8">로딩 중...</div>
          ) : error ? (
            <div className="text-center text-red-500 py-8">{error}</div>
          ) : chatRooms.length === 0 ? (
            <div className="text-center text-gray-500 py-8">
              채팅방이 없습니다. 새 채팅을 시작해보세요.
            </div>
          ) : (
            <div className="space-y-3">
              {chatRooms.map((room) => (
                <div
                  key={room.id}
                  onClick={() => navigate(`/chat/${room.id}`)}
                  className="p-6 bg-white rounded-xl border border-gray-200 hover:bg-gray-50 cursor-pointer transition-all hover:shadow-md"
                >
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-2">
                        <h3 className="font-bold text-lg text-gray-900">
                          {getChatRoomName(room)}
                        </h3>
                        {room.isGroup && (
                          <span className="px-3 py-1 text-xs bg-green-100 text-green-700 rounded-full font-semibold">
                            그룹
                          </span>
                        )}
                      </div>

                      {room.lastMessage && (
                        <>
                          <p className="text-sm text-gray-600 mb-1">
                            {formatLastMessage(room.lastMessage)}
                          </p>
                          <p className="text-xs text-gray-400">
                            {new Date(room.lastMessage.createdAt).toLocaleString()}
                          </p>
                        </>
                      )}
                    </div>

                    {room.unreadCount > 0 && (
                      <span className="px-3 py-1 bg-red-500 text-white text-xs rounded-full font-semibold">
                        {room.unreadCount}
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>
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
