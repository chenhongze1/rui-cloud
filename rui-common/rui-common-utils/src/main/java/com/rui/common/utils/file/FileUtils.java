package com.rui.common.utils.file;

import com.rui.common.core.exception.ServiceException;
import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 文件处理工具类
 * 
 * @author ruoyi
 */
public class FileUtils {
    
    private static final Tika tika = new Tika();
    
    /**
     * 允许的图片类型
     */
    public static final List<String> IMAGE_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp", "image/webp"
    );
    
    /**
     * 允许的文档类型
     */
    public static final List<String> DOCUMENT_TYPES = Arrays.asList(
        "application/pdf", "application/msword", 
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    );
    
    /**
     * 上传文件
     * 
     * @param file 文件
     * @param uploadPath 上传路径
     * @return 文件相对路径
     */
    public static String uploadFile(MultipartFile file, String uploadPath) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("文件不能为空");
        }
        
        try {
            // 创建上传目录
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            
            // 生成文件名
            String fileName = generateFileName(file.getOriginalFilename());
            
            // 按日期分目录
            String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Path targetDir = uploadDir.resolve(dateDir);
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            
            // 保存文件
            Path targetFile = targetDir.resolve(fileName);
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
            
            return dateDir + "/" + fileName;
        } catch (IOException e) {
            throw new ServiceException("文件上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成唯一文件名
     * 
     * @param originalFilename 原始文件名
     * @return 新文件名
     */
    public static String generateFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isEmpty()) {
            return UUID.randomUUID().toString();
        }
        
        String extension = getFileExtension(originalFilename);
        return UUID.randomUUID().toString() + (extension.isEmpty() ? "" : "." + extension);
    }
    
    /**
     * 获取文件扩展名
     * 
     * @param filename 文件名
     * @return 扩展名
     */
    public static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }
    
    /**
     * 检测文件类型
     * 
     * @param file 文件
     * @return MIME类型
     */
    public static String detectFileType(MultipartFile file) {
        try {
            return tika.detect(file.getInputStream(), file.getOriginalFilename());
        } catch (IOException e) {
            throw new ServiceException("文件类型检测失败: " + e.getMessage());
        }
    }
    
    /**
     * 检测文件类型
     * 
     * @param filePath 文件路径
     * @return MIME类型
     */
    public static String detectFileType(String filePath) {
        try {
            return tika.detect(Paths.get(filePath));
        } catch (IOException e) {
            throw new ServiceException("文件类型检测失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证文件类型
     * 
     * @param file 文件
     * @param allowedTypes 允许的类型列表
     * @return 是否允许
     */
    public static boolean isAllowedFileType(MultipartFile file, List<String> allowedTypes) {
        String fileType = detectFileType(file);
        return allowedTypes.contains(fileType);
    }
    
    /**
     * 验证是否为图片文件
     * 
     * @param file 文件
     * @return 是否为图片
     */
    public static boolean isImageFile(MultipartFile file) {
        return isAllowedFileType(file, IMAGE_TYPES);
    }
    
    /**
     * 验证是否为文档文件
     * 
     * @param file 文件
     * @return 是否为文档
     */
    public static boolean isDocumentFile(MultipartFile file) {
        return isAllowedFileType(file, DOCUMENT_TYPES);
    }
    
    /**
     * 获取文件大小的可读格式
     * 
     * @param size 文件大小（字节）
     * @return 可读格式
     */
    public static String getReadableFileSize(long size) {
        if (size <= 0) return "0 B";
        
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
    
    /**
     * 删除文件
     * 
     * @param filePath 文件路径
     * @return 是否删除成功
     */
    public static boolean deleteFile(String filePath) {
        try {
            return Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 复制文件
     * 
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     */
    public static void copyFile(String sourcePath, String targetPath) {
        try {
            Path source = Paths.get(sourcePath);
            Path target = Paths.get(targetPath);
            
            // 创建目标目录
            Path targetDir = target.getParent();
            if (targetDir != null && !Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ServiceException("文件复制失败: " + e.getMessage());
        }
    }
    
    /**
     * 移动文件
     * 
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     */
    public static void moveFile(String sourcePath, String targetPath) {
        try {
            Path source = Paths.get(sourcePath);
            Path target = Paths.get(targetPath);
            
            // 创建目标目录
            Path targetDir = target.getParent();
            if (targetDir != null && !Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ServiceException("文件移动失败: " + e.getMessage());
        }
    }
    
    /**
     * 读取文件内容为字符串
     * 
     * @param filePath 文件路径
     * @return 文件内容
     */
    public static String readFileToString(String filePath) {
        try {
            return Files.readString(Paths.get(filePath));
        } catch (IOException e) {
            throw new ServiceException("文件读取失败: " + e.getMessage());
        }
    }
    
    /**
     * 写入字符串到文件
     * 
     * @param content 内容
     * @param filePath 文件路径
     */
    public static void writeStringToFile(String content, String filePath) {
        try {
            Path path = Paths.get(filePath);
            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            Files.writeString(path, content);
        } catch (IOException e) {
            throw new ServiceException("文件写入失败: " + e.getMessage());
        }
    }
}