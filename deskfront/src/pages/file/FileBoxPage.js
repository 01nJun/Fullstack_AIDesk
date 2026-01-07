import { useEffect } from "react";
import { useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import FileBoxComponent from "../../components/file/FileBoxComponent";

const FileBoxPage = () => {
    const loginState = useSelector((state) => state.loginSlice);
    const navigate = useNavigate();

  useEffect(() => {
    if (!loginState.email) {
      alert("로그인이 필요한 서비스입니다.");
      navigate("/member/login", { replace: true });
    }
  }, [loginState.email, navigate]);

  return (
    <div className="w-full bg-baseBg min-h-screen py-6 lg:py-8">
      <div className="ui-container">
        <div className="p-6 lg:p-8 min-h-[600px]">
            <FileBoxComponent />
        </div>
      </div>
    </div>
  );
};

export default FileBoxPage;