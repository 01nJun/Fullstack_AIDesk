import JoinComponent from "../../components/member/JoinComponent";
import BasicMenu from "../../components/menu/BasicMenu";

const JoinPage = () => {
  return (
    <div className="w-full bg-gray-100 min-h-screen">
      <BasicMenu />
      <div className="max-w-[1280px] mx-auto px-4 py-16 flex justify-center items-center">
        <div className="w-full max-w-lg">
          <JoinComponent />
        </div>
      </div>
    </div>
  );
};

export default JoinPage;