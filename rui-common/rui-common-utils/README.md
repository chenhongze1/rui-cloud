# RUI Common Utils

通用工具模块，提供文件处理、图片处理、Excel处理、加密解密、二维码等功能。

## 功能特性

### 1. 文件工具类 (FileUtils)
- 文件上传处理
- 文件类型检测
- 文件大小验证
- 文件操作（复制、移动、删除）
- 文件内容读写

### 2. Excel工具类 (ExcelUtils)
- Excel文件导入导出
- 支持.xls和.xlsx格式
- 自定义样式设置
- 数据验证
- 批量处理

### 3. 图片工具类 (ImageUtils)
- 图片压缩
- 图片缩放和裁剪
- 图片旋转
- 添加水印（图片/文字）
- 格式转换
- 缩略图生成

### 4. 二维码工具类 (QrCodeUtils)
- 生成二维码
- 解析二维码
- 支持Logo嵌入
- 彩色二维码
- 批量生成

### 5. 加密工具类 (CryptoUtils)
- AES/RSA加密解密
- MD5/SHA哈希
- HMAC签名
- Base64编解码
- 密码加盐处理

### 6. 压缩工具类 (CompressUtils)
- ZIP压缩解压
- TAR.GZ压缩解压
- GZIP压缩解压
- 目录压缩
- 文件批量处理

### 7. JSON工具类 (JsonUtils)
- JSON序列化/反序列化
- JSON格式化
- JSON文件读写
- JSON节点操作
- 类型转换

## 使用方式

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.rui</groupId>
    <artifactId>rui-common-utils</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置属性

在`application.yml`中配置：

```yaml
rui:
  utils:
    file-upload:
      upload-path: /data/upload
      max-file-size: 50
      allowed-types: [jpg, jpeg, png, gif, pdf, doc, docx, xls, xlsx]
    image:
      quality: 0.8
      thumbnail-width: 200
      thumbnail-height: 200
    qr-code:
      size: 300
      margin: 1
      format: PNG
    crypto:
      aes-key: YourSecretKey2024
      salt-length: 16
```

### 3. 使用示例

#### 文件上传
```java
@Autowired
private FileUtils fileUtils;

public String uploadFile(MultipartFile file) {
    return fileUtils.uploadFile(file, "/upload/images/");
}
```

#### Excel导出
```java
@Autowired
private ExcelUtils excelUtils;

public void exportUsers(List<User> users, HttpServletResponse response) {
    excelUtils.exportExcel(users, "用户列表", User.class, response);
}
```

#### 图片处理
```java
@Autowired
private ImageUtils imageUtils;

public void compressImage(String inputPath, String outputPath) {
    imageUtils.compressImage(inputPath, outputPath, 0.8f);
}
```

#### 二维码生成
```java
@Autowired
private QrCodeUtils qrCodeUtils;

public byte[] generateQrCode(String content) {
    return qrCodeUtils.generateQrCode(content, 300, 300);
}
```

#### 数据加密
```java
@Autowired
private CryptoUtils cryptoUtils;

public String encryptData(String data, String key) {
    return cryptoUtils.aesEncrypt(data, key);
}
```

## 注意事项

1. 文件上传需要配置合适的上传路径和权限
2. 图片处理需要足够的内存空间
3. 加密功能请妥善保管密钥
4. Excel处理大文件时注意内存使用
5. 二维码生成建议设置合理的尺寸

## 依赖说明

- Apache POI: Excel文件处理
- Thumbnailator: 图片处理
- ZXing: 二维码处理
- Bouncy Castle: 加密算法
- Apache Tika: 文件类型检测
- Apache Commons Compress: 压缩功能
- Jackson: JSON处理