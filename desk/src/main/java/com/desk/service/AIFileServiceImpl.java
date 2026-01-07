package com.desk.service;

import com.desk.domain.Department;
import com.desk.domain.Member;
import com.desk.domain.TicketFile;
import com.desk.dto.AIFileRequestDTO;
import com.desk.dto.AIFileResponseDTO;
import com.desk.dto.AIFileResultDTO;
import com.desk.repository.MemberRepository;
import com.desk.repository.TicketFileRepository;
import com.desk.util.AIFilePromptUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Log4j2
@RequiredArgsConstructor
public class AIFileServiceImpl implements AIFileService {

    private final TicketFileRepository ticketFileRepository;
    private final MemberRepository memberRepository;
    private final AITicketClientService aiClient; // AI 클라이언트 추가
    private final ObjectMapper objectMapper; // JSON 파싱용

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    @Override
    public AIFileResponseDTO chat(String receiverEmail, AIFileRequestDTO request) {
        String userInput = request != null ? request.getUserInput() : null;
        String base = userInput == null ? "" : userInput.trim();

        // [1단계] 자바 정규식 기반 파싱 먼저 시도 (빠름)
        DateRange range = parseDateRange(base);
        String withoutDate = stripDateTokens(base).trim();
        NaturalFilter filter = parseNaturalFilter(withoutDate);

        // [2단계] 자바 파싱 결과 검증 - 불완전하면 AI 파싱 시도
        boolean needsAIParsing = false;
        
        // 날짜가 없거나, 부서/상대/키워드가 모두 없으면 AI 파싱 시도
        if (range == null && filter.department == null && filter.counterEmail == null 
                && (filter.keyword == null || filter.keyword.trim().isEmpty())) {
            needsAIParsing = true;
        }
        
        // 또는 날짜가 애매한 표현("쯤", "정도", "한달전쯤" 등)이 포함되어 있으면 AI 파싱 시도
        if (range == null && (base.contains("쯤") || base.contains("정도") || base.contains("한달전") 
                || base.contains("한달 전") || base.contains("두달전") || base.contains("두달 전"))) {
            needsAIParsing = true;
        }

        // [3단계] AI 파싱 (fallback)
        if (needsAIParsing) {
            try {
                String prompt = AIFilePromptUtil.getFileSearchParsePrompt(base);
                String jsonResult = aiClient.generateJson(prompt);
                
                JsonNode rootNode = objectMapper.readTree(jsonResult);
                JsonNode dateRangeNode = rootNode.path("dateRange");
                
                // 날짜 범위 파싱 (AI 결과로 덮어쓰기)
                if (!dateRangeNode.isMissingNode() && !dateRangeNode.isNull()) {
                    String fromStr = dateRangeNode.path("from").asText(null);
                    String toStr = dateRangeNode.path("to").asText(null);
                    if (fromStr != null && !fromStr.equals("null") && toStr != null && !toStr.equals("null")) {
                        try {
                            LocalDate fromDate = LocalDate.parse(fromStr);
                            LocalDate toDate = LocalDate.parse(toStr);
                            range = new DateRange(fromDate.atStartOfDay(), toDate.atTime(LocalTime.MAX));
                            log.info("[AI File Parse] AI가 날짜 추출: {} ~ {}", fromStr, toStr);
                        } catch (Exception e) {
                            log.warn("[AI File Parse] 날짜 파싱 실패: {} ~ {}", fromStr, toStr);
                        }
                    }
                }
                
                // 상대 이메일 파싱 (AI 결과로 보완)
                String counterEmailOrNickname = rootNode.path("counterEmail").asText(null);
                if ((filter.counterEmail == null) && counterEmailOrNickname != null 
                        && !counterEmailOrNickname.equals("null") && !counterEmailOrNickname.isEmpty()) {
                    // 이메일 형식인지 확인
                    if (EMAIL_PATTERN.matcher(counterEmailOrNickname).matches()) {
                        filter = new NaturalFilter(counterEmailOrNickname, filter.department, 
                                filter.keyword, filter.senderOnly, filter.receiverOnly);
                        log.info("[AI File Parse] AI가 상대 이메일 추출: {}", counterEmailOrNickname);
                    } else {
                        // 닉네임인 경우 이메일로 변환
                        Optional<Member> found = memberRepository.findByNickname(counterEmailOrNickname);
                        if (found.isPresent()) {
                            filter = new NaturalFilter(found.get().getEmail(), filter.department, 
                                    filter.keyword, filter.senderOnly, filter.receiverOnly);
                            log.info("[AI File Parse] AI가 상대 닉네임 추출: {} → {}", counterEmailOrNickname, found.get().getEmail());
                        }
                    }
                }
                
                // 부서 파싱 (AI 결과로 보완)
                String deptStr = rootNode.path("department").asText(null);
                if (filter.department == null && deptStr != null && !deptStr.equals("null") && !deptStr.isEmpty()) {
                    try {
                        Department department = Department.valueOf(deptStr.toUpperCase());
                        filter = new NaturalFilter(filter.counterEmail, department, 
                                filter.keyword, filter.senderOnly, filter.receiverOnly);
                        log.info("[AI File Parse] AI가 부서 추출: {}", deptStr);
                    } catch (IllegalArgumentException e) {
                        log.warn("[AI File Parse] 알 수 없는 부서명: {}", deptStr);
                    }
                }
                
                // 키워드 보완 (AI가 더 정확하게 추출한 경우)
                String aiKeyword = rootNode.path("keyword").asText("").trim();
                if (aiKeyword != null && !aiKeyword.isEmpty() 
                        && (filter.keyword == null || filter.keyword.trim().isEmpty() || aiKeyword.length() > filter.keyword.length())) {
                    filter = new NaturalFilter(filter.counterEmail, filter.department, 
                            aiKeyword, filter.senderOnly, filter.receiverOnly);
                    log.info("[AI File Parse] AI가 키워드 보완: {}", aiKeyword);
                }
                
                // 보낸/받은 필터 (AI 결과로 보완)
                boolean aiSenderOnly = rootNode.path("senderOnly").asBoolean(false);
                boolean aiReceiverOnly = rootNode.path("receiverOnly").asBoolean(false);
                if (aiSenderOnly != filter.senderOnly || aiReceiverOnly != filter.receiverOnly) {
                    filter = new NaturalFilter(filter.counterEmail, filter.department, 
                            filter.keyword, aiSenderOnly, aiReceiverOnly);
                    log.info("[AI File Parse] AI가 보낸/받은 필터 보완: senderOnly={}, receiverOnly={}", aiSenderOnly, aiReceiverOnly);
                }
                
            } catch (Exception e) {
                log.warn("[AI File Parse] AI 파싱 실패, 자바 파싱 결과 사용: {}", e.getMessage());
                // AI 파싱 실패 시 자바 파싱 결과 그대로 사용
            }
        }

        String kw = filter.keyword != null ? filter.keyword : "";

        PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // "보낸" 파일만 조회
        if (filter.senderOnly) {
            Page<TicketFile> page = ticketFileRepository.findByWriterAndSearch(receiverEmail, kw, pageable);
            return buildResponse(request, page);
        }
        
        // "받은" 파일만 조회
        if (filter.receiverOnly) {
            Page<TicketFile> page = ticketFileRepository.findByReceiverAndSearch(receiverEmail, kw, pageable);
            return buildResponse(request, page);
        }

        // 기본: 전체 조회 (기간/상대/부서 필터링)
        Page<TicketFile> page = ticketFileRepository.searchAccessibleFilesForAI(
                receiverEmail,
                kw,
                range != null ? range.from : null,
                range != null ? range.to : null,
                filter.counterEmail,
                filter.department,
                pageable
        );
        
        return buildResponse(request, page);
    }
    
