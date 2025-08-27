package com.rui.common.utils.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 工具类配置属性
 * 
 * @author ruoyi
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "rui.utils")
public class UtilsProperties {

    /**
     * 文件上传配置
     */
    private FileUpload fileUpload = new FileUpload();

    /**
     * 图片处理配置
     */
    private Image image = new Image();

    /**
     * 二维码配置
     */
    private QrCode qrCode = new QrCode();

    /**
     * 加密配置
     */
    private Crypto crypto = new Crypto();

    public FileUpload getFileUpload() {
        return fileUpload;
    }

    public void setFileUpload(FileUpload fileUpload) {
        this.fileUpload = fileUpload;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public QrCode getQrCode() {
        return qrCode;
    }

    public void setQrCode(QrCode qrCode) {
        this.qrCode = qrCode;
    }

    public Crypto getCrypto() {
        return crypto;
    }

    public void setCrypto(Crypto crypto) {
        this.crypto = crypto;
    }

    /**
     * 文件上传配置
     */
    public static class FileUpload {
        /**
         * 上传路径
         */
        private String uploadPath = "/tmp/upload";

        /**
         * 最大文件大小（MB）
         */
        private long maxFileSize = 10;

        /**
         * 允许的文件类型
         */
        private String[] allowedTypes = {"jpg", "jpeg", "png", "gif", "pdf", "doc", "docx", "xls", "xlsx"};

        public String getUploadPath() {
            return uploadPath;
        }

        public void setUploadPath(String uploadPath) {
            this.uploadPath = uploadPath;
        }

        public long getMaxFileSize() {
            return maxFileSize;
        }

        public void setMaxFileSize(long maxFileSize) {
            this.maxFileSize = maxFileSize;
        }

        public String[] getAllowedTypes() {
            return allowedTypes;
        }

        public void setAllowedTypes(String[] allowedTypes) {
            this.allowedTypes = allowedTypes;
        }
    }

    /**
     * 图片处理配置
     */
    public static class Image {
        /**
         * 默认压缩质量
         */
        private float quality = 0.8f;

        /**
         * 默认缩略图宽度
         */
        private int thumbnailWidth = 200;

        /**
         * 默认缩略图高度
         */
        private int thumbnailHeight = 200;

        public float getQuality() {
            return quality;
        }

        public void setQuality(float quality) {
            this.quality = quality;
        }

        public int getThumbnailWidth() {
            return thumbnailWidth;
        }

        public void setThumbnailWidth(int thumbnailWidth) {
            this.thumbnailWidth = thumbnailWidth;
        }

        public int getThumbnailHeight() {
            return thumbnailHeight;
        }

        public void setThumbnailHeight(int thumbnailHeight) {
            this.thumbnailHeight = thumbnailHeight;
        }
    }

    /**
     * 二维码配置
     */
    public static class QrCode {
        /**
         * 默认二维码大小
         */
        private int size = 300;

        /**
         * 默认边距
         */
        private int margin = 1;

        /**
         * 默认格式
         */
        private String format = "PNG";

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public int getMargin() {
            return margin;
        }

        public void setMargin(int margin) {
            this.margin = margin;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }
    }

    /**
     * 加密配置
     */
    public static class Crypto {
        /**
         * 默认AES密钥
         */
        private String aesKey = "RuiFramework2024";

        /**
         * 默认盐值长度
         */
        private int saltLength = 16;

        public String getAesKey() {
            return aesKey;
        }

        public void setAesKey(String aesKey) {
            this.aesKey = aesKey;
        }

        public int getSaltLength() {
            return saltLength;
        }

        public void setSaltLength(int saltLength) {
            this.saltLength = saltLength;
        }
    }
}