import { Link } from "react-router-dom";
import { getKakaoLoginLink } from "../../api/kakaoApi";

const KakaoLoginComponent = () => {
  const link = getKakaoLoginLink();

  return (
    <Link to={link} className="block">
      <div className="flex justify-center items-center bg-[#FEE500] hover:bg-[#FADA0A] p-4 rounded-2xl shadow-md transition-all hover:scale-[1.02] active:scale-95 group">
        <span className="text-gray-900 font-black italic tracking-tighter text-lg uppercase flex items-center gap-3">
          <span className="text-2xl">💬</span> KAKAO LOGIN
        </span>
      </div>
    </Link>
  );
};

export default KakaoLoginComponent;