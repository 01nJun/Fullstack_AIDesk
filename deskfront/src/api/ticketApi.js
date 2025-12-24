import jwtAxios from "../util/jwtUtil";
import { API_SERVER_HOST } from "./memberApi";

const host = `${API_SERVER_HOST}/api/tickets`;

export const getSentTickets = async (writer, pageParam, filter) => {
    const res = await jwtAxios.get(`${host}/sent`, { params: { writer, ...pageParam, ...filter } });
    return res.data;
};

export const getReceivedTickets = async (receiver, pageParam, filter) => {
    const res = await jwtAxios.get(`${host}/received`, { params: { receiver, ...pageParam, ...filter } });
    return res.data;
};

// 전체 티켓 조회
export const getAllTickets = async (email, pageParam, filter) => {
    const res = await jwtAxios.get(`${host}/all`, { params: { email, ...pageParam, ...filter } });
    return res.data;
};