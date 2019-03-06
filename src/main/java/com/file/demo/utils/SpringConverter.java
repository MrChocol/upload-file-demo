package com.file.demo.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.support.FormattingConversionService;

public class SpringConverter {
    private static Logger logger = LoggerFactory.getLogger(SpringConverter.class);
    private static FormattingConversionService formattingConversionService;

    public SpringConverter() {
    }

    public static void setFormattingConversionService(FormattingConversionService FormattingConversionService) {
        formattingConversionService = FormattingConversionService;
    }

    public static <T> T convert(Object value, Class<T> targetType) {
        if (value == null) {
            return null;
        } else if (formattingConversionService.canConvert(value.getClass(), targetType)) {
            return formattingConversionService.convert(value, targetType);
        } else {
            logger.debug("不能将 {} 转换为 {} 类型!", value, targetType);
            return (T) value;
        }
    }
}
