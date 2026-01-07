package com.desk.util;

public class DuplicateMemberException extends RuntimeException {
    public DuplicateMemberException() {
        super("이미 존재하는 이메일입니다.");
    }
}
