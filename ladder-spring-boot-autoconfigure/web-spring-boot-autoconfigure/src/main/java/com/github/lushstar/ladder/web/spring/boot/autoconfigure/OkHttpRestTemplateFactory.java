package com.github.lushstar.ladder.web.spring.boot.autoconfigure;

import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PreDestroy;
import javax.net.SocketFactory;
import java.util.concurrent.TimeUnit;

/**
 * <p>description : OkHttpRestTemplateFactory
 *
 * <p>blog : https://blog.csdn.net/masteryourself
 *
 * @author : masteryourself
 * @version : 1.0.0
 * @date : 2020/4/8 11:45
 */
@Slf4j
public class OkHttpRestTemplateFactory implements RestTemplateFactory {

    private final RestTemplateProperties restTemplateProperties;

    private final KeyStoreProperties keyStoreProperties;

    private OkHttpClient okHttpClient;

    private OkHttpClient sslOkHttpClient;

    public OkHttpRestTemplateFactory(RestTemplateProperties restTemplateProperties, KeyStoreProperties keyStoreProperties) {
        this.restTemplateProperties = restTemplateProperties;
        this.keyStoreProperties = keyStoreProperties;
    }

    @Override
    public RestTemplate createRestTemplate() {
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(restTemplateProperties.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(restTemplateProperties.getSocketTimeout(), TimeUnit.MILLISECONDS)
                .socketFactory(SocketFactory.getDefault())
                .connectionPool(this.getConnectionPool())
                .build();
        return new RestTemplate(new OkHttp3ClientHttpRequestFactory(okHttpClient));
    }

    @Override
    public RestTemplate createSslRestTemplate() {
        SslParamsUtils.SslParams sslParams = null;
        try {
            sslParams = SslParamsUtils.initSslParams(keyStoreProperties);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException("OkHttpRestTemplateFactory createSslRestTemplate init error", e);
        }
        sslOkHttpClient = new OkHttpClient.Builder()
                .connectTimeout(restTemplateProperties.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(restTemplateProperties.getSocketTimeout(), TimeUnit.MILLISECONDS)
                .sslSocketFactory(sslParams.sslContext.getSocketFactory(), sslParams.trustManager)
                .hostnameVerifier(new SslParamsUtils.HostnameVerifierCustomizer(keyStoreProperties))
                .connectionPool(this.getConnectionPool())
                .build();
        return new RestTemplate(new OkHttp3ClientHttpRequestFactory(sslOkHttpClient));
    }

    private ConnectionPool getConnectionPool() {
        return new ConnectionPool(restTemplateProperties.getMaxTotal(),
                restTemplateProperties.getKeeAliveTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void close() throws Exception {
        if (okHttpClient != null) {
            log.info("release okHttpClient resource");
            okHttpClient.dispatcher().executorService().shutdown();
            okHttpClient.connectionPool().evictAll();
        }
        if (sslOkHttpClient != null) {
            log.info("release sslOkHttpClient resource");
            sslOkHttpClient.dispatcher().executorService().shutdown();
            sslOkHttpClient.connectionPool().evictAll();
        }
    }

}
