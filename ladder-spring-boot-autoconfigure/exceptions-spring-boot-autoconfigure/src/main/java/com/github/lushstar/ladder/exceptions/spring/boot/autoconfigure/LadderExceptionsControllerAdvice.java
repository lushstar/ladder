package com.github.lushstar.ladder.exceptions.spring.boot.autoconfigure;

import com.github.lushstar.ladder.commons.exceptions.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>description : LadderExceptionsControllerAdvice
 *
 * <p>blog : https://blog.csdn.net/masteryourself
 *
 * @author : masteryourself
 * @version : 1.0.0
 * @date : 2020/6/22 19:42
 */
@ControllerAdvice
@Slf4j
public class LadderExceptionsControllerAdvice {

    @ExceptionHandler(BizException.class)
    @ResponseBody
    public Map<String, Object> bizExceptionHandler(BizException bizException) {
        log.error(bizException.getMessage(), bizException);
        Map<String, Object> result = new HashMap<>(10);
        result.put("code", bizException.getCode());
        result.put("message", bizException.getMessage());
        return result;
    }

}
