package com.mall.pointsmall.controller;

import com.mall.pointsmall.common.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {
    @PostMapping("/upload")
    public ApiResponse<?> upload(MultipartFile file) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", file.getOriginalFilename());
        payload.put("size", file.getSize());
        payload.put("url", "https://cos.example.com/" + file.getOriginalFilename());
        return ApiResponse.ok("上传成功", payload);
    }
}
