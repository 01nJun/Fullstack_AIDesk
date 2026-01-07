import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
import { getCookie } from "../util/cookieUtil";
const API_SERVER_HOST = process.env.REACT_APP_API_SERVER_HOST;


/**
 * WebSocket 채팅 클라이언트
 */
class ChatWebSocketClient {
  constructor() {
    this.client = null;
    this.currentRoomId = null;
    this.onMessageCallback = null;
    this.onConnectCallback = null;
    this.onDisconnectCallback = null;
    this.reconnectDelay = 5000; // 5초
    this.maxReconnectAttempts = 5;
    this.reconnectAttempts = 0;
  }

  /**
   * WebSocket 연결
   * @param {number} roomId - 채팅방 ID
   * @param {Function} onMessage - 메시지 수신 콜백
   * @param {Function} onConnect - 연결 성공 콜백 (선택)
   * @param {Function} onDisconnect - 연결 해제 콜백 (선택)
   */
  connect(roomId, onMessage, onConnect, onDisconnect) {
    if (this.client && this.client.connected) {
      // 이미 연결되어 있고 같은 방이면 재연결 불필요
      if (this.currentRoomId === roomId) {
        return;
      }
      // 다른 방이면 기존 연결 해제
      this.disconnect();
    }

    this.currentRoomId = roomId;
    this.onMessageCallback = onMessage;
    this.onConnectCallback = onConnect;
    this.onDisconnectCallback = onDisconnect;

    // JWT 토큰 가져오기
    const memberInfo = getCookie("member");
    if (!memberInfo || !memberInfo.accessToken) {
      console.error("JWT 토큰이 없습니다. 로그인이 필요합니다.");
      return;
    }

    const token = memberInfo.accessToken;

    // SockJS 연결
    const socket = new SockJS(`${API_SERVER_HOST}/ws`);
    this.client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      debug: (str) => {
        // 개발 환경에서만 로그 출력
        if (process.env.NODE_ENV === "development") {
          console.log("STOMP:", str);
        }
      },
      reconnectDelay: this.reconnectDelay,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        console.log("WebSocket 연결 성공");
        this.reconnectAttempts = 0;

        // 채팅방 구독
        this.client.subscribe(`/topic/chat/${roomId}`, (message) => {
          try {
            const data = JSON.parse(message.body);
            if (this.onMessageCallback) {
              this.onMessageCallback(data);
            }
          } catch (error) {
            console.error("메시지 파싱 실패:", error);
          }
        });

        // 연결 상태 콜백 호출
        if (this.onConnectCallback) {
          this.onConnectCallback();
        }
      },
      onStompError: (frame) => {
        console.error("STOMP 에러:", frame);
      },
      onWebSocketClose: () => {
        console.log("WebSocket 연결 종료");
        
        // 연결 해제 콜백 호출
        if (this.onDisconnectCallback) {
          this.onDisconnectCallback();
        }
        
        // 자동 재연결 시도
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
          this.reconnectAttempts++;
          setTimeout(() => {
            if (this.currentRoomId) {
              console.log(`재연결 시도 ${this.reconnectAttempts}/${this.maxReconnectAttempts}`);
              this.connect(this.currentRoomId, this.onMessageCallback, this.onConnectCallback, this.onDisconnectCallback);
            }
          }, this.reconnectDelay);
        } else {
          console.error("최대 재연결 시도 횟수 초과");
        }
      },
      onDisconnect: () => {
        console.log("WebSocket 연결 해제");
        
        // 연결 해제 콜백 호출
        if (this.onDisconnectCallback) {
          this.onDisconnectCallback();
        }
      },
    });

    this.client.activate();
  }

  /**
   * 메시지 전송
   * @param {number} roomId - 채팅방 ID
   * @param {Object} message - { content: string, ticketId?: number, messageType?: string, aiEnabled?: boolean }
   */
  send(roomId, { content, ticketId, messageType = "TEXT", aiEnabled = false }) {
    if (!this.client || !this.client.connected) {
      console.warn("WebSocket이 연결되지 않았습니다.");
      return false;
    }

    try {
      this.client.publish({
        destination: `/app/chat/send/${roomId}`,
        body: JSON.stringify({
          content,
          ticketId,
          messageType,
          aiEnabled,
        }),
      });
      return true;
    } catch (error) {
      console.error("메시지 전송 실패:", error);
      return false;
    }
  }

  /**
   * 연결 해제
   */
  disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
    this.currentRoomId = null;
    this.onMessageCallback = null;
    this.onConnectCallback = null;
    this.onDisconnectCallback = null;
    this.reconnectAttempts = 0;
  }

  /**
   * 연결 상태 확인
   */
  isConnected() {
    return this.client && this.client.connected;
  }
}

// 싱글톤 인스턴스
const chatWsClient = new ChatWebSocketClient();

export default chatWsClient;

