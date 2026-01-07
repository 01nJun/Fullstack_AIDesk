import jwtAxios from "../util/jwtUtil";
const API_SERVER_HOST = process.env.REACT_APP_API_SERVER_HOST;


const host = `${API_SERVER_HOST}/api/tickets/pins`;

export const getPinItems = async () => {
    const res = await jwtAxios.get(`${host}/items`);
    return res.data;
};

export const postTogglePin = async (tno) => {
    const res = await jwtAxios.post(`${host}/toggle/${tno}`);
    return res.data;
};