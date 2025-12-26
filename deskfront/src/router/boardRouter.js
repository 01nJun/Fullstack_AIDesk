import { Suspense, lazy } from "react";
import { Navigate } from "react-router-dom";

const Loading = <div className={"bg-white text-2xl font-bold"}>Loading...</div>;

// 각 페이지 컴포넌트들을 lazy 로딩으로 불러옵니다.
// path: "list"
const BoardList = lazy(() => import("../pages/board/ListPage")); // 목록 페이지

const BoardRead = lazy(() => import("../pages/board/ReadPage")); // 상세 보기 페이지 (추가됨)
const BoardAdd = lazy(() => import("../pages/board/AddPage")); // 등록 페이지 (추가됨)
const BoardModify = lazy(() => import("../pages/board/ModifyPage")); // 수정 페이지 (추가됨)

const boardRouter = () => {
  return [
    {
      path: "list",
      element: (
        <Suspense fallback={Loading}>
          <BoardList />
        </Suspense>
      ),
    },
    {
      path: "",
      element: <Navigate replace to="list" />,
    },
    {
      // ★ 게시글 상세 조회 경로 추가
      // :bno는 변수입니다. /board/read/40 에서 40을 bno라는 이름으로 받습니다.
      path: "read/:bno",
      element: (
        <Suspense fallback={Loading}>
          <BoardRead />
        </Suspense>
      ),
    },
    {
      // ★ 게시글 등록 경로 추가
      path: "add",
      element: (
        <Suspense fallback={Loading}>
          <BoardAdd />
        </Suspense>
      ),
    },
    {
      // ★ 게시글 수정 경로 추가
      path: "modify/:bno",
      element: (
        <Suspense fallback={Loading}>
          <BoardModify />
        </Suspense>
      ),
    },
  ];
};

export default boardRouter;
