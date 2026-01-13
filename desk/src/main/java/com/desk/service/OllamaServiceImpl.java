package com.desk.service;

import com.desk.config.OllamaConfig;
import com.desk.domain.Member;
import com.desk.dto.MeetingMinutesDTO;
import com.desk.repository.MemberRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

@Service
@RequiredArgsConstructor
@Log4j2
public class OllamaServiceImpl implements OllamaService {

    private final ObjectMapper objectMapper;
    private final OllamaConfig ollamaConfig;
    private final MemberRepository memberRepository; // 담당자

    // 정규화 규칙 (순서 중요: LinkedHashMap) -> 가장 먼저 실행됨
    private final LinkedHashMap<Pattern, String> normalizeRules = new LinkedHashMap<>();
    // ✅ 불용어 목록 정의 (회의 중 자주 나오는 쓸데없는 말들)
    private List<String> stopWords = new ArrayList<>();
    // 불용어 리스트
    private final Set<String> stopWordSet = new HashSet<>();
    private final List<Pattern> stopRegexList = new ArrayList<>();

    // 하드코딩된 필수 패턴
    private Pattern leadingFillerPattern;  // 문장 시작 말잇기
    private Pattern trailingEndingPattern; // 문장 끝 어미 압축
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\d{1,2}:\\d{2}");

