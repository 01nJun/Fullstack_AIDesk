package com.desk.util;

import com.desk.domain.UploadTicketFile;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Log4j2
@RequiredArgsConstructor
public class CustomFileUtil {

    @Value("${com.desk.upload.path}")
    private String uploadPath;

    @PostConstruct
    public void init() {
        File tempFolder = new File(uploadPath);

        if(tempFolder.exists() == false) {
            tempFolder.mkdir();
        }

        uploadPath = tempFolder.getAbsolutePath();

        log.info("-------------------------------------");
        log.info(uploadPath);
    }

    public List<UploadTicketFile> saveFiles(List<MultipartFile> files)throws RuntimeException{

        if(files == null || files.size() == 0){
            return null; //List.of();
        }

        List<UploadTicketFile> result = new ArrayList<>();

        for (MultipartFile multipartFile : files) {

            String originalName = multipartFile.getOriginalFilename();
            if (originalName == null || originalName.isBlank()) continue;

            // 1) 확장자 분리 (".jpg" 형태로 저장 추천)
            String ext = "";
            int dotIndex = originalName.lastIndexOf(".");
            if (dotIndex > -1 && dotIndex < originalName.length() - 1) {
                ext = originalName.substring(dotIndex); // ".jpg"
            }

            // 2) UUID 생성
            String uuid = UUID.randomUUID().toString();

            // 3) 실제 저장 파일명 (UUID_원본명)
            String savedName = uuid + "_" + originalName;
            Path savePath = Paths.get(uploadPath, savedName);

            try {
                Files.copy(multipartFile.getInputStream(), savePath, StandardCopyOption.REPLACE_EXISTING);

                String contentType = multipartFile.getContentType();
                boolean isImage = (contentType != null && contentType.startsWith("image"));

                // 썸네일 생성 (이미지일 때)
                if (isImage) {
                    Path thumbnailPath = Paths.get(uploadPath, "s_" + savedName);
                    Thumbnails.of(savePath.toFile())
                            .size(250, 250)
                            .toFile(thumbnailPath.toFile());
                }

                // 4) 메타 객체 생성해서 반환 리스트에 담기
                UploadTicketFile meta = UploadTicketFile.builder()
                        .uuid(uuid)
                        .originalName(originalName)
                        .ext(ext)

                        .size(multipartFile.getSize())
                        .image(isImage)
                        .build();

                result.add(meta);

            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        return result;
    }

    // [NEW] 파일 다운로드 처리 (강제 다운로드 헤더 설정)
    public ResponseEntity<Resource> downloadFile(String savedName) {
        Resource resource = new FileSystemResource(uploadPath + File.separator + savedName);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        // savedName에서 uuid_ 제거하고 순수 파일명 추출
        String originalName = savedName.substring(savedName.indexOf("_") + 1);

        HttpHeaders headers = new HttpHeaders();
        try {
            // 한글 파일명 깨짐 방지 인코딩
            String encodedFileName = URLEncoder.encode(originalName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename(encodedFileName, StandardCharsets.UTF_8)
                    .build());

            headers.add(HttpHeaders.CONTENT_TYPE, Files.probeContentType(resource.getFile().toPath()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok().headers(headers).body(resource);
    }

    // [NEW] 일반 파일 조회 (View/Image Src용)
    public ResponseEntity<Resource> getFile(String fileName) {
        Resource resource = new FileSystemResource(uploadPath + File.separator + fileName);

        if (!resource.exists()) {
            // 기본 이미지 처리 (없으면 404)
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        try {
//            headers.add("Content-Type", Files.probeContentType(resource.getFile().toPath()));

            String contentType = Files.probeContentType(resource.getFile().toPath());
            if (contentType == null) {
                contentType = "image/jpeg"; // 기본값 설정
            }
            headers.add("Content-Type", contentType);


        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok().headers(headers).body(resource);
    }

    // [NEW] 단일 파일 삭제
    public void deleteFile(String savedName, boolean isImage) {
        Path filePath = Paths.get(uploadPath, savedName);
        deleteIfExists(filePath);

        if (isImage) {
            Path thumbPath = Paths.get(uploadPath, "s_" + savedName);
            deleteIfExists(thumbPath);
        }
    }


    public ResponseEntity<Resource> getThumbnailFile(String savedName) {

        if (savedName == null || savedName.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            // (1) 기본적인 경로 탐색 공격 방지
            Path root = Paths.get(uploadPath).toAbsolutePath().normalize();

            // 썸네일은 "s_" prefix
            String thumbName = "s_" + savedName;

            Path thumbPath = root.resolve(thumbName).normalize();
            if (!thumbPath.startsWith(root)) {
                return ResponseEntity.badRequest().build();
            }

            // (2) 썸네일 있으면 썸네일 내려줌
            if (Files.exists(thumbPath)) {
                return buildFileResponse(thumbPath);
            }

            // (3) 썸네일 없으면 원본으로 fallback (이미지면 원본이라도 브라우저 표시 가능)
            Path originPath = root.resolve(savedName).normalize();
            if (!originPath.startsWith(root)) {
                return ResponseEntity.badRequest().build();
            }

            if (Files.exists(originPath)) {
                return buildFileResponse(originPath);
            }

            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("썸네일 조회 실패: {}", savedName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ✅ 공통 응답 생성 (원본/썸네일 모두 사용)
    private ResponseEntity<Resource> buildFileResponse(Path path) throws IOException {

        Resource resource = new FileSystemResource(path);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(path);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, contentType);

        // 썸네일은 inline 표시가 일반적이라 Content-Disposition은 굳이 첨부 안 해도 돼요.
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }


    public void deleteFiles(List<UploadTicketFile> files) {

        if (files == null || files.isEmpty()) return;

        for (UploadTicketFile file : files) {

            // 1️⃣ 원본 파일 삭제
            Path filePath = Paths.get(uploadPath, file.getLink());
            deleteIfExists(filePath);

            // 2️⃣ 썸네일 삭제 (이미지일 경우만)
            if (file.isImage()) {
                Path thumbPath = Paths.get(uploadPath, "s_" + file.getLink());
                deleteIfExists(thumbPath);
            }
        }
    }

    private void deleteIfExists(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error("파일 삭제 실패: {}", path, e);
        }
    }


    public String makeViewUrl(String savedName){
        // path variable 인코딩 (공백/특수문자 대비)
        return "/api/files/view/" + URLEncoder.encode(savedName, StandardCharsets.UTF_8);
    }

    public String makePreviewUrl(UploadTicketFile f){
        if (f.isImage()) {
            // 썸네일 보여주는 엔드포인트가 이미 있다면 그걸 쓰고,
            // 없다면 view로도 이미지 출력은 가능해요(다만 목록에선 thumb가 더 좋아요)
            return "/api/files/view/" + URLEncoder.encode("s_" + f.getLink(), StandardCharsets.UTF_8);
        }
        // 문서 아이콘은 프론트 정적 리소스로 두는 걸 추천
        // 예: /assets/file-icons/pdf.png
        String ext = (f.getExt() == null) ? "" : f.getExt().toLowerCase(); // ".pdf"
        String key = ext.startsWith(".") ? ext.substring(1) : ext;         // "pdf"
        if (key.isBlank()) key = "file";
        return "/assets/file-icons/" + key + ".png";
    }
    public String makeDownloadUrl(String savedName){
        return "/api/files/download/" + savedName;
    }
}