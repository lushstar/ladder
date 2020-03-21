package com.lushstar.ladder.http;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>description : RestTemplateProperties
 *
 * <p>blog : https://Blog.csdn.net/masteryourself
 *
 * @author : masteryourself
 * @version : 1.0.0
 * @date : 2020/3/20 23:21
 */
@Data
@ConfigurationProperties(prefix = "ladder.rest.template")
public class RestTemplateProperties {

    /**
     * 连接建立成功的超时时间 5s, 否则无限大
     */
    private Integer connectTimeout = 5000;

    /**
     * 读取数据的响应超时时间 5s, 否则无限大
     */
    private Integer socketTimeout = 5000;

    /**
     * 允许连接保持的活动时间 15分钟, 默认 Long.MAX_VALUE
     * 此参数值参考 spring cloud feign timeToLive 属性
     */
    private Integer keeAliveTimeMillis = 900000;

    /**
     * 关闭空闲超过 30s 的连接
     * 此参数值参考 HttpClient 官网例子
     */
    private Integer idleTimeout = 30;

    /**
     * 总共连接数
     * 此参数值参考 spring cloud netflix zuul
     */
    private Integer maxTotal = 200;

    /**
     * 每一路路由的最大数量
     * 此参数值参考 spring cloud netflix zuul
     */
    private Integer defaultMaxPerRoute = 20;

    /**
     * 设置从连接池获取一个连接的请求超时时间(连接池中连接不够用的时候等待超时时间)
     * 此参数值无考究对象
     */
    private Integer connectionRequestTimeout = 5000;

    /**
     * 线程池初始延时时间 10s
     */
    private Integer initialDelay = 10000;

    /**
     * 每次任务完成后的延时时间 10s
     */
    private Integer delay = 10000;

    /**
     * HttpClient 连接池名字
     */
    private String httpClientPoolName = "ladder-http-pool";

    /**
     * SslHttpClient 连接池名字
     */
    private String sslHttpClientPoolName = "ladder-ssl-http-pool";

    /**
     * 证书配置相关
     */
    private KeyStoreProperties keyStoreProperties;

    /**
     * 是否保留 Spring 中的原生 restTemplate 组件, 如果不保留, 那么容器中 beanName 为 restTemplate 的会被 rdfaRestTemplate 替换
     * true 表示保留, 但不建议保留
     */
    private boolean keep = true;

}
