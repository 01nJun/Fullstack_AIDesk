import React, { useMemo, useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import ChatRoom from "../../components/chat/ChatRoom";
import useCustomLogin from "../../hooks/useCustomLogin";
import { getChatRoom } from "../../api/chatApi";

const ChatPage = () => {
  const { chatRoomId } = useParams();
  const navigate = useNavigate();
  const { loginState } = useCustomLogin();
  const currentUserId = loginState?.email || "";

  const [chatRoomInfo, setChatRoomInfo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // 채팅방 정보 로드
  useEffect(() => {
    const loadChatRoom = async () => {
      const id = Number(chatRoomId);
      if (!id) {
        setError("잘못된 채팅방 ID입니다.");
        setLoading(false);
        return;
      }

      setLoading(true);
      setError(null);
      try {
        const room = await getChatRoom(id);
        // 백엔드 응답을 프론트엔드 형식으로 변환
        const otherParticipants = room.participants?.filter(
          (p) => p.userId !== currentUserId
        ) || [];

        const transformedRoom = {
          id: room.id,
          isGroup: room.roomType === "GROUP",
          name: room.name,
          participants: room.participants?.map((p) => p.userId) || [],
          participantIds: room.participants?.map((p) => p.userId) || [],
          participantInfo: room.participants?.map((p) => ({
            email: p.userId,
            nickname: p.nickname || p.userId,
          })) || [],
          // 1:1 채팅용
          user1Id: currentUserId,
          user2Id: otherParticipants.length > 0 ? otherParticipants[0].userId : null,
        };
        setChatRoomInfo(transformedRoom);
      } catch (err) {
        console.error("채팅방 정보 로드 실패:", err);
        setError("채팅방 정보를 불러오는데 실패했습니다.");
      } finally {
        setLoading(false);
      }
    };

    if (currentUserId) {
      loadChatRoom();
    }
  }, [chatRoomId, currentUserId]);

  if (!currentUserId) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-gray-500">로그인이 필요합니다.</div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-gray-500">로딩 중...</div>
      </div>
    );
  }

  if (error || !chatRoomInfo) {
    return (
      <div className="flex items-center justify-center h-screen flex-col gap-4">
        <div className="text-gray-500">{error || "존재하지 않는 채팅방입니다."}</div>
        <button
          className="px-4 py-2 rounded-lg bg-gray-900 text-white"
          onClick={() => navigate("/chat")}
        >
          목록으로
        </button>
      </div>
    );
  }

  // 1:1 채팅 상대 계산
  const otherUserId =
    !chatRoomInfo.isGroup
      ? chatRoomInfo.user1Id === currentUserId
        ? chatRoomInfo.user2Id
        : chatRoomInfo.user1Id
      : null;

  return (
    <ChatRoom
      chatRoomId={Number(chatRoomId)}
      currentUserId={currentUserId}
      otherUserId={otherUserId}
      chatRoomInfo={chatRoomInfo}
    />
  );
};

export default ChatPage;
