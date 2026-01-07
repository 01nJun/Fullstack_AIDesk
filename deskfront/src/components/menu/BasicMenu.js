import { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { useSelector, useDispatch } from "react-redux";
import { logout } from "../../slices/loginSlice";
import CommonModal from "../common/CommonModal";
import useCustomLogin from "../../hooks/useCustomLogin";
import AIChatWidget from "./AIChatWidget"; // 같은 폴더 내 위치
import useCustomPin from "../../hooks/useCustomPin";

const BasicMenu = () => {
  const loginState = useSelector((state) => state.loginSlice);
  const dispatch = useDispatch();
  const location = useLocation();
  const { moveToPath } = useCustomLogin();
  const { resetPins } = useCustomPin();

  const [isLogoutModalOpen, setIsLogoutModalOpen] = useState(false);
  const [isAIWidgetOpen, setIsAIWidgetOpen] = useState(false);

  const isAdmin =
    loginState.roleNames && loginState.roleNames.includes("ADMIN");

  const handleClickLogout = () => setIsLogoutModalOpen(true);
  const handleConfirmLogout = () => {
    dispatch(logout());
    resetPins();
    setIsLogoutModalOpen(false);
    moveToPath("/");
  };
  const handleCloseModal = () => setIsLogoutModalOpen(false);

  const getMenuClass = (path) => {
    const baseClass = "px-4 py-2 font-medium transition-colors duration-200 ";
    const isActive =
      location.pathname === path ||
      (path !== "/" && location.pathname.startsWith(path));

    if (path === "/admin") {
      return isActive ? baseClass + "ui-nav-active" : baseClass + "ui-nav-link";
    }

    return isActive ? baseClass + "ui-nav-active" : baseClass + "ui-nav-link";
  };

  const openAIWidget = () => setIsAIWidgetOpen(true);
  const closeAIWidget = () => setIsAIWidgetOpen(false);

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

      {isAIWidgetOpen && <AIChatWidget onClose={closeAIWidget} />}

      <header className="w-full bg-baseBg border-b border-baseBorder shadow-ui sticky top-0 z-50">
        <div className="ui-container h-16 flex items-center justify-between">
          <div className="flex items-center gap-6">
            <Link to="/" className="flex items-center gap-2">
              <div className="w-8 h-8 bg-brandNavy rounded-ui flex items-center justify-center">
                <span className="text-white font-bold text-sm">TF</span>
              </div>
              <span className="text-xl font-semibold text-baseText tracking-tight">
                TaskFlow
              </span>
            </Link>

            {loginState.email && (
              <div className="hidden md:flex items-center text-xs bg-baseSurface px-3 py-1 rounded-full border border-baseBorder">
                <span className="font-medium text-baseMuted">
                  {loginState.department || "부서명"}
                </span>
              </div>
            )}
          </div>

          <nav className="hidden lg:flex items-center gap-1">
            <Link to="/" className={getMenuClass("/")}>
              대시보드
            </Link>
            <Link to="/chat/" className={getMenuClass("/chat/")}>
              채팅
            </Link>

            <button
              type="button"
              onClick={() => {
                if (loginState.email) {
                  openAIWidget();
                } else {
                  alert("로그인이 필요한 서비스입니다.");
                  moveToPath("/member/login");
                }
              }}
              className="ui-nav-link"
            >
              요청서
            </button>

            <Link to="/tickets/" className={getMenuClass("/tickets/")}>
              업무 현황
            </Link>
            <Link to="/file/" className={getMenuClass("/file/")}>
              파일함
            </Link>
            <Link to="/board" className={getMenuClass("/board")}>
              공지사항
            </Link>

            {isAdmin && (
              <Link to="/admin" className={getMenuClass("/admin")}>
                관리자
              </Link>
            )}
          </nav>

          <div className="flex items-center gap-4">
            {!loginState.email ? (
              <Link to="/member/login" className="ui-btn-primary">
                Login
              </Link>
            ) : (
              <div className="flex items-center gap-3">
                <Link
                  to="/member/modify"
                  className="flex flex-col items-end hidden sm:block hover:opacity-70 transition-opacity"
                >
                  <span className="text-xs text-baseMuted">Welcome</span>
                  <span className="text-sm font-semibold text-baseText">
                    {loginState.nickname}님
                  </span>
                </Link>
                <Link
                  to="/member/modify"
                  className="w-8 h-8 bg-baseSurface rounded-full flex items-center justify-center text-baseMuted border border-baseBorder hover:bg-baseSurface/80 transition-colors"
                >
                  👤
                </Link>
                <button
                  onClick={handleClickLogout}
                  className="ui-btn-ghost text-xs"
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
