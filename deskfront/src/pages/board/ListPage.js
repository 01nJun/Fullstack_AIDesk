// src/pages/board/ListPage.js
import ListComponent from "../../components/board/ListComponent";

const ListPage = () => {
  return (
    <div className="w-full bg-baseBg min-h-screen py-6 lg:py-8">
      <div className="ui-container">
        <div className="p-6 lg:p-8 min-h-[600px]">
          <ListComponent />
          </div>
      </div>
    </div>
  );
};

export default ListPage;
