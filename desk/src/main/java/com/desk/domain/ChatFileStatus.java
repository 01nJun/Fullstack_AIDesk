package com.desk.domain;

public enum ChatFileStatus {
    TEMP,   // 업로드 직후, 메시지에 바인딩 전
    BOUND,  // 메시지에 바인딩 완료
    DELETED // 소프트 삭제됨 (물리 파일은 배치에서 정리)
}

