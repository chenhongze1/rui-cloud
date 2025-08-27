package com.rui.common.utils.image;

import com.rui.common.core.exception.ServiceException;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 图片处理工具类
 * 
 * @author ruoyi
 */
public class ImageUtils {
    
    /**
     * 图片压缩
     * 
     * @param inputPath 输入文件路径
     * @param outputPath 输出文件路径
     * @param quality 压缩质量 (0.0-1.0)
     */
    public static void compressImage(String inputPath, String outputPath, double quality) {
        try {
            Thumbnails.of(inputPath)
                    .scale(1.0)
                    .outputQuality(quality)
                    .toFile(outputPath);
        } catch (IOException e) {
            throw new ServiceException("图片压缩失败: " + e.getMessage());
        }
    }
    
    /**
     * 图片压缩
     * 
     * @param inputStream 输入流
     * @param outputStream 输出流
     * @param quality 压缩质量 (0.0-1.0)
     */
    public static void compressImage(InputStream inputStream, OutputStream outputStream, double quality) {
        try {
            Thumbnails.of(inputStream)
                    .scale(1.0)
                    .outputQuality(quality)
                    .toOutputStream(outputStream);
        } catch (IOException e) {
            throw new ServiceException("图片压缩失败: " + e.getMessage());
        }
    }
    
    /**
     * 图片缩放
     * 
     * @param inputPath 输入文件路径
     * @param outputPath 输出文件路径
     * @param width 目标宽度
     * @param height 目标高度
     */
    public static void resizeImage(String inputPath, String outputPath, int width, int height) {
        try {
            Thumbnails.of(inputPath)
                    .size(width, height)
                    .toFile(outputPath);
        } catch (IOException e) {
            throw new ServiceException("图片缩放失败: " + e.getMessage());
        }
    }
    
    /**
     * 图片缩放（保持比例）
     * 
     * @param inputPath 输入文件路径
     * @param outputPath 输出文件路径
     * @param scale 缩放比例
     */
    public static void scaleImage(String inputPath, String outputPath, double scale) {
        try {
            Thumbnails.of(inputPath)
                    .scale(scale)
                    .toFile(outputPath);
        } catch (IOException e) {
            throw new ServiceException("图片缩放失败: " + e.getMessage());
        }
    }
    
    /**
     * 图片裁剪
     * 
     * @param inputPath 输入文件路径
     * @param outputPath 输出文件路径
     * @param x 起始x坐标
     * @param y 起始y坐标
     * @param width 裁剪宽度
     * @param height 裁剪高度
     */
    public static void cropImage(String inputPath, String outputPath, int x, int y, int width, int height) {
        try {
            BufferedImage originalImage = ImageIO.read(new File(inputPath));
            BufferedImage croppedImage = originalImage.getSubimage(x, y, width, height);
            
            String formatName = getImageFormat(inputPath);
            ImageIO.write(croppedImage, formatName, new File(outputPath));
        } catch (IOException e) {
            throw new ServiceException("图片裁剪失败: " + e.getMessage());
        }
    }
    
    /**
     * 图片旋转
     * 
     * @param inputPath 输入文件路径
     * @param outputPath 输出文件路径
     * @param angle 旋转角度
     */
    public static void rotateImage(String inputPath, String outputPath, double angle) {
        try {
            Thumbnails.of(inputPath)
                    .scale(1.0)
                    .rotate(angle)
                    .toFile(outputPath);
        } catch (IOException e) {
            throw new ServiceException("图片旋转失败: " + e.getMessage());
        }
    }
    
    /**
     * 添加水印
     * 
     * @param inputPath 输入文件路径
     * @param outputPath 输出文件路径
     * @param watermarkPath 水印图片路径
     * @param position 水印位置
     * @param opacity 透明度 (0.0-1.0)
     */
    public static void addWatermark(String inputPath, String outputPath, String watermarkPath, 
                                   Positions position, float opacity) {
        try {
            BufferedImage watermark = ImageIO.read(new File(watermarkPath));
            
            Thumbnails.of(inputPath)
                    .scale(1.0)
                    .watermark(position, watermark, opacity)
                    .toFile(outputPath);
        } catch (IOException e) {
            throw new ServiceException("添加水印失败: " + e.getMessage());
        }
    }
    
