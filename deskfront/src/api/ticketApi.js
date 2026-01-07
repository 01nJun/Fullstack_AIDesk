import jwtAxios from "../util/jwtUtil";
const API_SERVER_HOST = process.env.REACT_APP_API_SERVER_HOST;


const host = `${API_SERVER_HOST}/api/tickets`;

// 보낸 티켓 조회
export const getSentTickets = async (writer, pageParam, filter) => {
    const res = await jwtAxios.get(`${host}/sent`, { params: { writer, ...pageParam, ...filter } });
    return res.data;
};

// 받은 티켓 조회
export const getReceivedTickets = async (receiver, pageParam, filter) => {
    const res = await jwtAxios.get(`${host}/received`, { params: { receiver, ...pageParam, ...filter } });
    return res.data;
};

// 전체 티켓 조회
export const getAllTickets = async (email, pageParam, filter) => {
    const res = await jwtAxios.get(`${host}/all`, { params: { email, ...pageParam, ...filter } });
    return res.data;
};

// 홈 화면용: 내가 받은 티켓 중 최신 3개 가져오기
export const getRecentReceivedTickets = async (receiver) => {
    const res = await jwtAxios.get(`${host}/received`, {
        params: { receiver, page: 1, size: 3, sort: "pno,desc" }
    });
    return res.data.dtoList;
};

// 홈 화면용: 통계 데이터 (안읽음 개수, 보낸 티켓 중 미완료 개수 등)
export const getTicketStats = async (email) => {
    const unreadRes = await jwtAxios.get(`${host}/received`, { params: { receiver: email, read: false, size: 1 } });
    const pendingSentRes = await jwtAxios.get(`${host}/sent`, { params: { writer: email, size: 1 } });

    return {
        unreadCount: unreadRes.data.totalCount,
        pendingSentCount: pendingSentRes.data.totalCount,
    };
};

// 받은 티켓 단일 조회
export const getReceivedTicketDetailByTno = async (tno, receiver, markAsRead = true) => {
    const res = await jwtAxios.get(`${host}/received/by-tno/${tno}`, {
        params: { receiver, markAsRead },
    });
    return res.data;
};

// 보낸 티켓 상세 조회
export const getSentTicketDetail = async (tno, writer) => {
    const res = await jwtAxios.get(`${host}/sent/${tno}`, {
        params: {writer,},
    });
    return res.data;
};

// 삭제 (발신 전용)
export const deleteTicket = async (tno, writer) => {
    const res = await jwtAxios.delete(`${host}/${tno}`, {
        params: {
          writer,
        },
    });
    return res.data;
};

// 상태 변경 (수신 전용)
export const changeTicketState = async (pno, receiver, state) => {
    const res = await jwtAxios.patch(`${host}/received/${pno}/state`, null, {
        params: { receiver, state },
    });
    return res.data;
};

export const getTicketDetailByTno = async (tno, currentUser, rowWriter) => {
    //writer인지 확인
    try {
        const sentTicket = await getSentTicketDetail(tno, currentUser);
        // writer 검증 통과했다면
        return {
            ticket: sentTicket,
            isWriter: true,
            isReceiver: false,
        };
    } catch (sentError) {
        // writer가 아니거나 보낸 티켓이 아닌 경우
        try {
            const receivedTicket = await getReceivedTicketDetailByTno(tno, currentUser, true);
            return {
                ticket: receivedTicket,
                isWriter: false,
                isReceiver: true,
            };
        } catch (receivedError) {
            throw new Error("이 티켓에 대한 접근 권한이 없습니다.");
        }
    }
};