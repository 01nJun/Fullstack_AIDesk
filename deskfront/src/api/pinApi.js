import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./memberApi";

const host = `${API_SERVER_HOST}/api/tickets/pins`;

export const getPinItems = async () => {
    const res = await jwtAxios.get(`${host}/items`);
    return res.data;
};

export const postTogglePin = async (tno) => {
    const res = await jwtAxios.post(`${host}/toggle/${tno}`);
    return res.data;
};