    private AIFileResponseDTO buildResponse(AIFileRequestDTO request, Page<TicketFile> page) {
        AIFileResponseDTO resp = AIFileResponseDTO.builder()
                .conversationId(request != null ? request.getConversationId() : null)
                .results(page.getContent().stream().map(this::toResultDTO).toList())
                .build();

        int count = page.getNumberOfElements();
        if (count == 0) {
            resp.setAiMessage("검색 결과가 없습니다. 키워드/기간/상대 조건을 바꿔서 다시 시도해 주세요.");
        } else {
            resp.setAiMessage(String.format("검색 결과 %d건입니다. 우측 목록에서 다운로드할 파일을 선택하세요.", count));
        }
        return resp;
    }

    private AIFileResultDTO toResultDTO(TicketFile f) {
        return AIFileResultDTO.builder()
                .uuid(f.getUuid())
                .fileName(f.getFileName())
                .fileSize(f.getFileSize())
                .createdAt(f.getCreatedAt())
                .tno(f.getTicket() != null ? f.getTicket().getTno() : null)
                .ticketTitle(f.getTicket() != null ? f.getTicket().getTitle() : null)
                .writerEmail(f.getWriter())
                .receiverEmail(f.getReceiver())
                .build();
    }

    // -------------------------
    // Natural parsing helpers
    // -------------------------
    private static class DateRange {
        final LocalDateTime from;
        final LocalDateTime to;

