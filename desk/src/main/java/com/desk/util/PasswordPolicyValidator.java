package com.desk.util;

/**
 * 비밀번호 정책 검증 유틸리티
 * 
 * 정책 규칙:
 * - 최소 길이: 8자 이상
 * - 다음 항목 중 2종 이상 포함:
 *   - 영문 대문자 (A-Z)
 *   - 영문 소문자 (a-z)
 *   - 숫자 (0-9)
 *   - 특수문자 (!@#$%^&* 등)
 * 
 * 참고: NIST SP 800-63B, OWASP ASVS 기준 준수
 */
public class PasswordPolicyValidator {

    private static final int MIN_LENGTH = 8;

    /**
     * 비밀번호 정책 검증
     * @param password 검증할 비밀번호
     * @throws IllegalArgumentException 정책 위반 시
     */
    public static void validate(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }

        // 최소 길이 검증
        if (password.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이어야 합니다.");
        }

        // 문자 유형 검증
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasNumber = password.matches(".*[0-9].*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*(),.?\":{}|<>\\[\\]\\\\\\/\\-_+=~`;'<>].*");

        // 포함된 유형 개수 계산
        long typeCount = java.util.stream.Stream.of(hasUpper, hasLower, hasNumber, hasSpecial)
                .filter(Boolean::booleanValue)
                .count();

        // 2종 이상 포함 여부 검증
        if (typeCount < 2) {
            throw new IllegalArgumentException("영문 대소문자/숫자/특수문자 중 2종 이상 포함해야 합니다.");
        }
    }

    /**
     * 비밀번호 정책 검증 (검증만 수행, 예외 미발생)
     * @param password 검증할 비밀번호
     * @return 검증 통과 여부
     */
    public static boolean isValid(String password) {
        try {
            validate(password);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 비밀번호 정책 검증 및 에러 메시지 반환
     * @param password 검증할 비밀번호
     * @return 검증 결과 (통과 시 null, 실패 시 에러 메시지)
     */
    public static String validateWithMessage(String password) {
        try {
            validate(password);
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }
}

