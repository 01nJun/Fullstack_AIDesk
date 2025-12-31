import React, { useState, useEffect, useRef } from "react";
import { useSelector } from "react-redux";
import { aiSecretaryApi } from "../../api/aiSecretaryApi";
import FilePreview from "../common/FilePreview";
import "./AIChatWidget.css";
import html2canvas from "html2canvas"; // ✅ PDF용
import jsPDF from "jspdf"; // ✅ PDF용

// PDF 관련 라이브러리 import
import html2canvas from "html2canvas";
import jsPDF from "jspdf";

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

  // Ref 정의
  const fileInputRef = useRef(null);
  const messagesEndRef = useRef(null);
  const pdfRef = useRef(null); // ✅ PDF 캡처 영역 참조용 Ref

  const [targetDept, setTargetDept] = useState(null);
  const [isCompleted, setIsCompleted] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [submitSuccess, setSubmitSuccess] = useState(false);
  const [inputMessage, setInputMessage] = useState("");

<<<<<<< HEAD
=======
  const audioInputRef = useRef(null); // ✅ 오디오 전용 input ref
  const pdfTargetRef = useRef(null); // ✅ PDF 변환 대상 영역 ref

>>>>>>> 9a26bee9327a8cbf266b266bfef68982fcfdeb40
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

<<<<<<< HEAD
  const removeFile = (index) => {
    setSelectedFiles((prev) => prev.filter((_, i) => i !== index));
  };

=======
  // ✅ [NEW] 오디오 파일 업로드 및 분석 요청
  const handleAudioUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const userMsg = {
      role: "user",
      content: `🎙️ 회의록 분석 요청: ${file.name}`,
    };
    setMessages((prev) => [...prev, userMsg]);
    setIsLoading(true);

    try {
      // Python 서버로 전송
      const response = await aiSecretaryApi.analyzeMeetingAudio(
        file,
        conversationId
      );

      if (response.updated_ticket) {
        setCurrentTicket(response.updated_ticket);

        let aiMsg = "✅ 회의록 분석이 완료되었습니다.";
        if (response.summary) {
          aiMsg += `\n\n[요약]\n${response.summary}`;
        }
        setMessages((prev) => [...prev, { role: "assistant", content: aiMsg }]);

        if (response.identified_target_dept)
          setTargetDept(response.identified_target_dept);
        setIsCompleted(true);
      } else {
        setMessages((prev) => [
          ...prev,
          { role: "assistant", content: "분석 결과가 충분하지 않습니다." },
        ]);
      }
    } catch (error) {
      console.error(error);
      setMessages((prev) => [
        ...prev,
        {
          role: "assistant",
          content: "회의록 분석 중 오류가 발생했습니다. (서버 연결 확인 필요)",
        },
      ]);
    } finally {
      setIsLoading(false);
      e.target.value = null; // 초기화
    }
  };
  const removeFile = (index) => {
    setSelectedFiles((prev) => prev.filter((_, i) => i !== index));
  };

  // [중요] 유효성 검사 함수 (이게 false면 전송 안 됨)
