import { Suspense, lazy } from "react";
import { createBrowserRouter } from "react-router-dom";
import memberRouter from "./memberRouter";

const Loading = <div>Loading....</div>;

const Main = lazy(() => import("../pages/MainPage"));
const AdminPage = lazy(() => import("../pages/admin/AdminPage"));
const TicketPage = lazy(() => import("../pages/ticket/TicketPage"));

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
    path: "member",
    children: memberRouter(),
  },
  {
       path: "tickets",
       element: <Suspense fallback={Loading}><TicketPage /></Suspense>
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
