package com.mall.pointsmall.controller;

import com.mall.pointsmall.common.ApiResponse;
import com.mall.pointsmall.enums.AdminMenuKey;
import com.mall.pointsmall.exception.BusinessException;
import com.mall.pointsmall.security.SecurityUtils;
import com.mall.pointsmall.service.AdminPermissionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private static final long MAX_IMAGE_BYTES = 10 * 1024 * 1024L;
    private static final long MAX_IMAGE_PIXELS = 40_000_000L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private final Path uploadDirectory;
    private final AdminPermissionService adminPermissionService;

    public FileController(@Value("${app.upload.directory:uploads}") String directory,
                          AdminPermissionService adminPermissionService) throws IOException {
        this.uploadDirectory = Path.of(directory).toAbsolutePath().normalize();
        this.adminPermissionService = adminPermissionService;
        Files.createDirectories(uploadDirectory);
    }

    @PostMapping("/images")
    public ApiResponse<?> uploadImage(@RequestParam("file") MultipartFile file) {
        adminPermissionService.assertEdit(SecurityUtils.currentUser(), AdminMenuKey.PRODUCTS);
        if (file.isEmpty()) {
            throw new BusinessException("请选择图片");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BusinessException("单张图片不能超过 10MB");
        }
        String extension = extension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("仅支持 jpg、jpeg、png 图片");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!contentType.equals("image/jpeg") && !contentType.equals("image/png")) {
            throw new BusinessException("图片格式不正确");
        }
        validateImageContent(file, extension);
        String filename = UUID.randomUUID() + "." + extension;
        try {
            file.transferTo(uploadDirectory.resolve(filename));
        } catch (IOException ex) {
            throw new BusinessException("图片保存失败");
        }
        return ApiResponse.ok("上传成功", Map.of("url", "/api/files/content/" + filename));
    }

    private void validateImageContent(MultipartFile file, String extension) {
        try (ImageInputStream imageInput = ImageIO.createImageInputStream(file.getInputStream())) {
            if (imageInput == null) {
                throw new BusinessException("图片内容无法识别");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new BusinessException("图片内容无法识别");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                boolean expectedFormat = "png".equals(extension) ? "png".equals(format)
                        : "jpeg".equals(format) || "jpg".equals(format);
                if (!expectedFormat) {
                    throw new BusinessException("图片扩展名与实际格式不一致");
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw new BusinessException("图片尺寸过大或无效");
                }
            } finally {
                reader.dispose();
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new BusinessException("图片内容无法识别");
        }
    }

    @GetMapping("/content/{filename:.+}")
    public ResponseEntity<Resource> image(@PathVariable String filename) {
        if (!filename.matches("[a-f0-9-]+\\.(jpg|jpeg|png)")) {
            throw new BusinessException("图片不存在");
        }
        Path file = uploadDirectory.resolve(filename).normalize();
        if (!file.startsWith(uploadDirectory) || !Files.exists(file)) {
            throw new BusinessException("图片不存在");
        }
        try {
            String type = Files.probeContentType(file);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(type == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : type))
                    .body(new UrlResource(file.toUri()));
        } catch (IOException ex) {
            throw new BusinessException("图片读取失败");
        }
    }

    private String extension(String originalFilename) {
        String filename = originalFilename == null ? "" : originalFilename;
        int index = filename.lastIndexOf('.');
        return index < 0 ? "" : filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