>>>>>>> 9a26bee9327a8cbf266b266bfef68982fcfdeb40
  const isFormValid = () => {
    const t = currentTicket;
    const hasReceivers =
      t.receivers && t.receivers.length > 0 && t.receivers[0] !== "";
    return t.title?.trim() && t.content?.trim() && hasReceivers && t.deadline;
  };

  // ✅ PDF 다운로드 기능 구현
  const handleDownloadPdf = async () => {
    const element = pdfRef.current;
    if (!element) return;

    try {
      // 1. 해당 영역을 캔버스로 변환
      const canvas = await html2canvas(element, {
        scale: 2, // 해상도 2배 (선명하게)
        backgroundColor: "#ffffff", // 배경 흰색 고정
      });

      // 2. 캔버스를 이미지 데이터로 변환
      const imgData = canvas.toDataURL("image/png");

      // 3. A4 사이즈 기준 계산
      const imgWidth = 210; // A4 가로 (mm)
      const pageHeight = 297; // A4 세로 (mm)
      const imgHeight = (canvas.height * imgWidth) / canvas.width; // 비율에 맞춘 높이

      let heightLeft = imgHeight;
      let position = 0;

      // 4. PDF 생성
      const doc = new jsPDF("p", "mm", "a4");

      // 첫 페이지 작성
      doc.addImage(imgData, "PNG", 0, position, imgWidth, imgHeight);
      heightLeft -= pageHeight;

      // 내용이 길어서 넘어가는 경우 페이지 추가
      while (heightLeft >= 0) {
        position = heightLeft - imgHeight;
        doc.addPage();
        doc.addImage(imgData, "PNG", 0, position, imgWidth, imgHeight);
        heightLeft -= pageHeight;
      }

      // 5. 파일 저장
      const fileName = currentTicket.title
        ? `${currentTicket.title}_업무요청서.pdf`
        : "업무요청서.pdf";
      doc.save(fileName);
    } catch (error) {
      console.error("PDF 생성 실패:", error);
      alert("PDF 다운로드 중 오류가 발생했습니다.");
    }
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
      setMessages((prev) => [
        ...prev,
        { role: "assistant", content: response.ai_message },
      ]);
      setCurrentTicket(response.updated_ticket);
      setIsCompleted(response.is_completed);
      if (response.identified_target_dept)
        setTargetDept(response.identified_target_dept);
    } catch (error) {
      setMessages((prev) => [
        ...prev,
        { role: "assistant", content: "AI 서버 오류가 발생했습니다." },
      ]);
    } finally {
      setIsLoading(false);
    }
  };

