import React, { useState, useEffect, useRef } from "react";
import { useSelector } from "react-redux";
import { aiSecretaryApi } from "../../api/aiSecretaryApi";
import { aiFileApi } from "../../api/aiFileApi";
import { sttApi } from "../../api/sttApi";
import { sendMessageRest } from "../../api/chatApi";
import chatWsClient from "../../api/chatWs";
import { getMemberInfo } from "../../api/memberApi";
import FilePreview from "../common/FilePreview";
import AIFilePanel from "../file/AIFilePanel";
import "./AIChatWidget.css";

const generateUUID = () => {
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function (c) {
    var r = (Math.random() * 16) | 0,
      v = c === "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
};

const AIChatWidget = ({ onClose, chatRoomId, currentUserId }) => {
  // ✅ 오늘로부터 7일 후 날짜 (YYYY-MM-DD)
  const getDefaultDeadline = () => {
    const date = new Date();
    date.setDate(date.getDate() + 7);
    return date.toISOString().split("T")[0];
  };

  const loginState = useSelector((state) => state.loginSlice);
  const currentUserDept = loginState.department || "Unknown";
  const currentUserEmail = loginState.email;

  const [conversationId] = useState(generateUUID());
  const [messages, setMessages] = useState([
    {
      role: "assistant",
      content: "안녕하세요. 어떤 업무를 도와드릴까요?\n(ex: 파일조회, 업무티켓)",
    },
  ]);

  // null(선택 전) | "ticket" | "file"
  const [mode, setMode] = useState(null);
  const [aiFileResults, setAiFileResults] = useState([]);

  const [currentTicket, setCurrentTicket] = useState({
    title: "",
    content: "",
    purpose: "",
    requirement: "",
    grade: "MIDDLE",
    deadline: getDefaultDeadline(),
    receivers: [],
  });

  const [selectedFiles, setSelectedFiles] = useState([]);
  const fileInputRef = useRef(null);
  const audioInputRef = useRef(null);
  const messagesEndRef = useRef(null);
  const pdfRef = useRef(null);

  const [targetDept, setTargetDept] = useState(null);
  const [isCompleted, setIsCompleted] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [submitSuccess, setSubmitSuccess] = useState(false);
  const [inputMessage, setInputMessage] = useState("");
  const [isSttLoading, setIsSttLoading] = useState(false);
  // 여러 명 담당자 정보를 위한 배열
  const [assigneesInfo, setAssigneesInfo] = useState([]);

  const [shouldAnimate, setShouldAnimate] = useState(false);
  const overlayRef = useRef(null);
  const containerRef = useRef(null);
  const onCloseRef = useRef(onClose);

  // onClose 함수 참조 업데이트
  useEffect(() => {
    onCloseRef.current = onClose;
  }, [onClose]);

  // ESC 키로 모달 닫기 및 애니메이션 초기화
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        onCloseRef.current();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    
    // 모달이 열릴 때 약간의 지연 후 애니메이션 적용 (마운트 시에만 실행)
    setShouldAnimate(false);
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        setShouldAnimate(true);
      });
    });
    
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, []); // 빈 배열로 변경 - 마운트 시에만 실행

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  // 담당자 정보 조회 (receivers 변경 시) - 여러 명 지원
  useEffect(() => {
    const fetchAssigneesInfo = async () => {
      if (!currentTicket.receivers || currentTicket.receivers.length === 0) {
        setAssigneesInfo([]);
        return;
      }

      try {
        const promises = currentTicket.receivers
          .filter((email) => !!email)
          .map((email) => getMemberInfo(email).catch(() => null));

        const results = await Promise.all(promises);

        const cleaned = results
          .map((info, idx) =>
            info
              ? { ...info, email: currentTicket.receivers[idx] }
              : { email: currentTicket.receivers[idx] }
          )
          .filter(Boolean);

        setAssigneesInfo(cleaned);
      } catch (error) {
        console.error("담당자 정보 조회 실패:", error);
        setAssigneesInfo([]);
      }
    };

    fetchAssigneesInfo();
  }, [currentTicket.receivers]);

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

  // ✅ [Helper] 텍스트 요약 및 자르기 함수들
  const compressText = (text = "", max = 240) => {
    const t = String(text || "")
      .replace(/\r/g, "")
      .replace(/[ \t]+/g, " ")
      .replace(/\n{3,}/g, "\n\n")
      .trim();
    if (!t) return "";
    if (t.length <= max) return t;
    const sentences = t.split(/(?<=[.!?。]|다\.)\s+/);
    let out = "";
    for (const s of sentences) {
      if ((out + (out ? " " : "") + s).length > max) break;
      out += (out ? " " : "") + s;
    }
    if (out) return out;
    return t.slice(0, max - 1) + "…";
  };

  const compressList = (text = "", maxLines = 4, maxChars = 420) => {
    const t = String(text || "")
      .replace(/\r/g, "")
      .trim();
    if (!t) return "";
    const lines = t
      .split("\n")
      .map((l) => l.trim())
      .filter(Boolean);
    const bulletLike = lines.filter((l) =>
      /^(\d+\.|[-*•]|[가-힣]\.)\s*/.test(l)
    );
    const picked = (bulletLike.length ? bulletLike : lines).slice(0, maxLines);
    let out = picked.join("\n");
    if (out.length > maxChars) out = out.slice(0, maxChars - 1) + "…";
    return out;
  };

  const buildInputFromSummary = (s) => {
    const title = compressText(s?.title || "", 60);
    const content = [
      compressText(s?.overview || s?.shortSummary || "", 220),
      s?.conclusion ? `결론: ${compressText(s.conclusion, 140)}` : "",
    ]
      .filter(Boolean)
      .join("\n\n");
    const purpose = compressText(s?.overview || "", 120);
    const requirement = compressList(s?.details || "", 5, 520);

    // 참석자 전체를 receivers로 사용 (여러 명 지원)
    let receivers = [];
    if (Array.isArray(s?.attendees)) {
      receivers = s.attendees.filter((v) => !!v);
    } else if (typeof s?.attendees === "string") {
      receivers = s.attendees
        .split(",")
        .map((v) => v.trim())
        .filter((v) => !!v);
    }

    return { title, content, purpose, requirement, receivers };
  };

  // =====================================================================
  // ✅ [핵심 기능] STT 결과로 AI 요약 + PDF 생성 + 파일 첨부 자동화 함수
  // =====================================================================
  const autoProcessSttResult = async (text) => {
    if (!text) return;

    setIsLoading(true);
    // setAiSummary(
    //   "⏳ 음성 내용을 바탕으로 회의록을 작성하고 PDF를 생성 중입니다..."
    // );

    try {
      // 1. AI 요약 요청 (텍스트를 content에 담아서 요청)
      //    (기존 currentTicket에는 값이 없을 수 있으므로 text를 content로 강제 주입하여 요청)
      const mockTicket = { ...currentTicket, content: text };
      const summaryData = await aiSecretaryApi.getSummary(mockTicket, null);

      // 2. 파란창(AI 요약 리포트) 업데이트
      //   setAiSummary(summaryData);

      // 3. 우측 입력 폼 자동 채우기
      const { title, content, purpose, requirement, receivers } =
        buildInputFromSummary(summaryData);

      setCurrentTicket((prev) => ({
        ...prev,
        title: title || prev.title,
        content: content || prev.content, // 요약된 내용이 들어감 (원본 텍스트X)
        purpose: purpose || prev.purpose,
        requirement: requirement || prev.requirement,
        deadline: getDefaultDeadline(),

        receivers: receivers && receivers.length ? receivers : prev.receivers,
      }));

      // 4. PDF 생성 및 자동 첨부
      //    (요약된 summaryData 객체를 그대로 보냄)
      const pdfRes = await aiSecretaryApi.downloadSummaryPdf(summaryData);

      // Blob으로 변환
      const pdfBlob = new Blob([new Uint8Array(pdfRes)], {
        type: "application/pdf",
      });

      // File 객체로 변환 (파일명: 제목 + _Auto_Report.pdf)
      const fileName = `${title || "Voice_Memo"}_AI_Report.pdf`;
      const pdfFile = new File([pdfBlob], fileName, {
        type: "application/pdf",
      });

      // 첨부파일 목록에 추가
      setSelectedFiles((prev) => [...prev, pdfFile]);

      // 채팅창 알림
      setMessages((prev) => [
        ...prev,
        {
          role: "assistant",
          content: `✅ 음성 분석 완료! 회의록이 작성되었으며 PDF 파일('${fileName}')이 자동으로 첨부되었습니다.`,
        },
      ]);
    } catch (error) {
      console.error("Auto Process Error:", error);
      //   setAiSummary("자동 처리 중 오류가 발생했습니다.");
      setMessages((prev) => [
        ...prev,
        {
          role: "assistant",
          content: "❌ 음성 분석 후 요약/PDF 생성에 실패했습니다.",
        },
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  // ✅ STT 처리 함수 (수정됨)
  const handleAudioUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.includes("audio") && !file.name.endsWith(".mp3")) {
      alert("MP3 오디오 파일만 업로드 가능합니다.");
      return;
    }

    setIsSttLoading(true);
    setMessages((prev) => [
      ...prev,
      { role: "assistant", content: "🎤 음성 파일을 분석하고 있습니다..." },
    ]);

    try {
      const response = await sttApi.uploadAudio(file);
      const transcribedText = response.text || response.data?.text || "";

      if (transcribedText) {


        // ✅ [자동화 트리거] 변환된 텍스트로 요약 및 PDF 생성 시작
        await autoProcessSttResult(transcribedText);
      } else {
        setMessages((prev) => [
          ...prev,
          {
            role: "assistant",
            content: "음성을 텍스트로 변환하지 못했습니다. 다시 시도해주세요.",
          },
        ]);
      }
    } catch (error) {
      console.error("STT Error:", error);
      setMessages((prev) => [
        ...prev,
        { role: "assistant", content: "음성 변환 중 오류가 발생했습니다." },
      ]);
    } finally {
      setIsSttLoading(false);
      if (audioInputRef.current) {
        audioInputRef.current.value = "";
      }
    }
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
      // [모드 선택 전] 첫 입력으로 파일조회/업무티켓 분기
      if (!mode) {
        const text = userMsg.content.trim();
        const isFile = text.includes("파일");
        const isTicket = text.includes("업무") || text.includes("티켓");

        if (isFile) {
          setMode("file");
          setMessages((prev) => [
            ...prev,
            {
              role: "assistant",
              content:
                "좋아요. **파일조회**를 도와드릴게요.\n\n조회하실 파일에 대한 정보를 말씀해 주세요.\n(ex: 기간, 상대방, 파일명, 관련내용 등..)",
            },
          ]);
          return;
        }

        if (isTicket) {
          setMode("ticket");
          setMessages((prev) => [
            ...prev,
            {
              role: "assistant",
              content:
                "좋아요. **업무티켓** 작성을 도와드릴게요.\n\n요청하실 업무 내용을 말씀해 주세요.",
            },
          ]);
          return;
        }

        // 둘 다 아니면 재질문 (mode 유지)
        setMessages((prev) => [
          ...prev,
          {
            role: "assistant",
            content:
              "파일조회/업무티켓 중 어떤 기능을 원하시나요?\n\n- 파일조회: '파일'을 포함해서 입력\n- 업무티켓: '업무' 또는 '티켓'을 포함해서 입력",
          },
        ]);
        return;
      }

      // [파일조회 모드]
      if (mode === "file") {
        try {
          const response = await aiFileApi.sendMessage({
            conversation_id: conversationId,
            user_input: userMsg.content,
          });

          setMessages((prev) => [
            ...prev,
            {
              role: "assistant",
              content: response.aiMessage || "파일 검색 결과를 확인해 주세요.",
            },
          ]);
          setAiFileResults(response.results || []);
        } catch (error) {
          console.error("AI File Search Error:", error);
          setMessages((prev) => [
            ...prev,
            {
              role: "assistant",
              content: "파일 검색 중 오류가 발생했습니다. 다시 시도해 주세요.",
            },
          ]);
        } finally {
          setIsLoading(false);
        }
        return;
      }

      // [업무티켓 모드] (기존 로직 유지)
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
        { role: "assistant", content: response.aiMessage },
      ]);

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
      // 1. 티켓 저장
      const ticketResponse = await aiSecretaryApi.submitTicket(
        currentTicket,
        selectedFiles,
        currentUserEmail
      );

      // 2. 티켓 저장 성공 시 채팅방에 티켓 미리보기 메시지 실시간 전송 (WebSocket)
      if (chatRoomId && ticketResponse?.tno) {
        try {
          // WebSocket을 통해 실시간 전송 시도
          const wsSuccess = chatWsClient.send(chatRoomId, {
            content: `티켓이 생성되었습니다: ${currentTicket.title}`,
            messageType: "TICKET_PREVIEW",
            ticketId: ticketResponse.tno,
          });

          // WebSocket 실패 시 REST API로 fallback
          if (!wsSuccess) {
            await sendMessageRest(chatRoomId, {
              content: `티켓이 생성되었습니다: ${currentTicket.title}`,
              messageType: "TICKET_PREVIEW",
              ticketId: ticketResponse.tno,
            });
          }
        } catch (messageError) {
          console.error("채팅 메시지 전송 실패:", messageError);
          // 티켓은 저장되었지만 메시지 전송 실패 - 사용자에게 알림
          alert("티켓은 저장되었지만 채팅 메시지 전송에 실패했습니다.");
        }
      }

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
      setMode(null);
      setAiFileResults([]);
      setCurrentTicket({
        title: "",
        content: "",
        purpose: "",
        requirement: "",
        grade: "MIDDLE",
        deadline: getDefaultDeadline(), // ✅ 초기화 시에도 7일 후
        receivers: [],
      });
      setSelectedFiles([]);
      setTargetDept(null);
      setIsCompleted(false);
      setSubmitSuccess(false);
      //   setAiSummary("");
    }
  };


  return (
    <div 
      ref={overlayRef}
      className="ai-widget-overlay"
      style={{ 
        opacity: shouldAnimate ? 1 : 0,
        animation: shouldAnimate ? 'fadeInOverlay 0.2s ease-out' : 'none'
      }}
    >
      <div 
        ref={containerRef}
        className="ai-widget-container"
        style={{
          opacity: shouldAnimate ? 1 : 0,
          transform: shouldAnimate ? 'translateY(0) scale(1)' : 'translateY(20px) scale(0.96)',
          animation: shouldAnimate ? 'slideUpScaleModal 0.2s ease-out' : 'none',
          willChange: 'transform, opacity'
        }}
      >
        <div className="ai-widget-header">
          <h2>AI 업무 비서</h2>
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
              {mode !== "file" && (
                <>
                  <button
                    type="button"
                    className="mr-2.5 text-xl"
                    onClick={() => fileInputRef.current.click()}
                    title="파일 첨부"
                  >
                    📎
                  </button>
                  <button
                    type="button"
                    style={{
                      marginRight: "10px",
                      fontSize: "20px",
                      opacity: isSttLoading ? 0.5 : 1,
                      cursor: isSttLoading ? "not-allowed" : "pointer",
                    }}
                    onClick={() => audioInputRef.current.click()}
                    disabled={isSttLoading}
                    title="음성 파일 업로드 (MP3)"
                  >
                    {isSttLoading ? "⏳" : "📜"}
                  </button>
                </>
              )}
              <input
                type="file"
                multiple
                className="hidden"
                ref={fileInputRef}
                onChange={handleFileChange}
              />
              <input
                type="file"
                accept="audio/*,.mp3"
                className="hidden"
                ref={audioInputRef}
                onChange={handleAudioUpload}
              />
              <input
                type="text"
                className="chat-input"
                placeholder={
                  isSttLoading
                    ? "음성을 텍스트로 변환 중..."
                    : mode === "file"
                    ? "파일 검색 문장을 입력하세요..."
                    : "업무 요청 내용을 입력하세요..."
                }
                value={inputMessage}
                onChange={(e) => setInputMessage(e.target.value)}
                onKeyDown={(e) =>
                  e.key === "Enter" && !e.shiftKey && handleSendMessage()
                }
                disabled={isSttLoading}
              />
              <button
                className="reset-btn"
                onClick={handleSendMessage}
                disabled={
                  isLoading ||
                  submitSuccess ||
                  !inputMessage.trim() ||
                  isSttLoading
                }
              >
                전송
              </button>
            </div>
          </div>

          <div className="ai-ticket-section">
                      {mode === "file" ? (
                        <div style={{ height: "100%", display: "flex", flexDirection: "column" }}>
                          <div
                            style={{
                              display: "flex",
                              justifyContent: "space-between",
                              alignItems: "center",
                              padding: "10px 10px 0 10px",
                            }}
                          >
                            <div style={{ fontWeight: 800 }}>파일조회</div>
                            <button
                              className="reset-btn"
                              onClick={handleReset}
                              style={{ padding: "5px 10px", borderRadius: "4px", fontSize: "13px" }}
                            >
                              초기화
                            </button>
                          </div>
                          <div style={{ flex: 1, minHeight: 0 }}>
                            <AIFilePanel results={aiFileResults} />
                          </div>
                        </div>
                      ) : (
                        <>
            <div
              className="ticket-header-row"
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                marginBottom: "10px",
              }}
            >
              <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                <span style={{ fontWeight: "600", fontSize: "12px" }}>To:</span>

                {assigneesInfo && assigneesInfo.length > 0 ? (
                  assigneesInfo.length === 1 ? (
                    <>
                      <span className="dept-badge">
                        {assigneesInfo[0].department ||
                          targetDept ||
                          "부서 미지정"}
                      </span>
                      <span className="dept-badge">
                        {assigneesInfo[0].nickname ||
                          assigneesInfo[0].email ||
                          "담당자 미지정"}
                      </span>
                    </>
                  ) : (
                    assigneesInfo.map((info, idx) => (
                      <span
                        key={info.email || idx}
                        className="dept-badge"
                        title={info.department || targetDept || "부서 미지정"}
                      >
                        {info.nickname || info.email || "담당자 미지정"}
                      </span>
                    ))
                  )
                ) : currentTicket.receivers &&
                  currentTicket.receivers.length > 0 ? (
                  // 백엔드 정보가 아직 없을 때 fallback
                  currentTicket.receivers.map((email, idx) => (
                    <span
                      key={email || idx}
                      className="dept-badge"
                      title={targetDept || "부서 미지정"}
                    >
                      {email}
                    </span>
                  ))
                ) : (
                  <span className="dept-badge">담당자 미지정</span>
                )}
              </div>
              <div style={{ display: "flex", gap: "5px" }}>

                <button
                  className="reset-btn"
                  onClick={handleReset}
                  style={{
                    padding: "5px 10px",
                    borderRadius: "4px",
                    fontSize: "13px",
                  }}
                >
                  초기화
                </button>
              </div>
            </div>

            <div className="ticket-preview-box" ref={pdfRef}>


              <div className="form-group">
                <label>
                  제목 <span className="ui-required">*</span>
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
                  요약 <span className="ui-required">*</span>
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
                    목적 <span className="ui-required">*</span>
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
                    상세 <span className="ui-required">*</span>
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
                    마감일 <span className="ui-required">*</span>
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
                  담당자 <span className="ui-required">*</span>
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
                <div className="grid grid-cols-5 gap-1 mt-2.5">
                  {selectedFiles.map((file, idx) => (
                    <div
                      key={idx}
                      className="relative aspect-square border border-baseBorder rounded-lg overflow-hidden"
                    >
                      <FilePreview file={file} isLocal={true} />
                      <button
                        onClick={() => removeFile(idx)}
                        className="absolute top-0 right-0 bg-black/50 text-white border-none cursor-pointer w-5 h-5 flex items-center justify-center text-xs hover:bg-black/70 transition-colors"
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
                </>
              )}
            </div>
        </div>
      </div>
    </div>
  );
};

export default AIChatWidget;
