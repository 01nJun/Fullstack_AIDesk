import { Suspense, lazy } from "react";
import { Navigate } from "react-router-dom";

const Loading = <div className="p-10 font-bold text-2xl">Loading....</div>;
const FileBoxPage = lazy(() => import("../pages/file/FileBoxPage"));

const fileRouter = () => {
  return [
    {
      path: "list",
      element: (
        <Suspense fallback={Loading}>
          <FileBoxPage />
        </Suspense>
      ),
    },
    {
      path: "",
      element: <Navigate replace to="list" />,
    }
  ];
};

export default fileRouter;