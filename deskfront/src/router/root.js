import { Suspense, lazy } from "react";
import { createBrowserRouter } from "react-router-dom";
import memberRouter from "./memberRouter";
import ticketRouter from "./ticketRouter";
import adminRouter from "./adminRouter";
import fileRouter from "./fileRouter";
import boardRouter from "./boardRouter";
import BoardIndex from "../pages/board/IndexPage";
import LoadingModal from "../components/common/LoadingModal";


const Loading = <LoadingModal isOpen={true} message="로딩 중입니다" />;

const Main = lazy(() => import("../pages/MainPage"));
const TicketIndex = lazy(() => import("../pages/ticket/IndexPage"));
const AdminIndex = lazy(() => import("../pages/admin/IndexPage"));
const FileIndex = lazy(() => import("../pages/file/IndexPage"));

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
