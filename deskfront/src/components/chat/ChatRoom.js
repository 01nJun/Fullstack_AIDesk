import React, { useEffect, useMemo, useRef, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useSelector } from "react-redux";
import useInfiniteChat from "../../hooks/useInfiniteChat";
import MemberPickerModal from "./MemberPickerModal";
import TicketConfirmModal from "./TicketConfirmModal";
import AIChatWidget from "../menu/AIChatWidget";
import TicketDetailModal from "../ticket/TicketDetailModal";
import { searchMembers } from "../../api/memberApi";
import { 
  getMessages, 
  sendMessageRest, 
  markRead, 
  leaveRoom, 
  inviteUsers,
  uploadChatFile,
  downloadChatFile,
  getChatFileViewUrl
} from "../../api/chatApi";
import chatWsClient from "../../api/chatWs";

// 이미지 프리뷰 컴포넌트 (최적화: 직접 URL 사용)
const ImagePreviewComponent = ({ file, senderId, currentUserId, onImageClick }) => {
  const [error, setError] = useState(false);
  const imageUrl = getChatFileViewUrl(file.uuid);

  const handleImageError = () => {
    setError(true);
  };

  const handleImageClick = () => {
    // 클릭 시 원본 이미지 URL 전달
    onImageClick(imageUrl);
  };

  if (error) {
    return (
      <div className={`text-xs ${senderId === currentUserId ? "opacity-80" : "text-baseMuted"}`}>
        {file.fileName}
      </div>
    );
  }

  return (
    <div className="max-w-full">
      <img
        src={imageUrl}
        alt={file.fileName}
        className="max-w-full max-h-64 rounded-lg cursor-pointer hover:opacity-90 transition-opacity object-contain"
        onError={handleImageError}
        onClick={handleImageClick}
        loading="lazy"
      />
    </div>
  );
};

