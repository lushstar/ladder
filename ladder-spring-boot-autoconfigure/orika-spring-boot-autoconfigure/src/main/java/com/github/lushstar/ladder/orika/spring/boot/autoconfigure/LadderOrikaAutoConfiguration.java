package com.github.lushstar.ladder.orika.spring.boot.autoconfigure;

import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>description : LadderOrikaAutoConfiguration
 *
 * <p>blog : https://blog.csdn.net/masteryourself
 *
 * @author : masteryourself
 * @version : 1.0.0
 * @date : 2020/6/26 16:29
 */
@Configuration
@ConditionalOnClass({MapperFactory.class})
public class LadderOrikaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MapperFactory.class)
    public MapperFactory getFactory() {
        return new DefaultMapperFactory.Builder().build();
    }

    @Bean
    @ConditionalOnMissingBean(MapperFacade.class)
    public MapperFacade mapperFacade() {
        return getFactory().getMapperFacade();
    }

}
