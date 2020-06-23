package com.github.lushstar.ladder.web.spring.boot.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>description : HttpClientRestTemplateFactory
 *
 * <p>blog : https://blog.csdn.net/masteryourself
 *
 * @author : masteryourself
 * @version : 1.0.0
 * @date : 2020/4/8 11:43
 */
@Slf4j
public class HttpClientRestTemplateFactory implements RestTemplateFactory {

    private final RestTemplateProperties restTemplateProperties;

    private final KeyStoreProperties keyStoreProperties;

    private CloseableHttpClient httpClient = null;

    private CloseableHttpClient sslHttpClient = null;

    public HttpClientRestTemplateFactory(RestTemplateProperties restTemplateProperties, KeyStoreProperties keyStoreProperties) {
        this.restTemplateProperties = restTemplateProperties;
        this.keyStoreProperties = keyStoreProperties;
    }

    @Override
    public RestTemplate createRestTemplate() {
        HttpClientConnectionManager connectionManager;
        try {
            connectionManager = this.createConnectionManager();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException("HttpClientRestTemplateFactory createSslRestTemplate init error", e);
        }
        httpClient = HttpClients.custom()
                .setDefaultRequestConfig(this.buildRequestConfig())
                .setConnectionManager(connectionManager)
                .setKeepAliveStrategy(connectionKeepAliveStrategy())
                .build();
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }

    @Override
    public RestTemplate createSslRestTemplate() {
        HttpClientConnectionManager sslConnectionManager;
        try {
            sslConnectionManager = this.createSslConnectionManager();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException("HttpClientRestTemplateFactory createSslRestTemplate init error", e);
        }
        sslHttpClient = HttpClients.custom()
                .setDefaultRequestConfig(this.buildRequestConfig())
                .setConnectionManager(sslConnectionManager)
                .setKeepAliveStrategy(connectionKeepAliveStrategy())
                .setSSLHostnameVerifier(new SslParamsUtils.HostnameVerifierCustomizer(keyStoreProperties))
                .build();
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(sslHttpClient));
    }

    @PreDestroy
    public void close() throws Exception {
        if (httpClient != null) {
            log.info("release httpClient resource");
            httpClient.close();
        }
        if (sslHttpClient != null) {
            log.info("release sslHttpClient resource");
            sslHttpClient.close();
        }
    }

    private RequestConfig buildRequestConfig() {
        return RequestConfig.custom()
                .setConnectionRequestTimeout(restTemplateProperties.getConnectionRequestTimeout())
                .setConnectTimeout(restTemplateProperties.getConnectTimeout())
                .setSocketTimeout(restTemplateProperties.getSocketTimeout())
                .build();
    }

    private HttpClientConnectionManager createConnectionManager() throws Exception {
        ConnectionSocketFactory connectionSocketFactory = new SSLConnectionSocketFactory(SSLContext.getDefault());
        return this.getPoolingHttpClientConnectionManager(connectionSocketFactory);
    }

    private HttpClientConnectionManager createSslConnectionManager() throws Exception {
        SslParamsUtils.SslParams sslParams = SslParamsUtils.initSslParams(keyStoreProperties);
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(
                sslParams.sslContext, new String[]{"TLSv1"}, null, SSLConnectionSocketFactory.getDefaultHostnameVerifier());
        HttpClientConnectionManager connectionManager = this.getPoolingHttpClientConnectionManager(sslConnectionSocketFactory);
        log.info("Pooling Connection Manager Initialisation success");
        return connectionManager;
    }

    private HttpClientConnectionManager getPoolingHttpClientConnectionManager(ConnectionSocketFactory connectionSocketFactory) {
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("https", connectionSocketFactory)
                .register("http", new PlainConnectionSocketFactory())
                .build();
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        // 设置连接池信息
        connectionManager.setMaxTotal(restTemplateProperties.getMaxTotal());
        connectionManager.setDefaultMaxPerRoute(restTemplateProperties.getDefaultMaxPerRoute());
        // 加入监控
        this.idleConnectionMonitor(connectionManager);
        return connectionManager;
    }

    /**
     * 空闲连接监控策略
     *
     * @param poolingConnectionManager 连接池
     */
    private void idleConnectionMonitor(PoolingHttpClientConnectionManager poolingConnectionManager) {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (poolingConnectionManager != null) {
                    log.info("run idleConnectionMonitor - Closing expired and idle connections...");
                    // 关闭过期连接
                    poolingConnectionManager.closeExpiredConnections();
                    // 关闭空闲连接
                    poolingConnectionManager.closeIdleConnections(restTemplateProperties.getIdleTimeout(), TimeUnit.SECONDS);
                } else {
                    log.info("run idleConnectionMonitor - Http Client Connection manager is not initialised");
                }
            }
        }, restTemplateProperties.getInitialDelay(), restTemplateProperties.getDelay(), TimeUnit.SECONDS);
    }

    /**
     * 连接保持活动的策略
     *
     * @return 保持活动策略
     */
    private ConnectionKeepAliveStrategy connectionKeepAliveStrategy() {
        return new DefaultConnectionKeepAliveStrategy() {
            @Override
            public long getKeepAliveDuration(final HttpResponse response, final HttpContext context) {
                // HttpClient 默认的策略是从响应头获取, 获取不到会返回 -1, 表示永久
                long keepAlive = super.getKeepAliveDuration(response, context);
                if (keepAlive == -1) {
                    keepAlive = restTemplateProperties.getKeeAliveTimeMillis();
                }
                return keepAlive;
            }
        };
    }

}
