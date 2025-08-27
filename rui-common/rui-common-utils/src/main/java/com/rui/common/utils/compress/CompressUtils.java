package com.rui.common.utils.compress;

import com.rui.common.core.exception.ServiceException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * 压缩解压工具类
 * 
 * @author ruoyi
 */
public class CompressUtils {
    
    private static final int BUFFER_SIZE = 8192;
    
    /**
     * 压缩文件为ZIP格式
     * 
     * @param sourceFile 源文件或目录
     * @param zipFile 目标ZIP文件
     */
    public static void zipFile(String sourceFile, String zipFile) {
        zipFile(Paths.get(sourceFile), Paths.get(zipFile));
    }
    
    /**
     * 压缩文件为ZIP格式
     * 
     * @param sourcePath 源文件或目录路径
     * @param zipPath 目标ZIP文件路径
     */
    public static void zipFile(Path sourcePath, Path zipPath) {
        try {
            // 确保目标目录存在
            Files.createDirectories(zipPath.getParent());
            
            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
                if (Files.isDirectory(sourcePath)) {
                    zipDirectory(sourcePath, sourcePath, zos);
                } else {
                    zipSingleFile(sourcePath, sourcePath.getFileName().toString(), zos);
                }
            }
        } catch (IOException e) {
            throw new ServiceException("压缩文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 解压ZIP文件
     * 
     * @param zipFile ZIP文件路径
     * @param destDir 目标目录
     */
    public static void unzipFile(String zipFile, String destDir) {
        unzipFile(Paths.get(zipFile), Paths.get(destDir));
    }
    
    /**
     * 解压ZIP文件
     * 
     * @param zipPath ZIP文件路径
     * @param destPath 目标目录路径
     */
    public static void unzipFile(Path zipPath, Path destPath) {
        try {
            // 确保目标目录存在
            Files.createDirectories(destPath);
            
            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path entryPath = destPath.resolve(entry.getName());
                    
                    // 防止目录遍历攻击
                    if (!entryPath.normalize().startsWith(destPath.normalize())) {
                        throw new ServiceException("不安全的ZIP条目: " + entry.getName());
                    }
                    
                    if (entry.isDirectory()) {
                        Files.createDirectories(entryPath);
                    } else {
                        Files.createDirectories(entryPath.getParent());
                        try (OutputStream os = Files.newOutputStream(entryPath)) {
                            copyStream(zis, os);
                        }
                    }
                    zis.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new ServiceException("解压文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 压缩文件为TAR.GZ格式
     * 
     * @param sourceFile 源文件或目录
     * @param tarGzFile 目标TAR.GZ文件
     */
    public static void tarGzFile(String sourceFile, String tarGzFile) {
        tarGzFile(Paths.get(sourceFile), Paths.get(tarGzFile));
    }
    
    /**
     * 压缩文件为TAR.GZ格式
     * 
     * @param sourcePath 源文件或目录路径
     * @param tarGzPath 目标TAR.GZ文件路径
     */
    public static void tarGzFile(Path sourcePath, Path tarGzPath) {
        try {
            // 确保目标目录存在
            Files.createDirectories(tarGzPath.getParent());
            
            try (OutputStream fos = Files.newOutputStream(tarGzPath);
                 GzipCompressorOutputStream gzos = new GzipCompressorOutputStream(fos);
                 TarArchiveOutputStream taos = new TarArchiveOutputStream(gzos)) {
                
                if (Files.isDirectory(sourcePath)) {
                    tarDirectory(sourcePath, sourcePath, taos);
                } else {
                    tarSingleFile(sourcePath, sourcePath.getFileName().toString(), taos);
                }
            }
        } catch (IOException e) {
            throw new ServiceException("压缩TAR.GZ文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 解压TAR.GZ文件
     * 
     * @param tarGzFile TAR.GZ文件路径
     * @param destDir 目标目录
     */
    public static void unTarGzFile(String tarGzFile, String destDir) {
        unTarGzFile(Paths.get(tarGzFile), Paths.get(destDir));
    }
    
    /**
     * 解压TAR.GZ文件
     * 
     * @param tarGzPath TAR.GZ文件路径
     * @param destPath 目标目录路径
     */
    public static void unTarGzFile(Path tarGzPath, Path destPath) {
        try {
            // 确保目标目录存在
            Files.createDirectories(destPath);
            
            try (InputStream fis = Files.newInputStream(tarGzPath);
                 GzipCompressorInputStream gzis = new GzipCompressorInputStream(fis);
                 TarArchiveInputStream tais = new TarArchiveInputStream(gzis)) {
                
                TarArchiveEntry entry;
                while ((entry = tais.getNextTarEntry()) != null) {
                    Path entryPath = destPath.resolve(entry.getName());
                    
                    // 防止目录遍历攻击
                    if (!entryPath.normalize().startsWith(destPath.normalize())) {
                        throw new ServiceException("不安全的TAR条目: " + entry.getName());
                    }
                    
                    if (entry.isDirectory()) {
                        Files.createDirectories(entryPath);
                    } else {
                        Files.createDirectories(entryPath.getParent());
                        try (OutputStream os = Files.newOutputStream(entryPath)) {
                            copyStream(tais, os);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new ServiceException("解压TAR.GZ文件失败: " + e.getMessage());
        }
    }
    
    /**
     * GZIP压缩单个文件
     * 
     * @param sourceFile 源文件
     * @param gzFile 目标GZ文件
     */
    public static void gzipFile(String sourceFile, String gzFile) {
        gzipFile(Paths.get(sourceFile), Paths.get(gzFile));
    }
    
    /**
     * GZIP压缩单个文件
     * 
     * @param sourcePath 源文件路径
     * @param gzPath 目标GZ文件路径
     */
    public static void gzipFile(Path sourcePath, Path gzPath) {
        try {
            // 确保目标目录存在
            Files.createDirectories(gzPath.getParent());
            
            try (InputStream fis = Files.newInputStream(sourcePath);
                 OutputStream fos = Files.newOutputStream(gzPath);
                 GzipCompressorOutputStream gzos = new GzipCompressorOutputStream(fos)) {
                
                copyStream(fis, gzos);
            }
        } catch (IOException e) {
            throw new ServiceException("GZIP压缩失败: " + e.getMessage());
        }
    }
    
    /**
     * GZIP解压文件
     * 
     * @param gzFile GZ文件路径
     * @param destFile 目标文件
     */
    public static void gunzipFile(String gzFile, String destFile) {
        gunzipFile(Paths.get(gzFile), Paths.get(destFile));
    }
    
    /**
     * GZIP解压文件
     * 
     * @param gzPath GZ文件路径
     * @param destPath 目标文件路径
     */
    public static void gunzipFile(Path gzPath, Path destPath) {
        try {
            // 确保目标目录存在
            Files.createDirectories(destPath.getParent());
            
            try (InputStream fis = Files.newInputStream(gzPath);
                 GzipCompressorInputStream gzis = new GzipCompressorInputStream(fis);
                 OutputStream fos = Files.newOutputStream(destPath)) {
                
                copyStream(gzis, fos);
            }
        } catch (IOException e) {
            throw new ServiceException("GZIP解压失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取ZIP文件中的条目列表
     * 
     * @param zipFile ZIP文件路径
     * @return 条目名称列表
     */
    public static List<String> listZipEntries(String zipFile) {
        return listZipEntries(Paths.get(zipFile));
    }
    
    /**
     * 获取ZIP文件中的条目列表
     * 
     * @param zipPath ZIP文件路径
     * @return 条目名称列表
     */
    public static List<String> listZipEntries(Path zipPath) {
        List<String> entries = new ArrayList<>();
        try (ZipArchiveInputStream zis = new ZipArchiveInputStream(Files.newInputStream(zipPath))) {
            ZipArchiveEntry entry;
            while ((entry = zis.getNextZipEntry()) != null) {
                entries.add(entry.getName());
            }
        } catch (IOException e) {
            throw new ServiceException("读取ZIP文件条目失败: " + e.getMessage());
        }
        return entries;
    }
    
    /**
     * 检查文件是否为ZIP格式
     * 
     * @param filePath 文件路径
     * @return 是否为ZIP格式
     */
    public static boolean isZipFile(String filePath) {
        return isZipFile(Paths.get(filePath));
    }
    
    /**
     * 检查文件是否为ZIP格式
     * 
     * @param path 文件路径
     * @return 是否为ZIP格式
     */
    public static boolean isZipFile(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            byte[] header = new byte[4];
            int bytesRead = is.read(header);
            if (bytesRead >= 4) {
                // ZIP文件的魔数是 0x504B0304 或 0x504B0506
                return (header[0] == 0x50 && header[1] == 0x4B && 
                       (header[2] == 0x03 || header[2] == 0x05) && 
                       (header[3] == 0x04 || header[3] == 0x06));
            }
        } catch (IOException e) {
            // 忽略异常，返回false
        }
        return false;
    }
    
    /**
     * 检查文件是否为GZIP格式
     * 
     * @param filePath 文件路径
     * @return 是否为GZIP格式
     */
    public static boolean isGzipFile(String filePath) {
        return isGzipFile(Paths.get(filePath));
    }
    
    /**
     * 检查文件是否为GZIP格式
     * 
     * @param path 文件路径
     * @return 是否为GZIP格式
     */
    public static boolean isGzipFile(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            byte[] header = new byte[2];
            int bytesRead = is.read(header);
            if (bytesRead >= 2) {
                // GZIP文件的魔数是 0x1F8B
                return header[0] == (byte) 0x1F && header[1] == (byte) 0x8B;
            }
        } catch (IOException e) {
            // 忽略异常，返回false
        }
        return false;
    }
    
    /**
     * 压缩目录到ZIP
     */
    private static void zipDirectory(Path sourceDir, Path baseDir, ZipOutputStream zos) throws IOException {
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String entryName = baseDir.relativize(file).toString().replace('\\', '/');
                zipSingleFile(file, entryName, zos);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!dir.equals(sourceDir)) {
                    String entryName = baseDir.relativize(dir).toString().replace('\\', '/') + "/";
                    ZipEntry entry = new ZipEntry(entryName);
                    zos.putNextEntry(entry);
                    zos.closeEntry();
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    /**
     * 压缩单个文件到ZIP
     */
    private static void zipSingleFile(Path file, String entryName, ZipOutputStream zos) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        
        try (InputStream is = Files.newInputStream(file)) {
            copyStream(is, zos);
        }
        
        zos.closeEntry();
    }
    
    /**
     * 压缩目录到TAR
     */
    private static void tarDirectory(Path sourceDir, Path baseDir, TarArchiveOutputStream taos) throws IOException {
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String entryName = baseDir.relativize(file).toString().replace('\\', '/');
                tarSingleFile(file, entryName, taos);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!dir.equals(sourceDir)) {
                    String entryName = baseDir.relativize(dir).toString().replace('\\', '/') + "/";
                    TarArchiveEntry entry = new TarArchiveEntry(entryName);
                    entry.setModTime(attrs.lastModifiedTime().toMillis());
                    taos.putArchiveEntry(entry);
                    taos.closeArchiveEntry();
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    /**
     * 压缩单个文件到TAR
     */
    private static void tarSingleFile(Path file, String entryName, TarArchiveOutputStream taos) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(entryName);
        entry.setSize(Files.size(file));
        entry.setModTime(Files.getLastModifiedTime(file).toMillis());
        taos.putArchiveEntry(entry);
        
        try (InputStream is = Files.newInputStream(file)) {
            copyStream(is, taos);
        }
        
        taos.closeArchiveEntry();
    }
    
    /**
     * 复制流
     */
    private static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }
}