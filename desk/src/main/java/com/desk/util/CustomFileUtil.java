package com.desk.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
@Log4j2
public class CustomFileUtil {

    @Value("${com.desk.upload.path}")
    private String uploadPath;

    // Spring Bean이 생성되고, 의존성 주입이 끝난 직후, 딱 한 번 자동 실행
    @PostConstruct
    public void init() {
        File tempFolder = new File(uploadPath);
        if (!tempFolder.exists()) tempFolder.mkdir();
        uploadPath = tempFolder.getAbsolutePath(); // 절대경로로 확정
    }

    public String saveFile(MultipartFile file) throws RuntimeException {
        String originalName = file.getOriginalFilename();
        String suffix = originalName.substring(originalName.lastIndexOf(".")); // 확장자 추출 (.jpg 등)
        String savedName = UUID.randomUUID().toString() + suffix; // uuid.확장자

        Path savePath = Paths.get(uploadPath, savedName);
        try {
            Files.copy(file.getInputStream(), savePath);
            /*
                [기능 확장: 썸네일 생성 로직]
                파일 첨부 시 즉시 서버에 저장하지 않고, 실제 저장 시점에만 업로드되도록 설계하였습니다.
                이에 따라 미저장 파일은 프론트엔드에서 미리보기를 제공하고,
                서버에 저장된 파일에 대해서만 서버 썸네일 생성 구조로 확장 가능하도록 구성하였습니다.
                현재는 저장 여부에 따른 프론트엔드 리사이징 방식을 적용하였으며,
                향후 이미지 사용량 증가 시 서버 썸네일 방식으로 확장할 수 있습니다.
             */
            /*
            String contentType = file.getContentType();
            if (contentType != null && contentType.startsWith("image")) {
                // 썸네일 파일명은 "s_uuid.확장자" 형태로 생성
                Path thumbnailPath = Paths.get(uploadPath, "s_" + savedName);

                Thumbnails.of(savePath.toFile())
                          .size(200, 200) // 썸네일 크기 설정
                          .toFile(thumbnailPath.toFile());

                log.info("썸네일 생성 완료: s_" + savedName);
            }
            */
            return savedName; // 이제 확장자가 포함된 이름을 반환
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public ResponseEntity<Resource> getFile(String fileName, String originalName) {
        Resource resource = new FileSystemResource(uploadPath + File.separator + fileName);
        if (!resource.isReadable()) return ResponseEntity.notFound().build();

        HttpHeaders headers = new HttpHeaders();
        try {
            // 1. 파일의 실제 MIME 타입을 체크 (image/jpeg 등)
            String contentType = Files.probeContentType(resource.getFile().toPath());
            headers.add("Content-Type", contentType);

            // 2. 다운로드 요청인 경우 (originalName이 있을 때) 파일명 인코딩 처리
            if (originalName != null) {
                String encodedName = UriUtils.encode(originalName, StandardCharsets.UTF_8);
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedName + "\"");
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok().headers(headers).body(resource);
    }

    public void deleteFile(String fileName) {
        if (fileName == null) return;

        log.info("물리 파일 삭제 시도: " + fileName);

        // 파일의 전체 경로 생성
        Path filePath = Paths.get(uploadPath, fileName);

        try {
            // 파일이 존재하면 삭제
            boolean deleted = Files.deleteIfExists(filePath);

            if (deleted) {
                log.info("파일 삭제 성공: " + fileName);
            } else {
                log.warn("삭제할 파일이 존재하지 않습니다: " + fileName);
            }

            // 만약 썸네일(s_ 접두어) 기능이 있다면 함께 삭제 로직 추가 가능
            // String thumbnailName = "s_" + fileName;
            // Files.deleteIfExists(Paths.get(uploadPath, thumbnailName));

        } catch (IOException e) {
            log.error("파일 삭제 중 오류 발생: " + e.getMessage());
            throw new RuntimeException("파일 삭제 실패: " + fileName);
        }
    }
}