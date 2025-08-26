package com.rui.common.redis.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Redis使用FastJson序列化
 *
 * @author rui
 */
public class FastJson2JsonRedisSerializer<T> implements RedisSerializer<T> {
    
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private Class<T> clazz;

    static {
        JSON.config(JSONReader.Feature.FieldBased, JSONReader.Feature.SupportArrayToBean);
    }

    public FastJson2JsonRedisSerializer(Class<T> clazz) {
        super();
        this.clazz = clazz;
    }

    @Override
    public byte[] serialize(T t) throws SerializationException {
        if (t == null) {
            return new byte[0];
        }
        return JSON.toJSONString(t, JSONWriter.Feature.WriteClassName).getBytes(DEFAULT_CHARSET);
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length <= 0) {
            return null;
        }
        String str = new String(bytes, DEFAULT_CHARSET);

        return JSON.parseObject(str, clazz, JSONReader.Feature.SupportAutoType);
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        // FastJson2不需要ObjectMapper，此方法为兼容性保留
    }

    protected JavaType getJavaType(Class<?> clazz) {
        return null;
    }

    /**
     * 兼容性接口
     */
    public static class JavaType {
        // 兼容性保留
    }
}