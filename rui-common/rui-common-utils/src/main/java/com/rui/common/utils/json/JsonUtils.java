package com.rui.common.utils.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rui.common.core.exception.ServiceException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

/**
 * JSON处理工具类
 * 
 * @author ruoyi
 */
public class JsonUtils {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    static {
        // 配置ObjectMapper
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        
        // 注册Java 8时间模块
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        
        // 设置日期格式
        OBJECT_MAPPER.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    }
    
    /**
     * 对象转JSON字符串
     * 
     * @param object 对象
     * @return JSON字符串
     */
    public static String toJsonString(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new ServiceException("对象转JSON失败: " + e.getMessage());
        }
    }
    
    /**
     * 对象转格式化的JSON字符串
     * 
     * @param object 对象
     * @return 格式化的JSON字符串
     */
    public static String toPrettyJsonString(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new ServiceException("对象转格式化JSON失败: " + e.getMessage());
        }
    }
    
    /**
     * JSON字符串转对象
     * 
     * @param jsonString JSON字符串
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 对象
     */
    public static <T> T parseObject(String jsonString, Class<T> clazz) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(jsonString, clazz);
        } catch (JsonProcessingException e) {
            throw new ServiceException("JSON转对象失败: " + e.getMessage());
        }
    }
    
    /**
     * JSON字符串转对象（支持泛型）
     * 
     * @param jsonString JSON字符串
     * @param typeReference 类型引用
     * @param <T> 泛型类型
     * @return 对象
     */
    public static <T> T parseObject(String jsonString, TypeReference<T> typeReference) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(jsonString, typeReference);
        } catch (JsonProcessingException e) {
            throw new ServiceException("JSON转对象失败: " + e.getMessage());
        }
    }
    
    /**
     * JSON字符串转List
     * 
     * @param jsonString JSON字符串
     * @param clazz 元素类型
     * @param <T> 泛型类型
     * @return List对象
     */
    public static <T> List<T> parseArray(String jsonString, Class<T> clazz) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(jsonString, 
                OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException e) {
            throw new ServiceException("JSON转List失败: " + e.getMessage());
        }
    }
    
    /**
     * JSON字符串转Map
     * 
     * @param jsonString JSON字符串
     * @return Map对象
     */
    public static Map<String, Object> parseMap(String jsonString) {
        return parseObject(jsonString, new TypeReference<Map<String, Object>>() {});
    }
    
    /**
     * 从文件读取JSON并转换为对象
     * 
     * @param file JSON文件
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 对象
     */
    public static <T> T parseObjectFromFile(File file, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(file, clazz);
        } catch (IOException e) {
            throw new ServiceException("从文件读取JSON失败: " + e.getMessage());
        }
    }
    
    /**
     * 从输入流读取JSON并转换为对象
     * 
     * @param inputStream 输入流
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 对象
     */
    public static <T> T parseObjectFromStream(InputStream inputStream, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(inputStream, clazz);
        } catch (IOException e) {
            throw new ServiceException("从输入流读取JSON失败: " + e.getMessage());
        }
    }
    
    /**
     * 将对象写入JSON文件
     * 
     * @param object 对象
     * @param file 目标文件
     */
    public static void writeToFile(Object object, File file) {
        try {
            OBJECT_MAPPER.writeValue(file, object);
        } catch (IOException e) {
            throw new ServiceException("写入JSON文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 将对象写入格式化的JSON文件
     * 
     * @param object 对象
     * @param file 目标文件
     */
    public static void writePrettyToFile(Object object, File file) {
        try {
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, object);
        } catch (IOException e) {
            throw new ServiceException("写入格式化JSON文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取JSON节点
     * 
     * @param jsonString JSON字符串
     * @return JsonNode
     */
    public static JsonNode getJsonNode(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readTree(jsonString);
        } catch (JsonProcessingException e) {
            throw new ServiceException("解析JSON节点失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取JSON节点的值
     * 
     * @param jsonString JSON字符串
     * @param path 节点路径（支持嵌套，如 "user.name"）
     * @return 节点值
     */
    public static String getJsonValue(String jsonString, String path) {
        JsonNode rootNode = getJsonNode(jsonString);
        if (rootNode == null) {
            return null;
        }
        
        JsonNode node = rootNode;
        String[] pathParts = path.split("\\.");
        
        for (String part : pathParts) {
            node = node.get(part);
            if (node == null) {
                return null;
            }
        }
        
        return node.isTextual() ? node.textValue() : node.toString();
    }
    
    /**
     * 检查字符串是否为有效的JSON
     * 
     * @param jsonString 待检查的字符串
     * @return 是否为有效JSON
     */
    public static boolean isValidJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return false;
        }
        try {
            OBJECT_MAPPER.readTree(jsonString);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }
    
    /**
     * 检查字符串是否为有效的JSON对象
     * 
     * @param jsonString 待检查的字符串
     * @return 是否为有效JSON对象
     */
    public static boolean isValidJsonObject(String jsonString) {
        if (!isValidJson(jsonString)) {
            return false;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(jsonString);
            return node.isObject();
        } catch (JsonProcessingException e) {
            return false;
        }
    }
    
    /**
     * 检查字符串是否为有效的JSON数组
     * 
     * @param jsonString 待检查的字符串
     * @return 是否为有效JSON数组
     */
    public static boolean isValidJsonArray(String jsonString) {
        if (!isValidJson(jsonString)) {
            return false;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(jsonString);
            return node.isArray();
        } catch (JsonProcessingException e) {
            return false;
        }
    }
    
    /**
     * 合并两个JSON对象
     * 
     * @param json1 第一个JSON字符串
     * @param json2 第二个JSON字符串
     * @return 合并后的JSON字符串
     */
    public static String mergeJson(String json1, String json2) {
        try {
            JsonNode node1 = OBJECT_MAPPER.readTree(json1);
            JsonNode node2 = OBJECT_MAPPER.readTree(json2);
            
            if (!node1.isObject() || !node2.isObject()) {
                throw new ServiceException("只能合并JSON对象");
            }
            
            // 创建合并后的对象
            Map<String, Object> merged = OBJECT_MAPPER.convertValue(node1, Map.class);
            Map<String, Object> toMerge = OBJECT_MAPPER.convertValue(node2, Map.class);
            
            merged.putAll(toMerge);
            
            return toJsonString(merged);
        } catch (JsonProcessingException e) {
            throw new ServiceException("合并JSON失败: " + e.getMessage());
        }
    }
    
    /**
     * 深度复制对象（通过JSON序列化/反序列化）
     * 
     * @param object 源对象
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 复制的对象
     */
    public static <T> T deepCopy(Object object, Class<T> clazz) {
        if (object == null) {
            return null;
        }
        String json = toJsonString(object);
        return parseObject(json, clazz);
    }
    
    /**
     * 对象转换（通过JSON序列化/反序列化）
     * 
     * @param object 源对象
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 转换后的对象
     */
    public static <T> T convertValue(Object object, Class<T> clazz) {
        if (object == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(object, clazz);
        } catch (IllegalArgumentException e) {
            throw new ServiceException("对象转换失败: " + e.getMessage());
        }
    }
    
    /**
     * 对象转换（支持泛型）
     * 
     * @param object 源对象
     * @param typeReference 类型引用
     * @param <T> 泛型类型
     * @return 转换后的对象
     */
    public static <T> T convertValue(Object object, TypeReference<T> typeReference) {
        if (object == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(object, typeReference);
        } catch (IllegalArgumentException e) {
            throw new ServiceException("对象转换失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取ObjectMapper实例
     * 
     * @return ObjectMapper实例
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }
}