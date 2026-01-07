package com.desk.service;

import com.desk.dto.MeetingMinutesDTO;
import org.springframework.web.multipart.MultipartFile;


public interface OllamaService {

    public MeetingMinutesDTO getMeetingInfoFromAi(MultipartFile file, String title, String content, String purpose, String requirement);

    public byte[] generatePdf(MeetingMinutesDTO summary);

}
