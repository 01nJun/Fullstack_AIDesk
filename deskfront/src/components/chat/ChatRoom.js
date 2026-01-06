import React, { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useSelector } from "react-redux";
import useInfiniteChat from "../../hooks/useInfiniteChat";
import MemberPickerModal from "./MemberPickerModal";
import TicketConfirmModal from "./TicketConfirmModal";
import AIChatWidget from "../menu/AIChatWidget";
import TicketDetailModal from "../ticket/TicketDetailModal";
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
  
  // 티켓 상세 모달 관련 상태
  const [selectedTicketId, setSelectedTicketId] = useState(null);
  const [isTicketDetailModalOpen, setIsTicketDetailModalOpen] = useState(false);

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

  // ✅ 티켓 미리보기 클릭 - 티켓 상세 모달 띄우기
  const handleTicketPreviewClick = (ticketId) => {
    if (ticketId) {
      setSelectedTicketId(ticketId);
      setIsTicketDetailModalOpen(true);
    }
  };
  
  // ✅ 티켓 상세 모달 닫기
  const handleCloseTicketDetailModal = () => {
    setIsTicketDetailModalOpen(false);
    setSelectedTicketId(null);
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
    <div className="h-[calc(100vh-120px)] lg:h-[calc(100vh-160px)] overflow-hidden flex flex-col bg-chatBg">
      {/* Header */}
      <div className="shrink-0 w-full px-4 lg:px-6 py-4 lg:py-6 border-b border-chatBorder bg-chatBg">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div className="flex-1 min-w-0">
            <div className="text-xs uppercase tracking-widest text-chatMuted mb-1">
              {chatRoomInfo?.isGroup ? "그룹 채팅" : "1:1 채팅"}
            </div>
            <h1 className="text-xl lg:text-2xl font-semibold text-chatText truncate">
              {chatRoomName}
            </h1>
            <div className="flex items-center gap-3 mt-2">
              {chatRoomInfo?.isGroup && Array.isArray(chatRoomInfo?.participantIds) && (
                <span className="text-xs text-chatMuted">
                  참여자 {chatRoomInfo.participantIds.length}명
                </span>
              )}
              {!chatRoomInfo?.isGroup && (
                <span className="text-xs text-chatMuted">
                  {otherUserId || "알 수 없음"}
                </span>
              )}
              <div className={`text-xs flex items-center gap-1 ${connected ? "ui-status-connected" : "ui-status-disconnected"}`}>
                <span className="w-1.5 h-1.5 rounded-full bg-current"></span>
                {connected ? "연결됨" : "연결 끊김"}
              </div>
            </div>
          </div>

          {/* 액션 버튼들 */}
          <div className="flex gap-2 shrink-0">
            {chatRoomInfo?.isGroup && (
              <button
                onClick={handleOpenInviteModal}
                className="bg-white border border-chatBorder text-chatText px-4 py-2 rounded-chat font-semibold text-sm hover:border-chatNavy transition-all shadow-chat focus:outline-none focus:ring-2 focus:ring-chatNavy focus:ring-offset-2"
              >
                초대
              </button>
            )}
            <button
              onClick={handleLeaveRoom}
              className="bg-white border border-chatBorder text-chatText px-4 py-2 rounded-chat font-semibold text-sm hover:border-chatOrange transition-all shadow-chat focus:outline-none focus:ring-2 focus:ring-chatOrange focus:ring-offset-2"
            >
              나가기
            </button>
          </div>
        </div>
      </div>

      {/* Messages (scroll) */}
      <div className="flex-1 overflow-hidden w-full">
        <div className="h-full bg-chatSurface overflow-hidden flex flex-col">
          <div
            ref={chatContainerRef}
            onScroll={onScroll}
            className="h-full overflow-y-auto px-4 lg:px-6 py-4 lg:py-6 space-y-3"
          >
            {loading ? (
              <div className="text-center text-chatMuted mt-8">
                <p className="text-base font-medium">메시지를 불러오는 중...</p>
              </div>
            ) : Array.isArray(visibleMessages) && visibleMessages.length === 0 ? (
              <div className="text-center text-chatMuted mt-8">
                <p className="text-base font-medium">메시지가 없습니다.</p>
                <p className="text-sm mt-2">대화를 시작해보세요.</p>
              </div>
            ) : null}

            {Array.isArray(visibleMessages) &&
              visibleMessages.map((msg) => (
                <div key={msg.id} className={`flex ${msg.senderId === currentUserId ? "justify-end" : "justify-start"}`}>
                  <div className={`max-w-[75%] sm:max-w-md ${msg.senderId !== currentUserId ? "flex flex-col" : ""}`}>
                    {/* 그룹 채팅: 발신자 표시 */}
                    {chatRoomInfo?.isGroup && msg.senderId !== currentUserId && (
                      <div className="text-xs text-chatMuted mb-1 px-2 font-medium">
                        {msg.senderNickname || msg.senderId}
                      </div>
                    )}

                    <div
                      className={`px-4 py-2.5 rounded-chatLg ${
                        msg.senderId === currentUserId
                          ? "bg-chatNavy text-white"
                          : "bg-chatBg text-chatText border border-chatBorder"
                      }`}
                    >
                      {msg.isTicketPreview ? (
                        <div
                          onClick={() => handleTicketPreviewClick(msg.ticketId)}
                          className="cursor-pointer hover:opacity-80 transition-opacity"
                        >
                          <div className={`font-semibold mb-1 text-sm ${msg.senderId === currentUserId ? "text-white" : "text-chatText"}`}>
                            🎫 티켓 미리보기
                          </div>
                          <div className={`text-xs ${msg.senderId === currentUserId ? "opacity-90" : "text-chatMuted"}`}>
                            클릭하여 티켓 정보 확인
                          </div>
                        </div>
                      ) : (
                        <div className="text-sm leading-relaxed whitespace-pre-wrap break-words">{msg.content}</div>
                      )}

                      <div className={`text-xs mt-1.5 flex items-center gap-1.5 ${msg.senderId === currentUserId ? "text-white/80" : "text-chatMuted"}`}>
                        <span>
                          {new Date(msg.createdAt).toLocaleTimeString("ko-KR", {
                            hour: "2-digit",
                            minute: "2-digit",
                          })}
                        </span>
                        {msg.senderId !== currentUserId && msg.isRead === false && (
                          <span className="text-chatOrange">●</span>
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

      {/* Input */}
      <div className="shrink-0 w-full px-4 lg:px-6 py-4 border-t border-chatBorder bg-chatBg">
        <div className="flex flex-col sm:flex-row gap-2">
          <div className="flex-1 flex gap-2">
            <input
              type="text"
              value={inputMessage}
              onChange={(e) => setInputMessage(e.target.value)}
              onKeyPress={handleKeyPress}
              placeholder="메시지를 입력하세요..."
              className="flex-1 px-4 py-2.5 border border-chatBorder rounded-chat bg-chatBg text-chatText placeholder-chatMuted focus:outline-none focus:ring-2 focus:ring-chatNavy focus:border-chatNavy text-sm"
              disabled={!connected}
            />
            {/* AI 메시지 처리 토글 버튼 */}
            <button
              type="button"
              onClick={() => setAiEnabled(!aiEnabled)}
              className={`px-4 py-2.5 rounded-chat font-semibold text-xs transition-all ${
                aiEnabled
                  ? "bg-chatNavy text-white hover:opacity-90 shadow-chat"
                  : "bg-white border border-chatBorder text-chatText hover:border-chatNavy shadow-chat"
              } focus:outline-none focus:ring-2 focus:ring-chatNavy focus:ring-offset-2`}
              title={aiEnabled ? "AI 메시지 처리 ON" : "AI 메시지 처리 OFF"}
            >
              AI {aiEnabled ? "ON" : "OFF"}
            </button>
          </div>
          <div className="flex gap-2">
            <button
              onClick={handleSendMessage}
              disabled={!connected || !inputMessage.trim()}
              className="bg-chatNavy text-white px-6 py-2.5 rounded-chat font-semibold text-sm hover:opacity-90 disabled:bg-chatMuted disabled:cursor-not-allowed transition-all shadow-chat focus:outline-none focus:ring-2 focus:ring-chatNavy focus:ring-offset-2 disabled:opacity-50"
            >
              전송
            </button>
            <button
              onClick={() => navigate("/chat")}
              className="bg-white border border-chatBorder text-chatText px-4 py-2.5 rounded-chat font-semibold text-sm hover:border-chatNavy transition-all shadow-chat focus:outline-none focus:ring-2 focus:ring-chatNavy focus:ring-offset-2"
            >
              목록
            </button>
          </div>
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
      {isTicketModalOpen && (
        <AIChatWidget 
          onClose={closeTicketModal}
          chatRoomId={chatRoomId}
          currentUserId={currentUserId}
        />
      )}

      {/* 티켓 상세 모달 */}
      {isTicketDetailModalOpen && selectedTicketId && (
        <TicketDetailModal
          tno={selectedTicketId}
          onClose={handleCloseTicketDetailModal}
          onDelete={handleCloseTicketDetailModal}
        />
      )}
    </div>
  );
};

export default ChatRoom;
