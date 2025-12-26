import BasicMenu from "../components/menu/BasicMenu";

const BasicLayout = ({ children }) => {
  return (
    <>
      {/* 상단 메뉴바 */}
      <BasicMenu />

      {/* 메인 컨텐츠: 카트를 없애고 화면을 꽉 차게(w-full) 사용합니다. */}
      <div className="bg-white my-5 w-full flex flex-col">
        <main className="w-full px-5 py-5">{children}</main>
      </div>
    </>
  );
};

export default BasicLayout;