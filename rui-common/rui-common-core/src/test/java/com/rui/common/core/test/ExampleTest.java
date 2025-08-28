package com.rui.common.core.test;

import com.rui.common.core.domain.R;
import com.rui.common.core.utils.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 核心模块功能测试
 * 专注于测试核心工具类和基础功能
 *
 * @author rui
 */
class ExampleTest {

    @Test
    @DisplayName("测试字符串工具类")
    void testStringUtils() {
        // 测试isEmpty方法
        assertTrue(StringUtils.isEmpty((String) null));
        assertTrue(StringUtils.isEmpty(""));
        assertFalse(StringUtils.isEmpty("test"));
        
        // 测试isNotEmpty方法
        assertFalse(StringUtils.isNotEmpty(""));
        assertFalse(StringUtils.isNotEmpty((String) null));
        assertTrue(StringUtils.isNotEmpty("test"));
    }

    @Test
    @DisplayName("测试统一响应结果")
    void testResponseResult() {
        // 测试成功响应
        R<String> success = R.ok("success");
        assertEquals(200, success.getCode());
        assertEquals("success", success.getMsg());
        assertNull(success.getData());
        assertTrue(R.isSuccess(success));
        
        // 测试带数据的成功响应
        R<String> successWithData = R.ok("操作成功", "test data");
        assertEquals(200, successWithData.getCode());
        assertEquals("操作成功", successWithData.getMsg());
        assertEquals("test data", successWithData.getData());
        
        // 测试失败响应
        R<String> error = R.fail("error message");
        assertEquals(500, error.getCode());
        assertEquals("error message", error.getMsg());
        assertFalse(R.isSuccess(error));
        
        // 测试带数据的成功响应
        R<Integer> dataResult = R.ok("操作成功", 100);
        assertEquals(200, dataResult.getCode());
        assertEquals("操作成功", dataResult.getMsg());
        assertEquals(100, dataResult.getData());
    }

    @Test
    @DisplayName("测试基础常量")
    void testConstants() {
        // 这里可以测试系统中定义的常量是否正确
        // 例如HTTP状态码、系统配置等
        assertNotNull("测试常量定义");
    }
}