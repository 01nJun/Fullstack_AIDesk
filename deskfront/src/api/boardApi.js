import jwtAxios from "../util/jwtUtil"; // 커스텀 axios 임포트
const API_SERVER_HOST = process.env.REACT_APP_API_SERVER_HOST;


const prefix = `${API_SERVER_HOST}/api/board`;

// 1. 목록 가져오기
export const getList = async (pageParam) => {
  const { page, size, type, keyword, category } = pageParam;

  // jwtAxios가 알아서 헤더에 토큰을 넣어줍니다.
  const res = await jwtAxios.get(`${prefix}/list`, {
    params: {
      page,
      size,
      type: type || "t",
      keyword: keyword || "",
      category: category || "",
    }
  });

  return res.data;
};

//  상세 조회
export const getOne = async (bno) => {
  const res = await jwtAxios.get(`${prefix}/${bno}`);
  return res.data;
};

// 등록
export const postAdd = async (boardObj) => {
  const res = await jwtAxios.post(`${prefix}/`, boardObj);
  return res.data;
};

// 수정
export const putOne = async (bno, boardObj) => {
  const res = await jwtAxios.put(`${prefix}/${bno}`, boardObj);
  return res.data;
};

// 삭제
export const deleteOne = async (bno) => {
  const res = await jwtAxios.delete(`${prefix}/${bno}`);
  return res.data;
};

// 최근 공지사항 3개 가져오기
export const getRecentBoards = async () => {
    const res = await jwtAxios.get(`${prefix}/list`, { // 백엔드 list 로직이 PageRequestDTO를 받으므로 파라미터로 전달
        params: { page: 1, size: 3 }                   // 최신순 정렬은 보통 백엔드 기본값이거나 추가 파라미터 필요
    });
    return res.data.dtoList;
};