import ModifyComponent from "../../components/member/ModifyComponent";
import BasicMenu from "../../components/menu/BasicMenu";

const ModfyPage = () => {
  return (
    <div className="w-full bg-gray-100 min-h-screen">
      <BasicMenu />
      <div className="max-w-[1280px] mx-auto px-4 py-16 flex justify-center items-center">
        <div className="w-full max-w-lg">
          <ModifyComponent />
        </div>
      </div>
    </div>
  );
};

export default ModfyPage;