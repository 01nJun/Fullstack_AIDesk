import React, { useState, useEffect, useRef } from "react";
import { useSelector } from "react-redux";
import { aiSecretaryApi } from "../../api/aiSecretaryApi";
import FilePreview from "../common/FilePreview";
import "./AIChatWidget.css";

const generateUUID = () => {
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function (c) {
    var r = (Math.random() * 16) | 0,
      v = c === "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
};

const AIChatWidget = ({ onClose }) => {
  const loginState = useSelector((state) => state.loginSlice);
  const currentUserDept = loginState.department || "Unknown";
  const currentUserEmail = loginState.email;

  const [conversationId] = useState(generateUUID());
  const [messages, setMessages] = useState([
    { role: "assistant", content: "안녕하세요. 어떤 업무를 도와드릴까요?" },
  ]);

  const [currentTicket, setCurrentTicket] = useState({
    title: "",
    content: "",
    purpose: "",
    requirement: "",
    grade: "MIDDLE",
    deadline: "",
    receivers: [],
  });

  const [selectedFiles, setSelectedFiles] = useState([]);
  const fileInputRef = useRef(null);
  const [targetDept, setTargetDept] = useState(null);
  const [isCompleted, setIsCompleted] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [submitSuccess, setSubmitSuccess] = useState(false);
  const [inputMessage, setInputMessage] = useState("");
  const messagesEndRef = useRef(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const handleManualChange = (e) => {
    const { name, value } = e.target;
    setCurrentTicket((prev) => {
      if (name === "receivers")
        return { ...prev, [name]: value.split(",").map((s) => s.trim()) };
      return { ...prev, [name]: value };
    });
  };

  const handleFileChange = (e) => {
    const files = Array.from(e.target.files);
    setSelectedFiles((prev) => [...prev, ...files]);
  };

  const removeFile = (index) => {
    setSelectedFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const isFormValid = () => {
    const t = currentTicket;
    // receiver가 없거나 빈 문자열이면 false
    const hasReceivers =
      t.receivers && t.receivers.length > 0 && t.receivers[0] !== "";
    return t.title?.trim() && t.content?.trim() && hasReceivers && t.deadline;
  };

  const handleSendMessage = async () => {
    if (!inputMessage.trim()) return;
    const userMsg = { role: "user", content: inputMessage };
    setMessages((prev) => [...prev, userMsg]);
    setInputMessage("");
    setIsLoading(true);

    try {
      const response = await aiSecretaryApi.sendMessage({
        conversation_id: conversationId,
        sender_dept: currentUserDept,
        target_dept: targetDept,
        user_input: userMsg.content,
        chat_history: messages,
        current_ticket: currentTicket,
      });

      // [핵심 수정] Java Backend(CamelCase) 응답에 맞춰 변수명 수정
      // ai_message -> aiMessage
      // updated_ticket -> updatedTicket
      // is_completed -> isCompleted
      // identified_target_dept -> identifiedTargetDept

      setMessages((prev) => [
        ...prev,
        { role: "assistant", content: response.aiMessage },
      ]);

      // AI가 분석한 티켓 정보를 상태에 반영 (이제 정상적으로 들어옵니다)
      if (response.updatedTicket) {
        setCurrentTicket(response.updatedTicket);
      }

      setIsCompleted(response.isCompleted);

      if (response.identifiedTargetDept) {
        setTargetDept(response.identifiedTargetDept);
      }
    } catch (error) {
      console.error("AI Chat Error:", error);
      setMessages((prev) => [
        ...prev,
        { role: "assistant", content: "AI 서버 오류가 발생했습니다." },
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSubmitTicket = async () => {
    if (!isFormValid()) {
      alert("필수 항목(제목, 내용, 담당자, 마감일)을 모두 확인해 주세요.");
      return;
    }

    setIsLoading(true);

    try {
      await aiSecretaryApi.submitTicket(
        currentTicket,
        selectedFiles,
        currentUserEmail
      );
      setSubmitSuccess(true);
      setTimeout(() => {
        onClose();
      }, 2000);
    } catch (error) {
      console.error("전송 중 에러 발생:", error);
      alert("티켓 전송에 실패했습니다. 로그를 확인하세요.");
      setIsLoading(false);
    }
  };

  const handleReset = () => {
    if (window.confirm("초기화하시겠습니까?")) {
      setMessages([{ role: "assistant", content: "대화가 초기화되었습니다." }]);
      setCurrentTicket({
        title: "",
        content: "",
        purpose: "",
        requirement: "",
        grade: "MIDDLE",
        deadline: "",
        receivers: [],
      });
      setSelectedFiles([]);
      setTargetDept(null);
      setIsCompleted(false);
      setSubmitSuccess(false);
    }
  };

  return (
    <div className="ai-widget-overlay">
      <div className="ai-widget-container">
        <div className="ai-widget-header">
          <h2>🤖 AI 업무 비서</h2>
          <button className="close-btn" onClick={onClose}>
            &times;
          </button>
        </div>

        <div className="ai-widget-body">
          <div className="ai-chat-section">
            <div className="chat-messages-area">
              {messages.map((msg, idx) => (
                <div key={idx} className={`chat-message ${msg.role}`}>
                  <div className="chat-avatar">
                    {msg.role === "user" ? "👤" : "🤖"}
                  </div>
                  <div className="chat-bubble">{msg.content}</div>
                </div>
              ))}
              <div ref={messagesEndRef} />
            </div>

            <div className="chat-input-wrapper">
              <button
                type="button"
                style={{ marginRight: "10px", fontSize: "20px" }}
                onClick={() => fileInputRef.current.click()}
              >
                📎
              </button>
              <input
                type="file"
                multiple
                className="hidden"
                ref={fileInputRef}
                onChange={handleFileChange}
              />

              <input
                type="text"
                className="chat-input"
                placeholder="업무 요청 내용을 입력하세요..."
                value={inputMessage}
                onChange={(e) => setInputMessage(e.target.value)}
                onKeyDown={(e) =>
                  e.key === "Enter" && !e.shiftKey && handleSendMessage()
                }
              />
              <button
                className="reset-btn"
                onClick={handleSendMessage}
                disabled={isLoading || submitSuccess || !inputMessage.trim()}
              >
                전송
              </button>
            </div>
          </div>

          <div className="ai-ticket-section">
            <div className="ticket-header-row">
              <span className="dept-badge">To: {targetDept || "(미지정)"}</span>
              <button className="reset-btn" onClick={handleReset}>
                🔄 초기화
              </button>
            </div>

            <div className="ticket-preview-box">
              <div className="form-group">
                <label>
                  제목 <span className="text-red-500">*</span>
                </label>
                <input
                  name="title"
                  className="st-input"
                  value={currentTicket?.title || ""}
                  onChange={handleManualChange}
                />
              </div>
              <div className="form-group">
                <label>
                  요약 <span className="text-red-500">*</span>
                </label>
                <textarea
                  name="content"
                  className="st-textarea"
                  rows="3"
                  value={currentTicket?.content || ""}
                  onChange={handleManualChange}
                />
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label>
                    목적 <span className="text-red-500">*</span>
                  </label>
                  <textarea
                    name="purpose"
                    className="st-textarea"
                    rows="2"
                    value={currentTicket?.purpose || ""}
                    onChange={handleManualChange}
                  />
                </div>
                <div className="form-group">
                  <label>
                    상세 <span className="text-red-500">*</span>
                  </label>
                  <textarea
                    name="requirement"
                    className="st-textarea"
                    rows="2"
                    value={currentTicket?.requirement || ""}
                    onChange={handleManualChange}
                  />
                </div>
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label>
                    마감일 <span className="text-red-500">*</span>
                  </label>
                  <input
                    name="deadline"
                    type="date"
                    className="st-input"
                    value={currentTicket?.deadline || ""}
                    onChange={handleManualChange}
                  />
                </div>
                <div className="form-group">
                  <label>중요도</label>
                  <select
                    name="grade"
                    className="st-input"
                    value={currentTicket?.grade || "MIDDLE"}
                    onChange={handleManualChange}
                  >
                    <option value="LOW">LOW</option>
                    <option value="MIDDLE">MIDDLE</option>
                    <option value="HIGH">HIGH</option>
                    <option value="URGENT">URGENT</option>
                  </select>
                </div>
              </div>
              <div className="form-group">
                <label>
                  담당자 <span className="text-red-500">*</span>
                </label>
                <input
                  name="receivers"
                  className="st-input"
                  value={currentTicket?.receivers?.join(",") || ""}
                  onChange={handleManualChange}
                />
              </div>

              <div className="form-group">
                <label>첨부 파일 ({selectedFiles.length})</label>
                <div
                  style={{
                    display: "grid",
                    gridTemplateColumns: "repeat(5, 1fr)",
                    gap: "5px",
                    marginTop: "10px",
                  }}
                >
                  {selectedFiles.map((file, idx) => (
                    <div
                      key={idx}
                      style={{
                        position: "relative",
                        aspectRatio: "1/1",
                        border: "1px solid #ddd",
                        borderRadius: "8px",
                        overflow: "hidden",
                      }}
                    >
                      <FilePreview file={file} isLocal={true} />
                      <button
                        onClick={() => removeFile(idx)}
                        style={{
                          position: "absolute",
                          top: 0,
                          right: 0,
                          background: "rgba(0,0,0,0.5)",
                          color: "white",
                          border: "none",
                          cursor: "pointer",
                          width: "20px",
                        }}
                      >
                        ×
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {submitSuccess ? (
              <div className="success-box">✅ 티켓 전송 완료</div>
            ) : (
              (isCompleted || isFormValid()) && (
                <button
                  className="submit-btn"
                  onClick={handleSubmitTicket}
                  disabled={isLoading}
                >
                  {isLoading ? "전송 중..." : "🚀 업무 티켓 전송"}
                </button>
              )
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default AIChatWidget;
