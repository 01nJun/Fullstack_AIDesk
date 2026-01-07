import { configureStore } from "@reduxjs/toolkit";
import loginSlice from "./slices/loginSlice";
import pinSlice from "./slices/pinSlice";

export default configureStore({
  reducer: {
    loginSlice: loginSlice,
    pinSlice: pinSlice,
  },
});
