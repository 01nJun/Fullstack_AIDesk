package com.desk.service.chat;

import com.desk.domain.ChatFile;
import com.desk.domain.ChatMessage;
import com.desk.dto.chat.ChatFileDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 채팅 파일 서비스 인터페이스
 */
public interface ChatFileService {
    
    /**
     * 파일 업로드 (TEMP 상태로 저장)
     */
    ChatFileDTO uploadFile(Long chatRoomId, MultipartFile file, String userId);
    
    /**
     * 파일 조회 (권한 검증 포함)
     */
    ChatFileDTO getFile(String uuid, String userId);
    
    /**
     * 파일 조회 (인증 없이, 이미지 프리뷰용)
     */
    ChatFileDTO getFileWithoutAuth(String uuid);
    
    /**
     * 메시지에 첨부된 파일 목록 조회
     */
    List<ChatFileDTO> getFilesByMessageId(Long messageId, String userId);
    
    /**
     * 파일 삭제
     */
    void deleteFile(String uuid, String userId);
    
    /**
     * 파일들을 메시지에 바인딩 (TEMP -> BOUND)
     */
    List<ChatFileDTO> bindFilesToMessage(
            List<String> fileUuids, 
            Long chatRoomId, 
            String uploaderId, 
            ChatMessage message
    );
    
    /**
     * ChatFile 엔티티를 DTO로 변환
     */
    ChatFileDTO toDTO(ChatFile chatFile);
}

