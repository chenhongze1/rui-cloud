package com.rui.common.utils.config;

import com.rui.common.utils.compress.CompressUtils;
import com.rui.common.utils.crypto.CryptoUtils;
import com.rui.common.utils.excel.ExcelUtils;
import com.rui.common.utils.file.FileUtils;
import com.rui.common.utils.image.ImageUtils;
import com.rui.common.utils.json.JsonUtils;
import com.rui.common.utils.qrcode.QrCodeUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 工具类自动配置
 * 
 * @author ruoyi
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(UtilsProperties.class)
public class UtilsAutoConfiguration {

    /**
     * 文件工具类
     */
    @Bean
    @ConditionalOnMissingBean
    public FileUtils fileUtils() {
        return new FileUtils();
    }

    /**
     * Excel工具类
     */
    @Bean
    @ConditionalOnMissingBean
    public ExcelUtils excelUtils() {
        return new ExcelUtils();
    }

    /**
     * 图片工具类
     */
    @Bean
    @ConditionalOnMissingBean
    public ImageUtils imageUtils() {
        return new ImageUtils();
    }

    /**
     * 二维码工具类
     */
    @Bean
    @ConditionalOnMissingBean
    public QrCodeUtils qrCodeUtils() {
        return new QrCodeUtils();
    }

    /**
     * 加密工具类
     */
    @Bean
    @ConditionalOnMissingBean
    public CryptoUtils cryptoUtils() {
        return new CryptoUtils();
    }

    /**
     * 压缩工具类
     */
    @Bean
    @ConditionalOnMissingBean
    public CompressUtils compressUtils() {
        return new CompressUtils();
    }

    /**
     * JSON工具类
     */
    @Bean
    @ConditionalOnMissingBean
    public JsonUtils jsonUtils() {
        return new JsonUtils();
    }
}