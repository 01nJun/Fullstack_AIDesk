/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{js,jsx,ts,tsx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['"Noto Sans KR"', 'sans-serif'],
      },
      colors: {
        // 채팅 전용 토큰 (유지)
        chatNavy: "#1f3a68",
        chatOrange: "#ff8a2a",
        chatBg: "#ffffff",
        chatSurface: "#f8f9fb",
        chatText: "#1a1a1a",
        chatMuted: "#8a8f98",
        chatBorder: "#e6e8ec",
        // 전역 브랜드 토큰
        brandNavy: "#1f3a68",
        brandOrange: "#ff8a2a",
        baseBg: "#ffffff",
        baseSurface: "#f8f9fb",
        baseText: "#1a1a1a",
        baseMuted: "#8a8f98",
        baseBorder: "#e6e8ec",
        // 특수 케이스 (카카오)
        kakaoYellow: "#FEE500",
        kakaoYellowHover: "#FADA0A",
        // 파일 타입별 색상
        filePdf: "#E53E3E",
        fileZip: "#D69E2E",
        fileXlsx: "#38A169",
        fileDocx: "#3182CE",
        fileTxt: "#718096",
        fileMp3: "#805AD5",
        fileMp4: "#ED64A6",
        fileDefault: "#A0AEC0",
      },
      borderRadius: {
        chat: "10px",
        chatLg: "12px",
        ui: "12px",
      },
      boxShadow: {
        chat: "0 1px 3px rgba(0, 0, 0, 0.05)",
        chatMd: "0 2px 6px rgba(0, 0, 0, 0.08)",
        ui: "0 1px 3px rgba(0, 0, 0, 0.08)",
      },
    },
  },
  plugins: [],
}
