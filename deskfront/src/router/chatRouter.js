import { Suspense, lazy } from "react";
import { Navigate } from "react-router-dom";

const Loading = <div className="p-10 font-bold text-2xl">Loading....</div>;
const ChatListPage = lazy(() => import("../pages/chat/ChatListPage"));
const ChatPage = lazy(() => import("../pages/chat/ChatPage"));

const chatRouter = () => {
  return [
    {
      path: "",
      element: (
        <Suspense fallback={Loading}>
          <ChatListPage />
        </Suspense>
      ),
    },
    {
      path: ":chatRoomId",
      element: (
        <Suspense fallback={Loading}>
          <ChatPage />
        </Suspense>
      ),
    },
  ];
};

export default chatRouter;