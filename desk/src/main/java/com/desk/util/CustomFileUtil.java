package com.desk.util;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

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

    public List<String> saveFiles(List<MultipartFile> files) {

        if (files == null || files.isEmpty()) {
            return null;
        }

        List<String> uploadNames = new ArrayList<>();

        for (MultipartFile multipartFile : files) {

            String originalName = multipartFile.getOriginalFilename();
            if (originalName == null) continue;

            // 파일명 / 확장자 분리
            String fileName;
            String ext;

            int dotIndex = originalName.lastIndexOf(".");
            if (dotIndex > -1) {
                fileName = originalName.substring(0, dotIndex);
                ext = originalName.substring(dotIndex); // .jpg
            } else {
                fileName = originalName;
                ext = "";
            }

            String savedName = originalName;
            Path savePath = Paths.get(uploadPath, savedName);

            int count = 1;
            // 동일 파일명 존재 시 (1), (2) 증가
            while (Files.exists(savePath)) {
                savedName = fileName + "(" + count + ")" + ext;
                savePath = Paths.get(uploadPath, savedName);
                count++;
            }

            try {
                Files.copy(multipartFile.getInputStream(), savePath);

                String contentType = multipartFile.getContentType();
                if (contentType != null && contentType.startsWith("image")) {

                    Path thumbnailPath = Paths.get(uploadPath, "s_" + savedName);

                    Thumbnails.of(savePath.toFile())
                            .size(200, 200)
                            .toFile(thumbnailPath.toFile());
                }

                uploadNames.add(savedName);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return uploadNames;
    }

//    public List<String> saveFiles(List<MultipartFile> files)throws RuntimeException{
//
//        if(files == null || files.size() == 0){
//            return null; //List.of();
//        }
//
//        List<String> uploadNames = new ArrayList<>();
//
//        for (MultipartFile multipartFile : files) {
//
//            String savedName = UUID.randomUUID().toString() + "_" + multipartFile.getOriginalFilename();
//
//            Path savePath = Paths.get(uploadPath, savedName);
//
//            try {
//                Files.copy(multipartFile.getInputStream(), savePath);
//                String contentType = multipartFile.getContentType();
//
//                if(contentType != null && contentType.startsWith("image")){
//
//                    Path thumbnailPath = Paths.get(uploadPath, "s_"+savedName);
//
//                    Thumbnails.of(savePath.toFile())
//                            .size(200, 200)
//                            .toFile(thumbnailPath.toFile());
//                }
//                uploadNames.add(savedName);
//            } catch (IOException e) {
//                throw new RuntimeException(e.getMessage());
//            }
//        }//end for
//        return uploadNames;
//    }


    // 파일 데이터를 읽어서 스프리엥서 제공하는 Resource 타입으로 변환하는 메서드
    public ResponseEntity<Resource> getFile(String fileName){
        Resource resource = new FileSystemResource(uploadPath + File.separator + fileName);

        if( !resource.isReadable()){
            resource = new FileSystemResource(uploadPath + File.separator + "winter.jpg");
        }

        HttpHeaders headers = new HttpHeaders();

        try{
            headers.add("Content-Type", Files.probeContentType(resource.getFile().toPath()));
        }catch(Exception e){
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok().headers(headers).body(resource);
    }

    public void deleteFiles(List<String> fileNames){

        if(fileNames == null || fileNames.size() == 0){
            return;
        }

        fileNames.forEach(fileName ->{

            // 썸네일이 있는지 확인하고 삭제
            String thumbnailFileName = "s_" + fileName;
            Path thumbnailPath = Paths.get(uploadPath, thumbnailFileName);
            Path filePath = Paths.get(uploadPath, fileName);

            try{
                Files.deleteIfExists(filePath);
                Files.deleteIfExists(thumbnailPath);
            }catch(IOException e){
                throw new RuntimeException(e.getMessage());
            }
        });
    }

}