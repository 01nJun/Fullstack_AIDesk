package com.desk.service.chat;

import com.desk.domain.*;
import com.desk.dto.chat.*;
import com.desk.repository.MemberRepository;
import com.desk.repository.chat.ChatParticipantRepository;
import com.desk.repository.chat.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final MemberRepository memberRepository;
    private final ChatMessageService chatMessageService;

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomDTO> getRooms(String userId) {
        // 1. 사용자가 참여 중인 활성 채팅방 목록 조회
        List<ChatRoom> rooms = chatRoomRepository.findActiveRoomsByUserId(userId);
        if (rooms.isEmpty()) return Collections.emptyList();

        // 2. 일괄 조회를 위해 채팅방 ID 목록 추출
        List<Long> roomIds = rooms.stream().map(ChatRoom::getId).collect(Collectors.toList());

        // 3. [개선] 모든 채팅방의 참여자들을 한 번의 쿼리로 조회 (N+1 방지)
        List<ChatParticipant> allParticipants = chatParticipantRepository.findByChatRoomIdIn(roomIds);

        // 4. [개선] 모든 참여자의 닉네임과 부서를 한 번의 쿼리로 조회 (N+1 방지)
        Set<String> allUserIds = allParticipants.stream().map(ChatParticipant::getUserId).collect(Collectors.toSet());
        List<Member> members = memberRepository.findAllById(allUserIds);
        Map<String, String> nicknameMap = members.stream()
                .collect(Collectors.toMap(m -> m.getEmail(), m -> m.getNickname(), (existing, replacement) -> existing));
        Map<String, String> departmentMap = members.stream()
                .collect(Collectors.toMap(m -> m.getEmail(), 
                    m -> m.getDepartment() != null ? m.getDepartment().name() : null, 
                    (existing, replacement) -> existing));

        // 5. 채팅방 ID별로 참여자 그룹화 (메모리 내 정렬)
        Map<Long, List<ChatParticipant>> participantsGroupByRoom = allParticipants.stream()
                .collect(Collectors.groupingBy(p -> p.getChatRoom().getId()));

        return rooms.stream().map(room -> {
            List<ChatParticipant> participants = participantsGroupByRoom.getOrDefault(room.getId(), Collections.emptyList());

            // 현재 로그인한 사용자의 참여 정보 찾기
            ChatParticipant myParticipant = participants.stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .findFirst().orElse(null);

            // 안 읽은 메시지 수 계산
            Long unreadCount = 0L;
            if (myParticipant != null && room.getLastMsgSeq() != null) {
                unreadCount = Math.max(0, room.getLastMsgSeq() - myParticipant.getLastReadSeq());
            }

            // [개선] 닉네임과 부서를 이미 조회된 Map에서 꺼내서 DTO 변환
            List<ChatParticipantDTO> participantDTOs = participants.stream()
                    .map(p -> toChatParticipantDTO(p, 
                        nicknameMap.getOrDefault(p.getUserId(), p.getUserId()),
                        departmentMap.getOrDefault(p.getUserId(), null)))
                    .collect(Collectors.toList());

            ChatRoomDTO dto = toChatRoomDTO(room, myParticipant, unreadCount);
            dto.setParticipants(participantDTOs);

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ChatRoomDTO getRoom(Long roomId, String userId) {
        // 1. 채팅방 정보 조회
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));

        // 2. 해당 방의 모든 참여자 목록을 한 번에 조회
        List<ChatParticipant> participants = chatParticipantRepository.findByChatRoomId(roomId);

        // 3. 현재 접속 유저(userId)가 참여자인지 리스트에서 확인 (별도 쿼리 없이 처리)
        ChatParticipant myParticipant = participants.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("User is not a participant of this room"));

        // 4. [핵심] 참여자들의 모든 닉네임과 부서를 한 번에 조회 (Bulk Fetch)
        Set<String> allUserIdsInRoom = participants.stream()
                .map(ChatParticipant::getUserId)
                .collect(Collectors.toSet());

        List<Member> members = memberRepository.findAllById(allUserIdsInRoom);
        Map<String, String> nicknameMap = members.stream()
                .collect(Collectors.toMap(m -> m.getEmail(), m -> m.getNickname(), (existing, replacement) -> existing));
        Map<String, String> departmentMap = members.stream()
                .collect(Collectors.toMap(m -> m.getEmail(), 
                    m -> m.getDepartment() != null ? m.getDepartment().name() : null, 
                    (existing, replacement) -> existing));

        // 5. 안 읽은 메시지 개수 계산
        Long unreadCount = 0L;
        if (room.getLastMsgSeq() != null) {
            unreadCount = Math.max(0, room.getLastMsgSeq() - myParticipant.getLastReadSeq());
        }

        // 6. DTO 변환 시 이미 조회한 Map에서 닉네임과 부서를 꺼내 전달
        List<ChatParticipantDTO> participantDTOs = participants.stream()
                .map(p -> toChatParticipantDTO(p, 
                    nicknameMap.getOrDefault(p.getUserId(), p.getUserId()),
                    departmentMap.getOrDefault(p.getUserId(), null)))
                .collect(Collectors.toList());

        // 7. 결과 조립 및 리턴
        ChatRoomDTO dto = toChatRoomDTO(room, myParticipant, unreadCount);
        dto.setParticipants(participantDTOs);

        return dto;
    }

    @Override
    public ChatRoomDTO createGroupRoom(ChatRoomCreateDTO createDTO, String creatorId) {
        // 그룹 채팅방 생성
        ChatRoom room = ChatRoom.builder()
                .roomType(ChatRoomType.GROUP)
                .name(createDTO.getName())
                .lastMsgSeq(0L)
                .build();

        room = chatRoomRepository.save(room);

        // 생성자 참여자 추가
        ChatParticipant creator = ChatParticipant.builder()
                .chatRoom(room)
                .userId(creatorId)
                .status(ChatStatus.ACTIVE)
                .lastReadSeq(0L)
                .build();
        chatParticipantRepository.save(creator);

        // 초대된 사용자들 참여자 추가
        Set<String> userIds = new HashSet<>(createDTO.getUserIds());
        userIds.add(creatorId); // 중복 방지

        for (String userId : userIds) {
            if (!userId.equals(creatorId)) {
                // 사용자 존재 확인
                memberRepository.findById(userId).orElseThrow(
                        () -> new IllegalArgumentException("User not found: " + userId));

                ChatParticipant participant = ChatParticipant.builder()
                        .chatRoom(room)
                        .userId(userId)
                        .status(ChatStatus.ACTIVE)
                        .lastReadSeq(0L)
                        .build();
                chatParticipantRepository.save(participant);
            }
        }

        // 시스템 메시지 생성 (그룹 채팅방 생성)
        String creatorNickname = memberRepository.findById(creatorId)
                .map(m -> m.getNickname())
                .orElse(creatorId);
        chatMessageService.createSystemMessage(room.getId(),
                creatorNickname + "님이 채팅방을 생성했습니다.", creatorId);

        // 참여자 정보 조회하여 DTO에 포함
        List<ChatParticipant> allParticipants = chatParticipantRepository.findByChatRoomId(room.getId());
        Set<String> allUserIds = allParticipants.stream().map(ChatParticipant::getUserId).collect(Collectors.toSet());
        List<Member> members = memberRepository.findAllById(allUserIds);
        Map<String, String> nicknameMap = members.stream()
                .collect(Collectors.toMap(m -> m.getEmail(), m -> m.getNickname(), (existing, replacement) -> existing));
        Map<String, String> departmentMap = members.stream()
                .collect(Collectors.toMap(m -> m.getEmail(), 
                    m -> m.getDepartment() != null ? m.getDepartment().name() : null, 
                    (existing, replacement) -> existing));
        List<ChatParticipantDTO> participantDTOs = allParticipants.stream()
                .map(p -> toChatParticipantDTO(p, 
                    nicknameMap.getOrDefault(p.getUserId(), p.getUserId()),
                    departmentMap.getOrDefault(p.getUserId(), null)))
                .collect(Collectors.toList());

        ChatRoomDTO dto = toChatRoomDTO(room, creator, 0L);
        dto.setParticipants(participantDTOs);
        return dto;
    }

    @Override
    public ChatRoomDTO createOrGetDirectRoom(DirectChatRoomCreateDTO createDTO, String userId) {
        String targetUserId = createDTO.getTargetUserId();

        if (userId.equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot create direct room with yourself");
        }

        // 사용자 존재 확인
        memberRepository.findById(targetUserId).orElseThrow(
                () -> new IllegalArgumentException("User not found: " + targetUserId));

        // pairKey 생성 (정렬하여 항상 동일한 키 생성)
        String[] users = {userId, targetUserId};
        Arrays.sort(users);
        String pairKey = users[0] + "_" + users[1];

        // 기존 DIRECT 방 조회
        Optional<ChatRoom> existingRoom = chatRoomRepository
                .findByRoomTypeAndPairKey(ChatRoomType.DIRECT, pairKey);

        if (existingRoom.isPresent()) {
            ChatRoom room = existingRoom.get();

            // 사용자가 이미 참여 중인지 확인
            Optional<ChatParticipant> participant = chatParticipantRepository
                    .findByChatRoomIdAndUserId(room.getId(), userId);

            if (participant.isPresent() && participant.get().getStatus() == ChatStatus.ACTIVE) {
                // 이미 참여 중
                Long unreadCount = 0L;
                if (room.getLastMsgSeq() != null) {
                    unreadCount = Math.max(0, room.getLastMsgSeq() - participant.get().getLastReadSeq());
                }
                return buildChatRoomDTOWithParticipants(room, participant.get(), unreadCount);
            } else if (participant.isPresent()) {
                // 나갔다가 다시 들어오는 경우
                ChatParticipant p = participant.get();
                p = ChatParticipant.builder()
                        .id(p.getId())
                        .chatRoom(room)
                        .userId(userId)
                        .status(ChatStatus.ACTIVE)
                        .lastReadSeq(p.getLastReadSeq())
                        .joinedAt(LocalDateTime.now())
                        .leftAt(null)
                        .build();
                chatParticipantRepository.save(p);

                Long unreadCount = 0L;
                if (room.getLastMsgSeq() != null) {
                    unreadCount = Math.max(0, room.getLastMsgSeq() - p.getLastReadSeq());
                }
                return buildChatRoomDTOWithParticipants(room, p, unreadCount);
            } else {
                // 방은 있지만 참여하지 않은 경우 (이론적으로 발생하지 않아야 함)
                ChatParticipant newParticipant = ChatParticipant.builder()
                        .chatRoom(room)
                        .userId(userId)
                        .status(ChatStatus.ACTIVE)
                        .lastReadSeq(0L)
                        .build();
                chatParticipantRepository.save(newParticipant);
                return buildChatRoomDTOWithParticipants(room, newParticipant, 0L);
            }
        }

        // 새 DIRECT 방 생성
        ChatRoom newRoom = ChatRoom.builder()
                .roomType(ChatRoomType.DIRECT)
                .pairKey(pairKey)
                .lastMsgSeq(0L)
                .build();
        newRoom = chatRoomRepository.save(newRoom);

        // 두 사용자 모두 참여자로 추가
        ChatParticipant participant1 = ChatParticipant.builder()
                .chatRoom(newRoom)
                .userId(userId)
                .status(ChatStatus.ACTIVE)
                .lastReadSeq(0L)
                .build();
        chatParticipantRepository.save(participant1);

        ChatParticipant participant2 = ChatParticipant.builder()
                .chatRoom(newRoom)
                .userId(targetUserId)
                .status(ChatStatus.ACTIVE)
                .lastReadSeq(0L)
                .build();
        chatParticipantRepository.save(participant2);

        return buildChatRoomDTOWithParticipants(newRoom, participant1, 0L);
    }

    @Override
    public void leaveRoom(Long roomId, String userId) {
        ChatParticipant participant = chatParticipantRepository
                .findByChatRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new IllegalArgumentException("User is not a participant of this room"));

        if (participant.getStatus() == ChatStatus.LEFT) {
            throw new IllegalArgumentException("User has already left this room");
        }

        // 나가기 처리 (레코드 삭제하지 않고 status 변경)
        participant.leave();

        // 시스템 메시지 생성 (채팅방 나가기)
        String userNickname = memberRepository.findById(userId)
                .map(m -> m.getNickname())
                .orElse(userId);
        chatMessageService.createSystemMessage(roomId,
                userNickname + "님이 채팅방을 나갔습니다.", userId);
    }

    @Override
    public void inviteUsers(Long roomId, ChatInviteDTO inviteDTO, String inviterId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));

        // 초대자가 참여자인지 확인
        if (!chatParticipantRepository.existsByChatRoomIdAndUserIdAndActive(roomId, inviterId)) {
            throw new IllegalArgumentException("Only participants can invite users");
        }

        // 그룹 채팅방만 초대 가능
        if (room.getRoomType() != ChatRoomType.GROUP) {
            throw new IllegalArgumentException("Only group rooms can invite users");
        }

        // 초대할 사용자들 추가
        for (String userId : inviteDTO.getUserIds()) {
            // 사용자 존재 확인
            memberRepository.findById(userId).orElseThrow(
                    () -> new IllegalArgumentException("User not found: " + userId));

            // 이미 참여 중인지 확인
            Optional<ChatParticipant> existing = chatParticipantRepository
                    .findByChatRoomIdAndUserId(roomId, userId);

            if (existing.isPresent()) {
                ChatParticipant p = existing.get();
                if (p.getStatus() == ChatStatus.ACTIVE) {
                    continue; // 이미 참여 중
                } else {
                    // 나갔다가 다시 들어오는 경우
                    p = ChatParticipant.builder()
                            .id(p.getId())
                            .chatRoom(room)
                            .userId(userId)
                            .status(ChatStatus.ACTIVE)
                            .lastReadSeq(p.getLastReadSeq())
                            .joinedAt(LocalDateTime.now())
                            .leftAt(null)
                            .build();
                    chatParticipantRepository.save(p);
                }
            } else {
                // 새 참여자 추가
                ChatParticipant participant = ChatParticipant.builder()
                        .chatRoom(room)
                        .userId(userId)
                        .status(ChatStatus.ACTIVE)
                        .lastReadSeq(room.getLastMsgSeq() != null ? room.getLastMsgSeq() : 0L)
                        .build();
                chatParticipantRepository.save(participant);
            }
        }

        // 시스템 메시지 생성 (채팅방 초대)
        String inviterNickname = memberRepository.findById(inviterId)
                .map(m -> m.getNickname())
                .orElse(inviterId);
        String invitedUsers = inviteDTO.getUserIds().stream()
                .map(id -> memberRepository.findById(id)
                        .map(m -> m.getNickname())
                        .orElse(id))
                .collect(java.util.stream.Collectors.joining(", "));
        chatMessageService.createSystemMessage(roomId,
                inviterNickname + "님이 " + invitedUsers + "님을 초대했습니다.", inviterId);
    }

    private ChatRoomDTO toChatRoomDTO(ChatRoom room, ChatParticipant participant, Long unreadCount) {
        return ChatRoomDTO.builder()
                .id(room.getId())
                .roomType(room.getRoomType())
                .pairKey(room.getPairKey())
                .name(room.getName())
                .lastMsgContent(room.getLastMsgContent())
                .lastMsgAt(room.getLastMsgAt())
                .lastMsgSeq(room.getLastMsgSeq())
                .createdAt(room.getCreatedAt())
                .unreadCount(unreadCount)
                .build();
    }

    private ChatParticipantDTO toChatParticipantDTO(ChatParticipant participant, String nickname, String department) {
        return ChatParticipantDTO.builder()
                .id(participant.getId())
                .chatRoomId(participant.getChatRoom().getId())
                .userId(participant.getUserId())
                .nickname(nickname) // DB 조회 없이 매개변수로 받은 값 사용
                .department(department) // DB 조회 없이 매개변수로 받은 값 사용
                .status(participant.getStatus())
                .lastReadSeq(participant.getLastReadSeq())
                .joinedAt(participant.getJoinedAt())
                .leftAt(participant.getLeftAt())
                .build();
    }

    private ChatRoomDTO buildChatRoomDTOWithParticipants(ChatRoom room, ChatParticipant myParticipant, Long unreadCount) {
        // 참여자 정보 조회하여 DTO에 포함
        List<ChatParticipant> allParticipants = chatParticipantRepository.findByChatRoomId(room.getId());
        Set<String> allUserIds = allParticipants.stream().map(ChatParticipant::getUserId).collect(Collectors.toSet());
        List<Member> members = memberRepository.findAllById(allUserIds);
        Map<String, String> nicknameMap = members.stream()
                .collect(Collectors.toMap(m -> m.getEmail(), m -> m.getNickname(), (existing, replacement) -> existing));
        Map<String, String> departmentMap = members.stream()
                .collect(Collectors.toMap(m -> m.getEmail(), 
                    m -> m.getDepartment() != null ? m.getDepartment().name() : null, 
                    (existing, replacement) -> existing));
        List<ChatParticipantDTO> participantDTOs = allParticipants.stream()
                .map(p -> toChatParticipantDTO(p, 
                    nicknameMap.getOrDefault(p.getUserId(), p.getUserId()),
                    departmentMap.getOrDefault(p.getUserId(), null)))
                .collect(Collectors.toList());

        ChatRoomDTO dto = toChatRoomDTO(room, myParticipant, unreadCount);
        dto.setParticipants(participantDTOs);
        return dto;
    }
}

