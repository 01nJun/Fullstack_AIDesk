import BasicMenu from "../components/menu/BasicMenu";
import PinDrawer from "../components/PinDrawer";

const BasicLayout = ({ children }) => {
  return (
    <>
      <BasicMenu></BasicMenu>
      <PinDrawer />
      <div>
        <main>
          {children}
        </main>


          {/* 상단 여백 py-40 wprj flex 제거 */}
          {/* <CartComponent /> */}

      </div>
    </>
  );
};

export default BasicLayout;