    /**
     * 添加文字水印
     * 
     * @param inputPath 输入文件路径
     * @param outputPath 输出文件路径
     * @param text 水印文字
     * @param font 字体
     * @param color 颜色
     * @param x x坐标
     * @param y y坐标
     */
    public static void addTextWatermark(String inputPath, String outputPath, String text, 
                                       Font font, Color color, int x, int y) {
        try {
            BufferedImage originalImage = ImageIO.read(new File(inputPath));
            Graphics2D g2d = originalImage.createGraphics();
            
            // 设置抗锯齿
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setFont(font);
            g2d.setColor(color);
            
            // 绘制文字
            g2d.drawString(text, x, y);
            g2d.dispose();
            
            String formatName = getImageFormat(inputPath);
            ImageIO.write(originalImage, formatName, new File(outputPath));
        } catch (IOException e) {
            throw new ServiceException("添加文字水印失败: " + e.getMessage());
        }
    }
    
    /**
     * 图片格式转换
     * 
     * @param inputPath 输入文件路径
     * @param outputPath 输出文件路径
     * @param targetFormat 目标格式 (jpg, png, gif等)
     */
    public static void convertFormat(String inputPath, String outputPath, String targetFormat) {
        try {
            Thumbnails.of(inputPath)
                    .scale(1.0)
                    .outputFormat(targetFormat)
                    .toFile(outputPath);
        } catch (IOException e) {
            throw new ServiceException("图片格式转换失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取图片信息
     * 
     * @param imagePath 图片路径
     * @return 图片信息
     */
    public static ImageInfo getImageInfo(String imagePath) {
        try {
            BufferedImage image = ImageIO.read(new File(imagePath));
            if (image == null) {
                throw new ServiceException("无法读取图片文件");
            }
            
            Path path = Paths.get(imagePath);
            long fileSize = Files.size(path);
            String format = getImageFormat(imagePath);
            
            return new ImageInfo(image.getWidth(), image.getHeight(), format, fileSize);
        } catch (IOException e) {
            throw new ServiceException("获取图片信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取图片格式
     * 
     * @param imagePath 图片路径
     * @return 图片格式
     */
    private static String getImageFormat(String imagePath) {
        String fileName = Paths.get(imagePath).getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex != -1 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "jpg"; // 默认格式
    }
    
    /**
     * 验证是否为图片文件
     * 
     * @param filePath 文件路径
     * @return 是否为图片
     */
    public static boolean isImageFile(String filePath) {
        try {
            BufferedImage image = ImageIO.read(new File(filePath));
            return image != null;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 创建缩略图
     * 
     * @param inputPath 输入文件路径
     * @param outputPath 输出文件路径
     * @param maxWidth 最大宽度
     * @param maxHeight 最大高度
     */
    public static void createThumbnail(String inputPath, String outputPath, int maxWidth, int maxHeight) {
        try {
            Thumbnails.of(inputPath)
                    .size(maxWidth, maxHeight)
                    .keepAspectRatio(true)
                    .toFile(outputPath);
        } catch (IOException e) {
            throw new ServiceException("创建缩略图失败: " + e.getMessage());
        }
    }
    
    /**
     * 图片信息类
     */
    public static class ImageInfo {
        private final int width;
        private final int height;
        private final String format;
        private final long fileSize;
        
        public ImageInfo(int width, int height, String format, long fileSize) {
            this.width = width;
            this.height = height;
            this.format = format;
            this.fileSize = fileSize;
        }
        
        public int getWidth() {
            return width;
        }
        
        public int getHeight() {
            return height;
        }
        
        public String getFormat() {
            return format;
        }
        
        public long getFileSize() {
            return fileSize;
        }
        
        @Override
        public String toString() {
            return String.format("ImageInfo{width=%d, height=%d, format='%s', fileSize=%d}", 
                               width, height, format, fileSize);
        }
    }
}