const ChatRoom = ({ chatRoomId, currentUserId, otherUserId, chatRoomInfo }) => {
  const navigate = useNavigate();
  const loginInfo = useSelector((state) => state.loginSlice);

  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState("");
  const [connected, setConnected] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false); // 이전 메시지 로딩 중
  const [hasMore, setHasMore] = useState(true);
  const [currentPage, setCurrentPage] = useState(1);
  const currentPageRef = useRef(1); // currentPage의 최신 값을 추적
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

  // 이미지 모달 관련 상태
  const [selectedImageUrl, setSelectedImageUrl] = useState(null);
  const [isImageModalOpen, setIsImageModalOpen] = useState(false);

  // 파일 업로드 관련 상태
  const [uploadingFiles, setUploadingFiles] = useState([]);
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef(null);

  const messagesEndRef = useRef(null);
  const chatContainerRef = useRef(null);
  const lastMessageIdRef = useRef(null); // 마지막 메시지 ID 추적 (새 메시지 감지용)

  // 무한 스크롤 훅
  const { visibleMessages, onScroll: infiniteChatOnScroll, scrollToBottom, setContainerRef, reset } = useInfiniteChat(messages, 30);

  // 컨테이너 ref 설정
  useEffect(() => {
    setContainerRef(chatContainerRef.current);
  }, [setContainerRef]);

  // 방 변경 시 초기화
  useEffect(() => {
    reset();
    setMessages([]);
    setHasMore(true);
    setCurrentPage(1);
    currentPageRef.current = 1;
  }, [chatRoomId, reset]);

  // currentPage ref 동기화
  useEffect(() => {
    currentPageRef.current = currentPage;
  }, [currentPage]);

  // 이전 메시지 로드 함수
  const loadPreviousMessages = useCallback(async () => {
    if (!chatRoomId || !currentUserId || loadingMore || !hasMore) return;

    setLoadingMore(true);
    try {
      const nextPage = currentPageRef.current + 1;
      const response = await getMessages(chatRoomId, { page: nextPage, size: pageSize });
      
      if (!response.dtoList || response.dtoList.length === 0) {
        setHasMore(false);
        setLoadingMore(false);
        return;
      }

      // 백엔드 응답을 프론트엔드 형식으로 변환 (최신순이므로 reverse)
      const transformedMessages = response.dtoList
        .reverse()
        .map((msg) => {
          const isTicketPreview = msg.messageType === "TICKET_PREVIEW" || 
                                  String(msg.messageType).toUpperCase() === "TICKET_PREVIEW";
          return {
            id: msg.id,
            chatRoomId: msg.chatRoomId,
            senderId: msg.senderId,
            senderNickname: msg.senderNickname || msg.senderId,
            receiverId: chatRoomInfo?.isGroup ? null : (msg.senderId === currentUserId ? otherUserId : currentUserId),
            content: msg.content,
            createdAt: msg.createdAt,
            isRead: msg.senderId === currentUserId ? (msg.unreadCount === 0) : true, // 내 메시지는 unreadCount로 판단
            isTicketPreview: isTicketPreview,
            ticketId: msg.ticketId,
            messageSeq: msg.messageSeq,
            unreadCount: msg.unreadCount, // 추가
            files: msg.files || [], // 파일 정보 추가
            messageType: msg.messageType, // 메시지 타입 추가
          };
        });

      // 스크롤 위치 보정을 위해 현재 스크롤 위치 저장
      const container = chatContainerRef.current;
      const prevScrollHeight = container ? container.scrollHeight : 0;
      const prevScrollTop = container ? container.scrollTop : 0;

      // 기존 메시지 앞에 추가
      setMessages((prev) => [...transformedMessages, ...prev]);
      setCurrentPage(nextPage);
      setHasMore(response.totalCount > (nextPage * pageSize));

      // 스크롤 위치 보정
      if (container) {
        requestAnimationFrame(() => {
          const newScrollHeight = container.scrollHeight;
          const heightDiff = newScrollHeight - prevScrollHeight;
          container.scrollTop = prevScrollTop + heightDiff;
        });
      }
    } catch (err) {
      console.error("이전 메시지 로드 실패:", err);
    } finally {
      setLoadingMore(false);
    }
  }, [chatRoomId, currentUserId, loadingMore, hasMore, chatRoomInfo, otherUserId, pageSize]);

  // 커스텀 스크롤 핸들러 (무한 스크롤 + 이전 메시지 로드)
  const handleScroll = (e) => {
    const el = e.target;
    if (!el) return;

    // useInfiniteChat의 스크롤 핸들러 호출
    infiniteChatOnScroll(e);

    // 스크롤이 최상단에 가까우면 이전 메시지 로드
    if (el.scrollTop < 100 && hasMore && !loadingMore && !loading) {
      loadPreviousMessages();
    }
  };

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
          .map((msg) => {
            const isTicketPreview = msg.messageType === "TICKET_PREVIEW" || 
                                    String(msg.messageType).toUpperCase() === "TICKET_PREVIEW";
            return {
              id: msg.id,
              chatRoomId: msg.chatRoomId,
              senderId: msg.senderId,
              senderNickname: msg.senderNickname || msg.senderId,
              receiverId: chatRoomInfo?.isGroup ? null : (msg.senderId === currentUserId ? otherUserId : currentUserId),
              content: msg.content,
              createdAt: msg.createdAt,
              isRead: msg.isRead != null ? msg.isRead : (msg.senderId === currentUserId ? (msg.unreadCount === 0) : true), // 서버에서 받은 isRead 우선 사용
              isTicketPreview: isTicketPreview,
              ticketId: msg.ticketId,
              messageSeq: msg.messageSeq,
              unreadCount: msg.unreadCount, // 추가
              files: msg.files || [], // 파일 정보 추가
              messageType: msg.messageType, // 메시지 타입 추가
            };
          });
        setMessages(transformedMessages);
        setHasMore(response.totalCount > transformedMessages.length);
        setCurrentPage(1);
        currentPageRef.current = 1;

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
        // 티켓 미리보기 메시지 확인
        const isTicketPreview = newMessage.messageType === "TICKET_PREVIEW" || 
                                String(newMessage.messageType).toUpperCase() === "TICKET_PREVIEW";
        
        // 백엔드 응답을 프론트엔드 형식으로 변환
        const transformedMessage = {
          id: newMessage.id,
          chatRoomId: newMessage.chatRoomId,
          senderId: newMessage.senderId,
          senderNickname: newMessage.senderNickname || newMessage.senderId,
          receiverId: chatRoomInfo?.isGroup ? null : (newMessage.senderId === currentUserId ? otherUserId : currentUserId),
          content: newMessage.content,
          createdAt: newMessage.createdAt,
          isRead: newMessage.senderId === currentUserId ? (newMessage.unreadCount === 0) : true, // 내가 보낸 메시지는 unreadCount로 판단
          isTicketPreview: isTicketPreview,
          ticketId: newMessage.ticketId,
          messageSeq: newMessage.messageSeq,
          unreadCount: newMessage.unreadCount, // 추가
          files: newMessage.files || [], // 파일 정보 추가
          messageType: newMessage.messageType, // 메시지 타입 추가
        };

        // 티켓 트리거만 있고 실제 메시지가 없는 경우(id가 null) 메시지 목록에 추가하지 않음
        if (newMessage.ticketTrigger && !newMessage.id) {
          // 티켓 생성 문맥 감지 시 확인 모달 띄우기
          openConfirmModal();
          return;
        }

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

  // ✅ 새 메시지 추가 시 맨 아래로 스크롤 (이전 메시지 로드 시에는 제외)
  // useInfiniteChat 훅에서 이미 처리하므로 여기서는 제거
  // 단, WebSocket으로 새 메시지가 왔을 때는 useInfiniteChat이 감지하지 못할 수 있으므로
  // 마지막 메시지 ID 변경을 추적하여 처리
  useEffect(() => {
    // 이전 메시지 로드 중이면 스크롤하지 않음
    if (loadingMore) return;

    // 메시지가 없으면 스크롤하지 않음
    if (messages.length === 0) {
      lastMessageIdRef.current = null;
      return;
    }

    const lastMessage = messages[messages.length - 1];
    const lastMessageId = lastMessage?.id;

    // 마지막 메시지 ID가 변경되었을 때만 스크롤 (새 메시지가 뒤에 추가된 경우)
    if (lastMessageIdRef.current !== null && lastMessageIdRef.current !== lastMessageId) {
      // 새 메시지가 추가된 경우에만 스크롤
      scrollToBottom();
    } else if (lastMessageIdRef.current === null && messages.length > 0) {
      // 초기 로드 시
      scrollToBottom();
    }

    lastMessageIdRef.current = lastMessageId;
  }, [messages, loadingMore, scrollToBottom]);

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
        
        // 티켓 미리보기 메시지 확인
        const isTicketPreview = newMessage.messageType === "TICKET_PREVIEW" || 
                                String(newMessage.messageType).toUpperCase() === "TICKET_PREVIEW";
        
        // 백엔드 응답을 프론트엔드 형식으로 변환
        const transformedMessage = {
          id: newMessage.id,
          chatRoomId: newMessage.chatRoomId,
          senderId: newMessage.senderId,
          senderNickname: newMessage.senderNickname || newMessage.senderId,
          receiverId: chatRoomInfo?.isGroup ? null : (newMessage.senderId === currentUserId ? otherUserId : currentUserId),
          content: newMessage.content,
          createdAt: newMessage.createdAt,
          isRead: newMessage.senderId === currentUserId ? (newMessage.unreadCount === 0) : true, // 내가 보낸 메시지는 unreadCount로 판단
          isTicketPreview: isTicketPreview,
          ticketId: newMessage.ticketId,
          messageSeq: newMessage.messageSeq,
          unreadCount: newMessage.unreadCount, // 추가
        };

        // 티켓 트리거만 있고 실제 메시지가 없는 경우(id가 null) 메시지 목록에 추가하지 않음
        if (newMessage.ticketTrigger && !newMessage.id) {
          // 티켓 생성 문맥 감지 시 확인 모달 띄우기
          openConfirmModal();
          return;
        }

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

  // 파일 선택 핸들러
  const handleFileSelect = async (files) => {
    if (!files || files.length === 0) return;

    const fileArray = Array.from(files);
    for (const file of fileArray) {
      await handleFileUpload(file);
    }
  };

  // 파일 업로드 및 메시지 전송
  const handleFileUpload = async (file) => {
    if (!file) return;

    const fileId = Date.now() + Math.random();
    setUploadingFiles((prev) => [...prev, { id: fileId, name: file.name }]);

    try {
      // 1. 파일 업로드 (TEMP 상태)
      const fileDTO = await uploadChatFile(chatRoomId, file);

      // 2. 파일 메시지 전송 (바인딩)
      const newMessage = await sendMessageRest(chatRoomId, {
        content: file.name, // 파일명
        messageType: "FILE",
        fileUuids: [fileDTO.uuid],
      });

      // 메시지 목록에 추가
      const transformedMessage = {
        id: newMessage.id,
        chatRoomId: newMessage.chatRoomId,
        senderId: newMessage.senderId,
        senderNickname: newMessage.senderNickname || newMessage.senderId,
        receiverId: chatRoomInfo?.isGroup 
          ? null 
          : (newMessage.senderId === currentUserId ? otherUserId : currentUserId),
        content: newMessage.content,
        createdAt: newMessage.createdAt,
        isRead: newMessage.senderId === currentUserId 
          ? (newMessage.unreadCount === 0) 
          : true,
        isTicketPreview: false,
        ticketId: null,
        messageSeq: newMessage.messageSeq,
        unreadCount: newMessage.unreadCount,
        files: newMessage.files || [],
        messageType: newMessage.messageType,
      };

      setMessages((prev) => [...prev, transformedMessage]);
    } catch (err) {
      console.error("파일 업로드 실패:", err);
      alert("파일 업로드에 실패했습니다: " + (err.response?.data?.message || err.message));
    } finally {
      setUploadingFiles((prev) => prev.filter((f) => f.id !== fileId));
    }
  };

  // Drag & Drop 핸들러
  const handleDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragOver(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragOver(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragOver(false);

    const files = e.dataTransfer.files;
    if (files && files.length > 0) {
      handleFileSelect(files);
    }
  };

  const handleFileButtonClick = () => {
    fileInputRef.current?.click();
  };

  // 파일 크기 포맷팅
  const formatFileSize = (bytes) => {
    if (!bytes) return "0 B";
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
    return (bytes / (1024 * 1024)).toFixed(1) + " MB";
  };

  // 이미지인지 확인
  const isImageFile = (mimeType) => {
    return mimeType && mimeType.startsWith("image/");
  };

  // 파일 다운로드 핸들러
  const handleFileDownload = async (file) => {
    try {
      await downloadChatFile(file.uuid, file.fileName);
    } catch (err) {
      console.error("파일 다운로드 실패:", err);
      alert("파일 다운로드에 실패했습니다.");
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

  // 1:1 채팅의 경우 상대방 이름 가져오기
  const otherUserInfo = chatRoomInfo?.participantInfo?.find(
    (p) => p.email === otherUserId
  );
  const otherUserName = otherUserInfo?.nickname || otherUserId || "채팅";

  const chatRoomName = chatRoomInfo?.isGroup
    ? chatRoomInfo.name || "그룹 채팅"
    : otherUserName;

  return (
    <div 
      className="h-[calc(100vh-120px)] lg:h-[calc(100vh-160px)] overflow-hidden flex flex-col bg-baseBg"
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      {/* Header */}
      <div className="shrink-0 w-full px-4 lg:px-6 py-4 lg:py-6 border-b border-baseBorder bg-baseBg">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <div className="flex-1 min-w-0">
            <div className="text-xs uppercase tracking-widest text-baseMuted mb-1">
              {chatRoomInfo?.isGroup ? "그룹 채팅" : "1:1 채팅"}
            </div>
            <h1 className="text-xl lg:text-2xl font-semibold text-baseText truncate">
              {chatRoomName}
            </h1>
            <div className="flex items-center gap-3 mt-2 flex-wrap">
              {chatRoomInfo?.isGroup && Array.isArray(chatRoomInfo?.participantInfo) && (
                <>
                  <span className="text-xs text-baseMuted">
                    {chatRoomInfo.participantInfo.map((p) => p.nickname || p.email).join(", ")}
                  </span>
                  <span className="text-xs text-baseMuted">
                    참여자 {chatRoomInfo.participantInfo.length}명
                  </span>
                </>
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
                className="bg-white border border-baseBorder text-baseText px-4 py-2 rounded-ui font-semibold text-sm hover:border-brandNavy transition-all shadow-ui focus:outline-none focus:ring-2 focus:ring-brandNavy focus:ring-offset-2"
              >
                초대
              </button>
            )}
            <button
              onClick={handleLeaveRoom}
              className="bg-white border border-baseBorder text-baseText px-4 py-2 rounded-ui font-semibold text-sm hover:border-brandOrange transition-all shadow-ui focus:outline-none focus:ring-2 focus:ring-brandOrange focus:ring-offset-2"
            >
              나가기
            </button>
          </div>
        </div>
      </div>

      {/* Messages (scroll) */}
      <div className="flex-1 overflow-hidden w-full">
        <div className={`h-full bg-baseSurface overflow-hidden flex flex-col ${dragOver ? "ring-2 ring-brandNavy" : ""}`}>
          <div
            ref={chatContainerRef}
            onScroll={handleScroll}
            className="h-full overflow-y-auto px-4 lg:px-6 py-4 lg:py-6 space-y-3"
          >
            {loading ? (
              <div className="text-center text-baseMuted mt-8">
                <p className="text-base font-medium">메시지를 불러오는 중...</p>
              </div>
            ) : Array.isArray(visibleMessages) && visibleMessages.length === 0 ? (
              <div className="text-center text-baseMuted mt-8">
                <p className="text-base font-medium">메시지가 없습니다.</p>
                <p className="text-sm mt-2">대화를 시작해보세요.</p>
              </div>
            ) : null}

            {loadingMore && (
              <div className="text-center text-baseMuted py-4">
                <p className="text-sm">이전 메시지를 불러오는 중...</p>
              </div>
            )}

            {Array.isArray(visibleMessages) &&
              visibleMessages.map((msg) => (
                <div key={msg.id} className={`flex ${msg.senderId === currentUserId ? "justify-end" : "justify-start"}`}>
                  <div className={`max-w-[75%] sm:max-w-md min-w-0 ${msg.senderId !== currentUserId ? "flex flex-col" : ""}`}>
                    {/* 그룹 채팅: 발신자 표시 */}
                    {chatRoomInfo?.isGroup && msg.senderId !== currentUserId && (
                      <div className="text-xs text-baseMuted mb-1 px-2 font-medium">
                        {msg.senderNickname || msg.senderId}
                      </div>
                    )}

                    {/* 메시지 컨테이너 - relative로 배지 위치 지정 */}
                    <div className="relative inline-block max-w-full">
                      {(() => {
                        // 이미지 파일만 있는지 확인
                        const isImageOnlyMessage = msg.messageType === "FILE" && 
                          msg.files && 
                          msg.files.length > 0 && 
                          msg.files.every(file => isImageFile(file.mimeType));
                        
                        // 이미지만 있는 경우 배경 없이 표시
                        if (isImageOnlyMessage) {
                          return (
                            <>
                              <div className="space-y-2">
                                {msg.files.map((file) => (
                                  <div key={file.uuid} className="max-w-full">
                                    <ImagePreviewComponent 
                                      file={file}
                                      senderId={msg.senderId}
                                      currentUserId={currentUserId}
                                      onImageClick={(url) => {
                                        setSelectedImageUrl(url);
                                        setIsImageModalOpen(true);
                                      }}
                                    />
                                  </div>
                                ))}
                              </div>
                              <div className={`text-xs mt-1.5 flex items-center gap-1.5 ${msg.senderId === currentUserId ? "text-baseMuted" : "text-baseMuted"}`}>
                                <span>
                                  {new Date(msg.createdAt).toLocaleTimeString("ko-KR", {
                                    hour: "2-digit",
                                    minute: "2-digit",
                                  })}
                                </span>
                              </div>
                            </>
                          );
                        }
                        
                        // 일반 메시지 또는 파일이 포함된 경우 기존 스타일 적용
                        return (
                          <div
                            className={`px-4 py-2.5 rounded-ui ${
                              msg.senderId === currentUserId
                                ? "bg-brandNavy text-white"
                                : "bg-baseBg text-baseText border border-baseBorder"
                            }`}
                          >
                            {msg.messageType === "FILE" && msg.files && msg.files.length > 0 ? (
                              // 파일 메시지 렌더링
                              <div className="space-y-2">
                                {msg.files.map((file) => (
                                  <div key={file.uuid} className="max-w-full">
                                    {isImageFile(file.mimeType) ? (
                                      // 이미지 파일: 프리뷰
                                      <ImagePreviewComponent 
                                        file={file}
                                        senderId={msg.senderId}
                                        currentUserId={currentUserId}
                                        onImageClick={(url) => {
                                          setSelectedImageUrl(url);
                                          setIsImageModalOpen(true);
                                        }}
                                      />
                                    ) : (
                                      // 일반 파일: 아이콘 + 다운로드
                                      <div className="flex items-center gap-2 max-w-full">
                                        <div className="text-2xl shrink-0">📎</div>
                                        <div className="flex-1 min-w-0">
                                          <div className={`text-sm font-medium break-words ${msg.senderId === currentUserId ? "text-white" : "text-baseText"}`}>
                                            {file.fileName}
                                          </div>
                                          <div className={`text-xs ${msg.senderId === currentUserId ? "opacity-80" : "text-baseMuted"}`}>
                                            {formatFileSize(file.fileSize)}
                                          </div>
                                        </div>
                                        <button
                                          onClick={() => handleFileDownload(file)}
                                          className={`px-3 py-1 rounded text-xs transition-colors whitespace-nowrap shrink-0 ${
                                            msg.senderId === currentUserId 
                                              ? "bg-white/20 hover:bg-white/30 text-white" 
                                              : "bg-baseSurface hover:bg-baseBorder text-baseText"
                                          }`}
                                        >
                                          다운로드
                                        </button>
                                      </div>
                                    )}
                                  </div>
                                ))}
                              </div>
                            ) : msg.isTicketPreview ? (
                          <div
                            onClick={() => handleTicketPreviewClick(msg.ticketId)}
                            className="cursor-pointer hover:opacity-80 transition-opacity"
                          >
                            <div className={`font-semibold mb-1 text-sm ${msg.senderId === currentUserId ? "text-white" : "text-baseText"}`}>
                              🎫 티켓 미리보기
                            </div>
                            <div className={`text-xs ${msg.senderId === currentUserId ? "opacity-90" : "text-baseMuted"}`}>
                              클릭하여 티켓 정보 확인
                            </div>
                          </div>
                        ) : (
                          <div className="text-sm leading-relaxed whitespace-pre-wrap break-words max-w-full">{msg.content}</div>
                        )}

                        <div className={`text-xs mt-1.5 flex items-center gap-1.5 ${msg.senderId === currentUserId ? "text-white/80" : "text-baseMuted"}`}>
                          <span>
                            {new Date(msg.createdAt).toLocaleTimeString("ko-KR", {
                              hour: "2-digit",
                              minute: "2-digit",
                            })}
                          </span>
                        </div>
                      </div>
                        );
                      })()}

                      {/* 보낸 사람(내가 보낸 메시지): 좌측 하단에 안 읽은 사람 수 표시 */}
                      {msg.senderId === currentUserId && 
                       msg.unreadCount != null && 
                       msg.unreadCount > 0 && (
                        <span className="absolute -left-3 bottom-0 text-brandNavy text-xs font-semibold">
                          {msg.unreadCount}
                        </span>
                      )}

                      {/* 받은 사람(상대방이 보낸 메시지): 우측 하단에 읽지 않았으면 표시 */}
                      {msg.senderId !== currentUserId && 
                       msg.isRead === false && (
                        <span className="absolute -right-3 bottom-0 text-brandNavy text-xs font-semibold">
                          1
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              ))}

            <div ref={messagesEndRef} />
          </div>
        </div>
      </div>

      {/* Input */}
      <div className="shrink-0 w-full px-4 lg:px-6 py-4 border-t border-baseBorder bg-baseBg">
        {/* 업로드 중 표시 */}
        {uploadingFiles.length > 0 && (
          <div className="mb-2 text-xs text-baseMuted">
            업로드 중: {uploadingFiles.map((f) => f.name).join(", ")}
          </div>
        )}

        <div className="flex flex-col sm:flex-row gap-2">
          <div className="flex-1 flex gap-2">
            {/* 파일 선택 버튼 */}
            <button
              type="button"
              onClick={handleFileButtonClick}
              className="px-4 py-2.5 border border-baseBorder rounded-ui bg-white text-baseText hover:border-brandNavy transition-all shadow-ui focus:outline-none focus:ring-2 focus:ring-brandNavy focus:ring-offset-2"
              title="파일 첨부"
            >
              📎
            </button>
            <input
              type="file"
              ref={fileInputRef}
              onChange={(e) => {
                handleFileSelect(e.target.files);
                e.target.value = ""; // 같은 파일 재선택 가능하도록
              }}
              className="hidden"
              multiple
            />

            <input
              type="text"
              value={inputMessage}
              onChange={(e) => setInputMessage(e.target.value)}
              onKeyPress={handleKeyPress}
              placeholder="메시지를 입력하세요..."
              className="flex-1 px-4 py-2.5 border border-baseBorder rounded-ui bg-baseBg text-baseText placeholder-baseMuted focus:outline-none focus:ring-2 focus:ring-brandNavy focus:border-brandNavy text-sm"
              disabled={!connected}
            />
            {/* AI 메시지 처리 토글 버튼 */}
            <button
              type="button"
              onClick={() => setAiEnabled(!aiEnabled)}
              className={`px-4 py-2.5 rounded-ui font-semibold text-xs transition-all ${
                aiEnabled
                  ? "bg-brandNavy text-white hover:opacity-90 shadow-ui"
                  : "bg-white border border-baseBorder text-baseText hover:border-brandNavy shadow-ui"
              } focus:outline-none focus:ring-2 focus:ring-brandNavy focus:ring-offset-2`}
              title={aiEnabled ? "AI 메시지 처리 ON" : "AI 메시지 처리 OFF"}
            >
              AI {aiEnabled ? "ON" : "OFF"}
            </button>
          </div>
          <div className="flex gap-2">
            <button
              onClick={handleSendMessage}
              disabled={!connected || !inputMessage.trim()}
              className="bg-brandNavy text-white px-6 py-2.5 rounded-ui font-semibold text-sm hover:opacity-90 disabled:bg-baseMuted disabled:cursor-not-allowed transition-all shadow-ui focus:outline-none focus:ring-2 focus:ring-brandNavy focus:ring-offset-2 disabled:opacity-50"
            >
              전송
            </button>
            <button
              onClick={() => navigate("/chat")}
              className="bg-white border border-baseBorder text-baseText px-4 py-2.5 rounded-ui font-semibold text-sm hover:border-brandNavy transition-all shadow-ui focus:outline-none focus:ring-2 focus:ring-brandNavy focus:ring-offset-2"
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

      {/* 이미지 모달 */}
      {isImageModalOpen && selectedImageUrl && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-75 p-4"
          onClick={() => {
            setIsImageModalOpen(false);
            setSelectedImageUrl(null);
          }}
        >
          <div
            className="relative max-w-full max-h-full"
            onClick={(e) => e.stopPropagation()}
          >
            <button
              onClick={() => {
                setIsImageModalOpen(false);
                setSelectedImageUrl(null);
              }}
              className="absolute -top-10 right-0 text-white text-2xl font-bold hover:opacity-70 transition-opacity"
              aria-label="닫기"
            >
              ×
            </button>
            <img
              src={selectedImageUrl}
              alt="원본 이미지"
              className="max-w-full max-h-[90vh] object-contain rounded-lg"
            />
          </div>
        </div>
      )}
    </div>
  );
};

export default ChatRoom;
