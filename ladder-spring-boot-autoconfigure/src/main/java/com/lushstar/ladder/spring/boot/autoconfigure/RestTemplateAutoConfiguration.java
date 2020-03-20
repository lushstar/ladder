package com.lushstar.ladder.spring.boot.autoconfigure;

import com.lushstar.ladder.http.RestTemplateFactory;
import com.lushstar.ladder.http.RestTemplateHttpClientFactory;
import com.lushstar.ladder.http.RestTemplateProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
 * <p>blog : https://Blog.csdn.net/masteryourself
 *
 * @author : masteryourself
 * @version : 1.0.0
 * @date : 2020/3/20 23:32
 */
@Configuration
@ConditionalOnClass({RestTemplate.class})
@EnableConfigurationProperties(RestTemplateProperties.class)
@Slf4j
public class RestTemplateAutoConfiguration implements EnvironmentAware, ApplicationContextAware, BeanPostProcessor/*, DisposableBean*/ {

    private static final String REST_TEMPLATE_BEAN_NAME = "restTemplate";
    private static final String LADDER_REST_TEMPLATE_BEAN_NAME = "ladderRestTemplate";
    private static final String SSL_REST_TEMPLATE_BEAN_NAME = "sslRestTemplate";

    private ApplicationContext context;

    private Environment environment;

    private RestTemplateProperties restTemplateProperties;

    public RestTemplateAutoConfiguration(RestTemplateProperties restTemplateProperties) {
        this.restTemplateProperties = restTemplateProperties;
    }

    /**
     * 用户可以自定义 RestTemplateFactory
     *
     * @return HttpClientPoolingFactory
     */
    @Bean
    @ConditionalOnMissingBean
    public RestTemplateFactory restTemplateFactory() {
        return new RestTemplateHttpClientFactory(restTemplateProperties);
    }

    /**
     * 当容器中不存在 restTemplate 时, 就往容器中注入一个
     *
     * @param restTemplateFactory restTemplateFactory
     * @return {@link RestTemplate}
     */
    @Bean(REST_TEMPLATE_BEAN_NAME)
    @ConditionalOnMissingBean(name = REST_TEMPLATE_BEAN_NAME)
    public RestTemplate restTemplate(RestTemplateFactory restTemplateFactory) {
        return this.wrapper(restTemplateFactory.createRestTemplate());
    }

    /**
     * 如果容器中存在 restTemplate, 就向容器中注入一个 ladderRestTemplate, 方便用户切换
     *
     * @param restTemplateFactory restTemplateFactory
     * @return {@link RestTemplate}
     */
    @Bean(LADDER_REST_TEMPLATE_BEAN_NAME)
    @ConditionalOnMissingBean(name = LADDER_REST_TEMPLATE_BEAN_NAME)
    public RestTemplate ladderRestTemplate(RestTemplateFactory restTemplateFactory) {
        return this.wrapper(restTemplateFactory.createRestTemplate());
    }

    /**
     * 给容器中注入一个 sslRestTemplate， 方便用户发起 Https 请求
     * 只有配置了证书路径和秘钥才会自动产生
     *
     * @param restTemplateFactory restTemplateFactory
     * @return {@link RestTemplate}
     */
    @Bean(SSL_REST_TEMPLATE_BEAN_NAME)
    @ConditionalOnMissingBean(name = SSL_REST_TEMPLATE_BEAN_NAME)
    @ConditionalOnExpression("('${ladder.rest.template.keyStoreProperties.serverPath:null}'!='null' && '${ladder.rest.template.keyStoreProperties.serverPassword:null}'!='null') ||" +
            "('${ladder.rest.template.keyStoreProperties.clientPath:null}'!='null' && '${ladder.rest.template.keyStoreProperties.clientPassword:null}'!='null')")
    public RestTemplate sslRestTemplate(RestTemplateFactory restTemplateFactory) {
        return this.wrapper(restTemplateFactory.createSslRestTemplate());
    }

    private RestTemplate wrapper(RestTemplate restTemplate) {
        // 扩展用户的 RestTemplate 组件, 如转换器、拦截器等
        for (ResponseErrorHandler responseErrorHandler : this.getBeansOfType(context, ResponseErrorHandler.class)) {
            restTemplate.setErrorHandler(responseErrorHandler);
        }
        for (HttpMessageConverter<?> httpMessageConverter : this.getBeansOfType(context, HttpMessageConverter.class)) {
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
        String keepRestTemplate = environment.getProperty("ladder.rest.template.keep", "true");
        // 如果设置替换项目中 restTemplate, 那么项目中的 restTemplate 将会被替换为 ladderRestTemplate
        if ("false".equalsIgnoreCase(keepRestTemplate)) {
            if (REST_TEMPLATE_BEAN_NAME.equals(beanName) && context.containsBean(LADDER_REST_TEMPLATE_BEAN_NAME)) {
                log.info("restTemplate has been replaced by rdfaRestTemplate");
                return this.context.getBean(LADDER_REST_TEMPLATE_BEAN_NAME);
            }
        }
        return bean;
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
