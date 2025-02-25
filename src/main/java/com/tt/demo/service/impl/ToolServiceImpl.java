package com.tt.demo.service.impl;

import com.tt.demo.service.ToolService;
import com.tt.demo.dto.ApkInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import net.dongliu.apk.parser.ApkFile;
import net.dongliu.apk.parser.bean.ApkMeta;
import net.dongliu.apk.parser.bean.UseFeature;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.stream.Collectors;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import java.io.InputStream;
import java.io.FileOutputStream;
import com.itextpdf.text.Font;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Element;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.BaseColor;
import java.lang.NumberFormatException;

@Slf4j
@Service
public class ToolServiceImpl implements ToolService {

    @Override
    public String convertToMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(input.getBytes());
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02X", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 算法不可用", e);
        }
    }

    @Override
    public String encodeBase64(String input) {
        return Base64.getEncoder().encodeToString(input.getBytes());
    }

    @Override
    public String decodeBase64(String input) {
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(input);
            return new String(decodedBytes);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("无效的Base64编码", e);
        }
    }

    @Override
    public byte[] convertPdfToWord(MultipartFile file) throws IOException {
        try (PDDocument pdf = PDDocument.load(file.getInputStream())) {
            XWPFDocument doc = new XWPFDocument();

            // 使用PDFBox提取文本
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdf);

            // 创建Word文档段落
            XWPFParagraph paragraph = doc.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText(text);

            // 将Word文档转换为字节数组
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);
            return out.toByteArray();
        }
    }

    @Override
    public byte[] convertWordToPdf(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream());
                ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream()) {

            Document pdfDocument = new Document(com.itextpdf.text.PageSize.A4); // 使用完整类名解决PageSize无法解析的问题
            PdfWriter pdfWriter = PdfWriter.getInstance(pdfDocument, pdfOutputStream);
            pdfDocument.open();

            // 设置中文字体
            BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font chineseFont = new Font(baseFont, 12, Font.NORMAL);

            // 遍历Word文档中的段落
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                Paragraph pdfParagraph = new Paragraph(paragraph.getText(), chineseFont);
                pdfDocument.add(pdfParagraph);
            }

            pdfDocument.close();
            return pdfOutputStream.toByteArray();

        } catch (Exception e) {
            log.error("转换Word到PDF失败: {}", e.getMessage(), e);
            throw new IOException("转换失败: " + e.getMessage(), e);
        }
    }

    @Override
    public ApkInfo parseApk(MultipartFile file) throws IOException {
        log.info("开始解析APK文件: {}, 大小: {}", file.getOriginalFilename(), formatFileSize(file.getSize()));

        // 创建临时目录
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "apk_analysis");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        // 使用原始文件名创建临时文件
        String originalFilename = file.getOriginalFilename();
        String tempFileName = System.currentTimeMillis() + "_"
                + (originalFilename != null ? originalFilename : "temp.apk");
        File tempFile = new File(tempDir, tempFileName);
        log.info("准备创建临时文件: {}", tempFile.getAbsolutePath());

        try {
            // 使用输入流写入文件
            try (InputStream inputStream = file.getInputStream();
                    FileOutputStream outputStream = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
            log.info("APK文件已保存到临时文件: {}", tempFile.getAbsolutePath());

            // 解析APK文件
            try (ApkFile apkFile = new ApkFile(tempFile)) {
                ApkMeta apkMeta = apkFile.getApkMeta();
                ApkInfo info = new ApkInfo();

                // 基本信息
                info.setPackageName(apkMeta.getPackageName());
                info.setVersionName(apkMeta.getVersionName());
                info.setVersionCode(apkMeta.getVersionCode());
                info.setMinSdkVersion(apkMeta.getMinSdkVersion());
                info.setTargetSdkVersion(apkMeta.getTargetSdkVersion());
                info.setAppName(apkMeta.getLabel());
                info.setFileSize(file.getSize());

                log.info("解析到APK基本信息: 应用名称={}, 包名={}, 版本={}, SDK版本范围={}-{}",
                        info.getAppName(),
                        info.getPackageName(),
                        info.getVersionName(),
                        info.getMinSdkVersion(),
                        info.getTargetSdkVersion());

                // 计算文件哈希值
                byte[] fileBytes = file.getBytes();
                info.setMd5(calculateHash(fileBytes, "MD5"));
                info.setSha1(calculateHash(fileBytes, "SHA-1"));
                info.setSha256(calculateHash(fileBytes, "SHA-256"));

                log.info("计算文件哈希值完成: MD5={}", info.getMd5());

                // 获取权限列表
                String permissions = apkMeta.getUsesPermissions().stream()
                        .collect(Collectors.joining("\n"));
                info.setPermissions(permissions);

                log.info("解析到权限数量: {}", apkMeta.getUsesPermissions().size());

                return info;
            }
        } catch (Exception e) {
            log.error("解析APK文件失败: {}", e.getMessage(), e);
            throw new IOException("解析失败: " + e.getMessage(), e);
        } finally {
            // 清理临时文件
            if (tempFile.exists()) {
                boolean deleted = tempFile.delete();
                log.info("临时文件删除{}: {}", deleted ? "成功" : "失败", tempFile.getAbsolutePath());
            }

            // 尝试清理临时目录（如果为空）
            if (tempDir.exists() && tempDir.list() != null && tempDir.list().length == 0) {
                boolean deleted = tempDir.delete();
                log.info("临时目录删除{}: {}", deleted ? "成功" : "失败", tempDir.getAbsolutePath());
            }
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes == 0)
            return "0 Bytes";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.2f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private String calculateHash(byte[] input, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(input);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            String result = hexString.toString().toUpperCase();
            log.debug("计算{}哈希值: {}", algorithm, result);
            return result;
        } catch (Exception e) {
            log.error("计算{}哈希值失败: {}", algorithm, e.getMessage(), e);
            return "计算失败";
        }
    }
}