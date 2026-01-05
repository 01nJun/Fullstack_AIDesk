import React from 'react';
import ChatListComponent from '../../components/chat/ChatListComponent';
import useCustomLogin from '../../hooks/useCustomLogin';

const ChatListPage = () => {
  const { loginState } = useCustomLogin();
  const currentUserId = loginState?.email || '';

  if (!currentUserId) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-gray-500">로그인이 필요합니다.</div>
      </div>
    );
  }

  return <ChatListComponent currentUserId={currentUserId} />;
};

export default ChatListPage;