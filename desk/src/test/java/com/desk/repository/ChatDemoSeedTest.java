package com.desk.repository;

import com.desk.domain.*;
import com.desk.repository.chat.ChatMessageRepository;
import com.desk.repository.chat.ChatParticipantRepository;
import com.desk.repository.chat.ChatRoomRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootTest
@Log4j2
@Disabled("팀 공용 DB에 채팅/멤버 더미 데이터가 들어가는 것을 방지하기 위해 기본 비활성화. 필요 시 주석 해제 후 실행하세요.")
public class ChatDemoSeedTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatParticipantRepository chatParticipantRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // 멤버 정보
    private static final Map<String, Department> MEMBER_DATA = Map.of(
            "김도현", Department.DEVELOPMENT,
            "박지민", Department.PLANNING,
            "이서연", Department.DESIGN,
            "정우석", Department.SALES,
            "최민지", Department.HR,
            "한승호", Department.FINANCE
    );

    // =========================
    // 1) 멤버 생성
    // =========================
    @Test
    @Transactional
    @Commit
    public void testCreateMembersOnly() {
        log.info("=== [TEST] 멤버 생성만 시작 ===");
        Map<String, Member> members = createMembers();
        log.info("=== [TEST] 멤버 생성 완료: {}명 ===", members.size());
    }

    // =========================
    // 2) 그룹 채팅방 더미데이터 200개 생성
    //    - 그룹 방에만 생깁니다.
    //    - createdAt: 2024-01-01 ~ 2024-01-03
    // =========================
    @Test
    @Transactional
    @Commit
    public void testGenerate200DummyMessages_GroupOnly_Jan1ToJan3_2024() {
        log.info("=== [TEST] 그룹 채팅방 더미 텍스트 200개 생성 (2024-01-01 ~ 2024-01-03) 시작 ===");

        Map<String, Member> members = createMembers();

        // 그룹 채팅방만 확보(없으면 생성). 참가자도 보장.
        ChatRoom roomA = getOrCreateGroupRoom(
                "2월 신규 기능 런칭 논의",
                Arrays.asList("김도현", "박지민", "이서연", "정우석"),
                members
        );
        ChatRoom roomB = getOrCreateGroupRoom(
                "인사·재무 협의",
                Arrays.asList("최민지", "한승호"),
                members
        );

        // 날짜 범위 고정
        LocalDateTime startA = LocalDateTime.of(2024, 1, 1, 9, 0);
        LocalDateTime endA   = LocalDateTime.of(2024, 1, 3, 18, 0);

        LocalDateTime startB = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime endB   = LocalDateTime.of(2024, 1, 3, 17, 30);

        // 총 200개: A 130개, B 70개 (원하면 비율만 바꾸면 됨)
        generateBulkDummyMessagesInRange(
                roomA,
                members,
                Arrays.asList("김도현", "박지민", "이서연", "정우석"),
                130,
                startA,
                endA,
                42L
        );

        generateBulkDummyMessagesInRange(
                roomB,
                members,
                Arrays.asList("최민지", "한승호"),
                70,
                startB,
                endB,
                84L
        );

        log.info("=== [TEST] 그룹 채팅방 더미 텍스트 200개 생성 완료 ===");
    }

    // =========================
    // 3) 대본(본문 대사) 생성
    // =========================
    @Test
    @Transactional
    @Commit
    public void testInsertScriptOnly() {
        log.info("=== [TEST] 대본(본문) 메시지 생성만 시작 ===");

        Map<String, Member> members = createMembers();

        // A/B/C 방 확보(없으면 생성). 참가자도 보장.
        ChatRoom roomA = getOrCreateGroupRoom(
                "2월 신규 기능 런칭 논의",
                Arrays.asList("김도현", "박지민", "이서연", "정우석"),
                members
        );

        ChatRoom roomB = getOrCreateGroupRoom(
                "인사·재무 협의",
                Arrays.asList("최민지", "한승호"),
                members
        );

        ChatRoom roomC = getOrCreateDirectRoom(
                "박지민", "정우석",
                members
        );

        // 본문 메시지 (최근 메시지)
        List<String> scriptA = Arrays.asList(
                "박지민", "이번 주 금요일에 신규 기능 데모 가능할까요? 영업팀 쪽에서 클라이언트 미팅 잡혀 있어서요.",
                "김도현", "핵심 기능은 거의 완료됐고, 에러 처리만 오늘 중으로 보면 될 것 같아요.",
                "이서연", "UI 쪽은 어제 전달드린 시안 기준으로 적용 중이죠? 모바일에서 버튼 여백만 한번 더 봐주시면 좋을 것 같아요.",
                "김도현", "네, 모바일 쪽은 지금 수정 중입니다. 오늘 저녁에 한번 빌드 올릴게요.",
                "정우석", "그럼 데모 시연은 금요일 오전 기준으로 준비하면 될까요? 클라이언트가 실사용 화면을 보고 싶어해서요.",
                "박지민", "네, 실제 데이터 대신 더미데이터로 보여주면 될 것 같아요.",
                "김도현", "팀별 사용자 시나리오도 같이 만들어둘게요."
        );

        List<String> scriptB = Arrays.asList(
                "최민지", "이번 분기 인원 충원 계획 관련해서 예산 확인 한번 부탁드릴게요.",
                "한승호", "개발 1명, 디자인 1명 기준으로는 가능할 것 같습니다. 다만 연봉 테이블은 기존 기준 유지해야 할 것 같아요.",
                "최민지", "그럼 채용 공고는 기존 레벨 기준으로 올릴게요."
        );

        List<String> scriptC = Arrays.asList(
                "정우석", "클라이언트가 실제 직원들이 쓰는 화면 같으면 좋겠다고 하네요.",
                "박지민", "그럼 더미데이터도 부서별로 다르게 구성해둘게요."
        );

        // 대본은 "오늘"로 삽입 (원하면 2024-01-03 끝으로 맞춰줄 수도 있음)
        createMessages(roomA, scriptA, members, 0);
        createMessages(roomB, scriptB, members, 0);
        createMessages(roomC, scriptC, members, 0);

        log.info("=== [TEST] 대본(본문) 메시지 생성 완료 ===");
    }

    // ==========================================================
    // 공통: 멤버 생성
    // ==========================================================
    private Map<String, Member> createMembers() {
        Map<String, Member> memberMap = new HashMap<>();

        for (Map.Entry<String, Department> entry : MEMBER_DATA.entrySet()) {
            String nickname = entry.getKey();
            Department department = entry.getValue();
            String email = generateEmail(nickname);

            // 이미 존재하면 스킵
            if (memberRepository.existsById(email)) {
                memberMap.put(nickname, memberRepository.findById(email).orElseThrow());
                continue;
            }

            Member member = Member.builder()
                    .email(email)
                    .pw(passwordEncoder.encode("1111"))
                    .nickname(nickname)
                    .social(false)
                    .department(department)
                    .isApproved(true)
                    .isDeleted(false)
                    .build();
            member.addRole(MemberRole.USER);

            memberRepository.save(member);
            memberMap.put(nickname, member);
        }

        return memberMap;
    }

    private String generateEmail(String nickname) {
        Map<String, String> emailMap = Map.of(
                "김도현", "kimdohyun@desk.com",
                "박지민", "parkjimin@desk.com",
                "이서연", "leeseoyeon@desk.com",
                "정우석", "jungwooseok@desk.com",
                "최민지", "choiminji@desk.com",
                "한승호", "hanseungho@desk.com"
        );
        String email = emailMap.get(nickname);
        if (email == null) throw new IllegalArgumentException("알 수 없는 닉네임: " + nickname);
        return email;
    }

    // ==========================================================
    // 공통: 채팅방 확보 + 참가자 보장
    // ==========================================================
    private ChatRoom getOrCreateGroupRoom(String roomName, List<String> participantNicknames, Map<String, Member> members) {
        List<ChatRoom> existingRooms = chatRoomRepository.findAll().stream()
                .filter(room -> ChatRoomType.GROUP.equals(room.getRoomType()) && roomName.equals(room.getName()))
                .collect(Collectors.toList());

        ChatRoom chatRoom = existingRooms.isEmpty()
                ? chatRoomRepository.save(ChatRoom.builder().roomType(ChatRoomType.GROUP).name(roomName).build())
                : existingRooms.get(0);

        ensureParticipants(chatRoom, participantNicknames, members);
        return chatRoom;
    }

    private ChatRoom getOrCreateDirectRoom(String nick1, String nick2, Map<String, Member> members) {
        Member m1 = members.get(nick1);
        Member m2 = members.get(nick2);
        if (m1 == null || m2 == null) throw new IllegalStateException("필수 멤버 누락");

        String pairKey = generatePairKey(m1.getEmail(), m2.getEmail());

        ChatRoom chatRoom = chatRoomRepository.findByRoomTypeAndPairKey(ChatRoomType.DIRECT, pairKey)
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.builder().roomType(ChatRoomType.DIRECT).pairKey(pairKey).build()));

        ensureParticipants(chatRoom, Arrays.asList(nick1, nick2), members);
        return chatRoom;
    }

    private void ensureParticipants(ChatRoom chatRoom, List<String> participantNicknames, Map<String, Member> members) {
        for (String nickname : participantNicknames) {
            Member member = members.get(nickname);
            if (member == null) continue;

            if (chatParticipantRepository.findByChatRoomIdAndUserId(chatRoom.getId(), member.getEmail()).isEmpty()) {
                chatParticipantRepository.save(ChatParticipant.builder()
                        .chatRoom(chatRoom)
                        .userId(member.getEmail())
                        .status(ChatStatus.ACTIVE)
                        .lastReadSeq(0L)
                        .build());
            }
        }
    }

    private String generatePairKey(String email1, String email2) {
        List<String> emails = Arrays.asList(email1, email2);
        Collections.sort(emails);
        return emails.get(0) + "_" + emails.get(1);
    }

    // ==========================================================
    // 2번용: 날짜 범위 내 더미 메시지 생성
    // ==========================================================
    private void generateBulkDummyMessagesInRange(
            ChatRoom room,
            Map<String, Member> members,
            List<String> nicknames,
            int count,
            LocalDateTime start,
            LocalDateTime end,
            long seed
    ) {
        if (count <= 0) return;
        if (end.isBefore(start)) throw new IllegalArgumentException("end가 start보다 빠릅니다.");

        Long maxSeq = chatMessageRepository.findMaxMessageSeqByChatRoomId(room.getId());
        if (maxSeq == null) maxSeq = 0L;
        long seq = maxSeq + 1;

        Random random = new Random(seed);

        long totalSeconds = Duration.between(start, end).getSeconds();
        if (totalSeconds <= 0) totalSeconds = 1;

        long step = Math.max(1, totalSeconds / count);

        for (int i = 0; i < count; i++) {
            String nickname = nicknames.get(random.nextInt(nicknames.size()));
            Member sender = members.get(nickname);
            if (sender == null) continue;

            String content = makeNaturalDummyText(room, nickname, i);

            long baseOffset = step * i;
            long jitter = (random.nextInt(21) - 10) * 60L; // -10~+10분
            long offsetSeconds = Math.min(Math.max(0, baseOffset + jitter), totalSeconds);

            LocalDateTime createdAt = start.plusSeconds(offsetSeconds);

            ChatMessage message = ChatMessage.builder()
                    .chatRoom(room)
                    .messageSeq(seq++)
                    .senderId(sender.getEmail())
                    .messageType(ChatMessageType.TEXT)
                    .content(content)
                    .createdAt(createdAt)
                    .build();

            chatMessageRepository.save(message);
        }

        updateRoomLastMessage(room);
    }

    private String makeNaturalDummyText(ChatRoom room, String nickname, int i) {
        String roomName = room.getName() != null ? room.getName() : "";

        if (roomName.contains("런칭")) {
            String[] pool = {
                    "오늘 빌드 올렸습니다. 확인 부탁드려요.",
                    "QA에서 이슈 하나 더 나왔는데 우선순위 어떻게 볼까요?",
                    "데모 시나리오 초안 공유드립니다.",
                    "모바일에서 버튼 간격만 다시 확인해보겠습니다.",
                    "클라이언트 질문 예상 리스트 정리해둘게요.",
                    "에러 로그 재현됐고 수정 중입니다.",
                    "테스트 계정/더미데이터는 오늘 안에 준비 가능해요.",
                    "배포 전 체크리스트 업데이트했습니다.",
                    "로그인 흐름에서 간헐적으로 튕김이 있어 원인 보고 있습니다.",
                    "디자인 수정사항 반영해서 다시 빌드 올리겠습니다."
            };
            return pool[i % pool.length];
        }

        if (roomName.contains("재무") || roomName.contains("인사")) {
            String[] pool = {
                    "채용 예산 산정 근거 파일 공유드립니다.",
                    "연봉 테이블 기준 유지로 정리하겠습니다.",
                    "결재 라인 확인 후 회신드릴게요.",
                    "입사일/온보딩 일정도 같이 맞춰볼까요?",
                    "채용 공고 문구 초안 검토 부탁드립니다.",
                    "채용 비용 항목(플랫폼/면접비 등)도 포함해서 계산하겠습니다.",
                    "이번 주 내로 예산 확정 가능할 것 같습니다.",
                    "결재 서류 양식 최신본으로 업데이트했습니다."
            };
            return pool[i % pool.length];
        }

        // fallback
        return "확인했습니다. 관련 내용 정리해서 공유드릴게요.";
    }

    // ==========================================================
    // 3번용: 대본 삽입 (기존 네 방식 유지)
    // ==========================================================
    private void createMessages(ChatRoom chatRoom, List<String> messageList, Map<String, Member> members, int daysAgo) {
        Long maxSeq = chatMessageRepository.findMaxMessageSeqByChatRoomId(chatRoom.getId());
        if (maxSeq == null) maxSeq = 0L;

        LocalDateTime baseTime = LocalDateTime.now().minusDays(daysAgo);
        long messageSeq = maxSeq + 1;

        // 메시지 리스트는 [닉네임, 내용, 닉네임, 내용, ...] 형태
        for (int i = 0; i < messageList.size(); i += 2) {
            if (i + 1 >= messageList.size()) break;

            String nickname = messageList.get(i);
            String content = messageList.get(i + 1);
            Member sender = members.get(nickname);

            if (sender == null) continue;

            int minutesOffset = (i / 2) * 15; // 15분씩 증가
            LocalDateTime createdAt = baseTime.plusMinutes(minutesOffset);

            ChatMessage message = ChatMessage.builder()
                    .chatRoom(chatRoom)
                    .messageSeq(messageSeq++)
                    .senderId(sender.getEmail())
                    .messageType(ChatMessageType.TEXT)
                    .content(content)
                    .createdAt(createdAt)
                    .build();

            chatMessageRepository.save(message);
        }

        updateRoomLastMessage(chatRoom);
    }

    private void updateRoomLastMessage(ChatRoom chatRoom) {
        Long finalMaxSeq = chatMessageRepository.findMaxMessageSeqByChatRoomId(chatRoom.getId());
        Optional<ChatMessage> lastMessage =
                chatMessageRepository.findFirstByChatRoomIdOrderByMessageSeqDesc(chatRoom.getId());

        if (lastMessage.isPresent()) {
            ChatMessage last = lastMessage.get();
            chatRoom.updateLastMessage(finalMaxSeq, last.getContent());
            chatRoomRepository.save(chatRoom);
        }
    }
}