        DateRange(LocalDateTime from, LocalDateTime to) {
            this.from = from;
            this.to = to;
        }
    }

    private static class NaturalFilter {
        final String counterEmail;
        final Department department;
        final String keyword;
        final boolean senderOnly;  // "보낸" 파일만
        final boolean receiverOnly; // "받은" 파일만

        NaturalFilter(String counterEmail, Department department, String keyword, boolean senderOnly, boolean receiverOnly) {
            this.counterEmail = counterEmail;
            this.department = department;
            this.keyword = keyword;
            this.senderOnly = senderOnly;
            this.receiverOnly = receiverOnly;
        }
    }

    private DateRange parseDateRange(String text) {
        if (text == null) return null;
        // "쯤/정도"는 기간 자체를 애매하게 만드는 게 아니라 '대략' 의미이므로 제거하고 파싱한다.
        String t = text.trim().replaceAll("(쯤|정도)", "");
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int currentMonth = today.getMonthValue();

        // 간단 키워드 기반
        if (t.contains("오늘")) {
            return new DateRange(today.atStartOfDay(), today.atTime(LocalTime.MAX));
        }
        if (t.contains("그제") || t.contains("그저께")) {
            LocalDate d = today.minusDays(2);
            return new DateRange(d.atStartOfDay(), d.atTime(LocalTime.MAX));
        }
        if (t.contains("어제")) {
            LocalDate d = today.minusDays(1);
            return new DateRange(d.atStartOfDay(), d.atTime(LocalTime.MAX));
        }
        if (t.contains("이번주") || t.contains("이번 주")) {
            LocalDate start = today.minusDays(today.getDayOfWeek().getValue() - 1L);
            LocalDate end = start.plusDays(6);
            return new DateRange(start.atStartOfDay(), end.atTime(LocalTime.MAX));
        }
        if (t.contains("지난주") || t.contains("지난 주") || t.contains("저번주") || t.contains("저번 주")) {
            LocalDate end = today.minusDays(today.getDayOfWeek().getValue());
            LocalDate start = end.minusDays(6);
            return new DateRange(start.atStartOfDay(), end.atTime(LocalTime.MAX));
        }
        if (t.contains("이번달") || t.contains("이번 달")) {
            LocalDate start = today.withDayOfMonth(1);
            LocalDate end = today.withDayOfMonth(today.lengthOfMonth());
            return new DateRange(start.atStartOfDay(), end.atTime(LocalTime.MAX));
        }
        if (t.contains("지난달") || t.contains("지난 달") || t.contains("저번달") || t.contains("저번 달")) {
            LocalDate d = today.minusMonths(1);
            LocalDate start = d.withDayOfMonth(1);
            LocalDate end = d.withDayOfMonth(d.lengthOfMonth());
            return new DateRange(start.atStartOfDay(), end.atTime(LocalTime.MAX));
        }
        if (t.contains("올해")) {
            LocalDate start = LocalDate.of(currentYear, 1, 1);
            LocalDate end = LocalDate.of(currentYear, 12, 31);
            return new DateRange(start.atStartOfDay(), end.atTime(LocalTime.MAX));
        }
        if (t.contains("작년")) {
            LocalDate start = LocalDate.of(currentYear - 1, 1, 1);
            LocalDate end = LocalDate.of(currentYear - 1, 12, 31);
            return new DateRange(start.atStartOfDay(), end.atTime(LocalTime.MAX));
        }
        if (t.contains("재작년")) {
            LocalDate start = LocalDate.of(currentYear - 2, 1, 1);
            LocalDate end = LocalDate.of(currentYear - 2, 12, 31);
            return new DateRange(start.atStartOfDay(), end.atTime(LocalTime.MAX));
        }

        // 상대기간/수식 파싱: "3일전", "2주후", "1달 전", "2개월 후", "1년 전" 등
        Matcher rel = Pattern.compile("(\\d{1,3})\\s*(일|주|달|개월|년)\\s*(전|후|남음)").matcher(t);
        if (rel.find()) {
            int n = Integer.parseInt(rel.group(1));
            String unit = rel.group(2);
            String dir = rel.group(3);

            LocalDate from;
            LocalDate to;

            boolean isBefore = "전".equals(dir);

            if ("일".equals(unit)) {
                LocalDate d = isBefore ? today.minusDays(n) : today.plusDays(n);
                from = d;
                to = d;
            } else if ("주".equals(unit)) {
                if (isBefore) {
                    from = today.minusWeeks(n);
                    to = today;
                } else {
                    from = today;
                    to = today.plusWeeks(n);
                }
            } else if ("달".equals(unit) || "개월".equals(unit)) {
                if (isBefore) {
                    from = today.minusMonths(n);
                    to = today;
                } else {
                    from = today;
                    to = today.plusMonths(n);
                }
            } else { // "년"
                if (isBefore) {
                    from = today.minusYears(n);
                    to = today;
                } else {
                    from = today;
                    to = today.plusYears(n);
                }
            }

            return new DateRange(from.atStartOfDay(), to.atTime(LocalTime.MAX));
        }

        // 단어 기반 기간: "일주일", "이주일", "사흘", "나흘"
        if (t.contains("일주일") || t.contains("1주일")) {
            LocalDate start = today.minusDays(6);
            return new DateRange(start.atStartOfDay(), today.atTime(LocalTime.MAX));
        }
        if (t.contains("이주일") || t.contains("2주일")) {
            LocalDate start = today.minusDays(13);
            return new DateRange(start.atStartOfDay(), today.atTime(LocalTime.MAX));
        }
        if (t.contains("사흘")) {
            LocalDate start = today.minusDays(2);
            return new DateRange(start.atStartOfDay(), today.atTime(LocalTime.MAX));
        }
        if (t.contains("나흘")) {
            LocalDate start = today.minusDays(3);
            return new DateRange(start.atStartOfDay(), today.atTime(LocalTime.MAX));
        }

        // 한글 숫자 기반: "한달전", "두달전" (자주 쓰는 표현만 우선 지원)
        if (t.contains("한달전") || t.contains("한달 전") || t.contains("한 달 전")) {
            LocalDate start = today.minusMonths(1);
            return new DateRange(start.atStartOfDay(), today.atTime(LocalTime.MAX));
        }
        if (t.contains("두달전") || t.contains("두달 전") || t.contains("두 달 전")) {
            LocalDate start = today.minusMonths(2);
            return new DateRange(start.atStartOfDay(), today.atTime(LocalTime.MAX));
        }

        // 월 범위 파싱: "12월에서 2월사이", "1월부터 3월까지" 등
        Matcher monthRangeMatcher = Pattern.compile("(\\d{1,2})월.*?(\\d{1,2})월").matcher(t);
        if (monthRangeMatcher.find()) {
            int startMonth = Integer.parseInt(monthRangeMatcher.group(1));
            int endMonth = Integer.parseInt(monthRangeMatcher.group(2));
            if (startMonth >= 1 && startMonth <= 12 && endMonth >= 1 && endMonth <= 12) {
                // 연도 추정: 현재 날짜와 가장 자연스러운 구간(현재를 포함하거나 가장 가까운 구간)을 선택
                int startYearA = currentYear;
                int endYearA = (endMonth < startMonth) ? currentYear + 1 : currentYear;
                LocalDate startA = LocalDate.of(startYearA, startMonth, 1);
                LocalDate endA = LocalDate.of(endYearA, endMonth, 1).withDayOfMonth(LocalDate.of(endYearA, endMonth, 1).lengthOfMonth());

                int startYearB = (endMonth < startMonth) ? currentYear - 1 : currentYear;
                int endYearB = currentYear;
                LocalDate startB = LocalDate.of(startYearB, startMonth, 1);
                LocalDate endB = LocalDate.of(endYearB, endMonth, 1).withDayOfMonth(LocalDate.of(endYearB, endMonth, 1).lengthOfMonth());

                boolean todayInA = !today.isBefore(startA) && !today.isAfter(endA);
                boolean todayInB = !today.isBefore(startB) && !today.isAfter(endB);

                LocalDateTime from;
                LocalDateTime to;
                if (todayInA && !todayInB) {
                    from = startA.atStartOfDay();
                    to = endA.atTime(LocalTime.MAX);
                } else if (todayInB && !todayInA) {
                    from = startB.atStartOfDay();
                    to = endB.atTime(LocalTime.MAX);
                } else {
                    // 둘 다 포함/둘 다 미포함이면 "오늘과의 거리"가 더 가까운 구간 선택
                    long distA = Math.min(Math.abs(java.time.temporal.ChronoUnit.DAYS.between(today, startA)),
                            Math.abs(java.time.temporal.ChronoUnit.DAYS.between(today, endA)));
                    long distB = Math.min(Math.abs(java.time.temporal.ChronoUnit.DAYS.between(today, startB)),
                            Math.abs(java.time.temporal.ChronoUnit.DAYS.between(today, endB)));
                    if (distA <= distB) {
                        from = startA.atStartOfDay();
                        to = endA.atTime(LocalTime.MAX);
                    } else {
                        from = startB.atStartOfDay();
                        to = endB.atTime(LocalTime.MAX);
                    }
                }
                return new DateRange(from, to);
            }
        }

        // 월 단위 파싱: "1월중", "1월중에", "12월", "2월" 등
        Matcher monthMatcher = Pattern.compile("(\\d{1,2})월").matcher(t);
        if (monthMatcher.find()) {
            int month = Integer.parseInt(monthMatcher.group(1));
            if (month >= 1 && month <= 12) {
                // 연도 추정: 현재 월보다 큰 월을 말하면(예: 1월에 12월) 기본은 "지난해"
                int year = currentYear;
                if (month > currentMonth && !(t.contains("내년") || t.contains("다음해") || t.contains("다음 해"))) {
                    year = currentYear - 1;
                }
                if (t.contains("내년") || t.contains("다음해") || t.contains("다음 해")) {
                    year = currentYear + 1;
                }
                if (t.contains("작년")) {
                    year = currentYear - 1;
                }
                LocalDate start = LocalDate.of(year, month, 1);
                LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
                return new DateRange(start.atStartOfDay(), end.atTime(LocalTime.MAX));
            }
        }

        // yyyy-mm-dd 단일 날짜가 있으면 그 날짜 하루로 처리
        Matcher m = Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})").matcher(t);
        if (m.find()) {
            int y = Integer.parseInt(m.group(1));
            int mo = Integer.parseInt(m.group(2));
            int da = Integer.parseInt(m.group(3));
            LocalDate d = LocalDate.of(y, mo, da);
            return new DateRange(d.atStartOfDay(), d.atTime(LocalTime.MAX));
        }

        // yyyy/mm/dd 또는 yyyy.mm.dd
        Matcher m2 = Pattern.compile("(\\d{4})[./](\\d{1,2})[./](\\d{1,2})").matcher(t);
        if (m2.find()) {
            int y = Integer.parseInt(m2.group(1));
            int mo = Integer.parseInt(m2.group(2));
            int da = Integer.parseInt(m2.group(3));
            LocalDate d = LocalDate.of(y, mo, da);
            return new DateRange(d.atStartOfDay(), d.atTime(LocalTime.MAX));
        }

        return null;
    }

    private String stripDateTokens(String text) {
        if (text == null) return "";
        return text
                .replace("오늘", "")
                .replace("어제", "")
                .replace("그제", "")
                .replace("그저께", "")
                .replace("쯤", "")
                .replace("정도", "")
                .replace("이번주", "")
                .replace("이번 주", "")
                .replace("지난주", "")
                .replace("지난 주", "")
                .replace("저번주", "")
                .replace("저번 주", "")
                .replace("이번달", "")
                .replace("이번 달", "")
                .replace("지난달", "")
                .replace("지난 달", "")
                .replace("저번달", "")
                .replace("저번 달", "")
                .replace("올해", "")
                .replace("작년", "")
                .replace("재작년", "")
                .replace("내년", "")
                .replaceAll("\\d{1,2}월", "")
                .replaceAll("\\d{4}-\\d{1,2}-\\d{1,2}", " ")
                .replaceAll("\\d{4}[./]\\d{1,2}[./]\\d{1,2}", " ")
                .replaceAll("에서.*?사이", "")
                .replaceAll("부터.*?까지", "")
                .replaceAll("\\d{1,3}\\s*(일|주|달|개월|년)\\s*(전|후|남음)", " ")
                .replace("일주일", "")
                .replace("1주일", "")
                .replace("이주일", "")
                .replace("2주일", "")
                .replace("사흘", "")
                .replace("나흘", "")
                .trim();
    }

    private NaturalFilter parseNaturalFilter(String text) {
        if (text == null) return new NaturalFilter(null, null, "", false, false);
        String t = text.trim();

        // 0) "보낸/받은" 필터링 감지 (제거 전에 먼저 체크)
        boolean senderOnly = t.contains("보낸") || t.contains("전송한") || t.contains("전달한");
        boolean receiverOnly = t.contains("받은") || t.contains("수신한");
        // "주고받은"은 둘 다 false (전체 조회)

        // 1) 이메일 추출 (상대 이메일)
        String counterEmail = null;
        Matcher m = EMAIL_PATTERN.matcher(t);
        if (m.find()) {
            counterEmail = m.group();
            t = t.replace(counterEmail, " ").trim();
        } else {
            // 닉네임 -> 이메일 (여러 토큰에서도 찾기)
            String[] tokens = t.split("\\s+");
            for (String token : tokens) {
                String cleaned = token.trim()
                        .replace("이랑", "")
                        .replace("랑", "")
                        .replace("와", "")
                        .replace("과", "")
                        .replace("한테", "")
                        .replace("에게", "");
                if (!cleaned.isEmpty()) {
                    Optional<Member> found = memberRepository.findByNickname(cleaned);
                    if (found.isPresent()) {
                        counterEmail = found.get().getEmail();
                        t = t.replace(token, " ").trim();
                        break;
                    }
                }
            }
        }

        // 2) 부서 추출 (한글/영문 모두 일부 지원, "디자인팀", "디자인 부서" 등도 인식)
        Department dept = parseDepartment(t);
        if (dept != null) {
            t = removeDepartmentToken(t);
        }

        // 3) 동작 키워드 제거 ("보낸", "받은", "주고받은", "얘기한" 등) - 필터링은 이미 감지했으므로 제거만
        t = t.replaceAll("보낸|받은|주고받은|얘기한|대화한|전송한|전달한|수신한", "").trim();

        // 4) 남은 텍스트는 키워드
        String keyword = t.trim();
        return new NaturalFilter(counterEmail, dept, keyword, senderOnly, receiverOnly);
    }

    private Department parseDepartment(String text) {
        if (text == null) return null;
        String u = text.toUpperCase(Locale.ROOT);
        // "디자인팀", "디자인 부서", "디자인" 모두 인식
        if (u.contains("DESIGN") || text.contains("디자인")) return Department.DESIGN;
        if (u.contains("DEVELOPMENT") || text.contains("개발")) return Department.DEVELOPMENT;
        if (u.contains("SALES") || text.contains("영업")) return Department.SALES;
        if (u.contains("HR") || text.contains("인사")) return Department.HR;
        if (u.contains("FINANCE") || text.contains("재무")) return Department.FINANCE;
        if (u.contains("PLANNING") || text.contains("기획")) return Department.PLANNING;
        return null;
    }

    private String removeDepartmentToken(String text) {
        if (text == null) return "";
        return text
                .replace("DESIGN", " ")
                .replace("DEVELOPMENT", " ")
                .replace("SALES", " ")
                .replace("HR", " ")
                .replace("FINANCE", " ")
                .replace("PLANNING", " ")
                .replace("디자인팀", " ")
                .replace("디자인 부서", " ")
                .replace("디자인", " ")
                .replace("개발팀", " ")
                .replace("개발 부서", " ")
                .replace("개발", " ")
                .replace("영업팀", " ")
                .replace("영업 부서", " ")
                .replace("영업", " ")
                .replace("인사팀", " ")
                .replace("인사 부서", " ")
                .replace("인사", " ")
                .replace("재무팀", " ")
                .replace("재무 부서", " ")
                .replace("재무", " ")
                .replace("기획팀", " ")
                .replace("기획 부서", " ")
                .replace("기획", " ")
                .trim();
    }
}


