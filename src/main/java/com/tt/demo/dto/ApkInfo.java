package com.tt.demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApkInfo {
    private String packageName;        // 包名
    private String versionName;        // 版本名称
    private long versionCode;          // 版本号
    private String minSdkVersion;      // 最小SDK版本
    private String targetSdkVersion;   // 目标SDK版本
    private String appName;            // 应用名称
    private long fileSize;             // 文件大小
    private String md5;                // MD5值
    private String sha1;               // SHA1值
    private String sha256;             // SHA256值
    private String permissions;        // 权限列表
} 