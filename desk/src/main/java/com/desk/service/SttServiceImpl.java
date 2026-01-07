package com.desk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Log4j2
public class SttServiceImpl implements SttService {

    private final OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;

    @Override
    public String stt(byte[] audioBytes) throws IOException {
        log.info("STT 처리 시작... 오디오 크기: {} bytes", audioBytes.length);

        // 음성 데이터(byte[])를 ByteArrayResource로 생성
        Resource audioResource = new ByteArrayResource(audioBytes);

        // 모델 옵션 설정
        OpenAiAudioTranscriptionOptions options = OpenAiAudioTranscriptionOptions.builder()
                .model("whisper-1")
                .language("ko") // 입력 음성 언어의 종류 설정, 출력 언어에도 영향을 미침
                .build();

        // 프롬프트 생성
        AudioTranscriptionPrompt prompt = new AudioTranscriptionPrompt(audioResource, options);

        // 모델을 호출하고 응답받기
        AudioTranscriptionResponse response = openAiAudioTranscriptionModel.call(prompt);
        String text = response.getResult().getOutput();

        log.info("STT 처리 완료: 변환된 텍스트 길이 = {} 문자", text.length());
        return text;
    }
}