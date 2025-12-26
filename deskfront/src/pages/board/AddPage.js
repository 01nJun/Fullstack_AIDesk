import AddComponent from "../../components/board/AddComponent";

const AddPage = () => {
  return (
    <div className="w-full bg-white">
      {/* 제목 부분 */}

      {/* 실제 입력창 본체 */}
      <div className="p-4">
        <AddComponent />
      </div>
    </div>
  );
};

export default AddPage;
