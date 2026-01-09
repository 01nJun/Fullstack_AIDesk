package com.desk.service;

import com.desk.domain.Department;
import com.desk.domain.Member;
import com.desk.domain.TicketFile;
import com.desk.domain.ChatFile;
import com.desk.dto.AIFileRequestDTO;
import com.desk.dto.AIFileResponseDTO;
import com.desk.dto.AIFileResultDTO;
import com.desk.repository.MemberRepository;
import com.desk.repository.TicketFileRepository;
import com.desk.repository.chat.ChatFileRepository;
import com.desk.util.AIFilePromptUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.Token;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.desk.util.text.TextSimilarityUtil;

@Service
@Log4j2
public class AIFileServiceImpl implements AIFileService {

    private final TicketFileRepository ticketFileRepository;
    private final ChatFileRepository chatFileRepository;
    private final MemberRepository memberRepository;
    private final AITicketClientService aiClient; // AI 클라이언트 추가
    private final ObjectMapper objectMapper; // JSON 파싱용

    // 한국어 형태소 분석기 (Komoran) - 자연어에서 명사만 추출
    private Komoran komoran;

    public AIFileServiceImpl(TicketFileRepository ticketFileRepository,
                             ChatFileRepository chatFileRepository,
                             MemberRepository memberRepository,
                             AITicketClientService aiClient,
                             ObjectMapper objectMapper) {
        this.ticketFileRepository = ticketFileRepository;
        this.chatFileRepository = chatFileRepository;
        this.memberRepository = memberRepository;
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initKomoran() {
        try {
            this.komoran = new Komoran(DEFAULT_MODEL.LIGHT); // LIGHT 모델 사용 (빠름)
            log.info("[Komoran] 형태소 분석기 초기화 완료");
        } catch (Exception e) {
            log.warn("[Komoran] 초기화 실패, 기본 토큰화 사용: {}", e.getMessage());
            this.komoran = null;
        }
    }

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    // 불용어(검색 의미가 약한 단어) - 키워드 오염 방지용
    private static final Pattern KEYWORD_NOISE_PATTERN = Pattern.compile(
            "(관련|파일|자료|내역|건|주고받은|주고 받은|주고받기|주고 받기|건네받은|건네 받은|내가준|내가 준|" +
            "사진|이미지|그림|문서|자료|대화|대화한|얘기|얘기한|전송|전송한|전달|전달한|수신|수신한|" +
            "채팅방|단톡방|단톡|톡방|나눈|나눈거|나눈파일|공유|공유한|" +
            // 자연어에서 자주 붙는 '꼬리말' (의미 없는 토큰으로 AND 필터를 망치는 케이스 방지)
            "관련한|관련한거|관련한것|관련한\\s*건|관련된|관련된거|관련된것|관련된\\s*건|관련해서|관련해서는|관련해서도|" +
            "한거|한것|한\\s*건|했던거|했던것|했던\\s*건|그거|그것|그\\s*건|이거|이것|이\\s*건|" +
            "내용|내용들|내용\\s*정리|정리|정리한|정리본|요약|요약본|" +
            "찾아|찾아줘|찾아주세요|조회|조회해|조회해줘|조회해주세요|" +
            "입니다|이에요|해줘|해주세요|좀|그거|이거)"
    );

    // 닉네임 캐시 (매 요청 DB 전체 스캔 방지)
    private static final long NICKNAME_CACHE_TTL_MS = 5 * 60 * 1000L; // 5분
    private volatile List<String> activeNicknamesCache = List.of();
    private volatile long activeNicknamesCacheAtMs = 0L;

    @Override
    public AIFileResponseDTO chat(String receiverEmail, AIFileRequestDTO request) {
        String userInput = request != null ? request.getUserInput() : null;
        String base = userInput == null ? "" : userInput.trim();

        // [1단계] 자바 정규식 기반 파싱 먼저 시도 (빠름)
        DateRange range = parseDateRange(base);
        String withoutDate = stripDateTokens(base).trim();
        NaturalFilter filter = parseNaturalFilter(withoutDate);

        // ✅ 디버그: 자연어 → (기간/상대/부서/키워드) 파싱 결과를 로그로 남겨서
        // "왜 자연어로 치면 0건인지"를 실제 값 기준으로 추적 가능하게 한다.
        try {
            List<String> dbgTokens = extractKeywordTokens(filter.keyword);
            String dbgKw = dbgTokens.isEmpty() ? "" : dbgTokens.get(0);
            log.info("[AI File Parse] input='{}' | range={} | counter={} | dept={} | keyword='{}' | tokens={} | kw='{}'",
                    base,
                    range == null ? null : (range.from + " ~ " + range.to),
                    filter.counterEmail,
                    filter.department,
                    filter.keyword,
                    dbgTokens,
                    dbgKw
            );
        } catch (Exception e) {
            log.warn("[AI File Parse] debug log failed: {}", e.getMessage());
        }

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

        // 키워드 토큰화: 불용어 제거 후 핵심 토큰을 뽑는다.
        // - 여기서 뽑힌 토큰은 "조건(내용)"으로 취급한다. (0건이라고 해서 몰래 제거하지 않는다)
        List<String> keywordTokens = extractKeywordTokens(filter.keyword);

        // ticket/chat 각각에서 조금 넉넉히 가져와 merge-sort 후 top10
        PageRequest pageable = PageRequest.of(0, 30, Sort.by(Sort.Direction.DESC, "createdAt"));

        // ✅ 최종 검색 전략:
        // 1) 사용자가 준 조건(기간/상대/부서/키워드)을 모두 AND로 검색
        // 2) 0건이면 AI가 "같은 조건"을 더 잘 해석(키워드 분해/정규화 중심)하여 다시 AND 검색
        // 3) 그래도 0건이면 "겹치는 조건 개수"가 가장 큰 결과를 제시 (3개→2개→1개), 안내문에는 '맞춘 조건'만 표시
        return searchWithAiAndOverlap(receiverEmail, request, base, range, filter, keywordTokens, pageable);
    }

    private enum Cond {
        DATE, DEPT, COUNTER, KEYWORD
    }

    private static class SearchParams {
        final LocalDateTime fromDt;
        final LocalDateTime toDt;
        final String counterEmail;
        final Department dept;
        final List<String> keywordTokens;

        SearchParams(LocalDateTime fromDt, LocalDateTime toDt, String counterEmail, Department dept, List<String> keywordTokens) {
            this.fromDt = fromDt;
            this.toDt = toDt;
            this.counterEmail = counterEmail;
            this.dept = dept;
            this.keywordTokens = keywordTokens == null ? List.of() : keywordTokens;
        }
    }

    private static class SearchResult {
        final List<TicketFile> ticketFiles;
        final List<ChatFile> chatFiles;
        SearchResult(List<TicketFile> ticketFiles, List<ChatFile> chatFiles) {
            this.ticketFiles = ticketFiles == null ? List.of() : ticketFiles;
            this.chatFiles = chatFiles == null ? List.of() : chatFiles;
        }
        boolean isEmpty() {
            return (ticketFiles == null || ticketFiles.isEmpty()) && (chatFiles == null || chatFiles.isEmpty());
        }
    }

    private static class AiParsed {
        final DateRange range;
        final NaturalFilter filter;
        AiParsed(DateRange range, NaturalFilter filter) {
            this.range = range;
            this.filter = filter;
        }
    }

    /**
     * strict 0건일 때만 "AI 재파싱"을 한 번 시도한다.
     * - 기존 chat()의 AI 파싱은 '거의 아무것도 못 뽑았을 때'만 돌도록 좁게 gating 되어 있음
     * - 사용자가 조건을 줬는데도(기간/부서/키워드 등) 자바 파싱이 엇나가면 strict=0이 나올 수 있으니,
     *   그때는 AI가 한 번 더 구조화해서 strict 재시도를 한다.
     * - 성능/비용: 방어적으로 결과가 나올 때만 채택한다(결과 없으면 기존 파싱 유지).
     */
    private AiParsed tryAiParseAll(String originalInput, DateRange currentRange, NaturalFilter currentFilter) {
        if (originalInput == null || originalInput.isBlank()) return null;
        try {
            String prompt = AIFilePromptUtil.getFileSearchParsePrompt(originalInput);
            String jsonResult = aiClient.generateJson(prompt);
            JsonNode rootNode = objectMapper.readTree(jsonResult);

            DateRange nextRange = currentRange;
            NaturalFilter nextFilter = currentFilter;

            // 날짜 범위
            JsonNode dateRangeNode = rootNode.path("dateRange");
            if (!dateRangeNode.isMissingNode() && !dateRangeNode.isNull()) {
                String fromStr = dateRangeNode.path("from").asText(null);
                String toStr = dateRangeNode.path("to").asText(null);
                if (fromStr != null && !fromStr.equals("null") && toStr != null && !toStr.equals("null")) {
                    try {
                        LocalDate fromDate = LocalDate.parse(fromStr);
                        LocalDate toDate = LocalDate.parse(toStr);
                        nextRange = new DateRange(fromDate.atStartOfDay(), toDate.atTime(LocalTime.MAX));
                    } catch (Exception ignore) {
                        // ignore
                    }
                }
            }

            // 상대(이메일/닉네임)
            String counterEmailOrNickname = rootNode.path("counterEmail").asText(null);
            if (counterEmailOrNickname != null && !counterEmailOrNickname.equals("null") && !counterEmailOrNickname.isBlank()) {
                String counterEmail = null;
                if (EMAIL_PATTERN.matcher(counterEmailOrNickname).matches()) {
                    counterEmail = counterEmailOrNickname;
                } else {
                    try {
                        Optional<Member> found = memberRepository.findByNickname(counterEmailOrNickname);
                        if (found.isPresent()) counterEmail = found.get().getEmail();
                    } catch (Exception ignore) {
                        // ignore
                    }
                }
                if (counterEmail != null && !counterEmail.isBlank()) {
                    nextFilter = new NaturalFilter(counterEmail, nextFilter.department, nextFilter.keyword, nextFilter.senderOnly, nextFilter.receiverOnly);
                }
            }

            // 부서
            String deptStr = rootNode.path("department").asText(null);
            if (deptStr != null && !deptStr.equals("null") && !deptStr.isBlank()) {
                try {
                    Department department = Department.valueOf(deptStr.toUpperCase(Locale.ROOT));
                    nextFilter = new NaturalFilter(nextFilter.counterEmail, department, nextFilter.keyword, nextFilter.senderOnly, nextFilter.receiverOnly);
                } catch (IllegalArgumentException ignore) {
                    // ignore
                }
            }

            // 키워드
            String aiKeyword = rootNode.path("keyword").asText("").trim();
            if (aiKeyword != null && !aiKeyword.isBlank()) {
                // 결과 유도 목적: 길이가 더 길면 우선 사용, 아니면 기존 유지
                if (nextFilter.keyword == null || nextFilter.keyword.isBlank() || aiKeyword.length() > nextFilter.keyword.length()) {
                    nextFilter = new NaturalFilter(nextFilter.counterEmail, nextFilter.department, aiKeyword, nextFilter.senderOnly, nextFilter.receiverOnly);
                }
            }

            // 보낸/받은 필터
            boolean aiSenderOnly = rootNode.path("senderOnly").asBoolean(false);
            boolean aiReceiverOnly = rootNode.path("receiverOnly").asBoolean(false);
            if (aiSenderOnly != nextFilter.senderOnly || aiReceiverOnly != nextFilter.receiverOnly) {
                nextFilter = new NaturalFilter(nextFilter.counterEmail, nextFilter.department, nextFilter.keyword, aiSenderOnly, aiReceiverOnly);
            }

            return new AiParsed(nextRange, nextFilter);
        } catch (Exception e) {
            log.warn("[AI File] ai parse-all failed: {}", e.getMessage());
            return null;
        }
    }

    private AIFileResponseDTO searchWithAiAndOverlap(String myEmail,
                                                     AIFileRequestDTO request,
                                                     String originalInput,
                                                     DateRange range,
                                                     NaturalFilter filter,
                                                     List<String> keywordTokens,
                                                     PageRequest pageable) {
        LocalDateTime fromDt = range != null ? range.from : null;
        LocalDateTime toDt = range != null ? range.to : null;

        SearchParams params = new SearchParams(fromDt, toDt, filter.counterEmail, filter.department, keywordTokens);

        // [1] strict AND search (all recognized conditions)
        Set<Cond> strict = buildPresentConds(params);
        SearchResult strictRes = runSearch(myEmail, params, filter, strict, pageable);
        if (!strictRes.isEmpty()) {
            AIFileResponseDTO resp = buildResponseMerged(request, strictRes.ticketFiles, strictRes.chatFiles, "", params.keywordTokens);
            // buildResponseMerged가 기본 메시지를 세팅하므로 그대로 둔다.
            return resp;
        }

        // [1.5] strict=0이면 AI가 한 번 더 "전체 파싱"을 시도해서 strict 재실행
        // - 자바 파싱이 엇나간 경우(부서/상대/날짜/키워드) 회복용
        AiParsed parsed = tryAiParseAll(originalInput, range, filter);
        if (parsed != null) {
            DateRange aiRange = parsed.range;
            NaturalFilter aiFilter = parsed.filter;
            LocalDateTime aiFrom = aiRange != null ? aiRange.from : null;
            LocalDateTime aiTo = aiRange != null ? aiRange.to : null;
            List<String> aiKeywordTokens = extractKeywordTokens(aiFilter.keyword);
            SearchParams aiParamsAll = new SearchParams(aiFrom, aiTo, aiFilter.counterEmail, aiFilter.department, aiKeywordTokens);
            Set<Cond> aiStrictAll = buildPresentConds(aiParamsAll);
            SearchResult aiStrictAllRes = runSearch(myEmail, aiParamsAll, aiFilter, aiStrictAll, pageable);
            if (!aiStrictAllRes.isEmpty()) {
                AIFileResponseDTO resp = buildResponseMerged(request, aiStrictAllRes.ticketFiles, aiStrictAllRes.chatFiles, "", aiKeywordTokens);
                return resp;
            }
            // 이후 단계에서도 "AI 파싱 결과"를 기반으로 진행 (겹치는 조건 계산 포함)
            range = aiRange;
            filter = aiFilter;
            fromDt = aiFrom;
            toDt = aiTo;
            params = aiParamsAll;
            strict = aiStrictAll;
        }

        // [2] AI 강개입(조건 유지): "키워드"가 난해한 경우에만 재해석/정규화해서 다시 AND 시도
        // - 자바로 충분히 잡힐 수 있는 간단 키워드는 AI를 태우지 않는다(비용/지연/오해 방지)
        if (shouldUseAiKeywordRefine(originalInput, filter.keyword)) {
            List<String> aiTokens = tryAiRefineKeywordTokens(originalInput);
            if (aiTokens != null && !aiTokens.isEmpty() && !aiTokens.equals(params.keywordTokens)) {
                SearchParams aiParams = new SearchParams(fromDt, toDt, params.counterEmail, params.dept, aiTokens);
                Set<Cond> aiStrict = buildPresentConds(aiParams);
                SearchResult aiStrictRes = runSearch(myEmail, aiParams, filter, aiStrict, pageable);
                if (!aiStrictRes.isEmpty()) {
                    AIFileResponseDTO resp = buildResponseMerged(request, aiStrictRes.ticketFiles, aiStrictRes.chatFiles, "", aiTokens);
                    return resp;
                }
                // 이후 overlap 단계에서도 AI 토큰을 우선 사용
                params = aiParams;
                strict = aiStrict;
            }
        }

        // [3] overlap: 조건 개수(4→3→2→1) 순으로 "가장 많이 겹치는 조건 조합 1개"를 선택해서 그 결과만 보여준다.
        // ✅ 중요: 여러 조합을 UNION으로 섞어버리면(예: {기간} + {키워드}) 사용자가 "파일명에 있는데 왜 못찾냐"로 체감한다.
        SearchResult best = null;
        Set<Cond> bestConds = null;
        int bestScore = Integer.MIN_VALUE;
        int maxK = strict.size();
        for (int k = maxK - 1; k >= 1; k--) {
            List<Set<Cond>> subsets = subsetsOfSize(strict, k);
            for (Set<Cond> s : subsets) {
                SearchResult r = runSearch(myEmail, params, filter, s, pageable);
                if (r.isEmpty()) continue;
                int score = subsetScore(s, r);
                if (score > bestScore) {
                    bestScore = score;
                    best = r;
                    bestConds = s;
                }
            }
            if (best != null) break; // 가장 큰 k에서 하나라도 찾으면 종료
        }

        if (best != null && bestConds != null && !best.isEmpty()) {
            List<String> shownTokens = bestConds.contains(Cond.KEYWORD) ? params.keywordTokens : List.of();
            AIFileResponseDTO resp = buildResponseMerged(request, best.ticketFiles, best.chatFiles, "", shownTokens);
            resp.setAiMessage(buildMatchedOnlyMessage(bestConds, range, filter, shownTokens));
            return resp;
        }

        // [4] still none → 안내문 + 입력 팁
        AIFileResponseDTO resp = AIFileResponseDTO.builder()
                .conversationId(request != null ? request.getConversationId() : null)
                .results(new ArrayList<>())
                .build();
        resp.setAiMessage(buildNotFoundMessageWithTips());
        return resp;
    }

    /**
     * AI는 "자바로 충분히 잡힐 수 있는 간단 키워드"에는 태우지 않는다.
     * - 붙여쓰기/복합어처럼 토큰 분해가 실패할 가능성이 높을 때만 사용
     * - ✅ 추출된 토큰이 모두 짧으면(1-2글자) AI에 바로 넘김
     */
    private boolean shouldUseAiKeywordRefine(String originalInput, String rawKeyword) {
        if (rawKeyword == null) return false;
        String kw = rawKeyword.trim();
        if (kw.isEmpty()) return false;

        // 불용어/꼬리말 제거 후에도 의미 있는 길이가 남아야 함
        String cleaned = KEYWORD_NOISE_PATTERN.matcher(kw).replaceAll(" ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        
        // ✅ 추출된 토큰 확인
        List<String> tokens = extractKeywordTokens(cleaned);
        
        // 토큰이 없거나 모두 2글자 이하면 AI에 바로 넘김
        if (tokens.isEmpty() || tokens.stream().allMatch(t -> t.length() <= 2)) {
            log.info("[AI Refine] 짧은 토큰만 존재 → AI 개입 | keyword={}", kw);
            return true;
        }
        
        // 원본이 4글자 미만이면 AI 불필요 (이미 Komoran이 충분히 처리)
        if (cleaned.length() < 4) return false;

        // 공백 없는 한글 복합어(예: 배너디자인/신제품기획서/귀여운짤 등)에서만 AI 리파인
        // (공백 있는 질의는 자바/유사도 검색으로 충분히 잡히는 경우가 많고, AI 오해도 잦음)
        if (cleaned.contains(" ")) return false;
        return containsHangul(cleaned);
    }

    private int subsetScore(Set<Cond> subset, SearchResult r) {
        // 조건 우선순위: KEYWORD(가장 핵심) > COUNTER > DEPT > DATE
        int w = 0;
        if (subset.contains(Cond.KEYWORD)) w += 1000;
        if (subset.contains(Cond.COUNTER)) w += 100;
        if (subset.contains(Cond.DEPT)) w += 10;
        if (subset.contains(Cond.DATE)) w += 1;

        int count = (r.ticketFiles == null ? 0 : r.ticketFiles.size()) + (r.chatFiles == null ? 0 : r.chatFiles.size());
        // 같은 subset 우선순위에서는 결과가 더 많은 쪽을 살짝 우대
        return w * 1000 + Math.min(count, 999);
    }

    private Set<Cond> buildPresentConds(SearchParams p) {
        LinkedHashSet<Cond> s = new LinkedHashSet<>();
        if (p.fromDt != null || p.toDt != null) s.add(Cond.DATE);
        if (p.dept != null) s.add(Cond.DEPT);
        if (p.counterEmail != null && !p.counterEmail.isBlank()) s.add(Cond.COUNTER);
        if (p.keywordTokens != null && !p.keywordTokens.isEmpty()) s.add(Cond.KEYWORD);
        return s;
    }

    private List<Set<Cond>> subsetsOfSize(Set<Cond> base, int k) {
        List<Cond> items = new ArrayList<>(base);
        List<Set<Cond>> out = new ArrayList<>();
        backtrackSubsets(items, 0, k, new LinkedHashSet<>(), out);
        return out;
    }

    private void backtrackSubsets(List<Cond> items, int idx, int k, LinkedHashSet<Cond> cur, List<Set<Cond>> out) {
        if (cur.size() == k) {
            out.add(new LinkedHashSet<>(cur));
            return;
        }
        if (idx >= items.size()) return;
        // pick
        cur.add(items.get(idx));
        backtrackSubsets(items, idx + 1, k, cur, out);
        cur.remove(items.get(idx));
        // skip
        backtrackSubsets(items, idx + 1, k, cur, out);
    }

    private SearchResult runSearch(String myEmail,
                                   SearchParams params,
                                   NaturalFilter filter,
                                   Set<Cond> conds,
                                   PageRequest pageable) {
        LocalDateTime fromDt = conds.contains(Cond.DATE) ? params.fromDt : null;
        LocalDateTime toDt = conds.contains(Cond.DATE) ? params.toDt : null;
        String counter = conds.contains(Cond.COUNTER) ? params.counterEmail : null;
        Department dept = conds.contains(Cond.DEPT) ? params.dept : null;
        List<String> tokens = conds.contains(Cond.KEYWORD) ? params.keywordTokens : List.of();

        // 1. 역발상 검색: 필터(기간/부서/상대)가 명확하면 키워드 없이 DB를 넓게 긁어온 뒤, Java에서 유사도 정렬
        boolean hasFilter = (fromDt != null || toDt != null || counter != null || dept != null);
        boolean hasKeyword = (tokens != null && !tokens.isEmpty());

        // 키워드만 있고 필터가 없으면 DB LIKE가 훨씬 빠르므로 기존 방식 (전체 스캔 방지)
        // 필터가 있으면 "후보군 Fetch -> 유사도 Scoring" 전략 사용
        if (hasFilter && hasKeyword) {
            return runSearchWithSimilarity(myEmail, fromDt, toDt, counter, dept, tokens, filter, pageable);
        }

        // 기존 로직 (키워드가 없거나, 필터 없이 키워드만 있는 경우)
        // seed tokens: 대표 1개 고정 금지. (합성어/띄어쓰기 불일치 대비)
        List<String> seeds = buildKeywordSeeds(tokens);

        LinkedHashMap<String, TicketFile> ticketMap = new LinkedHashMap<>();
        LinkedHashMap<String, ChatFile> chatMap = new LinkedHashMap<>();

        if (seeds.isEmpty()) seeds = List.of("");

        for (String seed : seeds) {
            String kw = seed == null ? "" : seed.trim();
            Page<TicketFile> ticketPage = ticketFileRepository.searchAccessibleFilesForAI(
                    myEmail, kw, fromDt, toDt, counter, dept, pageable
            );
            Page<ChatFile> chatPage = chatFileRepository.searchAccessibleChatFilesForAI(
                    myEmail, kw, fromDt, toDt, counter, dept, pageable
            );

            List<TicketFile> tf = ticketPage != null ? ticketPage.getContent() : List.of();
            List<ChatFile> cf = chatPage != null ? chatPage.getContent() : List.of();

            // AND 후처리 (내용 조건이 있을 때만)
            if (tokens != null && !tokens.isEmpty()) {
                tf = tf.stream().filter(f -> matchesAllTokens(f, tokens)).toList();
                Map<String, String> emailToNickname = new HashMap<>();
                cf = cf.stream().filter(f -> matchesAllTokensChat(f, tokens, emailToNickname)).toList();
            }

            // 보낸/받은 필터는 "항상" 적용 (사용자 입력에 기반)
            if (filter.senderOnly) {
                tf = tf.stream().filter(f -> myEmail.equalsIgnoreCase(f.getWriter())).toList();
                cf = cf.stream().filter(f -> myEmail.equalsIgnoreCase(f.getWriter())).toList();
            }
            if (filter.receiverOnly) {
                tf = tf.stream().filter(f -> myEmail.equalsIgnoreCase(f.getReceiver())).toList();
                // chat은 writer != myEmail로 처리 (receiver는 group에서 null일 수 있음)
                cf = cf.stream().filter(f -> f.getWriter() == null || !myEmail.equalsIgnoreCase(f.getWriter())).toList();
            }

            for (TicketFile f : tf) {
                if (f == null || f.getUuid() == null) continue;
                ticketMap.putIfAbsent(f.getUuid(), f);
            }
            for (ChatFile f : cf) {
                if (f == null || f.getUuid() == null) continue;
                chatMap.putIfAbsent(f.getUuid(), f);
            }
        }

        return new SearchResult(new ArrayList<>(ticketMap.values()), new ArrayList<>(chatMap.values()));
    }

    /**
     * [역발상 검색]
     * 1. DB: 키워드 조건 없이(kw="") 필터(기간/부서/상대)만으로 최신 100건 조회
     * 2. Java: 키워드와의 유사도(Similarity) 계산
     * 3. 70점 이상만 남기고 정렬
     */
    private SearchResult runSearchWithSimilarity(String myEmail,
                                                 LocalDateTime fromDt, LocalDateTime toDt,
                                                 String counter, Department dept,
                                                 List<String> tokens,
                                                 NaturalFilter filter,
                                                 PageRequest originalPageable) {
        // DB Fetch용 페이징: 정렬은 최신순, 개수는 좀 넉넉하게 (예: 100개)
        // 너무 많이 가져오면 느리니까 적당히 끊음.
        PageRequest fetchPageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 1. DB Fetch (Keyword = "")
        Page<TicketFile> ticketPage = ticketFileRepository.searchAccessibleFilesForAI(
                myEmail, "", fromDt, toDt, counter, dept, fetchPageable
        );
        Page<ChatFile> chatPage = chatFileRepository.searchAccessibleChatFilesForAI(
                myEmail, "", fromDt, toDt, counter, dept, fetchPageable
        );

        List<TicketFile> tf = ticketPage != null ? ticketPage.getContent() : new ArrayList<>();
        List<ChatFile> cf = chatPage != null ? chatPage.getContent() : new ArrayList<>();

        // 2. 보낸/받은 필터 적용
        if (filter.senderOnly) {
            tf = tf.stream().filter(f -> myEmail.equalsIgnoreCase(f.getWriter())).toList();
            cf = cf.stream().filter(f -> myEmail.equalsIgnoreCase(f.getWriter())).toList();
        }
        if (filter.receiverOnly) {
            tf = tf.stream().filter(f -> myEmail.equalsIgnoreCase(f.getReceiver())).toList();
            cf = cf.stream().filter(f -> f.getWriter() == null || !myEmail.equalsIgnoreCase(f.getWriter())).toList();
        }

        // 3. Similarity Scoring & Filtering
        // 사용자가 입력한 키워드 전체를 하나의 문장으로 보고 비교 (tokens join)
        String userQuery = String.join(" ", tokens);

        List<TicketFile> sortedTf = tf.stream()
                .filter(f -> calculateMaxScore(f, userQuery) >= 0.7) // 70점 이상
                .sorted((f1, f2) -> Double.compare(calculateMaxScore(f2, userQuery), calculateMaxScore(f1, userQuery)))
                .toList();

        Map<String, String> emailToNickname = new HashMap<>(); // 채팅방 닉네임 캐시
        List<ChatFile> sortedCf = cf.stream()
                .filter(f -> calculateMaxScoreChat(f, userQuery, emailToNickname) >= 0.7)
                .sorted((f1, f2) -> Double.compare(calculateMaxScoreChat(f2, userQuery, emailToNickname), calculateMaxScoreChat(f1, userQuery, emailToNickname)))
                .toList();

        return new SearchResult(sortedTf, sortedCf);
    }

    private double calculateMaxScore(TicketFile f, String query) {
        if (f == null) return 0.0;
        double max = 0.0;
        // 파일명
        max = Math.max(max, TextSimilarityUtil.calculateSimilarity(f.getFileName(), query));
        // 티켓 제목/내용/요약
        if (f.getTicket() != null) {
            max = Math.max(max, TextSimilarityUtil.calculateSimilarity(f.getTicket().getTitle(), query));
            // 내용은 너무 길면 유사도가 떨어질 수 있으니, 부분 포함 가산점이 중요
            max = Math.max(max, TextSimilarityUtil.calculateSimilarity(f.getTicket().getContent(), query));
            max = Math.max(max, TextSimilarityUtil.calculateSimilarity(f.getTicket().getPurpose(), query));
            max = Math.max(max, TextSimilarityUtil.calculateSimilarity(f.getTicket().getRequirement(), query));
        }
        return max;
    }

    private double calculateMaxScoreChat(ChatFile f, String query, Map<String, String> emailToNickname) {
        if (f == null) return 0.0;
        double max = 0.0;
        // 파일명
        max = Math.max(max, TextSimilarityUtil.calculateSimilarity(f.getFileName(), query));
        
        // 채팅방 이름
        if (f.getChatRoom() != null) {
            max = Math.max(max, TextSimilarityUtil.calculateSimilarity(f.getChatRoom().getName(), query));
        }
        
        // 작성자 닉네임 (가끔 이름으로 파일을 기억하는 경우)
        String writerEmail = f.getWriter();
        if (writerEmail != null) {
             String nick = emailToNickname.get(writerEmail);
             if (nick == null) {
                 try {
                     nick = memberRepository.findById(writerEmail).map(Member::getNickname).orElse(null);
                 } catch(Exception e) { nick = null; }
                 if (nick != null) emailToNickname.put(writerEmail, nick);
             }
             if (nick != null) {
                 max = Math.max(max, TextSimilarityUtil.calculateSimilarity(nick, query));
             }
        }
        return max;
    }

    private List<String> buildKeywordSeeds(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) return List.of("");
        LinkedHashSet<String> seeds = new LinkedHashSet<>();
        // 1) 가장 정보량 큰 토큰들
        for (int i = 0; i < Math.min(3, tokens.size()); i++) {
            String t = tokens.get(i);
            if (t != null && !t.isBlank()) seeds.add(t.trim());
        }
        // 2) 너무 일반적인 토큰(예: "디자인")이 앞에 오는 것을 방지: 길이순 뒤쪽도 1개 추가
        if (tokens.size() > 3) {
            String last = tokens.get(tokens.size() - 1);
            if (last != null && !last.isBlank()) seeds.add(last.trim());
        }
        return new ArrayList<>(seeds);
    }

    private List<String> tryAiRefineKeywordTokens(String originalInput) {
        try {
            // 기존 parse 프롬프트를 재활용하되, 목적은 keyword 정규화/분해다.
            String prompt = AIFilePromptUtil.getFileSearchParsePrompt(originalInput == null ? "" : originalInput);
            String jsonResult = aiClient.generateJson(prompt);
            JsonNode rootNode = objectMapper.readTree(jsonResult);
            String aiKeyword = rootNode.path("keyword").asText("").trim();
            if (aiKeyword == null || aiKeyword.isBlank()) return List.of();
            return extractKeywordTokens(aiKeyword);
        } catch (Exception e) {
            log.warn("[AI File] keyword refine failed: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildMatchedOnlyMessage(Set<Cond> matched,
                                          DateRange range,
                                          NaturalFilter filter,
                                          List<String> keywordTokens) {
        List<String> parts = new ArrayList<>();
        if (matched.contains(Cond.DATE) && range != null) {
            // 날짜 포맷 자연스럽게 (예: 1월 1일 ~ 1월 31일, 혹은 연도 포함)
            LocalDate f = range.from.toLocalDate();
            LocalDate t = range.to.toLocalDate();
            String dateStr;
            if (f.getYear() == t.getYear()) {
                if (f.getMonthValue() == t.getMonthValue() && f.getDayOfMonth() == 1 && t.getDayOfMonth() == t.lengthOfMonth()) {
                    dateStr = f.getYear() + "년 " + f.getMonthValue() + "월";
                } else if (f.equals(t)) {
                    dateStr = f.getMonthValue() + "월 " + f.getDayOfMonth() + "일";
                } else {
                    dateStr = f.getMonthValue() + "월 " + f.getDayOfMonth() + "일 ~ " + t.getMonthValue() + "월 " + t.getDayOfMonth() + "일";
                }
            } else {
                dateStr = f.toString() + " ~ " + t.toString();
            }
            parts.add(dateStr);
        }
        if (matched.contains(Cond.DEPT) && filter.department != null) {
            // 부서명 한글화 (간단 매핑)
            String dName = filter.department.name();
            if (dName.equals("DESIGN")) dName = "디자인팀";
            else if (dName.equals("DEVELOPMENT")) dName = "개발팀";
            else if (dName.equals("SALES")) dName = "영업팀";
            else if (dName.equals("HR")) dName = "인사팀";
            else if (dName.equals("FINANCE")) dName = "재무팀";
            else if (dName.equals("PLANNING")) dName = "기획팀";
            parts.add(dName);
        }
        if (matched.contains(Cond.COUNTER) && filter.counterEmail != null) {
            // 이메일 대신 닉네임이나 ID만
            String display = filter.counterEmail;
            try {
                // 닉네임 조회 시도
                Optional<Member> m = memberRepository.findById(filter.counterEmail);
                if (m.isPresent()) display = m.get().getNickname() + "님";
                else {
                    int idx = display.indexOf("@");
                    if (idx > 0) display = display.substring(0, idx) + "님";
                }
            } catch (Exception e) {
                 int idx = display.indexOf("@");
                 if (idx > 0) display = display.substring(0, idx) + "님";
            }
            parts.add(display);
        }
        if (matched.contains(Cond.KEYWORD) && keywordTokens != null && !keywordTokens.isEmpty()) {
            // 너무 길게 노출하지 않도록 상위 3개만
            List<String> top = keywordTokens.size() > 3 ? keywordTokens.subList(0, 3) : keywordTokens;
            // 따옴표로 감싸서 키워드임을 명확히
            List<String> quoted = top.stream().map(s -> "'" + s + "'").toList();
            parts.add("내용 " + String.join(", ", quoted));
        }

        String joined = parts.isEmpty() ? "일부 조건" : String.join(", ", parts);
        return "요청하신 조건에 모두 일치하는 파일을 찾지 못해, " + joined + " 기준으로 검색된 결과를 보여드릴게요.\n"
                + "(" + buildSearchTipInline() + ")";
    }

    private String buildSearchTipInline() {
        return "팁: 기간, 부서, 이름, 업무내용을 적어주세요. 예) 1월, 디자인팀, 김철수, 신제품 기획서";
    }

    private String buildNotFoundMessageWithTips() {
        return "원하시는 조건에 모두 일치하는 파일을 찾지 못했습니다.\n"
                + "(" + buildSearchTipInline() + ")";
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

    private AIFileResultDTO toResultDTO(ChatFile f) {
        return AIFileResultDTO.builder()
                .uuid(f.getUuid())
                .fileName(f.getFileName())
                .fileSize(f.getFileSize())
                .createdAt(f.getCreatedAt())
                .tno(null)
                .ticketTitle(null)
                .writerEmail(f.getWriter())
                .receiverEmail(f.getReceiver())
                .build();
    }

    private AIFileResponseDTO buildResponseMerged(AIFileRequestDTO request,
                                                  List<TicketFile> ticketFiles,
                                                  List<ChatFile> chatFiles,
                                                  String kw,
                                                  List<String> keywordTokens) {
        List<AIFileResultDTO> merged = new ArrayList<>(
                (ticketFiles == null ? 0 : ticketFiles.size()) + (chatFiles == null ? 0 : chatFiles.size())
        );
        if (ticketFiles != null) merged.addAll(ticketFiles.stream().map(this::toResultDTO).toList());
        if (chatFiles != null) merged.addAll(chatFiles.stream().map(this::toResultDTO).toList());

        merged.sort(Comparator.comparing(AIFileResultDTO::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        // 최종 top10
        if (merged.size() > 10) {
            merged = merged.subList(0, 10);
        }

        AIFileResponseDTO resp = AIFileResponseDTO.builder()
                .conversationId(request != null ? request.getConversationId() : null)
                .results(merged)
                .build();

        int count = merged.size();
        if (count == 0) {
            // ✅ 0건이면 항상 동일한 안내/팁 포맷 사용 (사용자 입장에서 일관성)
            resp.setAiMessage(buildNotFoundMessageWithTips());
        } else {
            resp.setAiMessage(String.format("검색 결과 %d건입니다. 우측 목록에서 다운로드할 파일을 선택하세요.", count));
        }
        return resp;
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

        // 간단 키워드 기반 (정규식으로 더 정확하게 매칭)
        if (Pattern.compile("오늘").matcher(t).find()) {
            DateRange result = new DateRange(today.atStartOfDay(), today.atTime(LocalTime.MAX));
            log.info("[Date Parse] 오늘: {}", result);
            return result;
        }
        if (Pattern.compile("그제|그저께").matcher(t).find()) {
            LocalDate d = today.minusDays(2);
            DateRange result = new DateRange(d.atStartOfDay(), d.atTime(LocalTime.MAX));
            log.info("[Date Parse] 그제: {}", result);
            return result;
        }
        if (Pattern.compile("어제").matcher(t).find()) {
            LocalDate d = today.minusDays(1);
            DateRange result = new DateRange(d.atStartOfDay(), d.atTime(LocalTime.MAX));
            log.info("[Date Parse] 어제: {}", result);
            return result;
        }
        // 주/달은 "달력 기준"으로 계산한다.
        // - 이번주: 오늘이 속한 주의 월~일 (월요일 시작)
        // - 지난주/저번주: 그 전 주의 월~일
        if (Pattern.compile("이번\\s*주").matcher(t).find()) {
            LocalDate start = today.minusDays(today.getDayOfWeek().getValue() - 1L); // Monday
            LocalDate end = start.plusDays(6);
            DateRange result = new DateRange(start.atStartOfDay(), end.atTime(LocalTime.MAX));
            log.info("[Date Parse] 이번주(월~일): {} ~ {}", start, end);
            return result;
        }
        if (Pattern.compile("(지난|저번)\\s*주").matcher(t).find()) {
            LocalDate thisWeekStart = today.minusDays(today.getDayOfWeek().getValue() - 1L); // Monday
            LocalDate start = thisWeekStart.minusWeeks(1);
            LocalDate end = start.plusDays(6);
            DateRange result = new DateRange(start.atStartOfDay(), end.atTime(LocalTime.MAX));
            log.info("[Date Parse] 지난주(월~일): {} ~ {}", start, end);
            return result;
        }

        // - 이번달: 현재 달의 1일~말일
        // - 지난달/저번달: 이전 달의 1일~말일
        if (Pattern.compile("이번\\s*달").matcher(t).find()) {
            LocalDate start = today.withDayOfMonth(1);
            LocalDate end = today.withDayOfMonth(today.lengthOfMonth());
            DateRange result = new DateRange(start.atStartOfDay(), end.atTime(LocalTime.MAX));
            log.info("[Date Parse] 이번달(1일~말일): {} ~ {}", start, end);
            return result;
        }
        if (Pattern.compile("(지난|저번)\\s*달").matcher(t).find()) {
            LocalDate d = today.minusMonths(1);
            LocalDate start = d.withDayOfMonth(1);
            LocalDate end = d.withDayOfMonth(d.lengthOfMonth());
            DateRange result = new DateRange(start.atStartOfDay(), end.atTime(LocalTime.MAX));
            log.info("[Date Parse] 지난달(1일~말일): {} ~ {}", start, end);
            return result;
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
                .replace("중에", "")
                .replace("중", "")
                .trim();
    }

    private NaturalFilter parseNaturalFilter(String text) {
        if (text == null) return new NaturalFilter(null, null, "", false, false);
        String t = text.trim();

        // 0) "보낸/받은" 필터링 감지 (제거 전에 먼저 체크)
        boolean senderOnly = t.contains("보낸") || t.contains("전송한") || t.contains("전달한");
        boolean receiverOnly = t.contains("받은") || t.contains("수신한");
        // "주고받은"은 둘 다 false (전체 조회)

        // 1) 상대 식별 (이메일 > 닉네임 부분문자열 매칭)
        String counterEmail = null;

        // 1-1) 이메일이 있으면 최우선
        Matcher m = EMAIL_PATTERN.matcher(t);
        if (m.find()) {
            counterEmail = m.group();
            t = t.replace(counterEmail, " ").trim();
        } else {
            // 1-2) 닉네임을 "문장 안에서" 직접 찾는다 (띄어쓰기/님/씨/조사 여부 무관)
            String matchedNickname = findNicknameSubstring(t);
            if (matchedNickname != null) {
                Optional<Member> found = memberRepository.findByNickname(matchedNickname);
                if (found.isPresent()) {
                    counterEmail = found.get().getEmail();
                    // 닉네임 주변의 호칭/조사까지 같이 제거해서 keyword 오염 방지
                    t = stripNicknameToken(t, matchedNickname).trim();
                    log.info("[Nickname Parse] 부분문자열 매칭 성공: {} -> {}", matchedNickname, counterEmail);
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

        // 4) 키워드 정리: 불용어 제거 + 조사/호칭 잔여물 제거
        t = KEYWORD_NOISE_PATTERN.matcher(t).replaceAll(" ");
        t = t.replaceAll("(님|씨)(이랑|랑|과|와|한테|에게)?", " "); // "님이랑" 같은 잔여물
        t = t.replaceAll("\\s+", " ").trim();

        // 5) 남은 텍스트는 키워드
        String keyword = t;
        return new NaturalFilter(counterEmail, dept, keyword, senderOnly, receiverOnly);
    }

    /**
     * 입력 문자열에서 DB에 존재하는 닉네임(활성/승인)을 부분문자열로 찾아 반환.
     * - 가장 긴 닉네임 우선(예: "안은지" vs "은지" 같은 충돌 방지)
     * - 조사/호칭이 붙어있어도(안은지님이랑/안은지씨/안은지랑) "안은지" 자체가 포함되면 매칭됨
     */
    private String findNicknameSubstring(String text) {
        if (text == null) return null;
        String t = text.trim();
        if (t.isEmpty()) return null;

        List<String> nicknames = getActiveNicknamesCached();
        for (String nick : nicknames) {
            if (nick == null) continue;
            String n = nick.trim();
            if (n.length() < 2) continue;
            if (t.contains(n)) {
                return n;
            }
        }
        return null;
    }

    private List<String> getActiveNicknamesCached() {
        long now = System.currentTimeMillis();
        List<String> cached = activeNicknamesCache;
        if (!cached.isEmpty() && (now - activeNicknamesCacheAtMs) < NICKNAME_CACHE_TTL_MS) {
            return cached;
        }

        synchronized (this) {
            long now2 = System.currentTimeMillis();
            List<String> cached2 = activeNicknamesCache;
            if (!cached2.isEmpty() && (now2 - activeNicknamesCacheAtMs) < NICKNAME_CACHE_TTL_MS) {
                return cached2;
            }
            try {
                List<String> loaded = memberRepository.findAllActiveNicknames();
                loaded.sort(Comparator.comparingInt(String::length).reversed()); // 가장 긴 닉네임 우선
                activeNicknamesCache = loaded;
                activeNicknamesCacheAtMs = now2;
                return loaded;
            } catch (Exception e) {
                log.warn("[Nickname Cache] 닉네임 캐시 로드 실패: {}", e.getMessage());
                // 실패 시 기존 캐시(있으면) 유지
                return activeNicknamesCache;
            }
        }
    }

    private Department parseDepartment(String text) {
        if (text == null) return null;
        String u = text.toUpperCase(Locale.ROOT);
        // ✅ 합성어(예: "배너디자인")에서 "디자인"을 부서로 오인하지 않기 위해
        // 부서는 "디자인팀/디자인 부서"처럼 접미(팀|부서)가 있을 때만 인식한다.
        if (Pattern.compile("(?i)\\bDESIGN\\b").matcher(u).find()) return Department.DESIGN;
        if (Pattern.compile("(?i)\\bDEVELOPMENT\\b").matcher(u).find()) return Department.DEVELOPMENT;
        if (Pattern.compile("(?i)\\bSALES\\b").matcher(u).find()) return Department.SALES;
        if (Pattern.compile("(?i)\\bHR\\b").matcher(u).find()) return Department.HR;
        if (Pattern.compile("(?i)\\bFINANCE\\b").matcher(u).find()) return Department.FINANCE;
        if (Pattern.compile("(?i)\\bPLANNING\\b").matcher(u).find()) return Department.PLANNING;

        if (Pattern.compile("디자인\\s*(팀|부서)").matcher(text).find()) return Department.DESIGN;
        if (Pattern.compile("개발\\s*(팀|부서)").matcher(text).find()) return Department.DEVELOPMENT;
        if (Pattern.compile("영업\\s*(팀|부서)").matcher(text).find()) return Department.SALES;
        if (Pattern.compile("인사\\s*(팀|부서)").matcher(text).find()) return Department.HR;
        if (Pattern.compile("재무\\s*(팀|부서)").matcher(text).find()) return Department.FINANCE;
        if (Pattern.compile("기획\\s*(팀|부서)").matcher(text).find()) return Department.PLANNING;
        return null;
    }

    private String removeDepartmentToken(String text) {
        if (text == null) return "";
        String t = text;

        // 영문 부서 토큰
        t = t.replaceAll("(?i)\\bDESIGN\\b", " ")
                .replaceAll("(?i)\\bDEVELOPMENT\\b", " ")
                .replaceAll("(?i)\\bSALES\\b", " ")
                .replaceAll("(?i)\\bHR\\b", " ")
                .replaceAll("(?i)\\bFINANCE\\b", " ")
                .replaceAll("(?i)\\bPLANNING\\b", " ");

        // 한글 부서 토큰 + 조사까지 같이 제거 (ex: "디자인팀이랑", "개발팀과")
        // ✅ 접미(팀/부서)가 있을 때만 제거 (합성어 오인 제거 방지)
        t = t.replaceAll("디자인(\\s*(팀|부서))(이랑|랑|과|와)?", " ")
                .replaceAll("개발(\\s*(팀|부서))(이랑|랑|과|와)?", " ")
                .replaceAll("영업(\\s*(팀|부서))(이랑|랑|과|와)?", " ")
                .replaceAll("인사(\\s*(팀|부서))(이랑|랑|과|와)?", " ")
                .replaceAll("재무(\\s*(팀|부서))(이랑|랑|과|와)?", " ")
                .replaceAll("기획(\\s*(팀|부서))(이랑|랑|과|와)?", " ");

        return t.replaceAll("\\s+", " ").trim();
    }

    private String stripNicknameToken(String text, String nickname) {
        if (text == null || nickname == null || nickname.isBlank()) return text == null ? "" : text;
        String n = Pattern.quote(nickname.trim());
        // 닉네임 + 호칭/조사 패턴을 먼저 제거 (예: "안은지님이랑")
        String t = text.replaceAll(n + "(님|씨)?(이랑|랑|과|와|한테|에게)?", " ");
        // 그래도 남으면 닉네임 자체 제거
        t = t.replaceAll(n, " ");
        return t.replaceAll("\\s+", " ");
    }

    /**
     * Komoran 형태소 분석기를 사용해 명사 + 형용사 어간 추출.
     * - "신제품관련해서" → ["신제품", "관련"]
     * - "배너디자인" → ["배너", "디자인"]
     * - "귀여운거" → ["귀여운", "귀여"] (형용사 어간 + 앞부분)
     * - 자연어 검색을 위해 부분 매칭용 토큰도 생성
     */
    private List<String> extractKeywordTokens(String keyword) {
        if (keyword == null) return List.of();
        String t = keyword.trim();
        if (t.isEmpty()) return List.of();

        LinkedHashSet<String> allTokens = new LinkedHashSet<>();

        // Komoran 사용 가능하면 형태소 분석
        if (komoran != null) {
            try {
                List<Token> tokens = komoran.analyze(t).getTokenList();
                
                for (Token token : tokens) {
                    String pos = token.getPos(); // 품사 태그
                    String morph = token.getMorph(); // 형태소
                    
                    // 명사 추출 (NNG: 일반명사, NNP: 고유명사, SL: 외국어)
                    if ((pos.startsWith("NN") && !pos.equals("NNB")) || pos.equals("SL")) {
                        if (!morph.equals("팀") && !morph.equals("부서") && !morph.equals("부") 
                                && morph.length() >= 2) {
                            allTokens.add(morph);
                        }
                    }
                    // 형용사/동사 어간도 추출 (VA: 형용사, VV: 동사)
                    // "귀엽" → "귀여운짤"에서 "귀여"로 매칭 가능
                    if (pos.equals("VA") || pos.equals("VV")) {
                        if (morph.length() >= 2) {
                            allTokens.add(morph);
                        }
                    }
                    // 숫자(SN)도 포함 (날짜 등)
                    if (pos.equals("SN") && morph.length() >= 1) {
                        allTokens.add(morph);
                    }
                }
                
                if (!allTokens.isEmpty()) {
                    log.info("[Komoran] 토큰 추출: {} → {}", t, allTokens);
                }
            } catch (Exception e) {
                log.warn("[Komoran] 분석 실패: {}", e.getMessage());
            }
        }

        // ✅ 원본 키워드에서 한글 부분 추출 (Komoran 결과와 병합)
        // "귀여운거" → "귀여운", "귀여" 도 추가 (부분 매칭용)
        String cleaned = KEYWORD_NOISE_PATTERN.matcher(t).replaceAll(" ");
        cleaned = cleaned.replaceAll("(님|씨)(이랑|랑|과|와|한테|에게)?", " ");
        cleaned = cleaned.replaceAll("[^\\p{L}\\p{N}\\s._-]", " ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();

        if (!cleaned.isEmpty()) {
            String[] parts = cleaned.split("\\s+");
            for (String p : parts) {
                String s = p.trim();
                if (s.isEmpty() || s.length() < 2) continue;
                
                allTokens.add(s); // 원본 토큰
                
                // ✅ 한글 3글자 이상이면 앞부분도 추가 (부분 매칭용)
                // "귀여운거" → "귀여운", "귀여" 추가
                if (s.length() >= 3 && containsHangul(s)) {
                    // 앞에서 3글자
                    String prefix3 = s.substring(0, 3);
                    if (prefix3.length() >= 2) allTokens.add(prefix3);
                    
                    // 앞에서 2글자
                    String prefix2 = s.substring(0, 2);
                    if (prefix2.length() >= 2) allTokens.add(prefix2);
                }
                
                // ✅ 한글 4글자 이상이면 앞 4글자도 추가
                if (s.length() >= 4 && containsHangul(s)) {
                    String prefix4 = s.substring(0, 4);
                    allTokens.add(prefix4);
                }
            }
        }

        if (allTokens.isEmpty()) return List.of();

        List<String> result = new ArrayList<>(allTokens);
        
        // ✅ 1글자 토큰 제거 (노이즈 방지)
        result.removeIf(token -> token.length() < 2);
        
        // ✅ 긴 토큰(3글자 이상)이 있으면 2글자 토큰 제거 (우선순위 조정)
        // 예: "고양이짤" → "고양이"(3글자) + "짤"(2글자) → "고양이"만 사용
        boolean hasLongToken = result.stream().anyMatch(token -> token.length() >= 3);
        if (hasLongToken) {
            result.removeIf(token -> token.length() <= 2);
        }
        
        // 긴 토큰 우선 (정확한 매칭 우선, 부분 매칭은 후순위)
        result.sort((a, b) -> Integer.compare(b.length(), a.length()));
        
        log.info("[KeywordTokens] 최종: {} → {}", t, result);
        return result;
    }

    private boolean matchesAllTokens(TicketFile f, List<String> tokens) {
        if (f == null || tokens == null || tokens.isEmpty()) return true;
        StringBuilder sb = new StringBuilder();
        sb.append(f.getFileName() == null ? "" : f.getFileName()).append(" ");
        sb.append(f.getWriter() == null ? "" : f.getWriter()).append(" ");
        sb.append(f.getReceiver() == null ? "" : f.getReceiver()).append(" ");

        // 티켓 문맥: 제목/본문 + 작성자/수신자 닉네임도 포함(Repository 쿼리 스코프와 일치)
        if (f.getTicket() != null) {
            if (f.getTicket().getTitle() != null) sb.append(f.getTicket().getTitle()).append(" ");
            if (f.getTicket().getContent() != null) sb.append(f.getTicket().getContent()).append(" ");
            // ✅ 티켓함 '요약'에 포함되는 텍스트(목적/요구사항)도 검색 스코프에 포함
            if (f.getTicket().getPurpose() != null) sb.append(f.getTicket().getPurpose()).append(" ");
            if (f.getTicket().getRequirement() != null) sb.append(f.getTicket().getRequirement()).append(" ");
            if (f.getTicket().getWriter() != null) {
                if (f.getTicket().getWriter().getEmail() != null) sb.append(f.getTicket().getWriter().getEmail()).append(" ");
                if (f.getTicket().getWriter().getNickname() != null) sb.append(f.getTicket().getWriter().getNickname()).append(" ");
            }
            try {
                if (f.getTicket().getPersonalList() != null) {
                    f.getTicket().getPersonalList().forEach(tp -> {
                        if (tp == null || tp.getReceiver() == null) return;
                        if (tp.getReceiver().getEmail() != null) sb.append(tp.getReceiver().getEmail()).append(" ");
                        if (tp.getReceiver().getNickname() != null) sb.append(tp.getReceiver().getNickname()).append(" ");
                    });
                }
            } catch (Exception ignore) {
                // Lazy/프록시 환경에서 예외가 나더라도 토큰 매칭 전체가 깨지지 않도록 방어
            }
        }

        String haystack = sb.toString().toLowerCase(Locale.ROOT);

        for (String t : tokens) {
            if (t == null || t.isBlank()) continue;
            if (!haystack.contains(t.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesAllTokensChat(ChatFile f, List<String> tokens, Map<String, String> emailToNickname) {
        if (f == null || tokens == null || tokens.isEmpty()) return true;
        StringBuilder sb = new StringBuilder();
        sb.append(f.getFileName() == null ? "" : f.getFileName()).append(" ");
        sb.append(f.getWriter() == null ? "" : f.getWriter()).append(" ");
        sb.append(f.getReceiver() == null ? "" : f.getReceiver()).append(" ");
        if (f.getChatRoom() != null && f.getChatRoom().getName() != null) {
            sb.append(f.getChatRoom().getName()).append(" ");
        }

        // 업로더 닉네임 포함 (Repository의 AI 검색 스코프와 일치)
        final String writerEmail = f.getWriter();
        if (writerEmail != null && !writerEmail.isBlank()) {
            String nick = emailToNickname.get(writerEmail);
            if (nick == null) {
                try {
                    nick = memberRepository.findById(writerEmail).map(Member::getNickname).orElse(null);
                } catch (Exception ignore) {
                    nick = null;
                }
                if (nick != null) emailToNickname.put(writerEmail, nick);
            }
            if (nick != null) sb.append(nick).append(" ");
        }

        String haystack = sb.toString().toLowerCase(Locale.ROOT);

        for (String t : tokens) {
            if (t == null || t.isBlank()) continue;
            if (!haystack.contains(t.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return true;
    }

    private boolean containsHangul(String s) {
        if (s == null) return false;
        for (char c : s.toCharArray()) {
            if (c >= 0xAC00 && c <= 0xD7A3) return true;
        }
        return false;
    }
}


