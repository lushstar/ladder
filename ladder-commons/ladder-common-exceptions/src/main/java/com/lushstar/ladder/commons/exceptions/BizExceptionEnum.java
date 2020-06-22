package com.lushstar.ladder.commons.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>description : BizExceptionEnum，业务异常校验使用类，所有的自定义异常都应该在这里扩展
 *
 * <p>blog : https://blog.csdn.net/masteryourself
 *
 * @author : masteryourself
 * @version : 1.0.0
 * @date : 2020/6/22 18:42
 */
@Getter
@AllArgsConstructor
public enum BizExceptionEnum implements BizExceptionAssert {

    /**
     * 异常枚举信息定义
     */
    SYSTEM_ERROR(100000L, "系统内部异常");

    private final Long code;

    private final String message;

}
