import { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { useSelector, useDispatch } from "react-redux";
import { logout } from "../../slices/loginSlice";
import CommonModal from "../common/CommonModal";
import useCustomLogin from "../../hooks/useCustomLogin";
import useCustomPin from "../../hooks/useCustomPin";

const BasicMenu = () => {
  const loginState = useSelector((state) => state.loginSlice);
  const dispatch = useDispatch();
  const location = useLocation(); // 현재 경로 확인을 위함
  const { moveToPath } = useCustomLogin();
  const { resetPins } = useCustomPin();

  const [isLogoutModalOpen, setIsLogoutModalOpen] = useState(false);

  // 관리자 권한 확인
  const isAdmin = loginState.roleNames && loginState.roleNames.includes("ADMIN");

  // 로그아웃 핸들러
  const handleClickLogout = () => setIsLogoutModalOpen(true);

  const handleConfirmLogout = () => {
    dispatch(logout());
    resetPins();
    setIsLogoutModalOpen(false);
    moveToPath("/");
  };

  const handleCloseModal = () => setIsLogoutModalOpen(false);

  // 활성 메뉴 스타일 결정 함수
  const getMenuClass = (path) => {
    const baseClass = "px-4 py-2 font-medium transition-colors duration-200 ";
    return location.pathname === path
      ? baseClass + "text-indigo-600 border-b-2 border-indigo-600"
      : baseClass + "text-gray-500 hover:text-indigo-500";
  };

  return (
    <>
      {isLogoutModalOpen && (
        <CommonModal
          isOpen={isLogoutModalOpen}
          title={"Logout Check"}
          content={"정말 로그아웃 하시겠습니까?"}
          callbackFn={handleConfirmLogout}
          closeFn={handleCloseModal}
        />
      )}

      <header className="w-full bg-white border-b border-gray-100 shadow-sm sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">

          {/* --- 왼쪽 영역: 로고 및 팀 정보 --- */}
          <div className="flex items-center space-x-6">
            <Link to="/" className="flex items-center space-x-2">
              <div className="w-8 h-8 bg-indigo-600 rounded-lg flex items-center justify-center">
                <span className="text-white font-bold text-sm">TF</span>
              </div>
              <span className="text-xl font-bold text-gray-800 tracking-tight">TaskFlow</span>
            </Link>

            <div className="hidden md:flex items-center text-xs text-gray-400 bg-gray-50 px-3 py-1 rounded-full border border-gray-100">
              <span className="font-medium text-gray-500">회사명</span>
              <span className="mx-2">/</span>
              <span>디자인팀</span>
            </div>
          </div>

          {/* --- 중앙 영역: 메인 네비게이션 --- */}
          <nav className="hidden lg:flex items-center space-x-2">
            <Link to="/" className={getMenuClass("/")}>대시보드</Link>

            {loginState.email && (
              <>
                <Link to="/tickets/" className={getMenuClass("/tickets/")}>티켓</Link>
                <Link to="/about" className={getMenuClass("/about")}>공지사항</Link>
                <Link to="/todo/" className={getMenuClass("/todo/")}>할일목록</Link>
                {isAdmin && (
                  <Link to="/admin" className={getMenuClass("/admin")}>
                    <span className="text-amber-500">관리자</span>
                  </Link>
                )}
              </>
            )}
          </nav>

          {/* --- 오른쪽 영역: 유저 프로필 및 로그인/아웃 --- */}
          <div className="flex items-center space-x-4">
            {!loginState.email ? (
              <Link
                to="/member/login"
                className="text-sm font-semibold text-white bg-indigo-600 px-5 py-2 rounded-lg hover:bg-indigo-700 transition-all shadow-md active:scale-95"
              >
                Login
              </Link>
            ) : (
              <div className="flex items-center space-x-3">
                <div className="flex flex-col items-end hidden sm:block">
                  <span className="text-xs text-gray-400">Welcome</span>
                  <span className="text-sm font-bold text-gray-700">{loginState.nickname}님</span>
                </div>
                <div className="w-8 h-8 bg-gray-100 rounded-full flex items-center justify-center text-gray-500 border border-gray-200">
                  👤
                </div>
                <button
                  onClick={handleClickLogout}
                  className="text-xs font-medium text-gray-400 hover:text-red-500 transition-colors border border-gray-200 px-2 py-1 rounded"
                >
                  Logout
                </button>
              </div>
            )}
          </div>
        </div>
      </header>
    </>
  );
};

export default BasicMenu;