<<<<<<< HEAD
  const handleSubmitTicket = async () => {
    console.log("전송 버튼 클릭됨");
    if (!isFormValid()) {
      alert("필수 항목(제목, 내용, 담당자, 마감일)을 모두 확인해 주세요.");
=======
  // 5. 티켓 전송 (Java 서버로 전송)
  const handleSubmitTicket = async () => {
    if (!currentTicket.title || !currentTicket.content) {
      alert("제목과 내용은 필수입니다.");
>>>>>>> 9a26bee9327a8cbf266b266bfef68982fcfdeb40
      return;
    }
    setIsLoading(true);
    try {
<<<<<<< HEAD
=======
      console.log("API 호출 직전...");
      // 위에서 만든 API 호출
>>>>>>> 9a26bee9327a8cbf266b266bfef68982fcfdeb40
      await aiSecretaryApi.submitTicket(
        currentTicket,
        selectedFiles,
        currentUserEmail
      );
<<<<<<< HEAD
=======

      console.log("전송 프로세스 전체 완료");
>>>>>>> 9a26bee9327a8cbf266b266bfef68982fcfdeb40
      setSubmitSuccess(true);
      setTimeout(() => {
        onClose();
      }, 2000);
    } catch (error) {
      alert("티켓 전송에 실패했습니다.");
      setIsLoading(false);
    }
  };

  // ✅ [핵심] PDF 다운로드 기능 (A4 사이즈 완벽 대응)
  const handleDownloadPDF = async () => {
    const element = pdfTargetRef.current;
    if (!element) return;

    try {
      // 1. 화면 캡처 (옵션 중요!)
      const canvas = await html2canvas(element, {
        scale: 2, // 해상도 2배 (글자 선명하게)
        useCORS: true, // 이미지 로딩 허용
        backgroundColor: "#ffffff", // 배경을 강제로 흰색으로 (투명 방지)
        scrollY: -window.scrollY, // 스크롤 위치 보정 (잘림 방지)
        windowWidth: document.documentElement.offsetWidth, // 전체 너비 확보
      });

      // 2. PDF 생성
      const imgData = canvas.toDataURL("image/png");
      const pdf = new jsPDF("p", "mm", "a4"); // A4 세로

      const imgWidth = 210; // A4 너비 (mm)
      const pageHeight = 297; // A4 높이 (mm)

      // 이미지 비율에 맞춰 높이 계산
      const imgHeight = (canvas.height * imgWidth) / canvas.width;

      let heightLeft = imgHeight;
      let position = 0;

      // 첫 페이지
      pdf.addImage(imgData, "PNG", 0, position, imgWidth, imgHeight);
      heightLeft -= pageHeight;

      // 내용이 길면 다음 페이지 추가
      while (heightLeft >= 0) {
        position = heightLeft - imgHeight;
        pdf.addPage();
        pdf.addImage(imgData, "PNG", 0, position, imgWidth, imgHeight);
        heightLeft -= pageHeight;
      }

      // 3. 저장
      const fileName = `Ticket_${currentTicket.title || "Untitled"}.pdf`;
      pdf.save(fileName);
    } catch (err) {
      console.error("PDF Error:", err);
      alert("PDF 저장 중 오류가 발생했습니다.");
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
<<<<<<< HEAD
=======

              {/* 🎙️ 회의록(오디오) 첨부 버튼 */}
              <button
                type="button"
                className="icon-btn audio-btn"
                title="회의록(음성) 분석"
                onClick={() => audioInputRef.current.click()}
              >
                🎙️
              </button>
              <input
                type="file"
                accept="audio/*"
                className="hidden"
                ref={audioInputRef}
                onChange={handleAudioUpload}
              />
>>>>>>> 9a26bee9327a8cbf266b266bfef68982fcfdeb40

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
<<<<<<< HEAD
            <div
              className="ticket-header-row"
              style={{ display: "flex", gap: "10px" }}
            >
              <span className="dept-badge" style={{ marginRight: "auto" }}>
                To: {targetDept || "(미지정)"}
              </span>

              {/* ✅ PDF 다운로드 버튼 추가 */}
              <button
                type="button"
                onClick={handleDownloadPdf}
                style={{
                  background: "#ef4444",
                  color: "white",
                  border: "none",
                  borderRadius: "6px",
                  padding: "6px 12px",
                  cursor: "pointer",
                }}
              >
                📄 PDF 저장
              </button>

              <button className="reset-btn" onClick={handleReset}>
                🔄 초기화
              </button>
            </div>

            {/* ✅ PDF 캡처 대상에 ref 연결 */}
            <div className="ticket-preview-box" ref={pdfRef}>
=======
            <div className="ticket-header-row">
              <span className="dept-badge">To: {targetDept || "(미지정)"}</span>
              <div className="flex gap-2">
                {/* PDF 다운로드 버튼 */}
                <button
                  className="pdf-btn"
                  onClick={handleDownloadPDF}
                  title="PDF 다운로드"
                >
                  📄 PDF
                </button>

                <button className="reset-btn" onClick={handleReset}>
                  🔄 초기화
                </button>
              </div>
            </div>

            <div className="ticket-preview-box" ref={pdfTargetRef}>
>>>>>>> 9a26bee9327a8cbf266b266bfef68982fcfdeb40
              <div className="form-group">
                <label>
                  제목 <span className="text-red-500">*</span>
                </label>
                <input
                  name="title"
                  className="st-input"
                  value={currentTicket.title || ""}
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
                  value={currentTicket.content || ""}
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
                    value={currentTicket.purpose || ""}
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
                    value={currentTicket.requirement || ""}
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
                    value={currentTicket.deadline || ""}
                    onChange={handleManualChange}
                  />
                </div>
                <div className="form-group">
                  <label>중요도</label>
                  <select
                    name="grade"
                    className="st-input"
                    value={currentTicket.grade}
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
                  value={currentTicket.receivers.join(",")}
                  onChange={handleManualChange}
                />
              </div>

<<<<<<< HEAD
              {/* [파일 미리보기 영역] */}
              {selectedFiles.length > 0 && (
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
                        {/* data-html2canvas-ignore 속성을 쓰면 캡처시 X버튼은 안 보이게 할 수도 있습니다. */}
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
                          data-html2canvas-ignore="true"
                        >
                          ×
                        </button>
                      </div>
                    ))}
                  </div>
=======
              {/* [파일 미리보기 영역] 기존 스타일 유지 */}
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
                        data-html2canvas-ignore="true" // ✅ PDF 캡처 시 삭제 버튼 제외
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
>>>>>>> 9a26bee9327a8cbf266b266bfef68982fcfdeb40
                </div>
              )}
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
