package com.rui.common.utils.excel;

import com.rui.common.core.exception.ServiceException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Excel工具类
 * 
 * @author ruoyi
 */
public class ExcelUtils {
    
    /**
     * 导出Excel
     * 
     * @param data 数据列表
     * @param headers 表头
     * @param fileName 文件名
     * @param response HTTP响应
     */
    public static <T> void exportExcel(List<T> data, String[] headers, String fileName, HttpServletResponse response) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderStyle(workbook);
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 填充数据
            if (data != null && !data.isEmpty()) {
                Class<?> clazz = data.get(0).getClass();
                Field[] fields = clazz.getDeclaredFields();
                
                for (int i = 0; i < data.size(); i++) {
                    Row row = sheet.createRow(i + 1);
                    T item = data.get(i);
                    
                    for (int j = 0; j < Math.min(fields.length, headers.length); j++) {
                        Cell cell = row.createCell(j);
                        fields[j].setAccessible(true);
                        
                        try {
                            Object value = fields[j].get(item);
                            setCellValue(cell, value);
                        } catch (IllegalAccessException e) {
                            cell.setCellValue("");
                        }
                    }
                }
            }
            
            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 设置响应头
            setResponseHeaders(response, fileName);
            
            // 写入响应
            workbook.write(response.getOutputStream());
            
        } catch (IOException e) {
            throw new ServiceException("Excel导出失败: " + e.getMessage());
        }
    }
    
    /**
     * 导入Excel
     * 
     * @param file Excel文件
     * @param clazz 目标类型
     * @return 数据列表
     */
    public static <T> List<T> importExcel(MultipartFile file, Class<T> clazz) {
        List<T> result = new ArrayList<>();
        
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = createWorkbook(inputStream, file.getOriginalFilename())) {
            
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return result;
            }
            
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
            }
            
            // 跳过表头，从第二行开始读取数据
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                
                T instance = clazz.getDeclaredConstructor().newInstance();
                
                for (int j = 0; j < Math.min(fields.length, row.getLastCellNum()); j++) {
                    Cell cell = row.getCell(j);
                    if (cell != null) {
                        Object value = getCellValue(cell, fields[j].getType());
                        fields[j].set(instance, value);
                    }
                }
                
                result.add(instance);
            }
            
        } catch (Exception e) {
            throw new ServiceException("Excel导入失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 读取Excel数据为Map列表
     * 
     * @param file Excel文件
     * @param headers 表头映射
     * @return 数据列表
     */
    public static List<Map<String, Object>> importExcelAsMap(MultipartFile file, String[] headers) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = createWorkbook(inputStream, file.getOriginalFilename())) {
            
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return result;
            }
            
            // 跳过表头，从第二行开始读取数据
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                
                Map<String, Object> rowData = new HashMap<>();
                
                for (int j = 0; j < Math.min(headers.length, row.getLastCellNum()); j++) {
                    Cell cell = row.getCell(j);
                    Object value = getCellValue(cell, Object.class);
                    rowData.put(headers[j], value);
                }
                
                result.add(rowData);
            }
            
        } catch (Exception e) {
            throw new ServiceException("Excel导入失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 创建工作簿
     */
    private static Workbook createWorkbook(InputStream inputStream, String fileName) throws IOException {
        if (fileName.endsWith(".xlsx")) {
            return new XSSFWorkbook(inputStream);
        } else if (fileName.endsWith(".xls")) {
            return new HSSFWorkbook(inputStream);
        } else {
            throw new ServiceException("不支持的文件格式，请使用.xls或.xlsx文件");
        }
    }
    
    /**
     * 创建表头样式
     */
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        
        return style;
    }
    
    /**
     * 设置单元格值
     */
    private static void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof Date) {
            cell.setCellValue((Date) value);
        } else if (value instanceof LocalDateTime) {
            cell.setCellValue(((LocalDateTime) value).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        } else {
            cell.setCellValue(value.toString());
        }
    }
    
    /**
     * 获取单元格值
     */
    private static Object getCellValue(Cell cell, Class<?> targetType) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                String stringValue = cell.getStringCellValue();
                if (targetType == String.class) {
                    return stringValue;
                } else if (targetType == Integer.class || targetType == int.class) {
                    return Integer.parseInt(stringValue);
                } else if (targetType == Long.class || targetType == long.class) {
                    return Long.parseLong(stringValue);
                } else if (targetType == Double.class || targetType == double.class) {
                    return Double.parseDouble(stringValue);
                } else if (targetType == Boolean.class || targetType == boolean.class) {
                    return Boolean.parseBoolean(stringValue);
                }
                return stringValue;
                
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    if (targetType == Integer.class || targetType == int.class) {
                        return (int) numericValue;
                    } else if (targetType == Long.class || targetType == long.class) {
                        return (long) numericValue;
                    } else if (targetType == String.class) {
                        return String.valueOf(numericValue);
                    }
                    return numericValue;
                }
                
            case BOOLEAN:
                return cell.getBooleanCellValue();
                
            case FORMULA:
                return cell.getCellFormula();
                
            default:
                return null;
        }
    }
    
    /**
     * 设置响应头
     */
    private static void setResponseHeaders(HttpServletResponse response, String fileName) throws UnsupportedEncodingException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName + ".xlsx");
    }
    
    /**
     * 验证Excel文件
     * 
     * @param file 文件
     * @return 是否为有效的Excel文件
     */
    public static boolean isValidExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        
        String fileName = file.getOriginalFilename();
        return fileName != null && (fileName.endsWith(".xls") || fileName.endsWith(".xlsx"));
    }
    
    /**
     * 获取Excel文件的工作表数量
     * 
     * @param file Excel文件
     * @return 工作表数量
     */
    public static int getSheetCount(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = createWorkbook(inputStream, file.getOriginalFilename())) {
            
            return workbook.getNumberOfSheets();
            
        } catch (Exception e) {
            throw new ServiceException("获取工作表数量失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取Excel文件的工作表名称列表
     * 
     * @param file Excel文件
     * @return 工作表名称列表
     */
    public static List<String> getSheetNames(MultipartFile file) {
        List<String> sheetNames = new ArrayList<>();
        
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = createWorkbook(inputStream, file.getOriginalFilename())) {
            
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                sheetNames.add(workbook.getSheetName(i));
            }
            
        } catch (Exception e) {
            throw new ServiceException("获取工作表名称失败: " + e.getMessage());
        }
        
        return sheetNames;
    }
}