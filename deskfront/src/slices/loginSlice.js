import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import { loginPost } from "../api/memberApi";
import { getCookie, removeCookie, setCookie } from "../util/cookieUtil";

// Redux Toolkit을 이용한 로그인 상태 관리(slice)
// React 컴포넌트들은 Redux store의 slice state를 읽어서 로그인 상태를 판단 -> reducer내부의 함수를 통해 쿠키 설정 및 state 변환(로그인 상태)

const initState = {
  email: "",
};
const loadMemberCookie = () => {
  // 쿠키에서 로그인 정보 로딩 ('member' 이름의 쿠키 반환)
  const memberInfo = getCookie("member");

  //닉네임 처리하여 사용자가 입력한 값중에 특수문자나 공백이 포함되면 디코딩하여 제대로 된 형태로 표시
  if (memberInfo && memberInfo.nickname) {
    memberInfo.nickname = decodeURIComponent(memberInfo.nickname);
  }
  return memberInfo;
};

// 비동기 로그인 처리 (일반 로그인) - loginPost 실행. 자동으로 아래 상태 생성됨: pending, fulfilled, rejected
export const loginPostAsync = createAsyncThunk("loginPostAsync", (param) => {
  return loginPost(param);
});

// loginSlice : 로그인 상태를 관리하는 Redux 상태 묶음(모듈)  - 상태(state) + reducer들 + action 생성기들을 한 번에 묶어 놓은 객체
// 실행되는 코드가 아니라, Redux 설정용 “설계도 객체”
const loginSlice = createSlice({
  // createSlice : Redux에서 필요한 걸 한 번에 만들어줌
  name: "LoginSlice",
  // loadMemberCookie() 실행, 값이 “truthy”(true로 평가되는 값) 이면 → 그 값을 사용, 그렇지 않으면 → initState 사용
  initialState: loadMemberCookie() || initState, // 쿠키가 없다면 초깃값 사용
  reducers: {
    // 이 slice가 직접 정의한 action + reducer
    login: (state, action) => {
      // dispatch(login(payload)); -> 동작
      console.log("login....");

      // 소셜 로그인 회원이 사용
      const payload = action.payload; // action: dispatch할 때 전달한 객체 { type, payload }, 여기서 payload가 실제 로그인 정보
      setCookie("member", JSON.stringify(payload), 1); // 1일 - 로그인 정보를 쿠키에 저장

      // slice의 state를 payload로 변환
      return payload;
    },
    out: (state, action) => {
      // 쿠키 삭제, 로그인 상태 초기화, Redux state를 비로그인 상태로 되돌림
      console.log("logout....");
      removeCookie("member");
      return { ...initState };
    },
  },
  extraReducers: (builder) => {
    // slice의 기본 reducers가 아닌, 외부에서 만든 액션(createAsyncThunk)에 대응할 때 사용
    builder
      .addCase(loginPostAsync.fulfilled, (state, action) => {
        console.log("fulfilled");
        const payload = action.payload;

        // 정상적인 로그인시에만 저장
        if (!payload.error) {
          setCookie("member", JSON.stringify(payload), 1); //1일
          // setCookie("member", JSON.stringify(payload),1/24)
        }

        return payload;
      })
      .addCase(loginPostAsync.pending, (state, action) => {
        console.log("pending");
      })
      .addCase(loginPostAsync.rejected, (state, action) => {
        console.log("rejected");
      });
  },
});

export const { login, out: logout } = loginSlice.actions;
export default loginSlice.reducer;
