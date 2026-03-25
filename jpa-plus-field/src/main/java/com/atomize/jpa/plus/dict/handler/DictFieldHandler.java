package com.atomize.jpa.plus.dict.handler;

import com.atomize.jpa.plus.core.field.FieldHandler;
import com.atomize.jpa.plus.core.util.ReflectionUtils;
import com.atomize.jpa.plus.dict.annotation.Dict;
import com.atomize.jpa.plus.dict.spi.DictProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;

/**
 * 字典回写处理器
 *
 * <p>实现 {@link FieldHandler}，查询后自动将字典值翻译为标签并回写到目标字段。</p>
 *
 * <p><b>设计模式：</b>策略模式 —— 通过 {@link DictProvider} SPI 解耦字典数据获取</p>
 *
 * @author guanxiangkai
 * @since 2026年03月25日 星期三
 */
@Slf4j
@RequiredArgsConstructor
public class DictFieldHandler implements FieldHandler {

    /**
     * 字典数据提供者（由用户通过 SPI 或 Spring Bean 注入）
     */
    private final DictProvider dictProvider;

    @Override
    public int order() {
        return 200;
    }

    @Override
    public boolean supports(Field field) {
        return field.isAnnotationPresent(Dict.class);
    }

    @Override
    public void afterQuery(Object entity, Field field) {
        try {
            Dict dict = field.getAnnotation(Dict.class);
            Object dictValue = ReflectionUtils.getFieldValue(entity, field);

            dictProvider.getLabel(dict.type(), dictValue)
                    .ifPresent(label -> {
                        try {
                            String labelFieldName = dict.labelField().isEmpty()
                                    ? field.getName() + "Label"
                                    : dict.labelField();
                            Field labelField = ReflectionUtils.findField(entity.getClass(), labelFieldName);
                            if (labelField != null) {
                                ReflectionUtils.setFieldValue(entity, labelField, label);
                            }
                        } catch (Exception e) {
                            log.warn("字典标签回写失败: field={}", field.getName(), e);
                        }
                    });
        } catch (Exception e) {
            log.error("字典处理异常: field={}", field.getName(), e);
        }
    }
}

