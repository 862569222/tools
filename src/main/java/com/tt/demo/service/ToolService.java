package com.tt.demo.service;

import org.springframework.web.multipart.MultipartFile;
import com.tt.demo.dto.ApkInfo;
import java.io.IOException;

public interface ToolService {
    String convertToMd5(String input);
    String encodeBase64(String input);
    String decodeBase64(String input);
    byte[] convertPdfToWord(MultipartFile file) throws IOException;
    byte[] convertWordToPdf(MultipartFile file) throws IOException;
    ApkInfo parseApk(MultipartFile file) throws IOException;
}