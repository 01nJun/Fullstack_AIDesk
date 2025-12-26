import { useState } from "react";
import {
  createSearchParams,
  useNavigate,
  useSearchParams,
} from "react-router-dom";

const getNum = (param, defaultValue) => {
  if (!param) return defaultValue;
  return parseInt(param);
};

const useCustomMove = () => {
  const navigate = useNavigate();
  const [refresh, setRefresh] = useState(false);
  const [queryParams] = useSearchParams();

  // 1. [수정] 주소창에서 page, size, keyword, type 외에 'category'를 추가로 읽어옵니다.
  const page = getNum(queryParams.get("page"), 1);
  const size = getNum(queryParams.get("size"), 10);
  const keyword = queryParams.get("keyword") || "";
  const type = queryParams.get("type") || "t";
  const category = queryParams.get("category") || ""; // <-- 카테고리 추가!

  // 2. [수정] 기본 쿼리 스트링에 'category'를 포함시킵니다.
  // 이렇게 해야 글을 읽고 돌아와도 내가 보던 카테고리가 유지돼요.
  const queryDefault = createSearchParams({
    page,
    size,
    keyword,
    type,
    category, // <-- 추가
  }).toString();

  const moveToList = (pageParam) => {
    let queryStr = "";

    if (pageParam) {
      const pageNum = getNum(pageParam.page, page);
      const sizeNum = getNum(pageParam.size, size);
      const keywordStr =
        pageParam.keyword !== undefined ? pageParam.keyword : keyword;
      const typeStr = pageParam.type !== undefined ? pageParam.type : type;

      // 3. [추가] 카테고리 파라미터 처리
      const categoryStr =
        pageParam.category !== undefined ? pageParam.category : category;

      queryStr = createSearchParams({
        page: pageNum,
        size: sizeNum,
        keyword: keywordStr,
        type: typeStr,
        category: categoryStr, // <-- 주소창에 category 달아주기
      }).toString();
    } else {
      queryStr = queryDefault;
    }

    navigate({ pathname: `../list`, search: queryStr });
    setRefresh(!refresh);
  };

  const moveToAdd = () => {
    navigate({ pathname: `../add`, search: queryDefault });
  };

  const moveToModify = (num) => {
    navigate({ pathname: `../modify/${num}`, search: queryDefault });
  };

  const moveToRead = (num) => {
    navigate({ pathname: `../read/${num}`, search: queryDefault });
  };

  return {
    moveToRead,
    moveToModify,
    moveToList,
    moveToAdd,
    page,
    size,
    keyword,
    type,
    category, // 4. [중요] ListComponent에서 쓸 수 있도록 리턴에 꼭 넣어줘야 해요!
    refresh,
  };
};

export default useCustomMove;
