package com.github.lushstar.ladder.exceptions.spring.boot.autoconfigure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>description : LadderExceptionsAutoConfiguration
 *
 * <p>blog : https://blog.csdn.net/masteryourself
 *
 * @author : masteryourself
 * @version : 1.0.0
 * @date : 2020/6/22 19:38
 */
@Configuration
public class LadderExceptionsAutoConfiguration {

    @Bean
    public LadderExceptionsControllerAdvice ladderExceptionsControllerAdvice(){
        return new LadderExceptionsControllerAdvice();
    }

}
