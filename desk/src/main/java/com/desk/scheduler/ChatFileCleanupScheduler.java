package com.desk.scheduler;

import com.desk.domain.ChatFile;
import com.desk.domain.ChatFileStatus;
import com.desk.repository.chat.ChatFileRepository;
import com.desk.util.CustomFileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 채팅 파일 정리 스케줄러
 * - TEMP 상태 파일: 24시간 이상 지난 파일 자동 삭제
 * - DELETED 상태 파일: 7일 이상 지난 파일의 물리 파일 삭제
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class ChatFileCleanupScheduler {

    private final ChatFileRepository chatFileRepository;
    private final CustomFileUtil fileUtil;

    /**
     * TEMP 상태 파일 정리 (24시간마다 실행)
     * 업로드 후 24시간 동안 메시지에 바인딩되지 않은 파일 삭제
     */
    @Scheduled(cron = "0 0 2 * * ?") // 매일 새벽 2시 실행
    @Transactional
    public void cleanupTempFiles() {
        log.info("[ChatFileCleanup] TEMP 파일 정리 시작");
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        List<ChatFile> tempFiles = chatFileRepository.findTempFilesOlderThan(
                ChatFileStatus.TEMP, 
                cutoffTime
        );

        int deletedCount = 0;
        for (ChatFile file : tempFiles) {
            try {
                // 물리 파일 삭제
                fileUtil.deleteFile(file.getUuid());
                // DB 삭제
                chatFileRepository.delete(file);
                deletedCount++;
                log.debug("[ChatFileCleanup] TEMP 파일 삭제: uuid={}, fileName={}", 
                        file.getUuid(), file.getFileName());
            } catch (Exception e) {
                log.error("[ChatFileCleanup] TEMP 파일 삭제 실패: uuid={}, error={}", 
                        file.getUuid(), e.getMessage());
            }
        }

        log.info("[ChatFileCleanup] TEMP 파일 정리 완료: 삭제된 파일 수={}", deletedCount);
    }

    /**
     * DELETED 상태 파일의 물리 파일 정리 (매일 새벽 3시 실행)
     * 소프트 삭제 후 7일 이상 지난 파일의 물리 파일 삭제
     */
    @Scheduled(cron = "0 0 3 * * ?") // 매일 새벽 3시 실행
    @Transactional
    public void cleanupDeletedFiles() {
        log.info("[ChatFileCleanup] DELETED 파일 물리 파일 정리 시작");
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);
        List<ChatFile> deletedFiles = chatFileRepository.findDeletedFilesOlderThan(
                ChatFileStatus.DELETED, 
                cutoffTime
        );

        int deletedCount = 0;
        for (ChatFile file : deletedFiles) {
            try {
                // 물리 파일 삭제
                fileUtil.deleteFile(file.getUuid());
                // DB에서도 완전 삭제
                chatFileRepository.delete(file);
                deletedCount++;
                log.debug("[ChatFileCleanup] DELETED 파일 물리 파일 삭제: uuid={}, fileName={}", 
                        file.getUuid(), file.getFileName());
            } catch (Exception e) {
                log.error("[ChatFileCleanup] DELETED 파일 물리 파일 삭제 실패: uuid={}, error={}", 
                        file.getUuid(), e.getMessage());
            }
        }

        log.info("[ChatFileCleanup] DELETED 파일 물리 파일 정리 완료: 삭제된 파일 수={}", deletedCount);
    }
}

