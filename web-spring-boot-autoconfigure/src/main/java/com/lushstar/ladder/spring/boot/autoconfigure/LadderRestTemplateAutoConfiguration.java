package com.lushstar.ladder.spring.boot.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.cbor.MappingJackson2CborHttpMessageConverter;
import org.springframework.http.converter.feed.AtomFeedHttpMessageConverter;
import org.springframework.http.converter.feed.RssChannelHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.JsonbHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.smile.MappingJackson2SmileHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;

/**
 * <p>description : RestTemplateAutoConfiguration
 *
 * <p>blog : https://blog.csdn.net/masteryourself
 *
 * @author : masteryourself
 * @version : 1.0.0
 * @date : 2020/3/20 23:32
 */
@Configuration
@ConditionalOnClass({RestTemplate.class})
@EnableConfigurationProperties({RestTemplateProperties.class, KeyStoreProperties.class})
@Slf4j
public class LadderRestTemplateAutoConfiguration implements EnvironmentAware, ApplicationContextAware, BeanPostProcessor {

    private static final String REST_TEMPLATE_BEAN_NAME = "restTemplate";
    private static final String LADDER_REST_TEMPLATE_BEAN_NAME = "ladderRestTemplate";
    private static final String SSL_REST_TEMPLATE_BEAN_NAME = "sslRestTemplate";

    private ApplicationContext context;

    private Environment environment;

    /**
     * 默认规则：
     * 用户自定义 > ladder.http.client.httpClient(默认) > ladder.http.client.okHttp
     *
     * @param restTemplateProperties {@link RestTemplateProperties}
     * @param keyStoreProperties     {@link KeyStoreProperties}
     * @return {@link RestTemplateFactory}
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "ladder.http.client", name = "type", havingValue = "httpClient", matchIfMissing = true)
    public RestTemplateFactory httpClientRestTemplateFactory(RestTemplateProperties restTemplateProperties, KeyStoreProperties keyStoreProperties) {
        log.info("current project has been inject [HttpClientRestTemplateFactory] component");
        return new HttpClientRestTemplateFactory(restTemplateProperties, keyStoreProperties);
    }

    /**
     * 需要开启 ladder.http.client.type=okHttp
     *
     * @param restTemplateProperties {@link RestTemplateProperties}
     * @param keyStoreProperties     {@link KeyStoreProperties}
     * @return {@link RestTemplateFactory}
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "ladder.http.client", name = "type", havingValue = "okHttp")
    public RestTemplateFactory okHttpRestTemplateFactory(RestTemplateProperties restTemplateProperties, KeyStoreProperties keyStoreProperties) {
        return new OkHttpRestTemplateFactory(restTemplateProperties, keyStoreProperties);
    }

    /**
     * 当容器中不存在 beanName 为 restTemplate 的 bean 时, 就往容器中注入一个
     *
     * @param restTemplateFactory {@link RestTemplateFactory}
     * @return {@link RestTemplate}
     */
    @Bean(REST_TEMPLATE_BEAN_NAME)
    @ConditionalOnMissingBean(name = REST_TEMPLATE_BEAN_NAME)
    public RestTemplate restTemplate(RestTemplateFactory restTemplateFactory) {
        return this.wrapper(restTemplateFactory.createRestTemplate());
    }

    /**
     * 当容器中不存在 beanName 为 ladderRestTemplate 的 bean 时, 就往容器中注入一个
     *
     * @param restTemplateFactory {@link RestTemplateFactory}
     * @return {@link RestTemplate}
     */
    @Bean(LADDER_REST_TEMPLATE_BEAN_NAME)
    @ConditionalOnMissingBean(name = LADDER_REST_TEMPLATE_BEAN_NAME)
    @ConditionalOnProperty(prefix = "ladder.http.client", name = "ladder-rest-template", havingValue = "true", matchIfMissing = true)
    public RestTemplate ladderRestTemplate(RestTemplateFactory restTemplateFactory) {
        return this.wrapper(restTemplateFactory.createRestTemplate());
    }

