package com.tt.demo.controller;

import com.tt.demo.service.ToolService;
import com.tt.demo.dto.ApkInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/tools")
public class ToolController {

    @Autowired
    private ToolService toolService;

    @PostMapping("/md5")
    public String convertToMd5(@RequestBody String input) {
        return toolService.convertToMd5(input);
    }

    @PostMapping("/base64/encode")
    public String encodeBase64(@RequestBody String input) {
        return toolService.encodeBase64(input);
    }

    @PostMapping("/base64/decode")
    public String decodeBase64(@RequestBody String input) {
        return toolService.decodeBase64(input);
    }

    @PostMapping("/pdf2word")
    public ResponseEntity<byte[]> convertPdfToWord(@RequestParam("file") MultipartFile file) throws IOException {
        byte[] wordContent = toolService.convertPdfToWord(file);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "converted.docx");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(wordContent);
    }

    @PostMapping("/word2pdf")
    public ResponseEntity<byte[]> convertWordToPdf(@RequestParam("file") MultipartFile file) throws IOException {
        // 调用服务层方法进行转换
        byte[] pdfContent = toolService.convertWordToPdf(file);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "converted.pdf");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfContent);
    }

    @PostMapping("/parseApk")
    public ResponseEntity<ApkInfo> parseApk(@RequestParam("file") MultipartFile file) throws IOException {
        if (!file.getOriginalFilename().toLowerCase().endsWith(".apk")) {
            throw new IllegalArgumentException("请上传APK文件");
        }
        return ResponseEntity.ok(toolService.parseApk(file));
    }
}