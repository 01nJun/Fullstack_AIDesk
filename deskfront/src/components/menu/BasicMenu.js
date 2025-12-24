import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useSelector, useDispatch } from "react-redux";
import { logout } from "../../slices/loginSlice"; // 로그아웃 액션 (경로 확인 필요)
import CommonModal from "../common/CommonModal"; // 공통 모달 임포트
import useCustomLogin from "../../hooks/useCustomLogin";

const BasicMenu = () => {
  const loginState = useSelector((state) => state.loginSlice);
  const dispatch = useDispatch();
  const { moveToPath } = useCustomLogin();

  // 모달 상태 관리
  const [isLogoutModalOpen, setIsLogoutModalOpen] = useState(false);

  // 관리자 권한 확인
  const isAdmin =
    loginState.roleNames && loginState.roleNames.includes("ADMIN");

  // 로그아웃 버튼 클릭 핸들러
  const handleClickLogout = () => {
    setIsLogoutModalOpen(true); // 모달 열기
  };

  // 모달 확인(Yes) 버튼 핸들러 -> 실제 로그아웃 수행

  const handleConfirmLogout = () => {
    dispatch(logout()); // Redux 상태 및 쿠키 초기화
    setIsLogoutModalOpen(false); // 모달 닫기
    // alert("로그아웃 되었습니다.");
    moveToPath("/"); // 메인으로 이동
  };

  // 모달 취소/닫기 핸들러
  const handleCloseModal = () => {
    setIsLogoutModalOpen(false);
  };

  return (
    <>
      {/* --- 공통 모달 (로그아웃 확인용) --- */}
      {isLogoutModalOpen && (
        <CommonModal
          isOpen={isLogoutModalOpen}
          title={"Logout Check"}
          content={"정말 로그아웃 하시겠습니까?"}
          callbackFn={handleConfirmLogout} // 확인 시 실행
          closeFn={handleCloseModal} // 취소 시 실행
        />
      )}

      <nav id="navbar" className="flex bg-blue-300">
        <div className="w-4/5 bg-gray-500">
          <ul className="flex p-4 text-white font-bold">
            <li className="pr-6 text-2xl">
              <Link to={"/"}>Main</Link>
            </li>
            <li className="pr-6 text-2xl">
              <Link to={"/about"}>About</Link>
            </li>

            {loginState.email ? (
              <>
                <li className="pr-6 text-2xl">
                  <Link to={"/todo/"}>Todo</Link>
                </li>
                <li className="pr-6 text-2xl">
                  <Link to={"/tickets/"}>tickets</Link>
                </li>

                {isAdmin && (
                  <li className="pr-6 text-2xl text-yellow-300">
                    <Link to={"/admin"}>Admin</Link>
                  </li>
                )}
              </>
            ) : (
              <></>
            )}
          </ul>
        </div>

        <div className="w-1/5 flex justify-end bg-orange-300 p-4 font-medium">
          {!loginState.email ? (
            <div className="text-white text-sm m-1 rounded cursor-pointer">
              <Link to={"/member/login"}>Login</Link>
            </div>
          ) : (
            <div
              className="text-white text-sm m-1 rounded cursor-pointer font-bold hover:text-gray-200"
              onClick={handleClickLogout} // Link 대신 클릭 이벤트 연결
            >
              Logout
            </div>
          )}
        </div>
      </nav>
    </>
  );
};

export default BasicMenu;
