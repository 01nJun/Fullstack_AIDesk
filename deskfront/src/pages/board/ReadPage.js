import { useParams } from "react-router-dom";
import ReadComponent from "../../components/board/ReadComponent";

const ReadPage = () => {
  const { bno } = useParams();
  return (
    <div className="p-4 w-full bg-baseBg">
      <ReadComponent bno={bno} />
    </div>
  );
};
export default ReadPage;
