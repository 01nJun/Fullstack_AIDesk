import { createAsyncThunk, createSlice } from "@reduxjs/toolkit";
import { getPinItems, postTogglePin } from "../api/pinApi";

export const getPinItemsAsync = createAsyncThunk("getPinItemsAsync", getPinItems);
export const togglePinAsync = createAsyncThunk("togglePinAsync", postTogglePin);

const initState = [];

const pinSlice = createSlice({
  name: "pinSlice",
  initialState: initState,
  reducers: {
    // 로그아웃 시 호출할 초기화 액션
    clearPins: (state) => {
      return initState;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(getPinItemsAsync.fulfilled, (state, action) => action.payload)
      .addCase(togglePinAsync.fulfilled, (state, action) => action.payload);
  },
});

export const { clearPins } = pinSlice.actions; // 액션 내보내기
export default pinSlice.reducer;