import { useEffect } from "react";
import ChatListComponent from '../../components/chat/ChatListComponent';
import useCustomLogin from '../../hooks/useCustomLogin';
import { useNavigate } from "react-router-dom";

const ChatListPage = () => {
  const { loginState } = useCustomLogin();
  const currentUserId = loginState?.email || '';
  const navigate = useNavigate();

 useEffect(() => {
    if (!loginState.email) {
      alert("로그인이 필요한 서비스입니다.");
      navigate("/member/login", { replace: true });
    }
  }, [loginState.email, navigate]);


  if (!currentUserId) {
    return (
      <div className="chat-shell w-full bg-baseBg min-h-screen py-6 lg:py-8">
        <div className="ui-container">
          <div className="p-6 lg:p-8 min-h-[600px]">
            <div className="bg-baseBg rounded-ui shadow-ui border border-baseBorder p-8 min-h-[600px] flex items-center justify-center">
              <div className="text-baseMuted">로그인이 필요합니다.</div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="chat-shell w-full bg-baseBg min-h-screen py-6 lg:py-8">
      <div className="ui-container">
        <div className="p-6 lg:p-8 min-h-[600px]">
          <ChatListComponent currentUserId={currentUserId} />
        </div>
      </div>
    </div>
  );
};

export default ChatListPage;