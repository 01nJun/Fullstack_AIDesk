package com.desk.util.text;

public class TextSimilarityUtil {

    /**
     * 두 문자열의 유사도를 0.0 ~ 1.0 사이 값으로 반환
     * - 띄어쓰기 무시
     * - 한글 모음 정규화 (ㅐ=ㅔ, ㅒ=ㅖ)
     * - 부분 포함 시 가산점
     */
    public static double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        
        // 1. 전처리 (공백 제거, 소문자, 모음 정규화)
        String n1 = normalize(s1);
        String n2 = normalize(s2);
        
        if (n1.isEmpty() && n2.isEmpty()) return 1.0;
        if (n1.isEmpty() || n2.isEmpty()) return 0.0;
        
        // 2. 완전 포함 관계 체크 (가장 강력한 시그널)
        if (n1.contains(n2) || n2.contains(n1)) {
            // 길이 비율에 따라 점수 부여 (너무 짧은 단어가 긴 문장에 포함된 건 100점 주면 안됨)
            double ratio = (double) Math.min(n1.length(), n2.length()) / Math.max(n1.length(), n2.length());
            // 포함되면 기본 0.8점 + 길이비율 * 0.2
            return 0.8 + (ratio * 0.2);
        }
        
        // 3. Levenshtein 거리 기반 유사도
        int distance = levenshteinDistance(n1, n2);
        int maxLength = Math.max(n1.length(), n2.length());
        
        return 1.0 - ((double) distance / maxLength);
    }

    private static String normalize(String s) {
        // 공백 제거, 소문자 변환
        String t = s.replaceAll("\\s+", "").toLowerCase();
        // ㅐ/ㅔ, ㅒ/ㅖ 정규화 (유사 발음 통일)
        t = t.replace('ㅐ', 'ㅔ').replace('ㅒ', 'ㅖ');
        return t;
    }

    private static int levenshteinDistance(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else if (j > 0) {
                    int newValue = costs[j - 1];
                    if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                        newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                    }
                    costs[j - 1] = lastValue;
                    lastValue = newValue;
                }
            }
            if (i > 0) costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }
}


