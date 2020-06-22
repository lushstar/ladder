package com.lushstar.ladder.commons.exceptions;

/**
 * <p>description : BizAssert，此类用于判断是否为空，是否包含，字符串是否有值等等，所有的通用校验操作都应该在这里扩展
 *
 * <p>blog : https://blog.csdn.net/masteryourself
 *
 * @author : masteryourself
 * @version : 1.0.0
 * @date : 2020/6/22 18:42
 */
public interface BizAssert {

    /**
     * 抛出异常信息
     *
     * @param args 用于格式化异常信息的动态入参
     * @return {@link BizException}
     */
    BizException newException(Object... args);

    /**
     * 判断一定为空
     *
     * @param obj  校验对象
     * @param args 用于格式化异常信息的动态入参
     */
    default void isNull(Object obj, Object... args) {
        if (obj != null) {
            throw newException(args);
        }
    }

    /**
     * 判断一定不为空
     *
     * @param obj  校验对象
     * @param args 用于格式化异常信息的动态入参
     */
    default void notNull(Object obj, Object... args) {
        if (obj == null) {
            throw newException(args);
        }
    }

    /**
     * 判断为 true
     *
     * @param condition 校验表达式
     * @param args      用于格式化异常信息的动态入参
     */
    default void isTrue(boolean condition, Object... args) {
        if (!condition) {
            throw newException(args);
        }
    }

    /**
     * 判断有长度
     *
     * @param text 校验对象
     * @param args 用于格式化异常信息的动态入参
     */
    default void hasLength(String text, Object... args) {
        if (!(text != null && text.length() > 0)) {
            throw newException(args);
        }
    }

    /**
     * 判断有内容，空内容会报错
     *
     * @param text 校验对象
     * @param args 用于格式化异常信息的动态入参
     */
    default void hasText(String text, Object... args) {
        this.hasLength(text, args);
        int strLen = text.length();
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return;
            }
        }
        throw newException(args);
    }

}
