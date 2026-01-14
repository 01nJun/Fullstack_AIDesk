import React, { useState, useEffect, useRef, useCallback } from "react";
import { useSelector } from "react-redux";
import { getChatRooms, createOrGetDirectRoom, createGroupRoom, getMessages, markRead, leaveRoom, inviteUsers, sendMessageRest, sendMessageWithFilesRest } from "../../api/chatApi";
import { searchMembers } from "../../api/memberApi";
import chatWsClient from "../../api/chatWs";
import FilePreview from "../common/FilePreview";
import { downloadFile } from "../../api/fileApi";
import AiWarningModal from "../chat/AiWarningModal";
import AiForceModal from "../chat/AiForceModal";
import TicketConfirmModal from "../chat/TicketConfirmModal";
import TicketDetailModal from "../ticket/TicketDetailModal";
import AIChatWidget from "./AIChatWidget";
import "./AIChatWidget.css";

/**
 * 통합 AI 비서 모달
 * - 좌측: 채팅 메시지 영역
 * - 우측: 대화방 리스트 + 연락처 검색 (개인/그룹 탭)
 * - AI 업무모드 전환 버튼
 */
const AIAssistantModal = ({ onClose }) => {
  const loginInfo = useSelector((state) => state.loginSlice);
  const currentUserId = loginInfo?.email || "";

  // ==================== 우측 패널 상태 ====================
  const [activeTab, setActiveTab] = useState("rooms"); // "rooms" | "search"
  const [roomFilter, setRoomFilter] = useState("all"); // "all" | "direct" | "group"
  const [chatRooms, setChatRooms] = useState([]);
  const [roomsLoading, setRoomsLoading] = useState(true);
  const [selectedRoomId, setSelectedRoomId] = useState(null);
  const [selectedRoomInfo, setSelectedRoomInfo] = useState(null);

  // ==================== 알림(뮤트) / 컨텍스트 메뉴 ====================
  const [mutedRoomIds, setMutedRoomIds] = useState(() => {
    try {
      const raw = localStorage.getItem("desk.mutedRoomIds");
      const arr = raw ? JSON.parse(raw) : [];
      return Array.isArray(arr) ? arr : [];
    } catch {
      return [];
    }
  });
  const isRoomMuted = useCallback((roomId) => mutedRoomIds.includes(roomId), [mutedRoomIds]);
  useEffect(() => {
    try {
      localStorage.setItem("desk.mutedRoomIds", JSON.stringify(mutedRoomIds));
    } catch {
      // ignore
    }
  }, [mutedRoomIds]);

  const toggleMuteRoom = useCallback((roomId) => {
    setMutedRoomIds((prev) => (prev.includes(roomId) ? prev.filter((id) => id !== roomId) : [...prev, roomId]));
  }, []);

  const [contextMenu, setContextMenu] = useState(null); // { x, y, room }
  useEffect(() => {
    const close = () => setContextMenu(null);
    window.addEventListener("click", close);
    window.addEventListener("contextmenu", close);
    return () => {
      window.removeEventListener("click", close);
      window.removeEventListener("contextmenu", close);
    };
  }, []);

  // 그룹 참여자 목록 모달
  const [showParticipants, setShowParticipants] = useState(false);

  // 연락처 검색 상태
  const [searchKeyword, setSearchKeyword] = useState("");
  const [selectedDepartment, setSelectedDepartment] = useState("");
  const [searchResults, setSearchResults] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);

  // ==================== 좌측 채팅 패널 상태 ====================
  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState("");
  const [selectedFiles, setSelectedFiles] = useState([]);
  const [isDragging, setIsDragging] = useState(false);
  const [connected, setConnected] = useState(false);
  const [messagesLoading, setMessagesLoading] = useState(false);
  const [aiEnabled, setAiEnabled] = useState(false);

  // ==================== 메시지 페이징 (위로 무한스크롤) ====================
  const [msgPage, setMsgPage] = useState(1); // 1부터 시작
  const [msgHasMore, setMsgHasMore] = useState(false);
  const [msgLoadingMore, setMsgLoadingMore] = useState(false);
  const pendingScrollRef = useRef({ mode: null, seq: null }); // mode: "bottom" | "seq"
  const initialScrollDoneRef = useRef(false);

  // 연락처 검색에서 단톡 생성용 선택 상태
  const [selectedContacts, setSelectedContacts] = useState([]); // {email,nickname,department}
  const [groupName, setGroupName] = useState("");

  // 욕설 감지 관련 상태
  const [profanityCount, setProfanityCount] = useState(0);
  const profanityCountRef = useRef(0);
  const profanityTimerRef = useRef(null);
  const [showWarningModal, setShowWarningModal] = useState(false);
  const [showForceModal, setShowForceModal] = useState(false);
  const [warningModalShown, setWarningModalShown] = useState(false);
  const [userChoseOffAfterWarning, setUserChoseOffAfterWarning] = useState(false);
  const [forceOnRemaining, setForceOnRemaining] = useState(0);
  const forceOnTimerRef = useRef(null);
  const [showReleaseToast, setShowReleaseToast] = useState(false);
  const blinkTimeoutRef = useRef(null);
  const handleProfanityDetectedRef = useRef(null);

  // 티켓 모달 상태
  const [isTicketModalOpen, setIsTicketModalOpen] = useState(false);
  const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);
  const [selectedTicketId, setSelectedTicketId] = useState(null);
  const [isTicketDetailModalOpen, setIsTicketDetailModalOpen] = useState(false);

  // AI 업무모드 전환
  const [showAIWorkMode, setShowAIWorkMode] = useState(false);

  const messagesEndRef = useRef(null);
  const chatContainerRef = useRef(null);

  // 부서 목록
  const departments = [
    { value: "DEVELOPMENT", label: "개발" },
    { value: "SALES", label: "영업" },
    { value: "HR", label: "인사" },
    { value: "DESIGN", label: "디자인" },
    { value: "PLANNING", label: "기획" },
    { value: "FINANCE", label: "재무" },
  ];

  const getDepartmentLabel = (dept) => {
    const deptObj = departments.find((d) => d.value === dept);
    return deptObj ? deptObj.label : dept || "";
  };

  const formatTimeHHmm = (dt) => {
    if (!dt) return "";
    try {
      return new Date(dt).toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit" });
    } catch {
      return "";
    }
  };

  // ==================== 파일 프리뷰 라벨 (A안: 첫 파일 기준) ====================
  const getExt = (fileName) => {
    const n = String(fileName || "").trim();
    const idx = n.lastIndexOf(".");
    if (idx < 0) return "";
    const ext = n.slice(idx + 1).toLowerCase();
    return ext.length <= 10 ? ext : "";
  };

  const filePreviewLabel = (fileName) => {
    const ext = getExt(fileName);
    if (!ext) return "파일";

    const img = new Set(["jpg", "jpeg", "png", "gif", "webp", "svg", "bmp", "heic"]);
    const video = new Set(["mp4", "mov", "avi", "mkv", "webm", "wmv", "m4v"]);
    const audio = new Set(["mp3", "wav", "m4a", "aac", "flac", "ogg", "opus"]);

    const kind = img.has(ext) ? "사진" : video.has(ext) ? "동영상" : audio.has(ext) ? "소리" : "파일";
    // ✅ 요청: "사진.jpg" 형태 (콤마 X)
    return `${kind}.${ext}`;
  };

  const looksLikeFilename = (s) => {
    const t = String(s || "").trim();
    if (!t) return false;
    // 공백이 없고, 마지막에 .ext 형태
    if (/\s/.test(t)) return false;
    return /\.[a-zA-Z0-9]{1,10}$/.test(t);
  };

  const derivePreviewText = (msg) => {
    if (!msg) return "";
    const text = (msg.content || "").trim();
    if (text) return text;
    if (Array.isArray(msg.files) && msg.files.length > 0) {
      const fn = msg.files[0]?.fileName || msg.files[0]?.name || "";
      // ✅ 기본은 타입/확장자 라벨, 혹시 인식 실패하면 "첫 파일명"을 그대로 표시(요청한 fallback)
      if (!fn) return "";
      const label = filePreviewLabel(fn);
      return label ? label : fn;
    }
    if (msg.isTicketPreview) return "🎫 요청서";
    return "";
  };

  const bumpRoomPreview = useCallback((roomId, previewText, createdAt) => {
    if (!roomId) return;
    setChatRooms((prev) => {
      const idx = prev.findIndex((r) => r.id === roomId);
      if (idx < 0) return prev;
      const room = prev[idx];
      const updated = {
        ...room,
        lastMessage: { content: previewText || "", createdAt: createdAt || new Date().toISOString() },
      };
      // 최신 대화방을 위로
      const next = [updated, ...prev.slice(0, idx), ...prev.slice(idx + 1)];
      return next;
    });
  }, []);

  // ==================== 방 리스트 프리뷰 하이드레이션 ====================
  // - 모달 최초 진입 시, server lastMsgContent가 비어있는 "과거 파일-only" 방은 프리뷰가 공백일 수 있음
  // - 프리뷰가 비어있는 방만 최신 메시지 1개를 가져와 derivePreviewText로 채운다.
  const hydrateMissingRoomPreviews = useCallback(async (rooms) => {
    const list = Array.isArray(rooms) ? rooms : [];
    const targets = list
      .filter((r) => r?.id)
      .filter((r) => {
        const c = (r.lastMessage?.content || "").trim();
        return !c; // 공백인 것만 하이드레이션
      })
      // 너무 많이 호출되면 부담 → 상위 N개만
      .slice(0, 20);

    if (targets.length === 0) return;

    const batchSize = 5;
    for (let i = 0; i < targets.length; i += batchSize) {
      const batch = targets.slice(i, i + batchSize);
      await Promise.allSettled(
        batch.map(async (r) => {
          try {
            const res = await getMessages(r.id, { page: 1, size: 1 });
            const dto = Array.isArray(res?.dtoList) ? res.dtoList[0] : null;
            if (!dto) return;
            const msg = {
              id: dto.id,
              chatRoomId: dto.chatRoomId,
              senderId: dto.senderId,
              senderNickname: dto.senderNickname || dto.senderId,
              content: dto.content,
              createdAt: dto.createdAt,
              isTicketPreview: dto.messageType === "TICKET_PREVIEW",
              ticketId: dto.ticketId,
              messageSeq: dto.messageSeq,
              files: dto.files || [],
            };
            const preview = derivePreviewText(msg);
            if (preview) bumpRoomPreview(r.id, preview, msg.createdAt);
          } catch {
            // ignore
          }
        })
      );
    }
  }, []);

  // ==================== 채팅방 목록 로드 ====================
  const loadChatRooms = useCallback(async () => {
    setRoomsLoading(true);
    try {
      const rooms = await getChatRooms();
      if (rooms && !rooms.error) {
        const transformed = rooms.map((room) => {
          const otherParticipants = room.participants?.filter(
            (p) => p.userId !== currentUserId
          ) || [];
          return {
            id: room.id,
            isGroup: room.roomType === "GROUP",
            name: room.name,
            // ✅ server ChatParticipantDTO 그대로 보관 (lastReadSeq/status 포함) → 읽음표시 계산용
            participantsDetail: Array.isArray(room.participants) ? room.participants : [],
            participants: room.participants?.map((p) => p.userId) || [],
            participantInfo: room.participants?.map((p) => ({
              email: p.userId,
              nickname: p.nickname || p.userId,
              department: p.department || null,
            })) || [],
            lastMessage: (room.lastMsgAt || room.lastMsgContent)
              ? {
                  content: looksLikeFilename(room.lastMsgContent)
                    ? filePreviewLabel(room.lastMsgContent)
                    : (room.lastMsgContent || ""),
                  createdAt: room.lastMsgAt,
                }
              : null,
            unreadCount: room.unreadCount || 0,
            user2Id: otherParticipants.length > 0 ? otherParticipants[0].userId : null,
          };
        });
        setChatRooms(transformed);

        // ✅ 모달 최초 진입 시: lastMsgContent가 비어있는 방들의 프리뷰를 "최신 메시지 1개"로 채우기
        // (채팅방을 한번 들어갔다가 나오면 bumpRoomPreview로 채워졌던 문제를, 최초 로드에서도 해결)
        hydrateMissingRoomPreviews(transformed);
      }
    } catch (err) {
      console.error("채팅방 목록 로드 실패:", err);
    } finally {
      setRoomsLoading(false);
    }
  }, [currentUserId]);

  useEffect(() => {
    loadChatRooms();
  }, [loadChatRooms]);

  // ==================== 연락처 검색 ====================
  useEffect(() => {
    if (activeTab !== "search") return;
    if (searchKeyword.trim().length < 2 && !selectedDepartment) {
      setSearchResults([]);
      return;
    }

    const timeoutId = setTimeout(async () => {
      setSearchLoading(true);
      try {
        const data = await searchMembers(searchKeyword || null, 1, 30, selectedDepartment || null);
        const filtered = data.dtoList
          .filter((m) => m.email !== currentUserId)
          .map((m) => ({
            email: m.email,
            nickname: m.nickname || m.email,
            department: m.department || null,
          }));
        setSearchResults(filtered);
      } catch (err) {
        console.error("멤버 검색 실패:", err);
      } finally {
        setSearchLoading(false);
      }
    }, 300);

    return () => clearTimeout(timeoutId);
  }, [searchKeyword, selectedDepartment, activeTab, currentUserId]);

  // ==================== 채팅방 선택 ====================
  const handleSelectRoom = useCallback(async (room) => {
    setSelectedRoomId(room.id);
    setSelectedRoomInfo(room);
    setMessages([]);
    setMessagesLoading(true);
    setMsgPage(1);
    setMsgHasMore(false);
    setMsgLoadingMore(false);
    pendingScrollRef.current = { mode: null, seq: null };
    initialScrollDoneRef.current = false;

    try {
      const size = 50;
      const getPage = async (page) => {
        const response = await getMessages(room.id, { page, size });
        const list = (response.dtoList || [])
          .reverse()
          .map((msg) => ({
            id: msg.id,
            chatRoomId: msg.chatRoomId,
            senderId: msg.senderId,
            senderNickname: msg.senderNickname || msg.senderId,
            content: msg.content,
            createdAt: msg.createdAt,
            isTicketPreview: msg.messageType === "TICKET_PREVIEW",
            ticketId: msg.ticketId,
            messageSeq: msg.messageSeq,
            files: msg.files || [],
          }));
        // hasMore 판단: 서버 응답 포맷 다양성 대비
        const hasMore =
          (typeof response.hasNext === "boolean" && response.hasNext) ||
          (typeof response.page === "number" && typeof response.totalPage === "number" && response.page < response.totalPage) ||
          (Array.isArray(response.dtoList) && response.dtoList.length === size);
        return { list, hasMore, raw: response };
      };

      const myLastReadSeq = (() => {
        const ps = Array.isArray(room.participantsDetail) ? room.participantsDetail : [];
        const me = ps.find((p) => p.userId === currentUserId);
        return me?.lastReadSeq ?? 0;
      })();

      // 1) 최신부터 1페이지 로드 (UI는 오래된→최신 순으로 렌더)
      let loadedPage = 1;
      let pageRes = await getPage(1);
      let combined = pageRes.list;
      let hasMore = pageRes.hasMore;

      // 2) 안읽은 메시지가 있으면 "첫 unread"가 포함될 때까지 (최대 5페이지) 추가로 과거 로드
      const needUnreadAnchor = (room.unreadCount || 0) > 0;
      const findFirstUnread = (arr) => arr.find((m) => (m.messageSeq ?? 0) > myLastReadSeq)?.messageSeq ?? null;
      let firstUnreadSeq = needUnreadAnchor ? findFirstUnread(combined) : null;

      const MAX_PAGES_FOR_ANCHOR = 5;
      while (needUnreadAnchor && !firstUnreadSeq && hasMore && loadedPage < MAX_PAGES_FOR_ANCHOR) {
        loadedPage += 1;
        const next = await getPage(loadedPage);
        // 더 과거 메시지는 앞에 prepend (오래된→최신 순서 유지)
        combined = [...next.list, ...combined];
        hasMore = next.hasMore;
        firstUnreadSeq = findFirstUnread(combined);
      }

      setMessages(combined);
      setMsgPage(loadedPage);
      setMsgHasMore(hasMore);

      // ✅ 우측 리스트 프리뷰 즉시 동기화: 서버 ChatRoom.lastMsgContent가 비어있어도,
      //    "실제 마지막 메시지" 기준으로 프리뷰를 맞춘다.
      if (combined.length > 0) {
        const last = combined[combined.length - 1];
        bumpRoomPreview(room.id, derivePreviewText(last), last.createdAt);
      }

      // ✅ 진입 스크롤 규칙:
      // - unread가 있으면: 첫 unread 메시지로 이동
      // - unread 없으면: 항상 맨 아래(최신)로
      if (needUnreadAnchor && firstUnreadSeq) {
        pendingScrollRef.current = { mode: "seq", seq: firstUnreadSeq };
      } else {
        pendingScrollRef.current = { mode: "bottom", seq: null };
      }

      // 읽음 처리 (기존대로: 마지막 메시지 기준)
      if (combined.length > 0) {
        const lastMsg = combined[combined.length - 1];
        if (lastMsg.messageSeq) {
          await markRead(room.id, { messageSeq: lastMsg.messageSeq });

          // ✅ UI 즉시 반영: 방 리스트 unreadCount 제거 + 내 lastReadSeq 갱신
          setChatRooms((prev) =>
            prev.map((r) => {
              if (r.id !== room.id) return r;
              const nextParticipants = Array.isArray(r.participantsDetail)
                ? r.participantsDetail.map((p) =>
                    p.userId === currentUserId ? { ...p, lastReadSeq: lastMsg.messageSeq } : p
                  )
                : r.participantsDetail;
              return { ...r, unreadCount: 0, participantsDetail: nextParticipants };
            })
          );
          setSelectedRoomInfo((prev) => {
            if (!prev || prev.id !== room.id) return prev;
            const nextParticipants = Array.isArray(prev.participantsDetail)
              ? prev.participantsDetail.map((p) =>
                  p.userId === currentUserId ? { ...p, lastReadSeq: lastMsg.messageSeq } : p
                )
              : prev.participantsDetail;
            return { ...prev, unreadCount: 0, participantsDetail: nextParticipants };
          });
        }
      }
    } catch (err) {
      console.error("메시지 로드 실패:", err);
    } finally {
      setMessagesLoading(false);
    }
  }, []);

  // ==================== WebSocket 연결 ====================
  useEffect(() => {
    if (!selectedRoomId || !currentUserId) return;

    chatWsClient.connect(
      selectedRoomId,
      (newMessage) => {
        const transformed = {
          id: newMessage.id,
          chatRoomId: newMessage.chatRoomId,
          senderId: newMessage.senderId,
          senderNickname: newMessage.senderNickname || newMessage.senderId,
          content: newMessage.content,
          createdAt: newMessage.createdAt,
          isTicketPreview: newMessage.messageType === "TICKET_PREVIEW",
          ticketId: newMessage.ticketId,
          messageSeq: newMessage.messageSeq,
          files: newMessage.files || [],
        };

        setMessages((prev) => {
          if (prev.some((m) => m.id === transformed.id)) return prev;
          return [...prev, transformed];
        });

        // ✅ 우측 리스트 프리뷰 즉시 갱신 (WS로 온 최신 메시지)
        bumpRoomPreview(selectedRoomId, derivePreviewText(transformed), transformed.createdAt);

        // 읽음 처리
        if (transformed.senderId !== currentUserId && transformed.messageSeq) {
          markRead(selectedRoomId, { messageSeq: transformed.messageSeq }).catch(console.error);
        }

        // 욕설 감지
        if (transformed.senderId === currentUserId && newMessage.profanityDetected) {
          if (handleProfanityDetectedRef.current) {
            handleProfanityDetectedRef.current();
          }
        }

        // 티켓 트리거
        if (newMessage.ticketTrigger) {
          setIsConfirmModalOpen(true);
        }
      },
      () => setConnected(true),
      () => setConnected(false)
    );

    setConnected(chatWsClient.isConnected());

    return () => {
      chatWsClient.disconnect();
      setConnected(false);
    };
  }, [selectedRoomId, currentUserId]);

  // ==================== 욕설 감지 로직 ====================
  const handleProfanityBlink = useCallback(() => {
    if (aiEnabled) return;
    if (blinkTimeoutRef.current) clearTimeout(blinkTimeoutRef.current);
    setAiEnabled(true);
    blinkTimeoutRef.current = setTimeout(() => {
      setAiEnabled(false);
      blinkTimeoutRef.current = null;
    }, 800);
  }, [aiEnabled]);

  const handleProfanityDetected = useCallback(() => {
    if (aiEnabled || forceOnRemaining > 0) return;

    if (userChoseOffAfterWarning) {
      setShowForceModal(true);
      setProfanityCount(0);
      profanityCountRef.current = 0;
      if (profanityTimerRef.current) {
        clearTimeout(profanityTimerRef.current);
        profanityTimerRef.current = null;
      }
      return;
    }

    handleProfanityBlink();
    profanityCountRef.current += 1;
    const newCount = profanityCountRef.current;
    setProfanityCount(newCount);

    if (profanityTimerRef.current) clearTimeout(profanityTimerRef.current);
    profanityTimerRef.current = setTimeout(() => {
      setProfanityCount(0);
      profanityCountRef.current = 0;
    }, 10000);

    if (newCount >= 2 && !warningModalShown) {
      setShowWarningModal(true);
      setWarningModalShown(true);
      setProfanityCount(0);
      profanityCountRef.current = 0;
      if (profanityTimerRef.current) {
        clearTimeout(profanityTimerRef.current);
        profanityTimerRef.current = null;
      }
    }
  }, [aiEnabled, warningModalShown, userChoseOffAfterWarning, forceOnRemaining, handleProfanityBlink]);

  useEffect(() => {
    handleProfanityDetectedRef.current = handleProfanityDetected;
  }, [handleProfanityDetected]);

  const handleWarningSelectOn = useCallback(() => {
    setShowWarningModal(false);
    setAiEnabled(true);
    setUserChoseOffAfterWarning(false);
    setWarningModalShown(false);
  }, []);

  const handleWarningSelectOff = useCallback(() => {
    setShowWarningModal(false);
    setAiEnabled(false);
    setUserChoseOffAfterWarning(true);
  }, []);

  const handleForceConfirm = useCallback(() => {
    setShowForceModal(false);
    setAiEnabled(true);
    setForceOnRemaining(60);
    setUserChoseOffAfterWarning(false);

    if (forceOnTimerRef.current) clearInterval(forceOnTimerRef.current);
    forceOnTimerRef.current = setInterval(() => {
      setForceOnRemaining((prev) => {
        if (prev <= 1) {
          clearInterval(forceOnTimerRef.current);
          forceOnTimerRef.current = null;
          setAiEnabled(false);
          setWarningModalShown(false);
          setUserChoseOffAfterWarning(false);
          setShowReleaseToast(true);
          setTimeout(() => setShowReleaseToast(false), 3000);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  }, []);

  useEffect(() => {
    return () => {
      if (profanityTimerRef.current) clearTimeout(profanityTimerRef.current);
      if (forceOnTimerRef.current) clearInterval(forceOnTimerRef.current);
      if (blinkTimeoutRef.current) clearTimeout(blinkTimeoutRef.current);
    };
  }, []);

  // ==================== 메시지 스크롤 (진입 규칙 + 자동 스크롤) ====================
  useEffect(() => {
    const el = chatContainerRef.current;
    if (!el || messagesLoading) return;

    // 1) 방 진입 시: (첫 unread) 또는 (맨 아래)
    if (!initialScrollDoneRef.current && messages.length > 0) {
      const pending = pendingScrollRef.current;
      requestAnimationFrame(() => {
        if (!chatContainerRef.current) return;
        if (pending?.mode === "seq" && pending.seq) {
          const node = chatContainerRef.current.querySelector(`[data-seq="${pending.seq}"]`);
          if (node?.scrollIntoView) node.scrollIntoView({ block: "center" });
          else chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
        } else {
          chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
        }
        initialScrollDoneRef.current = true;
      });
      return;
    }

    // 2) 일반 메시지 추가: 사용자가 '거의 맨 아래'에 있을 때만 자동으로 맨 아래 유지
    const distanceFromBottom = el.scrollHeight - (el.scrollTop + el.clientHeight);
    if (distanceFromBottom < 120) {
      el.scrollTop = el.scrollHeight;
    }
  }, [messages, messagesLoading]);

  // ==================== 위로 무한스크롤: scrollTop=0 근처에서 과거 메시지 추가 로드 ====================
  const handleChatScroll = useCallback(async () => {
    const el = chatContainerRef.current;
    if (!el) return;
    if (messagesLoading || msgLoadingMore || !selectedRoomId || !msgHasMore) return;

    // 상단 근처에서만 트리거
    if (el.scrollTop > 40) return;

    setMsgLoadingMore(true);
    const prevScrollHeight = el.scrollHeight;

    try {
      const nextPage = msgPage + 1;
      const response = await getMessages(selectedRoomId, { page: nextPage, size: 50 });
      const older = (response.dtoList || [])
        .reverse()
        .map((msg) => ({
          id: msg.id,
          chatRoomId: msg.chatRoomId,
          senderId: msg.senderId,
          senderNickname: msg.senderNickname || msg.senderId,
          content: msg.content,
          createdAt: msg.createdAt,
          isTicketPreview: msg.messageType === "TICKET_PREVIEW",
          ticketId: msg.ticketId,
          messageSeq: msg.messageSeq,
          files: msg.files || [],
        }));

      const hasMore =
        (typeof response.hasNext === "boolean" && response.hasNext) ||
        (typeof response.page === "number" && typeof response.totalPage === "number" && response.page < response.totalPage) ||
        (Array.isArray(response.dtoList) && response.dtoList.length === 50);

      setMessages((prev) => {
        // 중복 방지 (혹시 서버가 겹치게 내려주는 케이스 대비)
        const exists = new Set(prev.map((m) => m.id));
        const filtered = older.filter((m) => !exists.has(m.id));
        return [...filtered, ...prev];
      });

      setMsgPage(nextPage);
      setMsgHasMore(hasMore);

      // prepend 후 스크롤 위치 유지
      requestAnimationFrame(() => {
        const cur = chatContainerRef.current;
        if (!cur) return;
        const newScrollHeight = cur.scrollHeight;
        cur.scrollTop = newScrollHeight - prevScrollHeight + cur.scrollTop;
      });
    } catch (e) {
      console.error("과거 메시지 로드 실패:", e);
    } finally {
      setMsgLoadingMore(false);
    }
  }, [messagesLoading, msgLoadingMore, selectedRoomId, msgHasMore, msgPage]);

  // ==================== 메시지 전송 ====================
  const handleSendMessage = async () => {
    if (showWarningModal || showForceModal) return;
    if (!inputMessage.trim() && selectedFiles.length === 0) return;
    if (!selectedRoomId) return;

    const content = inputMessage.trim();
    setInputMessage("");
    const filesToSend = selectedFiles;
    setSelectedFiles([]);

    if (filesToSend.length > 0) {
      try {
        const sent = await sendMessageWithFilesRest(selectedRoomId, {
          content,
          messageType: "TEXT",
          aiEnabled,
          files: filesToSend,
        });

        // REST 응답으로 즉시 반영(WS가 와도 id로 중복 방지됨)
        if (sent && sent.id) {
          const localMsg = {
            id: sent.id,
            chatRoomId: sent.chatRoomId,
            senderId: sent.senderId,
            senderNickname: sent.senderNickname || sent.senderId,
            content: sent.content,
            createdAt: sent.createdAt,
            isTicketPreview: sent.messageType === "TICKET_PREVIEW",
            ticketId: sent.ticketId,
            messageSeq: sent.messageSeq,
            files: sent.files || [],
          };
          setMessages((prev) => (prev.some((m) => m.id === localMsg.id) ? prev : [...prev, localMsg]));
          bumpRoomPreview(selectedRoomId, derivePreviewText(localMsg), localMsg.createdAt);
        } else {
          // 최소한 프리뷰는 즉시 반영 (A안: 첫 파일 기준으로 타입/확장자 표시)
          const fallback = content || (filesToSend[0]?.name ? filePreviewLabel(filesToSend[0].name) : "");
          bumpRoomPreview(selectedRoomId, fallback, new Date().toISOString());
        }
      } catch (err) {
        console.error("파일 메시지 전송 실패:", err);
        alert("메시지 전송에 실패했습니다.");
      }
      return;
    }

    const wsSuccess = chatWsClient.send(selectedRoomId, {
      content,
      messageType: "TEXT",
      aiEnabled,
    });

    // ✅ WS 전송 성공 시에도 프리뷰는 즉시 반영 (서버 브로드캐스트 도착 전 갭 제거)
    if (wsSuccess) {
      bumpRoomPreview(selectedRoomId, content, new Date().toISOString());
    }

    if (!wsSuccess) {
      try {
        const sent = await sendMessageRest(selectedRoomId, {
          content,
          messageType: "TEXT",
          aiEnabled,
        });
        if (sent && sent.id) {
          const localMsg = {
            id: sent.id,
            chatRoomId: sent.chatRoomId,
            senderId: sent.senderId,
            senderNickname: sent.senderNickname || sent.senderId,
            content: sent.content,
            createdAt: sent.createdAt,
            isTicketPreview: sent.messageType === "TICKET_PREVIEW",
            ticketId: sent.ticketId,
            messageSeq: sent.messageSeq,
            files: sent.files || [],
          };
          setMessages((prev) => (prev.some((m) => m.id === localMsg.id) ? prev : [...prev, localMsg]));
          bumpRoomPreview(selectedRoomId, derivePreviewText(localMsg), localMsg.createdAt);
        } else {
          bumpRoomPreview(selectedRoomId, content, new Date().toISOString());
        }
      } catch (err) {
        console.error("메시지 전송 실패:", err);
        alert("메시지 전송에 실패했습니다.");
      }
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  // ==================== Drag & Drop (파일 첨부와 동일 동작) ====================
  const handleDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.dataTransfer?.items && e.dataTransfer.items.length > 0) {
      setIsDragging(true);
    }
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    e.stopPropagation();
    // 드래그가 자식 요소로 들어갔을 때는 leave로 처리하지 않음
    if (e.relatedTarget && e.currentTarget.contains(e.relatedTarget)) return;
    setIsDragging(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);

    const files = e.dataTransfer?.files ? Array.from(e.dataTransfer.files) : [];
    if (!files.length) return;

    // 파일 첨부 버튼과 동일하게 selectedFiles에 누적
    setSelectedFiles((prev) => [...prev, ...files]);
  };

  // ==================== 연락처에서 대화 시작 ====================
  const handleStartChat = async (user) => {
    try {
      const room = await createOrGetDirectRoom({ targetEmail: user.email });
      await loadChatRooms();
      handleSelectRoom({
        id: room.id,
        isGroup: false,
        name: user.nickname,
        participantInfo: [{ email: user.email, nickname: user.nickname }],
        user2Id: user.email,
      });
      setActiveTab("rooms");
    } catch (err) {
      console.error("채팅방 생성 실패:", err);
      alert("채팅방 생성에 실패했습니다.");
    }
  };

  const toggleContact = (user) => {
    if (!user?.email) return;
    setSelectedContacts((prev) => {
      const exists = prev.some((u) => u.email === user.email);
      if (exists) return prev.filter((u) => u.email !== user.email);
      return [...prev, { email: user.email, nickname: user.nickname, department: user.department || null }];
    });
  };

  const removeSelectedContact = (email) => {
    setSelectedContacts((prev) => prev.filter((u) => u.email !== email));
  };

  const handleCreateGroup = async () => {
    if (selectedContacts.length < 2) {
      alert("단톡방은 최소 2명 이상 선택해주세요.");
      return;
    }
    try {
      const participants = selectedContacts.map((u) => u.email);
      const name =
        (groupName || "").trim() ||
        (selectedContacts.length <= 2
          ? selectedContacts.map((u) => u.nickname).join(", ")
          : `${selectedContacts.slice(0, 2).map((u) => u.nickname).join(", ")} 외 ${selectedContacts.length - 2}명`);

      const room = await createGroupRoom({ name, participantEmails: participants });
      // 방 목록 갱신 + 생성된 방으로 이동
      setGroupName("");
      setSelectedContacts([]);
      await loadChatRooms();
      // createGroupRoom 응답은 ChatRoomDTO 형태
      handleSelectRoom({
        id: room.id,
        isGroup: room.roomType === "GROUP",
        name: room.name,
        participantInfo: room.participants?.map((p) => ({ email: p.userId, nickname: p.nickname || p.userId })) || [],
        participantsDetail: room.participants || [],
        participants: room.participants?.map((p) => p.userId) || [],
        unreadCount: room.unreadCount || 0,
      });
      setActiveTab("rooms");
    } catch (err) {
      console.error("그룹 채팅방 생성 실패:", err);
      alert("그룹 채팅방 생성에 실패했습니다.");
    }
  };

  // ==================== 채팅방 이름 ====================
  const getChatRoomDisplayName = (room) => {
    if (room.isGroup) {
      // ✅ 그룹은 '방 이름' 우선 (생성자가 지정한 이름)
      if (room.name && String(room.name).trim()) {
        return room.name;
      }
      if (room.participantInfo && room.participantInfo.length > 0) {
        const names = room.participantInfo.map((p) => p.nickname);
        if (names.length <= 2) return names.join(", ");
        return `${names.slice(0, 2).join(", ")} 외 ${names.length - 2}명`;
      }
      return "그룹 채팅";
    } else {
      const other = room.participantInfo?.find((p) => p.email !== currentUserId);
      return other?.nickname || room.user2Id || "채팅";
    }
  };

  const getDirectOtherEmail = (room) => {
    if (!room || room.isGroup) return null;
    return room.participantInfo?.find((p) => p.email !== currentUserId)?.email || room.user2Id || null;
  };

  const getDirectDeptLabel = (room) => {
    if (!room || room.isGroup) return "";
    const other = room.participantInfo?.find((p) => p.email !== currentUserId);
    const dept = other?.department || null;
    return dept ? `${getDepartmentLabel(dept)}` : "";
  };

  const getDirectOtherDisplayName = (room) => {
    if (!room || room.isGroup) return "";
    const other = room.participantInfo?.find((p) => p.email !== currentUserId);
    return other?.nickname || room.user2Id || "";
  };

  // ==================== 읽음표시 계산 ====================
  const getUnreadCountForMessage = useCallback((msg) => {
    if (!msg || !msg.messageSeq || !selectedRoomInfo) return 0;
    const seq = msg.messageSeq;

    const participants = Array.isArray(selectedRoomInfo.participantsDetail)
      ? selectedRoomInfo.participantsDetail
      : [];

    // ACTIVE만 대상으로(서버 DTO에 status가 없을 수도 있어 안전하게 처리)
    const active = participants.filter((p) => !p.status || p.status === "ACTIVE");

    // DIRECT: 상대 1명 기준
    if (!selectedRoomInfo.isGroup) {
      if (msg.senderId !== currentUserId) return 0; // 1:1에서는 "내가 보낸 메시지"만 표시
      const other = active.find((p) => p.userId && p.userId !== currentUserId);
      const otherLast = other?.lastReadSeq ?? 0;
      return otherLast >= seq ? 0 : 1;
    }

    // GROUP: 모든 메시지에 대해 '안읽은 사람 수' 표시 (보통 sender 제외)
    const others = active.filter((p) => p.userId && p.userId !== msg.senderId);
    return others.filter((p) => (p.lastReadSeq ?? 0) < seq).length;
  }, [selectedRoomInfo, currentUserId]);

  // ==================== 필터된 채팅방 목록 ====================
  const filteredRooms = chatRooms.filter((room) => {
    if (roomFilter === "direct") return !room.isGroup;
    if (roomFilter === "group") return room.isGroup;
    return true;
  });

  // ==================== 안읽음 카운트 ====================
  // ✅ 뮤트된 방은 알림 카운트에서 제외 (숫자만 숨기되, 상태는 유지)
  const totalUnread = chatRooms.reduce((sum, r) => sum + (isRoomMuted(r.id) ? 0 : (r.unreadCount || 0)), 0);
  const directUnread = chatRooms.filter((r) => !r.isGroup).reduce((sum, r) => sum + (isRoomMuted(r.id) ? 0 : (r.unreadCount || 0)), 0);
  const groupUnread = chatRooms.filter((r) => r.isGroup).reduce((sum, r) => sum + (isRoomMuted(r.id) ? 0 : (r.unreadCount || 0)), 0);

  // ==================== 렌더링 ====================
  if (showAIWorkMode) {
    return <AIChatWidget onClose={() => setShowAIWorkMode(false)} />;
  }

  return (
    <div className="ai-widget-overlay">
      <div
        className="ai-widget-container relative"
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
      >
        {/* 우클릭 컨텍스트 메뉴 */}
        {contextMenu?.room && (
          <div
            className="fixed z-[10000] bg-white border border-baseBorder rounded-ui shadow-ui overflow-hidden text-sm"
            style={{ left: contextMenu.x, top: contextMenu.y }}
          >
            <button
              type="button"
              onClick={() => {
                toggleMuteRoom(contextMenu.room.id);
                setContextMenu(null);
              }}
              className="w-full text-left px-4 py-2 hover:bg-baseSurface"
            >
              {isRoomMuted(contextMenu.room.id) ? "알림 켜기" : "알림 끄기"}
            </button>
            <button
              type="button"
              onClick={async () => {
                const ok = window.confirm("채팅방을 나가시겠습니까?");
                if (!ok) return setContextMenu(null);
                try {
                  await leaveRoom(contextMenu.room.id);
                  // 선택된 방이면 초기화
                  if (selectedRoomId === contextMenu.room.id) {
                    setSelectedRoomId(null);
                    setSelectedRoomInfo(null);
                    setMessages([]);
                  }
                  await loadChatRooms();
                } catch (e) {
                  console.error(e);
                  alert("나가기에 실패했습니다.");
                } finally {
                  setContextMenu(null);
                }
              }}
              className="w-full text-left px-4 py-2 hover:bg-baseSurface text-red-600"
            >
              나가기
            </button>
          </div>
        )}

        {/* 그룹 참여자 목록 모달 */}
        {showParticipants && selectedRoomInfo?.isGroup && (
          <div className="absolute inset-0 z-[9999] bg-black/30 flex items-center justify-center">
            <div className="bg-white w-[420px] max-w-[90vw] rounded-ui shadow-ui border border-baseBorder overflow-hidden">
              <div className="flex items-center justify-between px-4 py-3 border-b border-baseBorder bg-baseSurface">
                <div className="font-bold text-baseText">
                  참여자 ({Array.isArray(selectedRoomInfo.participantInfo) ? selectedRoomInfo.participantInfo.length : 0})
                </div>
                <button className="close-btn" onClick={() => setShowParticipants(false)}>
                  &times;
                </button>
              </div>
              <div className="max-h-[50vh] overflow-y-auto p-3">
                {Array.isArray(selectedRoomInfo.participantInfo) && selectedRoomInfo.participantInfo.length > 0 ? (
                  selectedRoomInfo.participantInfo.map((p) => (
                    <div key={p.email} className="px-3 py-2 rounded-ui hover:bg-baseSurface">
                      <div className="flex items-center justify-between gap-3">
                        <div className="min-w-0">
                          <div className="font-semibold text-baseText truncate">{p.nickname || p.email}</div>
                          <div className="text-xs text-baseMuted truncate">{p.email}</div>
                        </div>
                        <div className="text-xs text-baseMuted">
                          {p.department ? getDepartmentLabel(p.department) : ""}
                        </div>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="text-center text-baseMuted py-10">참여자가 없습니다.</div>
                )}
              </div>
              <div className="px-4 py-3 border-t border-baseBorder bg-baseSurface flex justify-end">
                <button className="reset-btn" onClick={() => setShowParticipants(false)}>
                  닫기
                </button>
              </div>
            </div>
          </div>
        )}

        {/* 드래그 오버레이 */}
        {isDragging && (
          <div className="absolute inset-0 z-50 bg-brandNavy/10 border-2 border-dashed border-brandNavy flex items-center justify-center backdrop-blur-sm pointer-events-none">
            <div className="bg-white px-8 py-6 rounded-2xl shadow-xl flex flex-col items-center gap-3">
              <div className="text-5xl">📂</div>
              <div className="font-bold text-xl text-brandNavy">파일을 여기에 놓아주세요</div>
              <div className="text-sm text-baseMuted">아래 첨부 목록에 추가됩니다</div>
            </div>
          </div>
        )}
        {/* 헤더 */}
        <div className="ai-widget-header">
          <h2>
            <span className="text-2xl mr-2">💬</span>
            채팅
          </h2>
          <div className="flex items-center gap-3">
            {/* AI 업무모드 전환 버튼 */}
            <button
              onClick={() => setShowAIWorkMode(true)}
              className="ui-btn-primary"
              title="AI 업무모드로 전환"
            >
              AI 비서
            </button>
            <button className="close-btn" onClick={onClose}>
              &times;
            </button>
          </div>
        </div>

        <div className="ai-widget-body">
          {/* ==================== 좌측: 채팅 영역 ==================== */}
          <div className="ai-chat-section">
            {!selectedRoomId ? (
              // 채팅방 미선택 시
              <div className="flex-1 flex flex-col items-center justify-center text-baseMuted">
                <div className="text-6xl mb-4">💬</div>
                <p className="text-lg font-medium mb-2">대화를 시작해보세요</p>
                <p className="text-sm">우측에서 대화 상대를 선택하거나 검색하세요</p>
              </div>
            ) : (
              <>
                {/* 채팅방 헤더 (요구사항 재반영)
                    - (좌) 이름만
                    - (좌 옆 박스) 부서(위) / 이메일(아래)
                    - (빈 자리로 이동) 1:1채팅/그룹채팅 + 연결 상태
                */}
                {/* ✅ 헤더 정렬:
                    - 부서/이메일 박스 ↔ 우측 연결 상태: 하단선 유지
                    - 이름만: 부서/이메일 박스의 '세로 중앙'에 오게
                */}
                <div className="flex items-end justify-between mb-2 px-1 gap-3">
                  {/* 좌: 이름 + (옆) 부서/이메일 박스 */}
                  <div className="min-w-0 flex items-end gap-3">
                    {/* 이름: 박스 높이(고정) 안에서 세로 중앙 정렬 */}
                    <div className="h-[48px] flex items-center max-w-[240px]">
                      <div className="text-2xl font-extrabold text-baseText leading-tight truncate">
                        {selectedRoomInfo ? getChatRoomDisplayName(selectedRoomInfo) : ""}
                      </div>
                    </div>

                    {/* ✅ 배경 통일: 흰색 */}
                    <div className="shrink-0 h-[48px] px-3 rounded-ui border border-baseBorder bg-white leading-tight flex flex-col justify-center">
                      {!selectedRoomInfo?.isGroup ? (
                        <>
                          <div className="text-xs text-baseMuted font-semibold leading-tight">
                            {getDirectDeptLabel(selectedRoomInfo)}
                          </div>
                          <div className="text-[11px] text-baseMuted/80 leading-tight mt-0.5">
                            {getDirectOtherEmail(selectedRoomInfo) || ""}
                          </div>
                        </>
                      ) : (
                        <button
                          type="button"
                          onClick={() => setShowParticipants(true)}
                          className="text-left"
                          title="참여자 보기"
                        >
                          <div className="text-xs text-baseMuted font-semibold leading-tight">그룹</div>
                          <div className="text-[11px] text-baseMuted/80 leading-tight mt-0.5">
                            {Array.isArray(selectedRoomInfo?.participantInfo)
                              ? `${selectedRoomInfo.participantInfo.length}명`
                              : ""}
                          </div>
                        </button>
                      )}
                    </div>
                  </div>

                  {/* ✅ 1:1 채팅/연결됨: 우측 끝단 + 하단선 정렬 느낌 */}
                  <div className="flex flex-col items-end gap-1 shrink-0">
                    <div className="text-xs text-baseMuted uppercase tracking-wide leading-tight">
                      {selectedRoomInfo?.isGroup ? "그룹 채팅" : "1:1 채팅"}
                    </div>
                    <div className={`text-xs flex items-center gap-1 leading-tight ${connected ? "text-green-600" : "text-red-500"}`}>
                      <span className="w-1.5 h-1.5 rounded-full bg-current"></span>
                      {connected ? "연결됨" : "연결 끊김"}
                    </div>
                  </div>
                </div>

                {/* 메시지 영역 */}
                <div ref={chatContainerRef} className="chat-messages-area" onScroll={handleChatScroll}>
                  {messagesLoading ? (
                    <div className="text-center text-baseMuted mt-8">메시지를 불러오는 중...</div>
                  ) : messages.length === 0 ? (
                    <div className="text-center text-baseMuted mt-8">
                      <p className="font-medium">메시지가 없습니다.</p>
                      <p className="text-sm mt-1">대화를 시작해보세요.</p>
                    </div>
                  ) : (
                    messages.map((msg) => {
                      const isMine = msg.senderId === currentUserId;
                      const incomingName = selectedRoomInfo?.isGroup
                        ? msg.senderNickname
                        : getDirectOtherDisplayName(selectedRoomInfo);

                      // ✅ 가로폭 60% 넘으면 줄바꿈 (페이지 크기 따라 자동)
                      const bubbleClass = isMine
                        ? "max-w-[60%] px-4 py-3 rounded-ui text-sm leading-relaxed whitespace-pre-wrap break-words bg-brandNavy text-white text-right border-none"
                        : "max-w-[60%] px-4 py-3 rounded-ui text-sm leading-relaxed whitespace-pre-wrap break-words bg-baseBg text-baseText border border-baseBorder";

                      const unreadForThisMsg = getUnreadCountForMessage(msg);

                      return (
                      <div
                        key={msg.id}
                        data-seq={msg.messageSeq || ""}
                        className={`flex w-full mb-3 ${isMine ? "justify-end" : "justify-start"} items-start`}
                      >
                        {/* ✅ 상대 메시지: 채팅창 왼쪽 거터에 이름 (더 진하고, 간격 좁게) */}
                        {!isMine && (
                          <div className="shrink-0 w-[44px] pr-1 text-left">
                            <div className="text-sm font-extrabold text-slate-800 leading-tight truncate">
                              {incomingName}
                            </div>
                          </div>
                        )}

                        <div className={bubbleClass}>
                          {msg.isTicketPreview ? (
                            <div
                              onClick={() => {
                                setSelectedTicketId(msg.ticketId);
                                setIsTicketDetailModalOpen(true);
                              }}
                              className="cursor-pointer hover:opacity-80"
                            >
                              <div className="font-semibold mb-1 text-sm">🎫 요청서 미리보기</div>
                              <div className="text-xs opacity-80">클릭하여 확인</div>
                            </div>
                          ) : (
                            <div className="space-y-2">
                              {msg.content && <div className="whitespace-pre-wrap">{msg.content}</div>}
                              {Array.isArray(msg.files) && msg.files.length > 0 && (
                                <div className="grid grid-cols-2 gap-2 mt-2">
                                  {msg.files.map((f) => (
                                    <button
                                      key={f.uuid}
                                      onClick={() => downloadFile(f.uuid, f.fileName)}
                                      className={`text-left flex items-center gap-2 rounded-ui border px-2 py-2 ${
                                        isMine
                                          ? "border-white/30 bg-white/10 hover:bg-white/15"
                                          : "border-baseBorder bg-white hover:bg-baseSurface"
                                      }`}
                                    >
                                      <div className="w-8 h-8 rounded overflow-hidden bg-baseSurface flex-shrink-0">
                                        <FilePreview file={{ uuid: f.uuid, fileName: f.fileName }} />
                                      </div>
                                      <div className="min-w-0">
                                        <div className="text-xs font-semibold truncate">{f.fileName}</div>
                                        <div className={`text-[10px] ${isMine ? "opacity-80" : "text-baseMuted"}`}>
                                          {(f.fileSize / 1024).toFixed(1)} KB
                                        </div>
                                      </div>
                                    </button>
                                  ))}
                                </div>
                              )}
                            </div>
                          )}
                          <div className={`text-xs mt-2 flex items-center gap-2 ${isMine ? "justify-end text-white/80" : "justify-start text-baseMuted"}`}>
                            {/* 읽음표시: 1:1은 내가 보낸 메시지에만(1/0), 단톡은 모든 메시지(안읽은 사람 수) */}
                            {unreadForThisMsg > 0 && (
                              <span className="font-extrabold text-brandOrange">
                                {unreadForThisMsg}
                              </span>
                            )}
                            <span>
                              {new Date(msg.createdAt).toLocaleTimeString("ko-KR", {
                                hour: "2-digit",
                                minute: "2-digit",
                              })}
                            </span>
                          </div>
                        </div>
                      </div>
                    );
                    })
                  )}
                  <div ref={messagesEndRef} />
                </div>

                {/* 입력 영역 */}
                <div className="chat-input-wrapper">
                  {/* 파일 첨부 */}
                  <label className="cursor-pointer text-xl hover:opacity-70" title="파일 첨부">
                    📎
                    <input
                      type="file"
                      multiple
                      className="hidden"
                      onChange={(e) => {
                        const list = Array.from(e.target.files || []);
                        if (list.length) setSelectedFiles((prev) => [...prev, ...list]);
                        e.target.value = "";
                      }}
                    />
                  </label>
                  <input
                    type="text"
                    className="chat-input"
                    placeholder="메시지를 입력하세요..."
                    value={inputMessage}
                    onChange={(e) => setInputMessage(e.target.value)}
                    onKeyPress={handleKeyPress}
                    disabled={!connected || showWarningModal || showForceModal}
                  />
                  {/* AI 토글 */}
                  <div className="relative">
                    <button
                      onClick={() => {
                        if (forceOnRemaining > 0) return;
                        setAiEnabled(!aiEnabled);
                        setUserChoseOffAfterWarning(false);
                        setWarningModalShown(false);
                        setProfanityCount(0);
                        profanityCountRef.current = 0;
                      }}
                      className={`px-3 py-2 rounded-ui font-semibold text-xs transition-all ${
                        aiEnabled
                          ? "bg-brandNavy text-white"
                          : "bg-white border border-baseBorder text-baseText"
                      } ${forceOnRemaining > 0 ? "cursor-not-allowed opacity-75" : ""}`}
                      title={aiEnabled ? "AI ON" : "AI OFF"}
                    >
                      AI {aiEnabled ? "ON" : "OFF"}
                      {forceOnRemaining > 0 && <span className="ml-1">({forceOnRemaining})</span>}
                    </button>
                    {showReleaseToast && (
                      <div className="absolute bottom-full mb-2 left-1/2 -translate-x-1/2 bg-gray-800 text-white text-xs px-3 py-2 rounded shadow-lg whitespace-nowrap">
                        해제되었습니다
                      </div>
                    )}
                  </div>
                  <button
                    onClick={handleSendMessage}
                    disabled={!connected || (!inputMessage.trim() && selectedFiles.length === 0)}
                    className="reset-btn bg-brandNavy text-white hover:opacity-90 disabled:opacity-50"
                  >
                    전송
                  </button>
                </div>

                {/* 첨부 파일 미리보기 */}
                {selectedFiles.length > 0 && (
                  <div className="mt-2 flex flex-wrap gap-2 px-2">
                    {selectedFiles.map((f, idx) => (
                      <div key={idx} className="flex items-center gap-2 px-3 py-1 bg-baseSurface rounded-ui border border-baseBorder text-xs">
                        <span className="truncate max-w-[150px]">{f.name}</span>
                        <button
                          onClick={() => setSelectedFiles((prev) => prev.filter((_, i) => i !== idx))}
                          className="text-red-500 hover:text-red-700"
                        >
                          ×
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </>
            )}
          </div>

          {/* ==================== 우측: 대화방/연락처 패널 ==================== */}
          <div className="ai-ticket-section">
            {/* 탭 헤더 */}
            <div className="flex border-b border-baseBorder mb-4">
              <button
                onClick={() => setActiveTab("rooms")}
                className={`flex-1 py-3 text-sm font-semibold transition-colors ${
                  activeTab === "rooms"
                    ? "border-b-2 border-brandNavy text-brandNavy"
                    : "text-baseMuted hover:text-baseText"
                }`}
              >
                대화방
                {totalUnread > 0 && (
                  <span className="ml-2 px-2 py-0.5 bg-brandOrange text-white text-xs rounded-full">
                    {totalUnread}
                  </span>
                )}
              </button>
              <button
                onClick={() => setActiveTab("search")}
                className={`flex-1 py-3 text-sm font-semibold transition-colors ${
                  activeTab === "search"
                    ? "border-b-2 border-brandNavy text-brandNavy"
                    : "text-baseMuted hover:text-baseText"
                }`}
              >
                대화 상대 검색
              </button>
            </div>

            {activeTab === "rooms" ? (
              // ==================== 대화방 목록 ====================
              <div className="flex flex-col h-full">
                {/* 필터 탭 */}
                <div className="flex gap-2 mb-4">
                  <button
                    onClick={() => setRoomFilter("all")}
                    className={`flex-1 py-2 px-3 rounded-ui text-xs font-semibold transition-all ${
                      roomFilter === "all"
                        ? "bg-brandNavy text-white"
                        : "bg-baseSurface text-baseMuted hover:text-baseText"
                    }`}
                  >
                    전체
                    {totalUnread > 0 && (
                      <span className="ml-1 text-[10px]">({totalUnread})</span>
                    )}
                  </button>
                  <button
                    onClick={() => setRoomFilter("direct")}
                    className={`flex-1 py-2 px-3 rounded-ui text-xs font-semibold transition-all ${
                      roomFilter === "direct"
                        ? "bg-brandNavy text-white"
                        : "bg-baseSurface text-baseMuted hover:text-baseText"
                    }`}
                  >
                    개인
                    {directUnread > 0 && (
                      <span className="ml-1 text-[10px]">({directUnread})</span>
                    )}
                  </button>
                  <button
                    onClick={() => setRoomFilter("group")}
                    className={`flex-1 py-2 px-3 rounded-ui text-xs font-semibold transition-all ${
                      roomFilter === "group"
                        ? "bg-brandNavy text-white"
                        : "bg-baseSurface text-baseMuted hover:text-baseText"
                    }`}
                  >
                    그룹
                    {groupUnread > 0 && (
                      <span className="ml-1 text-[10px]">({groupUnread})</span>
                    )}
                  </button>
                </div>

                {/* 채팅방 리스트 */}
                <div className="flex-1 overflow-y-auto border border-baseBorder rounded-ui">
                  {roomsLoading ? (
                    <div className="p-8 text-center text-baseMuted">로딩 중...</div>
                  ) : filteredRooms.length === 0 ? (
                    <div className="p-8 text-center text-baseMuted">
                      <p className="font-medium">채팅방이 없습니다</p>
                      <p className="text-sm mt-1">연락처 검색에서 대화를 시작하세요</p>
                    </div>
                  ) : (
                    filteredRooms.map((room) => {
                      const name = getChatRoomDisplayName(room);
                      const deptLabel = room.isGroup ? "그룹" : getDirectDeptLabel(room);
                      const otherEmail = room.isGroup ? "" : (getDirectOtherEmail(room) || "");
                      const preview = room.lastMessage?.content || "";
                      const timeText = room.lastMessage?.createdAt ? formatTimeHHmm(room.lastMessage.createdAt) : "";
                      const muted = isRoomMuted(room.id);

                      return (
                        <div
                          key={room.id}
                          onClick={() => handleSelectRoom(room)}
                          onContextMenu={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                            setContextMenu({ x: e.clientX, y: e.clientY, room });
                          }}
                          className={`py-4 px-3 border-b border-baseBorder cursor-pointer transition-colors hover:bg-baseSurface ${
                            selectedRoomId === room.id ? "bg-baseSurface border-l-2 border-l-brandNavy" : ""
                          }`}
                        >
                          {/* ✅ 레이아웃 비율:
                              - (초록) 사용자정보: 비율 기반
                              - (빨강) 프리뷰: 남은 폭
                              - (우측) 뱃지/시간: 고정
                              - 초록↔빨강 사이 여백 0 (주황 제거)
                          */}
                          <div className="grid grid-cols-[minmax(0,32%)_1fr_56px] items-start gap-0">
                            {/* 좌측: 이름 + 부서(아래) */}
                            {/* 초록(사용자정보) 끝단과 빨강(프리뷰) 시작이 바로 맞닿게: 우측 패딩 0 */}
                            <div className="pr-0">
                              {/* ✅ 1번 예시 스타일: '부서 + (큰)이름' 한 줄, 다음 줄에 이메일 */}
                              <div className="flex items-baseline gap-2 leading-tight">
                                <div className="text-[11px] text-baseMuted font-semibold truncate max-w-[48px]">
                                  {deptLabel}
                                </div>
                                <div className="font-extrabold text-baseText truncate text-lg">
                                  {name}
                                </div>
                              </div>
                              <div className="text-[11px] text-baseMuted/80 leading-tight truncate mt-0.5">
                                {otherEmail}
                              </div>
                            </div>

                            {/* 가운데: 최근 메시지/파일명 프리뷰 (좌측 정렬, 깔끔하게) */}
                            <div className="min-w-0 flex items-center pl-0 pr-1">
                              <div className="text-sm text-slate-700 text-left w-full leading-snug line-clamp-2">
                                {preview || " "}
                              </div>
                            </div>

                            {/* 우측: 안읽음 + 시간 */}
                            <div className="flex flex-col items-end">
                              {!muted && room.unreadCount > 0 ? (
                                <span className="bg-brandOrange text-white text-xs font-bold px-2 py-0.5 rounded-full">
                                  {room.unreadCount}
                                </span>
                              ) : (
                                <span className="h-[18px] flex items-center">
                                  {muted && (
                                    <span className="material-symbols-outlined text-baseMuted text-[18px]" title="알림 꺼짐">
                                      notifications_off
                                    </span>
                                  )}
                                </span>
                              )}
                              <div className="mt-auto pt-2 text-[11px] text-baseMuted">{timeText}</div>
                            </div>
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>
              </div>
            ) : (
              // ==================== 연락처 검색 ====================
              <div className="flex flex-col h-full">
                {/* ✅ 단톡방 만들기: 선택/카운트/선택된 사람 표시 + 생성 */}
                {selectedContacts.length > 0 && (
                  <div className="mb-3 p-3 rounded-ui border border-baseBorder bg-baseSurface">
                    <div className="flex items-center justify-between gap-2">
                      <div className="text-xs font-semibold text-baseText">
                        선택됨 {selectedContacts.length}명
                      </div>
                      <button
                        type="button"
                        className="text-xs text-brandNavy font-semibold hover:underline"
                        onClick={() => { setSelectedContacts([]); setGroupName(""); }}
                      >
                        선택 해제
                      </button>
                    </div>
                    <div className="flex flex-wrap gap-2 mt-2">
                      {selectedContacts.map((u) => (
                        <div key={u.email} className="flex items-center gap-2 px-3 py-1 bg-white rounded-full border border-baseBorder text-xs">
                          <span className="font-semibold">{u.nickname}</span>
                          <button
                            type="button"
                            className="text-baseMuted hover:text-brandOrange"
                            onClick={() => removeSelectedContact(u.email)}
                            title="제거"
                          >
                            ×
                          </button>
                        </div>
                      ))}
                    </div>
                    <div className="flex gap-2 mt-3">
                      <input
                        value={groupName}
                        onChange={(e) => setGroupName(e.target.value)}
                        placeholder="단톡방 이름(선택)"
                        className="flex-1 px-3 py-2 rounded-ui border border-baseBorder text-sm focus:outline-none focus:border-brandNavy bg-white"
                      />
                      <button
                        type="button"
                        onClick={handleCreateGroup}
                        disabled={selectedContacts.length < 2}
                        className="px-4 py-2 rounded-ui bg-brandNavy text-white text-sm font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        방 만들기
                      </button>
                    </div>
                    <div className="text-[11px] text-baseMuted mt-2">
                      단톡방은 최소 2명 이상 선택해야 합니다.
                    </div>
                  </div>
                )}

                {/* 부서 필터 */}
                <div className="flex flex-wrap gap-2 mb-3">
                  <button
                    onClick={() => setSelectedDepartment("")}
                    className={`px-3 py-1.5 rounded-ui text-xs font-semibold transition-all ${
                      selectedDepartment === ""
                        ? "bg-brandNavy text-white"
                        : "bg-baseSurface text-baseMuted hover:text-baseText"
                    }`}
                  >
                    전체
                  </button>
                  {departments.map((dept) => (
                    <button
                      key={dept.value}
                      onClick={() => setSelectedDepartment(dept.value)}
                      className={`px-3 py-1.5 rounded-ui text-xs font-semibold transition-all ${
                        selectedDepartment === dept.value
                          ? "bg-brandNavy text-white"
                          : "bg-baseSurface text-baseMuted hover:text-baseText"
                      }`}
                    >
                      {dept.label}
                    </button>
                  ))}
                </div>

                {/* 검색 입력 */}
                <input
                  type="text"
                  value={searchKeyword}
                  onChange={(e) => setSearchKeyword(e.target.value)}
                  placeholder="이름으로 검색..."
                  className="w-full px-4 py-2.5 border border-baseBorder rounded-ui text-sm mb-3 focus:outline-none focus:border-brandNavy"
                />

                {/* 검색 결과 */}
                <div className="flex-1 overflow-y-auto border border-baseBorder rounded-ui">
                  {searchLoading ? (
                    <div className="p-8 text-center text-baseMuted">검색 중...</div>
                  ) : !selectedDepartment && searchKeyword.trim().length < 2 ? (
                    <div className="p-8 text-center text-baseMuted">
                      <p className="font-medium">부서를 선택하거나</p>
                      <p className="text-sm mt-1">이름을 검색하세요</p>
                    </div>
                  ) : searchResults.length === 0 ? (
                    <div className="p-8 text-center text-baseMuted">검색 결과가 없습니다</div>
                  ) : (
                    searchResults.map((user) => (
                      <div
                        key={user.email}
                        className={`p-4 border-b border-baseBorder hover:bg-baseSurface transition-colors ${
                          selectedContacts.some((u) => u.email === user.email) ? "bg-baseSurface" : ""
                        }`}
                      >
                        <div className="flex items-center justify-between">
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center gap-2 mb-1">
                              <input
                                type="checkbox"
                                checked={selectedContacts.some((u) => u.email === user.email)}
                                onChange={() => toggleContact(user)}
                                className="accent-brandNavy"
                                title="선택"
                              />
                              <span className="text-xs text-baseMuted">
                                {getDepartmentLabel(user.department)}
                              </span>
                              <span className="font-semibold text-baseText">
                                {user.nickname}
                              </span>
                            </div>
                            <p className="text-xs text-baseMuted truncate">{user.email}</p>
                          </div>
                          <button
                            onClick={() => handleStartChat(user)}
                            className="px-4 py-2 bg-brandNavy text-white text-xs font-semibold rounded-ui hover:opacity-90 transition-all"
                          >
                            1:1 대화
                          </button>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* 모달들 */}
      <AiWarningModal
        isOpen={showWarningModal}
        onSelectOn={handleWarningSelectOn}
        onSelectOff={handleWarningSelectOff}
      />
      <AiForceModal isOpen={showForceModal} onConfirm={handleForceConfirm} />
      <TicketConfirmModal
        isOpen={isConfirmModalOpen}
        onConfirm={() => {
          setIsConfirmModalOpen(false);
          setIsTicketModalOpen(true);
        }}
        onCancel={() => setIsConfirmModalOpen(false)}
      />
      {isTicketModalOpen && (
        <AIChatWidget
          onClose={() => setIsTicketModalOpen(false)}
          chatRoomId={selectedRoomId}
          currentUserId={currentUserId}
        />
      )}
      {isTicketDetailModalOpen && selectedTicketId && (
        <TicketDetailModal
          tno={selectedTicketId}
          onClose={() => {
            setIsTicketDetailModalOpen(false);
            setSelectedTicketId(null);
          }}
          onDelete={() => {
            setIsTicketDetailModalOpen(false);
            setSelectedTicketId(null);
          }}
        />
      )}
    </div>
  );
};

export default AIAssistantModal;



