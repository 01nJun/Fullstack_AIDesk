import { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { useSelector, useDispatch } from "react-redux";
import { logout } from "../../slices/loginSlice";
import CommonModal from "../common/CommonModal";
import useCustomLogin from "../../hooks/useCustomLogin";
import AIAssistantModal from "./AIAssistantModal"; // 통합 AI 비서 모달 (채팅 + 업무티켓)
import useCustomPin from "../../hooks/useCustomPin";

const BasicMenu = () => {
  const loginState = useSelector((state) => state.loginSlice);
  const dispatch = useDispatch();
  const location = useLocation();
  const { moveToPath } = useCustomLogin();
  const { resetPins } = useCustomPin();

  const [isLogoutModalOpen, setIsLogoutModalOpen] = useState(false);
  const [isAIWidgetOpen, setIsAIWidgetOpen] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const isAdmin =
    loginState.roleNames && loginState.roleNames.includes("ADMIN");

  const handleClickLogout = () => {
    setIsLogoutModalOpen(true);
    setIsMobileMenuOpen(false);
  };
  const handleConfirmLogout = () => {
    dispatch(logout());
    resetPins();
    setIsLogoutModalOpen(false);
    setIsMobileMenuOpen(false);
    moveToPath("/");
  };
  const handleCloseModal = () => setIsLogoutModalOpen(false);

  const toggleMobileMenu = () => setIsMobileMenuOpen(!isMobileMenuOpen);
  const closeMobileMenu = () => setIsMobileMenuOpen(false);

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

  // 모바일 메뉴용 클래스 (border 없이)
  const getMobileMenuClass = (path) => {
    const baseClass =
      "block px-4 py-3 rounded-ui transition-colors duration-200 ";
    const isActive =
      location.pathname === path ||
      (path !== "/" && location.pathname.startsWith(path));

    return isActive
      ? baseClass + "bg-baseSurface text-brandNavy font-semibold"
      : baseClass + "text-baseText hover:bg-baseSurface";
  };

  const openAIWidget = () => {
    setIsAIWidgetOpen(true);
    setIsMobileMenuOpen(false);
  };
  const closeAIWidget = () => setIsAIWidgetOpen(false);

  const handleMobileMenuClick = (callback) => {
    if (callback) callback();
    closeMobileMenu();
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

      {isAIWidgetOpen && <AIAssistantModal onClose={closeAIWidget} />}

      <header className="relative w-full bg-baseBg border-b border-baseBorder shadow-ui sticky top-0 z-50">
        <div className="ui-container h-16 flex items-center justify-between">
          {/* 로고 (왼쪽) */}
          <div className="flex items-center gap-6">
            <Link
              to="/"
              className="flex items-center gap-2"
              onClick={closeMobileMenu}
            >
              <div className="w-8 h-8 bg-brandNavy rounded-ui flex items-center justify-center">
                <span className="text-white font-bold text-sm">TF</span>
              </div>
              <span className="text-xl font-semibold text-baseText tracking-tight">
                TaskFlow
              </span>
            </Link>

            {/* 부서명 (데스크톱만) */}
            {loginState.email && (
              <div className="hidden lg:flex items-center text-xs bg-baseSurface px-3 py-1 rounded-full border border-baseBorder">
                <span className="font-medium text-baseMuted">
                  {loginState.department || "부서명"}
                </span>
              </div>
            )}
          </div>

          {/* 데스크톱 네비게이션 */}
          <nav className="hidden lg:flex items-center gap-1">
            <Link to="/" className={getMenuClass("/")}>
              대시보드
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
              AI 비서
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

          {/* 오른쪽 버튼들 */}
          <div className="flex items-center gap-2 lg:gap-4">
            {/* 햄버거 버튼 (모바일/태블릿) */}
            <button
              onClick={toggleMobileMenu}
              className="lg:hidden p-2 rounded-ui hover:bg-baseSurface transition-colors"
              aria-label="메뉴 열기"
            >
              {isMobileMenuOpen ? (
                <svg
                  className="w-6 h-6 text-baseText"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M6 18L18 6M6 6l12 12"
                  />
                </svg>
              ) : (
                <svg
                  className="w-6 h-6 text-baseText"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M4 6h16M4 12h16M4 18h16"
                  />
                </svg>
              )}
            </button>

            {/* 로그인/로그아웃 버튼 */}
            {!loginState.email ? (
              <Link
                to="/member/login"
                className="ui-btn-primary text-sm px-4 py-2"
                onClick={closeMobileMenu}
              >
                Login
              </Link>
            ) : (
              <div className="flex items-center gap-2 lg:gap-3">
                {/* Welcome 메시지 (데스크톱만) */}
                <Link
                  to="/member/modify"
                  className="hidden lg:flex flex-col items-end hover:opacity-70 transition-opacity"
                  onClick={closeMobileMenu}
                >
                  <span className="text-xs text-baseMuted">Welcome</span>
                  <span className="text-sm font-semibold text-baseText">
                    {loginState.nickname}님
                  </span>
                </Link>
                {/* 프로필 아이콘 (데스크톱만) */}
                <Link
                  to="/member/modify"
                  className="hidden lg:flex w-8 h-8 bg-baseSurface rounded-full items-center justify-center text-baseMuted border border-baseBorder hover:bg-baseSurface/80 transition-colors"
                  onClick={closeMobileMenu}
                >
                  👤
                </Link>
                <button
                  onClick={handleClickLogout}
                  className="ui-btn-ghost text-xs px-3 py-2"
                >
                  Logout
                </button>
              </div>
            )}
          </div>
        </div>

        {/* 모바일 드롭다운 메뉴 */}
        <div
          className={`lg:hidden absolute top-16 left-0 right-0 bg-baseBg border-b border-baseBorder shadow-lg overflow-hidden transition-all duration-300 ease-in-out ${
            isMobileMenuOpen ? "max-h-[600px] opacity-100" : "max-h-0 opacity-0"
          }`}
        >
          <nav className="ui-container py-4 space-y-1">
            <button
              type="button"
              onClick={() =>
                handleMobileMenuClick(() => {
                  if (loginState.email) {
                    openAIWidget();
                  } else {
                    alert("로그인이 필요한 서비스입니다.");
                    moveToPath("/member/login");
                  }
                })
              }
              className={`w-full text-left ${getMobileMenuClass("/")}`}
            >
              AI 비서
            </button>
            <button
              type="button"
              onClick={() =>
                handleMobileMenuClick(() => {
                  if (loginState.email) {
                    openAIWidget();
                  } else {
                    alert("로그인이 필요한 서비스입니다.");
                    moveToPath("/member/login");
                  }
                })
              }
              className={`w-full text-left ${getMobileMenuClass("/")}`}
            >
              AI 비서
            </button>
            <Link
              to="/tickets/"
              className={getMobileMenuClass("/tickets/")}
              onClick={closeMobileMenu}
            >
              업무 현황
            </Link>
            <Link
              to="/file/"
              className={getMobileMenuClass("/file/")}
              onClick={closeMobileMenu}
            >
              파일함
            </Link>
            <Link
              to="/board"
              className={getMobileMenuClass("/board")}
              onClick={closeMobileMenu}
            >
              공지사항
            </Link>
            {isAdmin && (
              <Link
                to="/admin"
                className={getMobileMenuClass("/admin")}
                onClick={closeMobileMenu}
              >
                관리자
              </Link>
            )}
            {/* 모바일에서 부서명 표시 */}
            {loginState.email && (
              <div className="px-4 py-3 mt-2 pt-4 border-t border-baseBorder">
                <div className="flex items-center text-xs bg-baseSurface px-3 py-1 rounded-full border border-baseBorder w-fit">
                  <span className="font-medium text-baseMuted">
                    {loginState.department || "부서명"}
                  </span>
                </div>
              </div>
            )}
          </nav>
        </div>
      </header>
    </>
  );
};

export default BasicMenu;
