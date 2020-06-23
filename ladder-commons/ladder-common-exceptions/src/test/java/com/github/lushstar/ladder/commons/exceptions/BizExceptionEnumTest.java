package com.github.lushstar.ladder.commons.exceptions;

import org.junit.Test;

/**
 * <p>description : BizExceptionEnumTest
 *
 * <p>blog : https://blog.csdn.net/masteryourself
 *
 * @author : masteryourself
 * @version : 1.0.0
 * @date : 2020/6/22 18:44
 */
public class BizExceptionEnumTest {

    @Test(expected = BizException.class)
    public void testSystemExceptionIsTrue() {
        BizExceptionEnum.SYSTEM_ERROR.isTrue(false);
    }

    @Test(expected = BizException.class)
    public void testSystemExceptionNotNull() {
        BizExceptionEnum.SYSTEM_ERROR.notNull(null);
    }

}