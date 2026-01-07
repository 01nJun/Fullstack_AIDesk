import jwtAxios from "../util/jwtUtil";
const API_SERVER_HOST = process.env.REACT_APP_API_SERVER_HOST;


const host = `${API_SERVER_HOST}/api/files`;

export const getFileBoxList = async (email, type, pageParam, filterParam) => {
    const { page, size } = pageParam;
    const { keyword, sort } = filterParam;
    const res = await jwtAxios.get(`${host}/list`, {
        params: { email, type, page, size, keyword, sort }
    });
    return res.data;
};

// 안전한 파일 다운로드 함수
export const downloadFile = async (uuid, fileName) => {
    try {
        const res = await jwtAxios.get(`${host}/download/${uuid}`, {
            params: { originalName: fileName },
            responseType: 'blob' // Blob으로 받는 이유: 브라우저가 파일로 저장할 수 있도록 바이너리 형태 유지
        });
        // 보안 때문: 브라우저에서 자바스크립트가 직접 로컬 디스크에 파일을 쓸 수 없음
        const url = window.URL.createObjectURL(new Blob([res.data]));
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', fileName);
        document.body.appendChild(link);
        link.click();
        link.parentNode.removeChild(link);
        window.URL.revokeObjectURL(url);
    } catch (error) {
        console.error("Download error:", error);
        alert("파일 다운로드 중 오류가 발생했습니다.");
    }
};

export const deleteFile = async (uuid) => {
    const res = await jwtAxios.delete(`${host}/${uuid}`);
    return res.data;
};