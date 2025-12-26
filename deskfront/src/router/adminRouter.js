import { Suspense, lazy } from "react";
import { Navigate } from "react-router-dom";

const Loading = <div className="p-10 font-bold text-2xl">Loading....</div>;
const AdminPage = lazy(() => import("../pages/admin/AdminPage"));

const adminRouter = () => {
  return [
    {
      path: "list",
      element: (
        <Suspense fallback={Loading}>
          <AdminPage />
        </Suspense>
      ),
    },
    {
      path: "",
      element: <Navigate replace to="list" />,
    }
  ];
};

export default adminRouter;