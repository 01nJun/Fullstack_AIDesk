import { Suspense, lazy } from "react";
import { createBrowserRouter } from "react-router-dom";
import memberRouter from "./memberRouter";
import boardRouter from "./boardRouter";
import BoardIndex from "../pages/board/IndexPage";

const Loading = <div>Loading....</div>;

const Main = lazy(() => import("../pages/MainPage"));
const AdminPage = lazy(() => import("../pages/admin/AdminPage"));

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
    path: "admin",
    element: (
      <Suspense fallback={Loading}>
        <AdminPage />
      </Suspense>
    ),
  },
]);

export default root;
