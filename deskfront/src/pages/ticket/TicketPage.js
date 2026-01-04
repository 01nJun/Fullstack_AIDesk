import { useEffect } from "react";
import { useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import TicketComponent from "../../components/ticket/TicketComponent";

const TicketPage = () => {
  const loginState = useSelector((state) => state.loginSlice);
  const navigate = useNavigate();

  useEffect(() => {
    if (!loginState.email) {
      alert("로그인이 필요한 서비스입니다.");
      navigate("/member/login", { replace: true });
    }
  }, [loginState.email, navigate]);

  if (!loginState.email) return null;

  return (
    <div className="w-full bg-gray-100 min-h-screen py-8">
      <div className="max-w-[1280px] mx-auto px-4">
        <div className="bg-white shadow-2xl rounded-3xl p-8 min-h-[800px]">
          <TicketComponent />
        </div>
      </div>
    </div>
  );
};

export default TicketPage;
