import { Outlet, useNavigate } from "react-router-dom";
import BasicLayout from "../../layouts/BasicLayout";
import { useCallback } from "react";

const IndexPage = () => {
  const navigate = useNavigate();

  const handleClickAdd = useCallback(() => {
    navigate({ pathname: "add" });
  });
  return (
    <BasicLayout>
      <div className="text-black font-extrabold -mt-10"></div>
      <div className="w-full flex m-2 p-2 ">
        <div
          className="text-xl m-1 p-2 w-20 font-extrabold  text-center underline"
          onClick={handleClickAdd}
        ></div>
      </div>
      <div className="flex flex-wrap w-full">
        <Outlet />
      </div>
    </BasicLayout>
  );
};

export default IndexPage;
