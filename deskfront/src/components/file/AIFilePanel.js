import React from "react";
import { aiFileApi } from "../../api/aiFileApi";

/**
 * results: AIFileResultDTO[]
 */
const AIFilePanel = ({ results = [] }) => {
  if (!results || results.length === 0) {
    return (
      <div style={{ padding: 12, color: "#6b7280" }}>
        파일 검색 결과가 없습니다.
      </div>
    );
  }

  return (
    <div style={{ padding: 12, overflowY: "auto", height: "100%" }}>
      <div style={{ fontWeight: 700, marginBottom: 10 }}>파일 조회 결과</div>
      <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
        {results.map((f) => (
          <div
            key={f.uuid}
            style={{
              border: "1px solid #e5e7eb",
              borderRadius: 10,
              padding: 10,
              display: "flex",
              gap: 10,
              alignItems: "center",
              background: "white",
            }}
          >
            <div
              style={{
                width: 54,
                height: 54,
                flex: "0 0 auto",
                borderRadius: 10,
                background: "#111827",
                color: "white",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontWeight: 800,
                fontSize: 12,
              }}
              title={f.fileName}
            >
              {(f.fileName || "FILE").split(".").pop()?.toUpperCase() || "FILE"}
            </div>

            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontWeight: 600, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>
                {f.fileName}
              </div>
              <div style={{ fontSize: 12, color: "#6b7280" }}>
                {f.ticketTitle ? `티켓: ${f.ticketTitle}` : ""}
              </div>
              <div style={{ fontSize: 12, color: "#6b7280" }}>
                {f.writerEmail ? `작성자: ${f.writerEmail}` : ""}
              </div>
            </div>

            <button
              type="button"
              onClick={() => aiFileApi.downloadFile(f.uuid, f.fileName)}
              style={{
                border: "1px solid #111827",
                background: "#111827",
                color: "white",
                padding: "8px 10px",
                borderRadius: 8,
                cursor: "pointer",
                fontWeight: 700,
              }}
            >
              다운로드
            </button>
          </div>
        ))}
      </div>
    </div>
  );
};

export default AIFilePanel;


