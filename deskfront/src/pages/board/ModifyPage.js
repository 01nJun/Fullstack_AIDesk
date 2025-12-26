import { useParams } from "react-router-dom";
import ModifyComponent from "../../components/board/ModifyComponent";

const ModifyPage = () => {
  // 1. URL 파라미터에서 bno 추출 (예: /board/modify/40 -> 40)
  const { bno } = useParams();

  return (
    <div className="p-4 w-full bg-white">
      {/* 상단 타이틀 영역 */}
      <div className="text-3xl font-extrabold pb-4 border-b">게시글 수정</div>

      {/* 2. 실제 수정 기능을 담당하는 컴포넌트 호출 및 bno 전달 */}
      <ModifyComponent bno={bno} />
    </div>
  );
};

export default ModifyPage;
