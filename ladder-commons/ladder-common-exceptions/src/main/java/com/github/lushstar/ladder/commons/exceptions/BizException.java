package com.github.lushstar.ladder.commons.exceptions;

import lombok.Data;

/**
 * <p>description : BizException，业务异常
 *
 * <p>blog : https://blog.csdn.net/masteryourself
 *
 * @author : masteryourself
 * @version : 1.0.0
 * @date : 2020/6/22 18:42
 */
@Data
public class BizException extends RuntimeException {

    /**
     * 错误 code
     */
    private Long code;

    /**
     * 错误 message
     */
    private String message;

    public BizException(Long code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

}