    @PostConstruct
    public void initStopWords() {

        log.info("정규화 규칙 로드, 불용어 로드, 필수 패턴 컴파일");

        // [1단계] 정규화 규칙 로드 (normalize-rules.txt)
        loadNormalizeRules();

        // [2단계] 불용어 로드 (stopwords.txt)
        loadStopWords();

        // [3단계] 필수 패턴 컴파일
        initEssentialPatterns();

        log.info("Loading stopwords from stopwords.txt...");

        // ClassPathResource는 src/main/resources 폴더를 가리킵니다.
        ClassPathResource resource = new ClassPathResource("stopwords.txt");

        // try-with-resources 구문을 사용하여 자동으로 스트림을 닫아줍니다.
        // exists() 체크 없이 바로 getInputStream()을 호출하는 것이 JAR 배포 시 안전합니다.
        try (InputStream inputStream = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String word = line.trim();
                // 빈 줄이 아니면 리스트에 추가
                if (!word.isEmpty()) {
                    stopWords.add(word);
                }
            }
            log.info("성공적으로 불용어 {}개를 로드했습니다.", stopWords.size());

        } catch (IOException e) {
            // 파일이 없거나 읽을 수 없을 때 발생하는 예외 처리
            log.warn("stopwords.txt 파일을 찾을 수 없거나 읽는 도중 오류가 발생했습니다. 불용어 필터링 없이 진행합니다.");
            // e.printStackTrace(); // 상세 에러가 보고 싶으면 주석 해제
        }
    }

    private void loadNormalizeRules() {
        normalizeRules.clear();
        String filename = "normalize-rules.txt"; // resources 루트 경로
        ClassPathResource resource = new ClassPathResource(filename);

        try (InputStream is = resource.getInputStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                String s = line.trim();
                // 주석(#)이나 빈 줄 건너뛰기
                if (s.isEmpty() || s.startsWith("#")) continue;

                // "패턴 => 변경값" 형식 파싱
                int sep = s.indexOf("=>");
                if (sep < 0) continue;

                String regex = s.substring(0, sep).trim();
                String repl = s.substring(sep + 2).trim();

                if (!regex.isEmpty()) {
                    // 대소문자 구분 없이 매칭 (UNICODE_CASE)
                    normalizeRules.put(Pattern.compile(regex, Pattern.UNICODE_CASE), repl);
                }
            }
            log.info("[초기화] {} 로드 완료: 규칙 {}개 적용 예정", filename, normalizeRules.size());

        } catch (IOException e) {
            log.warn("{} 파일을 찾을 수 없습니다. (정규화 단계 건너뜀)", filename);
        }
    }
    private void loadStopWords() {
        stopWordSet.clear();
        stopRegexList.clear();
        String filename = "stopwords.txt"; // resources 루트 경로
        ClassPathResource resource = new ClassPathResource(filename);

        try (InputStream inputStream = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String raw = line.trim();
                if (raw.isEmpty() || raw.startsWith("#")) continue;

                if (raw.startsWith("re:")) {
                    stopRegexList.add(Pattern.compile(raw.substring(3).trim()));
                } else {
                    stopWordSet.add(raw);
                }
            }
            log.info("[초기화] {} 로드 완료: 단어 {}개 / 정규식 {}개", filename, stopWordSet.size(), stopRegexList.size());

        } catch (IOException e) {
            log.warn("{} 파일을 찾을 수 없습니다. (기본 불용어 처리만 수행)", filename);
        }
    }
    private void initEssentialPatterns() {
        leadingFillerPattern = Pattern.compile(
                "^(?:아+|어+|음+|그+|저+|이제|일단|그러니까|그래서|근데|아무튼|어쨌든|하여튼|사실|약간|좀|뭐랄까|혹시)\\s*",
                Pattern.UNICODE_CASE
        );
        trailingEndingPattern = Pattern.compile(
                "(?:\\s*(?:거든요|잖아요|인데요|네요|죠|요|합니다|됩니다|했어요|할게요))\\s*(?=[.?!]|$)",
                Pattern.UNICODE_CASE
        );
    }

    // ==========================================================
    // 3. 텍스트 처리 파이프라인 (정규화 -> 불용어 제거)
    // ==========================================================
    private String processTextPipeline(String text) {
        if (text == null || text.isBlank()) return "";

        String processing = text;

        // [STEP 1] 정규화 규칙 적용 (가장 먼저 실행!)
        // normalize-rules.txt에 정의된 규칙대로 텍스트를 먼저 변환합니다.
        // 예: "그렇죠" -> "." (불필요한 서술어를 기호로 압축)
        if (!normalizeRules.isEmpty()) {
            for (Map.Entry<Pattern, String> entry : normalizeRules.entrySet()) {
                processing = entry.getKey().matcher(processing).replaceAll(entry.getValue());
            }
        }

        // [STEP 2] 불용어 제거 및 라인 최적화
        // 정규화된 텍스트를 바탕으로 나머지 잡음을 제거합니다.
        return removeStopWordsAndOptimize(processing);
    }

    private String removeStopWordsAndOptimize(String text) {
        String[] lines = text.replace("\r\n", "\n").split("\n");
        StringBuilder sb = new StringBuilder(text.length());

        List<String> sortedWords = new ArrayList<>(stopWordSet);
        sortedWords.sort((a, b) -> Integer.compare(b.length(), a.length()));

        for (String line : lines) {
            String s = line.trim();
            if (s.isEmpty()) continue;

            // 타임스탬프 제거
            s = TIMESTAMP_PATTERN.matcher(s).replaceAll(" ");

            // 문장 시작 말잇기 제거
            while (true) {
                String next = leadingFillerPattern.matcher(s).replaceFirst("");
                if (next.equals(s)) break;
                s = next.trim();
            }

            // 불용어 파일 정규식 제거
            boolean dropLine = false;
            for (Pattern p : stopRegexList) {
                if (p.matcher(s).find() && p.pattern().startsWith("^") && p.pattern().endsWith("$")) {
                    dropLine = true;
                    break;
                }
                s = p.matcher(s).replaceAll(" ");
            }
            if (dropLine) continue;

            // 불용어 단어 제거
            for (String w : sortedWords) {
                if (w.isBlank()) continue;
                String escaped = Pattern.quote(w);
                s = s.replaceAll("(?u)(^|\\s)" + escaped + "(?=\\s|[,.!?]|$)", " ");
            }

            // 문장 끝 어미 압축
            s = trailingEndingPattern.matcher(s).replaceAll("");

            // 공백 정리
            s = s.replaceAll("\\s{2,}", " ").trim();

            // 짧은 무의미한 라인 삭제
            if (s.length() <= 5 && s.matches("(?u)^(네|예|응|맞아요|그렇죠|알겠|좋|확인|진행)$")) {
                continue;
            }

            if (!s.isEmpty()) {
                sb.append(s).append("\n");
            }
        }
        return sb.toString().trim();
    }


    // [수정] 파일과 텍스트를 받아서 AI에게 요청
    @Override
    public MeetingMinutesDTO getMeetingInfoFromAi(MultipartFile file, String title, String content, String purpose, String requirement) {

        // 1. 파일 내용 추출
        StringBuilder extractedText = new StringBuilder();

        // 1-1. 기존 입력 텍스트 추가
        if (content != null && !content.trim().isEmpty()) {
            extractedText.append("[사용자 입력 내용]:\n").append(content).append("\n\n");
        }

        // 1-2. 파일 텍스트 추출 및 추가
        if (file != null && !file.isEmpty()) {
            try {
                String fileContent = extractTextFromFile(file);
                extractedText.append("[첨부 파일 내용]:\n").append(fileContent).append("\n\n");
            } catch (Exception e) {
                log.error("파일 읽기 실패", e);
                // 파일 읽기 실패해도 멈추지 않고 진행
                extractedText.append("(파일 내용을 읽을 수 없습니다.)\n");
            }
        }

        String rawContent = extractedText.toString();

        if (rawContent.trim().isEmpty()) {
            throw new RuntimeException("분석할 내용이 없습니다.");
        }

        //파이프라인 호출 (정규화 -> 불용어)
        String cleanedText = processTextPipeline(rawContent);

        log.info("[원본 텍스트] (길이: {}) → [불용어 제거 후 텍스트] (길이: {})", rawContent.length(), cleanedText.length());
        log.info("==================================================");
        log.info("📄 [원본 텍스트] (길이: {}): \n{}", rawContent.length(), rawContent);
        log.info("--------------------------------------------------");
        log.info("🧹 [불용어 제거 후 텍스트] (길이: {}): \n{}", cleanedText.length(), cleanedText);
        log.info("==================================================");


        String url = ollamaConfig.getBaseUrl() + "/api/generate";

        // -----------------------------------------------------------
        // [프롬프트 수정] 티켓 필드(제목, 목적, 상세, 마감일) 매핑 강화
        // -----------------------------------------------------------
        String prompt = String.format(
                "당신은 전문 회의 기록관이자 프로젝트 매니저입니다. 입력된 자료를 분석하여 업무 티켓을 생성할 수 있도록 정리하세요.\n" +
                        "없는 내용은 '내용 없음'으로, 날짜가 없으면 비워두세요.\n\n" +
                        "입력된 텍스트가 구어체(말하기)라면, '음', '어', '그' 같은 불필요한 감탄사를 무시하고 핵심 내용 위주로 요약하세요.\n\n" +
                        "### 작성 지침 ###\n" +
                        "1. **title**: 업무 티켓의 제목으로 적합한 한 줄 (예: 'OOO 프로젝트 기획 회의 결과')\n" +
                        "2. **overview**: (목적) 이 업무를 왜 해야 하는지 배경과 목적 기술\n" +
                        "3. **details**: (상세) 구체적으로 수행해야 할 요구사항 나열\n" +
                        "4. **shortSummary**: (요약) 전체 내용을 3줄로 핵심 요약\n" +
                        "5. **attendees**: (담당자) 회의 참석자나 담당자 이름을 배열로 추출\n" +
                        "6. **deadline**: 본문에 마감 기한이나 날짜(YYYY-MM-DD)가 명시되어 있다면 추출, 없으면 빈 문자열(\"\")\n\n" +
                        "7. **conclusion**: (결론) 회의에서 도출된 최종 결론 및 향후 계획을 명확하게 기술\n\n" +
                        "### 출력 포맷 (JSON) ###\n" +
                        "{\n" +
                        "  \"title\": \"...\",\n" +
                        "  \"overview\": \"...\",\n" +
                        "  \"details\": \"...\",\n" +
                        "  \"shortSummary\": \"...\",\n" +
                        "  \"attendees\": [\"이름1\", \"이름2\"],\n" +
                        "  \"deadline\": \"YYYY-MM-DD\"\n" +
                        "}\n\n" +
                        "### 입력 데이터 ###\n" +
                        "제목: %s\n목적: %s\n요구사항: %s\n본문 및 파일내용:\n%s",
                title, purpose, requirement,
                cleanedText
        );

        // ... (이
        return callOllamaApi(url, prompt); // (중복 코드 줄이기 위해 아래 메서드로 분리함)
    }

    private String extractTextFromFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) return "";

        String lowerFilename = filename.toLowerCase();

        // [추가] 오디오 파일이 넘어왔을 경우 처리 (Java에서 직접 분석 불가하므로 안내 메시지 반환)
        if (lowerFilename.endsWith(".mp3") || lowerFilename.endsWith(".wav") ||
                lowerFilename.endsWith(".m4a") || lowerFilename.endsWith(".flac")) {
            log.warn("Audio file detected in Java backend: {}", filename);
            return "(오디오 파일이 첨부되었습니다. 오디오 내용은 텍스트 변환 기능을 통해 본문에 포함시켜 주세요.)";
        }

        // 1. 텍스트 파일 (.txt, .md, .log)
        if (lowerFilename.endsWith(".txt") || lowerFilename.endsWith(".md") || lowerFilename.endsWith(".log")) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        }

        // 2. PDF 파일 (.pdf) - iText 사용
        if (lowerFilename.endsWith(".pdf")) {
            try (PdfReader reader = new PdfReader(file.getInputStream());
                 PdfDocument pdfDoc = new PdfDocument(reader)) {
                StringBuilder text = new StringBuilder();
                for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
                    text.append(PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i))).append("\n");
                }
                return text.toString();
            }
        }

        return "지원하지 않는 파일 형식입니다 (텍스트 내용 추출 불가).";
    }

    // [헬퍼] AI 호출 공통 로직
    private MeetingMinutesDTO callOllamaApi(String url, String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ollamaConfig.getModelName());
        requestBody.put("prompt", prompt);
        requestBody.put("format", "json");
        requestBody.put("stream", false);
        Map<String, Object> options = new HashMap<>();
        options.put("num_ctx", 4096);
        requestBody.put("options", options);

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String apiKey = ollamaConfig.getApiKey();  // ollamaConfig에서 가져오기
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.set("x-api-key", apiKey);
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            String jsonStr = root.path("response").asText();
            MeetingMinutesDTO result = objectMapper.readValue(jsonStr, MeetingMinutesDTO.class);

            // [추가] attendees의 nickname을 email로 변환
            if (result.getAttendees() != null && !result.getAttendees().isEmpty()) {
                List<String> emailList = new ArrayList<>();
                for (String attendee : result.getAttendees()) {
                    if (attendee == null || attendee.trim().isEmpty()) continue;

                    // nickname으로 DB 조회
                    Optional<Member> foundMember = memberRepository.findByNickname(attendee.trim());
                    if (foundMember.isPresent()) {
                        String email = foundMember.get().getEmail();
                        emailList.add(email);
                        log.info("담당자 변환: {} -> {}", attendee, email);
                    } else {
                        // 찾지 못한 경우 로그만 남기고 제외
                        log.warn("담당자를 찾을 수 없음: {}", attendee);
                    }
                }
                result.setAttendees(emailList);
            }

            return result;

        } catch (Exception e) {
            log.error("AI 요청 실패", e);
            throw new RuntimeException("AI 처리 실패: " + e.getMessage());
        }
    }
    @Override
    public byte[] generatePdf(MeetingMinutesDTO summary) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);

            // A4 용지 설정
            Document document = new Document(pdf, PageSize.A4);
            document.setMargins(40, 40, 40, 40); // 여백 주기

            // -----------------------------------------------------------
            // 1. 한글 폰트 설정 (맑은 고딕)
            // -----------------------------------------------------------
            String FONT_PATH = "C:/Windows/Fonts/malgun.ttf";
            PdfFont koreanFont = PdfFontFactory.createFont(FONT_PATH, PdfEncodings.IDENTITY_H);
            document.setFont(koreanFont); // 문서 전체 기본 폰트 설정

            // -----------------------------------------------------------
            // 2. 문서 제목 ("회 의 록") - 가운데 정렬, 크게
            // -----------------------------------------------------------
            Paragraph title = new Paragraph("회 의 록")
                    .setFontSize(24)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(30);
            document.add(title);

            // -----------------------------------------------------------
            // 3. 표 만들기 (4칸짜리 그리드 시스템 사용)
            // -----------------------------------------------------------
            // 열 비율: [제목라벨(1) : 내용(1) : 날짜라벨(1) : 내용(1)]
            // 전체 너비 100% 사용
            float[] columnWidths = {1, 2, 1, 2};
            Table table = new Table(UnitValue.createPercentArray(columnWidths));
            table.setWidth(UnitValue.createPercentValue(100));

            // [1행] 회의 제목 (오른쪽 3칸 합치기)
            table.addCell(createHeaderCell("회의 제목"));
            table.addCell(createValueCell(summary.getTitle(), 1, 3)); // rowspan 1, colspan 3

            // [2행] 회의 날짜 | [값] | 마감 날짜 | [값]
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String deadline = (summary.getDeadline() != null)
//                    ? summary.getDeadline().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    ? LocalDate.now().plusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    : "-";

            table.addCell(createHeaderCell("회의 날짜"));
            table.addCell(createValueCell(today, 1, 1));
            table.addCell(createHeaderCell("마감 날짜"));
            table.addCell(createValueCell(deadline, 1, 1));

            // [3행] 참석자 (큰 박스, 4칸 합치기)