    /**
     * 当容器中不存在 beanName 为 sslRestTemplate 的 bean 时, 且 trustPath 或者 clientPath、clientPassword 属性存在时, 就往容器中注入一个
     *
     * @param restTemplateFactory {@link RestTemplateFactory}
     * @return {@link RestTemplate}
     */
    @Bean(SSL_REST_TEMPLATE_BEAN_NAME)
    @ConditionalOnMissingBean(name = SSL_REST_TEMPLATE_BEAN_NAME)
    @ConditionalOnExpression("('${ladder.http.keystore.trust-path:null}'!='null') ||" +
            "('${ladder.http.keystore.client-path:null}'!='null' && '${ladder.http.keystore.client-password:null}'!='null')")
    public RestTemplate sslRestTemplate(RestTemplateFactory restTemplateFactory) {
        return this.wrapper(restTemplateFactory.createSslRestTemplate());
    }

    /**
     * 包装 {@link RestTemplate}, 只会包装 Spring 组件中的扩展, 方便用户自定义扩展组件
     *
     * @param restTemplate {@link RestTemplate}
     * @return {@link RestTemplate}
     */
    private RestTemplate wrapper(RestTemplate restTemplate) {
        // 设置 ResponseErrorHandler
        for (ResponseErrorHandler responseErrorHandler : this.getBeansOfType(context, ResponseErrorHandler.class)) {
            restTemplate.setErrorHandler(responseErrorHandler);
        }
        for (HttpMessageConverter<?> httpMessageConverter : this.getBeansOfType(context, HttpMessageConverter.class)) {
            // RestTemplate 的构造函数已经包含了这几个 converter, 防止重复
            if (httpMessageConverter instanceof ByteArrayHttpMessageConverter
                    || httpMessageConverter instanceof StringHttpMessageConverter
                    || httpMessageConverter instanceof ResourceHttpMessageConverter
                    || httpMessageConverter instanceof SourceHttpMessageConverter
                    || httpMessageConverter instanceof AllEncompassingFormHttpMessageConverter
                    || httpMessageConverter instanceof AtomFeedHttpMessageConverter
                    || httpMessageConverter instanceof RssChannelHttpMessageConverter
                    || httpMessageConverter instanceof MappingJackson2XmlHttpMessageConverter
                    || httpMessageConverter instanceof Jaxb2RootElementHttpMessageConverter
                    || httpMessageConverter instanceof MappingJackson2HttpMessageConverter
                    || httpMessageConverter instanceof GsonHttpMessageConverter
                    || httpMessageConverter instanceof JsonbHttpMessageConverter
                    || httpMessageConverter instanceof MappingJackson2SmileHttpMessageConverter
                    || httpMessageConverter instanceof MappingJackson2CborHttpMessageConverter) {
                continue;
            }
            restTemplate.getMessageConverters().add(httpMessageConverter);
        }
        // 设置 ClientHttpRequestInterceptor
        for (ClientHttpRequestInterceptor interceptor : this.getBeansOfType(context, ClientHttpRequestInterceptor.class)) {
            restTemplate.getInterceptors().add(interceptor);
        }
        return restTemplate;
    }

    private <T> Collection<T> getBeansOfType(ApplicationContext applicationContext, Class<T> type) {
        return applicationContext.getBeansOfType(type).values();
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        // 返回 true 表示要替换
        if (REST_TEMPLATE_BEAN_NAME.equals(beanName) && context.containsBean(LADDER_REST_TEMPLATE_BEAN_NAME) && replaceRestTemplate()) {
            log.info("restTemplate has been replaced by ladderRestTemplate");
            return this.context.getBean(LADDER_REST_TEMPLATE_BEAN_NAME);
        }
        return bean;
    }

    /**
     * 判断是否替换 RestTemplate
     *
     * @return 返回 true 表示要替换
     */
    private boolean replaceRestTemplate() {
        // 如果 keepRestTemplate 属性设置为 false, 表示不保留原来的 RestTemplate，将会被 ladderRestTemplate 替换，方便用户一键升级
        String keepRestTemplate1 = environment.getProperty("ladder.http.client.keepRestTemplate", "true");
        String keepRestTemplate2 = environment.getProperty("ladder.http.client.keep-rest-template", "true");
        return "false".equalsIgnoreCase(keepRestTemplate1) || "false".equalsIgnoreCase(keepRestTemplate2);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

}
