package com.desk.util;

public class MemberExistException extends RuntimeException {
        public MemberExistException() {
            super("MEMBER_EXIST"); // 프론트에서 이 메시지로 중복 처리 확인
        }
    }