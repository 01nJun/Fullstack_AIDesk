import { useCallback } from "react"; // [추가]
import { useDispatch, useSelector } from "react-redux";
import { getPinItemsAsync, togglePinAsync, clearPins } from "../slices/pinSlice";

const useCustomPin = () => {
  const pinItems = useSelector((state) => state.pinSlice);
  const dispatch = useDispatch();

  // useCallback으로 감싸서 함수의 참조값을 고정
  const refreshPins = useCallback(() => {
    dispatch(getPinItemsAsync());
  }, [dispatch]);

  const togglePin = useCallback((tno) => {
    dispatch(togglePinAsync(tno));
  }, [dispatch]);

  // 로그아웃 시 사용할 초기화 함수
  const resetPins = useCallback(() => {
    dispatch(clearPins());
  }, [dispatch]);

  const isPinned = (tno) => pinItems.some(item => item.tno === tno);

  return { pinItems, refreshPins, togglePin, isPinned, resetPins };
};

export default useCustomPin;