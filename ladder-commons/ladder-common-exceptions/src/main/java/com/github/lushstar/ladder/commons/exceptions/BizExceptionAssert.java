package com.github.lushstar.ladder.commons.exceptions;

import java.text.MessageFormat;

/**
 * <p>description : BizExceptionAssert，用于将枚举和异常结合使用
 *
 * <p>blog : https://blog.csdn.net/masteryourself
 *
 * @author : masteryourself
 * @version : 1.0.0
 * @date : 2020/6/22 18:42
 */
public interface BizExceptionAssert extends BizAssert {

    /**
     * 错误 code
     *
     * @return {@link BizExceptionEnum#getCode()}
     */
    Long getCode();

    /**
     * 错误 message
     *
     * @return {@link BizExceptionEnum#getMessage()}
     */
    String getMessage();

    /**
     * 抛出异常信息
     *
     * @param args 用于格式化异常信息的动态入参
     * @return {@link BizException}
     */
    @Override
    default BizException newException(Object... args) {
        String msg = MessageFormat.format(this.getMessage(), args);
        return new BizException(this.getCode(), msg);
    }

}
