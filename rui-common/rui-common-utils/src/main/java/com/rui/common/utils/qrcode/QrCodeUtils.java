package com.rui.common.utils.qrcode;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.rui.common.core.exception.ServiceException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 二维码工具类
 * 
 * @author ruoyi
 */
public class QrCodeUtils {
    
    private static final String DEFAULT_FORMAT = "PNG";
    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 300;
    private static final int DEFAULT_MARGIN = 1;
    
    /**
     * 生成二维码
     * 
     * @param content 二维码内容
     * @param width 宽度
     * @param height 高度
     * @param outputPath 输出文件路径
     */
    public static void generateQrCode(String content, int width, int height, String outputPath) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = getDefaultHints();
            
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            Path path = FileSystems.getDefault().getPath(outputPath);
            MatrixToImageWriter.writeToPath(bitMatrix, DEFAULT_FORMAT, path);
        } catch (WriterException | IOException e) {
            throw new ServiceException("生成二维码失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成二维码（默认尺寸）
     * 
     * @param content 二维码内容
     * @param outputPath 输出文件路径
     */
    public static void generateQrCode(String content, String outputPath) {
        generateQrCode(content, DEFAULT_WIDTH, DEFAULT_HEIGHT, outputPath);
    }
    
    /**
     * 生成二维码到输出流
     * 
     * @param content 二维码内容
     * @param width 宽度
     * @param height 高度
     * @param outputStream 输出流
     */
    public static void generateQrCode(String content, int width, int height, OutputStream outputStream) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = getDefaultHints();
            
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            MatrixToImageWriter.writeToStream(bitMatrix, DEFAULT_FORMAT, outputStream);
        } catch (WriterException | IOException e) {
            throw new ServiceException("生成二维码失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成二维码到字节数组
     * 
     * @param content 二维码内容
     * @param width 宽度
     * @param height 高度
     * @return 二维码字节数组
     */
    public static byte[] generateQrCodeBytes(String content, int width, int height) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            generateQrCode(content, width, height, outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new ServiceException("生成二维码失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成二维码到字节数组（默认尺寸）
     * 
     * @param content 二维码内容
     * @return 二维码字节数组
     */
    public static byte[] generateQrCodeBytes(String content) {
        return generateQrCodeBytes(content, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
    
    /**
     * 生成带Logo的二维码
     * 
     * @param content 二维码内容
     * @param width 宽度
     * @param height 高度
     * @param logoPath Logo图片路径
     * @param outputPath 输出文件路径
     */
    public static void generateQrCodeWithLogo(String content, int width, int height, 
                                             String logoPath, String outputPath) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = getDefaultHints();
            
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            
            // 添加Logo
            BufferedImage logoImage = ImageIO.read(new File(logoPath));
            BufferedImage combinedImage = addLogoToQrCode(qrImage, logoImage);
            
            ImageIO.write(combinedImage, DEFAULT_FORMAT, new File(outputPath));
        } catch (WriterException | IOException e) {
            throw new ServiceException("生成带Logo的二维码失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析二维码
     * 
     * @param imagePath 二维码图片路径
     * @return 二维码内容
     */
    public static String parseQrCode(String imagePath) {
        try {
            BufferedImage image = ImageIO.read(new File(imagePath));
            return parseQrCode(image);
        } catch (IOException e) {
            throw new ServiceException("解析二维码失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析二维码
     * 
     * @param inputStream 图片输入流
     * @return 二维码内容
     */
    public static String parseQrCode(InputStream inputStream) {
        try {
            BufferedImage image = ImageIO.read(inputStream);
            return parseQrCode(image);
        } catch (IOException e) {
            throw new ServiceException("解析二维码失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析二维码
     * 
     * @param image 二维码图片
     * @return 二维码内容
     */
    public static String parseQrCode(BufferedImage image) {
        try {
            BinaryBitmap binaryBitmap = new BinaryBitmap(
                new HybridBinarizer(new BufferedImageLuminanceSource(image))
            );
            
            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
            
            Result result = new MultiFormatReader().decode(binaryBitmap, hints);
            return result.getText();
        } catch (NotFoundException e) {
            throw new ServiceException("未找到二维码");
        } catch (Exception e) {
            throw new ServiceException("解析二维码失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证是否为有效的二维码图片
     * 
     * @param imagePath 图片路径
     * @return 是否为有效的二维码
     */
    public static boolean isValidQrCode(String imagePath) {
        try {
            parseQrCode(imagePath);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 生成彩色二维码
     * 
     * @param content 二维码内容
     * @param width 宽度
     * @param height 高度
     * @param foregroundColor 前景色
     * @param backgroundColor 背景色
     * @param outputPath 输出文件路径
     */
    public static void generateColorQrCode(String content, int width, int height,
                                          Color foregroundColor, Color backgroundColor, String outputPath) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = getDefaultHints();
            
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            BufferedImage image = createColoredQrCode(bitMatrix, foregroundColor, backgroundColor);
            
            ImageIO.write(image, DEFAULT_FORMAT, new File(outputPath));
        } catch (WriterException | IOException e) {
            throw new ServiceException("生成彩色二维码失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取默认编码参数
     */
    private static Map<EncodeHintType, Object> getDefaultHints() {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, DEFAULT_MARGIN);
        return hints;
    }
    
    /**
     * 添加Logo到二维码
     */
    private static BufferedImage addLogoToQrCode(BufferedImage qrImage, BufferedImage logoImage) {
        int qrWidth = qrImage.getWidth();
        int qrHeight = qrImage.getHeight();
        
        // Logo大小为二维码的1/5
        int logoWidth = qrWidth / 5;
        int logoHeight = qrHeight / 5;
        
        // 缩放Logo
        Image scaledLogo = logoImage.getScaledInstance(logoWidth, logoHeight, Image.SCALE_SMOOTH);
        BufferedImage scaledLogoImage = new BufferedImage(logoWidth, logoHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledLogoImage.createGraphics();
        g2d.drawImage(scaledLogo, 0, 0, null);
        g2d.dispose();
        
        // 合并图片
        BufferedImage combinedImage = new BufferedImage(qrWidth, qrHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combinedImage.createGraphics();
        g.drawImage(qrImage, 0, 0, null);
        
        // 在中心位置绘制Logo
        int logoX = (qrWidth - logoWidth) / 2;
        int logoY = (qrHeight - logoHeight) / 2;
        g.drawImage(scaledLogoImage, logoX, logoY, null);
        g.dispose();
        
        return combinedImage;
    }
    
    /**
     * 创建彩色二维码
     */
    private static BufferedImage createColoredQrCode(BitMatrix bitMatrix, Color foregroundColor, Color backgroundColor) {
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? foregroundColor.getRGB() : backgroundColor.getRGB());
            }
        }
        
        return image;
    }
    
    /**
     * 批量生成二维码
     * 
     * @param contents 二维码内容列表
     * @param width 宽度
     * @param height 高度
     * @param outputDir 输出目录
     * @param fileNamePrefix 文件名前缀
     */
    public static void batchGenerateQrCode(String[] contents, int width, int height, 
                                          String outputDir, String fileNamePrefix) {
        for (int i = 0; i < contents.length; i++) {
            String outputPath = outputDir + "/" + fileNamePrefix + "_" + (i + 1) + "." + DEFAULT_FORMAT.toLowerCase();
            generateQrCode(contents[i], width, height, outputPath);
        }
    }
}