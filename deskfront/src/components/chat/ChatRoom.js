import React, { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useSelector } from "react-redux";
import useInfiniteChat from "../../hooks/useInfiniteChat";
import MemberPickerModal from "./MemberPickerModal";
import TicketConfirmModal from "./TicketConfirmModal";
import AIChatWidget from "../menu/AIChatWidget";
import { searchMembers } from "../../api/memberApi";
import { getMessages, sendMessageRest, markRead, leaveRoom, inviteUsers } from "../../api/chatApi";
import chatWsClient from "../../api/chatWs";

const ChatRoom = ({ chatRoomId, currentUserId, otherUserId, chatRoomInfo }) => {
  const navigate = useNavigate();
  const loginInfo = useSelector((state) => state.loginSlice);

  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState("");
  const [connected, setConnected] = useState(false);
  const [loading, setLoading] = useState(true);
  const [hasMore, setHasMore] = useState(true);
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 20;
  const [aiEnabled, setAiEnabled] = useState(false); // AI 메시지 처리 ON/OFF

  // 사용자 초대 모달 관련 상태
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [selectedUsers, setSelectedUsers] = useState([]);
  const [searchKeyword, setSearchKeyword] = useState("");
  const [selectedDepartment, setSelectedDepartment] = useState("");
  const [searchResults, setSearchResults] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [searchError, setSearchError] = useState(null);
  const [userInfoMap, setUserInfoMap] = useState({});

  // 티켓 작성 모달 관련 상태
  const [isTicketModalOpen, setIsTicketModalOpen] = useState(false);
  const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);

  const messagesEndRef = useRef(null);
  const chatContainerRef = useRef(null);

  // 무한 스크롤 훅
  const { visibleMessages, onScroll, scrollToBottom, setContainerRef, reset } = useInfiniteChat(messages, 30);

  // 컨테이너 ref 설정
  useEffect(() => {
    setContainerRef(chatContainerRef.current);
  }, [setContainerRef]);

  // 방 변경 시 초기화
  useEffect(() => {
    reset();
  }, [chatRoomId, reset]);

  // 메시지 로드 (초기 로드)
  useEffect(() => {
    if (!chatRoomId || !currentUserId) return;

    const loadInitialMessages = async () => {
      setLoading(true);
      try {
        const response = await getMessages(chatRoomId, { page: 1, size: pageSize });
        // 백엔드 응답을 프론트엔드 형식으로 변환 (최신순이므로 reverse)
        const transformedMessages = (response.dtoList || [])
          .reverse()
          .map((msg) => ({
            id: msg.id,
            chatRoomId: msg.chatRoomId,
            senderId: msg.senderId,
            senderNickname: msg.senderNickname || msg.senderId,
            receiverId: chatRoomInfo?.isGroup ? null : (msg.senderId === currentUserId ? otherUserId : currentUserId),
            content: msg.content,
            createdAt: msg.createdAt,
            isRead: true, // 서버에서 이미 읽음 처리된 것으로 간주
            isTicketPreview: msg.messageType === "TICKET_PREVIEW",
            ticketId: msg.ticketId,
            messageSeq: msg.messageSeq,
          }));
        setMessages(transformedMessages);
        setHasMore(response.totalCount > transformedMessages.length);
        setCurrentPage(1);

        // 마지막 메시지 읽음 처리
        if (transformedMessages.length > 0) {
          const lastMessage = transformedMessages[transformedMessages.length - 1];
          if (lastMessage.messageSeq) {
            await markRead(chatRoomId, { messageSeq: lastMessage.messageSeq });
          }
        }
      } catch (err) {
        console.error("메시지 로드 실패:", err);
      } finally {
        setLoading(false);
      }
    };

    loadInitialMessages();
  }, [chatRoomId, currentUserId]);

  // WebSocket 연결
  useEffect(() => {
    if (!chatRoomId || !currentUserId) return;

    // WebSocket 연결
    chatWsClient.connect(
      chatRoomId,
      (newMessage) => {
        // 백엔드 응답을 프론트엔드 형식으로 변환
        const transformedMessage = {
          id: newMessage.id,
          chatRoomId: newMessage.chatRoomId,
          senderId: newMessage.senderId,
          senderNickname: newMessage.senderNickname || newMessage.senderId,
          receiverId: chatRoomInfo?.isGroup ? null : (newMessage.senderId === currentUserId ? otherUserId : currentUserId),
          content: newMessage.content,
          createdAt: newMessage.createdAt,
          isRead: newMessage.senderId === currentUserId, // 내가 보낸 메시지는 읽음
          isTicketPreview: newMessage.messageType === "TICKET_PREVIEW",
          ticketId: newMessage.ticketId,
          messageSeq: newMessage.messageSeq,
        };

        setMessages((prev) => {
          // 중복 방지
          if (prev.some((m) => m.id === transformedMessage.id)) {
            return prev;
          }
          return [...prev, transformedMessage];
        });

        // 읽음 처리 (내가 보낸 메시지가 아니고, 상대방이 보낸 메시지인 경우)
        if (transformedMessage.senderId !== currentUserId && transformedMessage.messageSeq) {
          markRead(chatRoomId, { messageSeq: transformedMessage.messageSeq }).catch(console.error);
        }
        
        // 티켓 생성 문맥 감지 시 확인 모달 띄우기
        if (newMessage.ticketTrigger) {
          openConfirmModal();
        }
      },
      () => {
        // 연결 성공 시
        setConnected(true);
      },
      () => {
        // 연결 해제 시
        setConnected(false);
      }
    );

    // 초기 연결 상태 확인 (한 번만)
    setConnected(chatWsClient.isConnected());

    // 컴포넌트 언마운트 시 연결 해제
    return () => {
      chatWsClient.disconnect();
      setConnected(false);
    };
  }, [chatRoomId, currentUserId]);

  // ✅ 새 메시지 추가 시 맨 아래로 스크롤
  useEffect(() => {
    scrollToBottom();
  }, [messages.length, scrollToBottom]);

  // 메시지 전송
  const handleSendMessage = async () => {
    if (!inputMessage.trim()) return;

    const content = inputMessage.trim();
    setInputMessage("");

    // WebSocket으로 전송 시도
    const wsSuccess = chatWsClient.send(chatRoomId, {
      content,
      messageType: "TEXT",
      aiEnabled: aiEnabled,
    });

    // WebSocket 실패 시 REST API로 fallback
    if (!wsSuccess) {
      try {
        const newMessage = await sendMessageRest(chatRoomId, {
          content,
          messageType: "TEXT",
          aiEnabled: aiEnabled,
        });
        
        // 백엔드 응답을 프론트엔드 형식으로 변환
        const transformedMessage = {
          id: newMessage.id,
          chatRoomId: newMessage.chatRoomId,
          senderId: newMessage.senderId,
          senderNickname: newMessage.senderNickname || newMessage.senderId,
          receiverId: chatRoomInfo?.isGroup ? null : (newMessage.senderId === currentUserId ? otherUserId : currentUserId),
          content: newMessage.content,
          createdAt: newMessage.createdAt,
          isRead: true,
          isTicketPreview: newMessage.messageType === "TICKET_PREVIEW",
          ticketId: newMessage.ticketId,
          messageSeq: newMessage.messageSeq,
        };

        setMessages((prev) => [...prev, transformedMessage]);
        
        // 티켓 생성 문맥 감지 시 확인 모달 띄우기
        if (newMessage.ticketTrigger) {
          openConfirmModal();
        }
      } catch (err) {
        console.error("메시지 전송 실패:", err);
        alert("메시지 전송에 실패했습니다.");
      }
    }
  };

  // ✅ 티켓 미리보기 클릭(로컬 목업)
  const handleTicketPreviewClick = (ticketId) => {
    // 나중에 getTicketInfo(ticketId)로 교체하면 됨
    alert(
      `티켓 정보(목업)\n\n` +
        `ticketId: ${ticketId}\n` +
        `보낸 사람: ${otherUserId || "someone@test.com"}\n` +
        `받는 사람: ${currentUserId}\n` +
        `생성일: ${new Date().toLocaleString()}`
    );
  };

  // ✅ Enter 키 처리
  const handleKeyPress = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  // 채팅방 나가기
  const handleLeaveRoom = async () => {
    if (!window.confirm("정말 채팅방을 나가시겠습니까?")) {
      return;
    }

    try {
      await leaveRoom(chatRoomId);
      chatWsClient.disconnect();
      navigate("/chat");
    } catch (err) {
      console.error("채팅방 나가기 실패:", err);
      alert("채팅방 나가기에 실패했습니다.");
    }
  };

  // ✅ 사용자 초대 모달 열기
  const handleOpenInviteModal = () => {
    setShowInviteModal(true);
    setSelectedUsers([]);
    setSearchKeyword("");
    setSelectedDepartment("");
  };

  // ✅ 티켓 작성 모달 열기/닫기
  const openTicketModal = () => setIsTicketModalOpen(true);
  const closeTicketModal = () => setIsTicketModalOpen(false);

  // ✅ 티켓 생성 확인 모달 열기/닫기
  const openConfirmModal = () => setIsConfirmModalOpen(true);
  const closeConfirmModal = () => setIsConfirmModalOpen(false);

  // ✅ 확인 모달에서 예를 눌렀을 때
  const handleConfirmTicket = () => {
    closeConfirmModal();
    openTicketModal();
  };

  // ✅ 멤버 검색 (디바운싱)
  useEffect(() => {
    if (!showInviteModal) return;
    
    if (searchKeyword.trim().length < 2 && !selectedDepartment) {
      setSearchResults([]);
      setSearchError(null);
      return;
    }

    const timeoutId = setTimeout(() => {
      handleSearchMembers(searchKeyword, selectedDepartment);
    }, 300);

    return () => clearTimeout(timeoutId);
  }, [searchKeyword, selectedDepartment, showInviteModal]);

  const handleSearchMembers = async (keyword, department) => {
    setSearchLoading(true);
    setSearchError(null);
    try {
      const data = await searchMembers(keyword || null, 1, 20, department || null);
      // 현재 사용자 및 이미 참여 중인 사용자 제외
      const currentParticipants = chatRoomInfo?.isGroup 
        ? (chatRoomInfo.participants || [])
        : [currentUserId, otherUserId].filter(Boolean);
      
      const filtered = data.dtoList
        .filter((m) => !currentParticipants.includes(m.email))
        .map((m) => ({
          email: m.email,
          nickname: m.nickname || m.email,
          department: m.department || null,
        }));
      setSearchResults(filtered);
      
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

  const toggleUserSelection = (email) => {
    setSelectedUsers((prev) =>
      prev.includes(email) ? prev.filter((id) => id !== email) : [...prev, email]
    );
  };

  // 사용자 초대 확인
  const handleInviteUsers = async () => {
    if (selectedUsers.length === 0) {
      return alert("최소 1명 이상의 사용자를 선택해주세요.");
    }

    try {
      await inviteUsers(chatRoomId, { inviteeEmails: selectedUsers });
      alert(`${selectedUsers.length}명의 사용자를 초대했습니다.`);
      setShowInviteModal(false);
      setSelectedUsers([]);
      setSearchKeyword("");
      setSelectedDepartment("");
    } catch (err) {
      console.error("사용자 초대 실패:", err);
      alert("사용자 초대에 실패했습니다.");
    }
  };

  const chatRoomName = chatRoomInfo?.isGroup
    ? chatRoomInfo.name || "그룹 채팅"
    : otherUserId || "채팅";

  return (
    <div className="h-screen overflow-hidden flex flex-col bg-gray-50/30">
      {/* Header */}
      <div className="shrink-0 max-w-7xl mx-auto w-full p-8 pb-4">
        <div className="flex justify-between items-end">
          <div className="relative inline-block">
            <span className="text-blue-600 font-black text-xs uppercase tracking-widest mb-3 block italic">
              {chatRoomInfo?.isGroup ? "Group Chat" : "Direct Message"}
            </span>
            <h1 className="text-4xl font-black text-[#111827] mb-4 tracking-tighter uppercase">
              {chatRoomName}
            </h1>
            <div className="h-1.5 w-full bg-blue-600 rounded-full shadow-[0_2px_10px_rgba(37,99,235,0.3)]"></div>
          </div>

          {/* 오른쪽 정보 및 버튼 */}
          <div className="flex flex-col items-end gap-3 pb-2">
            <div className="text-right space-y-1">
              <div className="text-sm font-black text-gray-900 italic">
                {chatRoomInfo?.isGroup && Array.isArray(chatRoomInfo?.participantIds) && (
                  <span>
                    PARTICIPANTS.{" "}
                    <span className="text-blue-600 underline decoration-2 underline-offset-4">
                      {chatRoomInfo.participantIds.length}명
                    </span>
                  </span>
                )}
                {!chatRoomInfo?.isGroup && (
                  <span>
                    TO.{" "}
                    <span className="text-blue-600 underline decoration-2 underline-offset-4">
                      {otherUserId || "Unknown"}
                    </span>
                  </span>
                )}
              </div>

              {/* 연결 상태 */}
              <div className={`text-[11px] font-bold italic uppercase tracking-widest ${connected ? "text-green-600" : "text-red-600"}`}>
                ● {connected ? "CONNECTED" : "DISCONNECTED"}
              </div>
            </div>

            {/* 액션 버튼들 */}
            <div className="flex gap-2">
              {chatRoomInfo?.isGroup && (
                <button
                  onClick={handleOpenInviteModal}
                  className="bg-blue-600 text-white px-6 py-2 rounded-xl font-black text-xs uppercase tracking-[0.15em] hover:bg-blue-700 hover:-translate-y-0.5 transition-all duration-300 shadow-md"
                >
                  사용자 초대
                </button>
              )}
              <button
                onClick={handleLeaveRoom}
                className="bg-red-500 text-white px-6 py-2 rounded-xl font-black text-xs uppercase tracking-[0.15em] hover:bg-red-600 hover:-translate-y-0.5 transition-all duration-300 shadow-md"
              >
                나가기
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Messages (scroll) */}
      <div className="flex-1 overflow-hidden max-w-7xl mx-auto w-full px-8 pb-8">
        <div className="h-full bg-white rounded-[2.5rem] overflow-hidden shadow-[0_25px_60px_rgba(0,0,0,0.05)] border border-gray-100 flex flex-col">
          <div className="shrink-0 bg-[#1a1f2c] px-10 py-5 flex justify-between items-center border-b border-gray-800">
            <h2 className="text-white font-black italic tracking-widest text-xs uppercase opacity-80">
              Chat Messages
            </h2>
            <div className="flex gap-2.5">
              <div className="w-3 h-3 rounded-full bg-[#ff5f56]"></div>
              <div className="w-3 h-3 rounded-full bg-[#ffbd2e]"></div>
              <div className="w-3 h-3 rounded-full bg-[#27c93f]"></div>
            </div>
          </div>

          <div className="flex-1 overflow-hidden bg-gradient-to-b from-white to-gray-50/30">
            <div
              ref={chatContainerRef}
              onScroll={onScroll}
              className="h-full overflow-y-auto p-12 space-y-4"
            >
              {loading ? (
                <div className="text-center text-gray-500 mt-8">
                  <p className="text-lg font-medium">메시지를 불러오는 중...</p>
                </div>
              ) : Array.isArray(visibleMessages) && visibleMessages.length === 0 ? (
                <div className="text-center text-gray-500 mt-8">
                  <p className="text-lg font-medium">메시지가 없습니다.</p>
                  <p className="text-sm mt-2">대화를 시작해보세요.</p>
                </div>
              ) : null}

              {Array.isArray(visibleMessages) &&
                visibleMessages.map((msg) => (
                  <div key={msg.id} className={`flex ${msg.senderId === currentUserId ? "justify-end" : "justify-start"}`}>
                    <div className={`max-w-xs lg:max-w-md ${msg.senderId !== currentUserId ? "flex flex-col" : ""}`}>
                      {/* 그룹 채팅: 발신자 표시(목업에서는 senderId로 표시) */}
                      {chatRoomInfo?.isGroup && msg.senderId !== currentUserId && (
                        <div className="text-xs text-gray-500 mb-1 px-2 font-semibold">
                          {msg.senderNickname || msg.senderId}
                        </div>
                      )}

                      <div
                        className={`px-5 py-3 rounded-xl shadow-sm ${
                          msg.senderId === currentUserId ? "bg-blue-600 text-white" : "bg-white text-gray-800 border-2 border-gray-200"
                        }`}
                      >
                        {msg.isTicketPreview ? (
                          <div
                            onClick={() => handleTicketPreviewClick(msg.ticketId)}
                            className="cursor-pointer hover:opacity-80 transition-opacity"
                          >
                            <div className="font-bold mb-1 text-base">🎫 티켓 미리보기</div>
                            <div className={`text-sm ${msg.senderId === currentUserId ? "opacity-90" : "text-gray-600"}`}>
                              클릭하여 티켓 정보 확인
                            </div>
                          </div>
                        ) : (
                          <div className="text-base leading-relaxed">{msg.content}</div>
                        )}

                        <div className={`text-xs mt-2 ${msg.senderId === currentUserId ? "text-blue-100" : "text-gray-500"}`}>
                          {new Date(msg.createdAt).toLocaleTimeString("ko-KR", {
                            hour: "2-digit",
                            minute: "2-digit",
                          })}
                          {msg.senderId !== currentUserId && msg.isRead === false && (
                            <span className="ml-2 text-red-500 font-bold">●</span>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                ))}

              <div ref={messagesEndRef} />
            </div>
          </div>
        </div>
      </div>

      {/* Input */}
      <div className="shrink-0 max-w-7xl mx-auto w-full px-8 pb-8">
        <div className="bg-white px-10 py-8 flex gap-4 border-t border-gray-100/60 rounded-b-[2.5rem]">
          <input
            type="text"
            value={inputMessage}
            onChange={(e) => setInputMessage(e.target.value)}
            onKeyPress={handleKeyPress}
            placeholder="메시지를 입력하세요..."
            className="flex-1 px-6 py-3.5 border-2 border-gray-300 rounded-2xl focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 font-medium"
            disabled={!connected}
          />
          {/* AI 메시지 처리 토글 버튼 */}
          <button
            type="button"
            onClick={() => setAiEnabled(!aiEnabled)}
            className={`px-6 py-3.5 rounded-2xl font-black text-xs uppercase tracking-[0.2em] transition-all duration-300 ${
              aiEnabled
                ? "bg-blue-600 text-white hover:bg-blue-700 shadow-md"
                : "bg-gray-100 text-gray-600 hover:bg-gray-200"
            }`}
            title={aiEnabled ? "AI 메시지 처리 ON" : "AI 메시지 처리 OFF"}
          >
            AI {aiEnabled ? "ON" : "OFF"}
          </button>
          <button
            onClick={handleSendMessage}
            disabled={!connected || !inputMessage.trim()}
            className="bg-[#111827] text-white px-10 py-3.5 rounded-2xl font-black text-xs uppercase tracking-[0.2em] hover:bg-blue-600 hover:-translate-y-1 disabled:bg-gray-400 disabled:cursor-not-allowed disabled:hover:translate-y-0 transition-all duration-300 shadow-xl shadow-gray-200"
          >
            전송
          </button>
          <button
            onClick={() => navigate("/chat")}
            className="bg-gray-100 text-gray-400 px-10 py-3.5 rounded-2xl font-black text-xs uppercase tracking-[0.2em] hover:bg-gray-200 hover:text-gray-600 transition-all duration-300"
          >
            목록으로
          </button>
        </div>
      </div>

      {/* 사용자 초대 모달 */}
      {chatRoomInfo?.isGroup && (
        <MemberPickerModal
          open={showInviteModal}
          title="사용자 초대"
          multi={true}
          keyword={searchKeyword}
          onChangeKeyword={setSearchKeyword}
          results={searchResults}
          selected={selectedUsers}
          onToggle={toggleUserSelection}
          loading={searchLoading}
          error={searchError}
          onClose={() => {
            setShowInviteModal(false);
            setSearchKeyword("");
            setSelectedUsers([]);
            setSelectedDepartment("");
          }}
          selectedDepartment={selectedDepartment}
          onChangeDepartment={setSelectedDepartment}
          onConfirm={handleInviteUsers}
          showGroupName={false}
          groupName=""
          onChangeGroupName={() => {}}
        />
      )}

      {/* 티켓 생성 확인 모달 */}
      <TicketConfirmModal
        isOpen={isConfirmModalOpen}
        onConfirm={handleConfirmTicket}
        onCancel={closeConfirmModal}
      />

      {/* 티켓 작성 모달 */}
      {isTicketModalOpen && <AIChatWidget onClose={closeTicketModal} />}
    </div>
  );
};

export default ChatRoom;
