import { Suspense, lazy } from "react";
import { createBrowserRouter } from "react-router-dom";
import memberRouter from "./memberRouter";
import ticketRouter from "./ticketRouter";
import adminRouter from "./adminRouter";
import fileRouter from "./fileRouter";
import boardRouter from "./boardRouter";
import chatRouter from "./chatRouter";
import BoardIndex from "../pages/board/IndexPage";


const Loading = <div>Loading....</div>;

const Main = lazy(() => import("../pages/MainPage"));
const TicketIndex = lazy(() => import("../pages/ticket/IndexPage"));
const AdminIndex = lazy(() => import("../pages/admin/IndexPage"));
const FileIndex = lazy(() => import("../pages/file/IndexPage"));
const ChatIndex = lazy(() => import("../pages/chat/IndexPage"));

const root = createBrowserRouter([
    {
        path: "/",
        element: (
            <Suspense fallback={Loading}>
                <Main />
            </Suspense>
        ),
    },
    {
        path: "board",
        element: (
            <Suspense fallback={Loading}>
                <BoardIndex />
            </Suspense>
        ),
        children: boardRouter(),
    },
    {
        path: "member",
        children: memberRouter(),
    },
    {
        path: "tickets",
        element: (
            <Suspense fallback={Loading}>
                <TicketIndex />
            </Suspense>
        ),
        children: ticketRouter(),
    },
    {
        path: "file",
        element: (
            <Suspense fallback={Loading}>
                <FileIndex />
            </Suspense>
        ),
        children: fileRouter(),
    },
    {
        path: "chat",
        element: (
            <Suspense fallback={Loading}>
                <ChatIndex />
            </Suspense>
        ),
        children: chatRouter(),
    },
    {
        path: "admin",
        element: (
            <Suspense fallback={Loading}>
                <AdminIndex />
            </Suspense>
        ),
        children: adminRouter(),
    },
]);

export default root;