//            String attendees = (summary.getAttendees() != null) ? summary.getAttendees().toString() : "";
//            table.addCell(createBigCell("참석자:\n" + attendees, 60)); // 높이 60
            StringBuilder attendeesNames = new StringBuilder();

            if (summary.getAttendees() != null) {
                for (int i = 0; i < summary.getAttendees().size(); i++) {
                    String email = summary.getAttendees().get(i);

                    // DB에서 이메일(ID)로 멤버 조회 -> 닉네임 가져오기
                    // (memberRepository는 위쪽에서 주입받고 있어야 합니다)
                    String displayName = memberRepository.findById(email)
                            .map(com.desk.domain.Member::getNickname) // 찾으면 닉네임
                            .orElse(email); // 못 찾으면 그냥 이메일 표시 (외부인 등)

                    attendeesNames.append(displayName);

                    // 마지막 사람이 아니면 쉼표 추가
                    if (i < summary.getAttendees().size() - 1) {
                        attendeesNames.append(", ");
                    }
                }
            }

            // PDF 표에는 변환된 한글 이름들(attendeesNames)을 넣음
            table.addCell(createBigCell("참석자:\n" + attendeesNames.toString(), 60));
            //
            // [4행] 회의 개요 및 목적 (큰 박스)
            String overview = (summary.getOverview() != null) ? summary.getOverview() : "";
            table.addCell(createBigCell("회의 개요 및 목적:\n" + overview, 80)); // 높이 80

            // [5행] 상세 논의 사항 (가장 큰 박스)
            String details = (summary.getDetails() != null) ? summary.getDetails() : "";
            table.addCell(createBigCell("상세 논의 사항\n\n" + details, 250)); // 높이 250 (제일 크게)

            // [6행] 결론 및 향후 계획 (큰 박스)
            String conclusion = (summary.getConclusion() != null) ? summary.getConclusion() : "";
            table.addCell(createBigCell("결론 및 향후 계획\n\n" + conclusion, 100)); // 높이 100

            // 표를 문서에 추가
            document.add(table);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 1. 회색 배경의 헤더 칸 만들기 (선택 사항: 심플하게 흰색으로 하려면 setBackgroundColor 삭제)
    private Cell createHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold())
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(5);
    }

    // 2. 일반 값 칸 만들기 (Colspan 지원)
    private Cell createValueCell(String text, int rowSpan, int colSpan) {
        return new Cell(rowSpan, colSpan)
                .add(new Paragraph(text != null ? text : ""))
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(5);
    }

    // 3. 내용이 들어가는 큰 박스 만들기 (높이 지정 가능)
    private Cell createBigCell(String content, float minHeight) {
        return new Cell(1, 4) // 무조건 가로 4칸 차지
                .add(new Paragraph(content))
                .setMinHeight(minHeight) // 최소 높이 설정 (내용이 많으면 늘어남)
                .setPadding(10)
                .setVerticalAlignment(VerticalAlignment.TOP); // 글자는 위에서부터 시작
    }
}