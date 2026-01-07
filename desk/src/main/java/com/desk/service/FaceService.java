package com.desk.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FaceService {
    String findFace(MultipartFile mf) throws IOException;
    void registerFace(String email, MultipartFile mf) throws IOException;
    void updateFaceStatus(String email, boolean status);
}