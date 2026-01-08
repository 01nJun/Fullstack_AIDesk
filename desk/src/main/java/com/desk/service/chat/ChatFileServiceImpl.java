package com.desk.service.chat;

import com.desk.domain.ChatFile;
import com.desk.domain.ChatFileStatus;
import com.desk.domain.ChatMessage;
import com.desk.dto.chat.ChatFileDTO;
import com.desk.repository.chat.ChatFileRepository;
import com.desk.repository.chat.ChatMessageRepository;
import com.desk.repository.chat.ChatParticipantRepository;
import com.desk.util.CustomFileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional
public class ChatFileServiceImpl implements ChatFileService {

    private final ChatFileRepository chatFileRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final CustomFileUtil fileUtil;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${com.desk.chat.file.max-size:10485760}") // 기본값 10MB
    private long maxFileSize;

    @Value("${com.desk.chat.file.allowed-types:image/jpeg,image/png,image/gif,application/pdf}")
    private String allowedTypes;

    /**
     * 파일 검증
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // 파일 크기 검증
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                    String.format("File size exceeds limit. Max size: %d bytes (%.2f MB)", 
                            maxFileSize, maxFileSize / (1024.0 * 1024.0)));
        }

        // 파일 타입 검증
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("File content type is null");
        }

        List<String> allowedTypeList = Arrays.asList(allowedTypes.split(","));
        boolean isAllowed = allowedTypeList.stream()
                .anyMatch(allowedType -> contentType.toLowerCase().startsWith(allowedType.trim().toLowerCase()));

        if (!isAllowed) {
            throw new IllegalArgumentException(
                    String.format("File type not allowed. Allowed types: %s", allowedTypes));
        }
    }

    @Override
    public ChatFileDTO uploadFile(Long chatRoomId, MultipartFile file, String userId) {
        // 채팅방 참여자 확인
        if (!chatParticipantRepository.existsByChatRoomIdAndUserIdAndActive(chatRoomId, userId)) {
            throw new IllegalArgumentException("User is not a participant of this room");
        }

        // 파일 검증
        validateFile(file);

        try {
            // 파일 저장 (CustomFileUtil 재사용) - uuid 반환
            String uuid = fileUtil.saveFile(file);

            // 확장자 추출
            String fileName = file.getOriginalFilename();
            String ext = fileName != null && fileName.contains(".")
                    ? fileName.substring(fileName.lastIndexOf("."))
                    : "";

            // ChatFile 엔티티 생성 (TEMP 상태, chat_message_id는 null)
            ChatFile chatFile = ChatFile.builder()
                    .uuid(uuid) // PK로 사용
                    .chatRoomId(chatRoomId)
                    .uploaderId(userId)
                    .fileName(fileName)
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .ext(ext)
                    .status(ChatFileStatus.TEMP)
                    .build();

            chatFile = chatFileRepository.save(chatFile);

            log.info("[ChatFile] 업로드 완료 | uuid={} | roomId={} | uploaderId={} | fileName={}", 
                    chatFile.getUuid(), chatRoomId, userId, fileName);

            return toDTO(chatFile);
        } catch (Exception e) {
            log.error("[ChatFile] 업로드 실패", e);
            throw new RuntimeException("File upload failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ChatFileDTO getFile(String uuid, String userId) {
        ChatFile chatFile = chatFileRepository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + uuid));

        // 채팅방 참여자 확인 (chat_room_id로 직접 확인)
        if (!chatParticipantRepository.existsByChatRoomIdAndUserIdAndActive(
                chatFile.getChatRoomId(), userId)) {
            throw new IllegalArgumentException("User is not a participant of this room");
        }

        return toDTO(chatFile);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatFileDTO getFileWithoutAuth(String uuid) {
        ChatFile chatFile = chatFileRepository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + uuid));

        // 인증 없이 파일 조회 (이미지 프리뷰용)
        return toDTO(chatFile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatFileDTO> getFilesByMessageId(Long messageId, String userId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        // 채팅방 참여자 확인
        Long chatRoomId = message.getChatRoom().getId();
        if (!chatParticipantRepository.existsByChatRoomIdAndUserIdAndActive(chatRoomId, userId)) {
            throw new IllegalArgumentException("User is not a participant of this room");
        }

        List<ChatFile> files = chatFileRepository.findByChatMessageId(messageId);
        return files.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteFile(String uuid, String userId) {
        ChatFile chatFile = chatFileRepository.findById(uuid)
                .orElseThrow(() -> new IllegalArgumentException("File not found: " + uuid));

        // 채팅방 참여자 확인
        if (!chatParticipantRepository.existsByChatRoomIdAndUserIdAndActive(
                chatFile.getChatRoomId(), userId)) {
            throw new IllegalArgumentException("User is not a participant of this room");
        }

        // 발신자만 삭제 가능하도록 추가 체크
        if (!chatFile.getUploaderId().equals(userId)) {
            throw new IllegalArgumentException("Only uploader can delete the file");
        }

        // 소프트 삭제 처리 (물리 파일은 배치에서 정리)
        chatFile.markAsDeleted();
        chatFileRepository.save(chatFile);
        
        log.info("[ChatFile] 소프트 삭제 완료 | uuid={} | userId={} | fileName={}", 
                uuid, userId, chatFile.getFileName());
    }

    /**
     * 파일들을 메시지에 바인딩 (sendMessage에서 호출)
     */
    @Override
    public List<ChatFileDTO> bindFilesToMessage(
            List<String> fileUuids, 
            Long chatRoomId, 
            String uploaderId, 
            ChatMessage message) {
        
        if (fileUuids == null || fileUuids.isEmpty()) {
            return List.of();
        }

        // 중복 제거
        Set<String> uniqueFileUuids = Set.copyOf(fileUuids);
        if (uniqueFileUuids.size() != fileUuids.size()) {
            throw new IllegalArgumentException("Duplicate file UUIDs are not allowed");
        }

        // TEMP 상태이고, chatRoomId와 uploaderId가 일치하는 파일들만 조회
        List<ChatFile> files = chatFileRepository.findByUuidInAndStatusAndChatRoomIdAndUploaderId(
                List.copyOf(uniqueFileUuids), 
                ChatFileStatus.TEMP, 
                chatRoomId, 
                uploaderId
        );

        // 조회된 파일 수가 요청한 파일 수와 일치하는지 확인
        if (files.size() != uniqueFileUuids.size()) {
            throw new IllegalArgumentException(
                    String.format("Some files are not found or already bound. Requested: %d, Found: %d", 
                            uniqueFileUuids.size(), files.size()));
        }

        // 파일들을 메시지에 바인딩
        files.forEach(file -> file.bindToMessage(message));
        chatFileRepository.saveAll(files);

        log.info("[ChatFile] 파일 바인딩 완료 | messageId={} | fileCount={}", message.getId(), files.size());

        return files.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * ChatFile을 DTO로 변환 (인터페이스 구현)
     */
    @Override
    public ChatFileDTO toDTO(ChatFile chatFile) {
        String baseUrl = contextPath.isEmpty() ? "" : contextPath;
        String downloadUrl = baseUrl + "/api/chat/files/" + chatFile.getUuid() + "/download";
        String viewUrl = baseUrl + "/api/chat/files/" + chatFile.getUuid() + "/view";

        return ChatFileDTO.builder()
                .uuid(chatFile.getUuid())
                .fileName(chatFile.getFileName())
                .fileSize(chatFile.getFileSize())
                .mimeType(chatFile.getMimeType())
                .ext(chatFile.getExt())
                .createdAt(chatFile.getCreatedAt())
                .downloadUrl(downloadUrl)
                .viewUrl(viewUrl)
                .build();
    }
}

