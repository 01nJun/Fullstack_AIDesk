package com.desk.service;

import java.io.IOException;

public interface SttService {
    String stt(byte[] audioBytes) throws IOException;
}