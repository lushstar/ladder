package com.lushstar.ladder.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PreDestroy;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>description : RestTemplateHttpClientFactory
 *
 * <p>blog : https://blog.csdn.net/masteryourself
 *
 * @author : masteryourself
 * @version : 1.0.0
 * @date : 2020/3/20 23:27
 */
@Slf4j
public class RestTemplateHttpClientFactory implements RestTemplateFactory {

    private RestTemplateProperties restTemplateProperties;

    private CloseableHttpClient httpClient = null;

    private CloseableHttpClient sslHttpClient = null;

    public RestTemplateHttpClientFactory(RestTemplateProperties restTemplateProperties) {
        this.restTemplateProperties = restTemplateProperties;
    }

    @Override
    public RestTemplate createRestTemplate() {
        String poolName = restTemplateProperties.getHttpClientPoolName();
        PoolingHttpClientConnectionManager connectionManager = this.createPoolingHttpClientConnectionManager(poolName);
        httpClient = this.buildHttpClient(connectionManager);
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }

    @Override
    public RestTemplate createSslRestTemplate() {
        String poolName = restTemplateProperties.getHttpClientPoolName();
        PoolingHttpClientConnectionManager connectionManager = this.createSslPoolingHttpClientConnectionManager(
                restTemplateProperties.getKeyStoreProperties(), poolName);
        sslHttpClient = this.buildHttpClient(connectionManager);
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(sslHttpClient));
    }

    @PreDestroy
    public void destroy() throws Exception {
        if (httpClient != null) {
            httpClient.close();
        }
        if (sslHttpClient != null) {
            sslHttpClient.close();
        }
    }

    private CloseableHttpClient buildHttpClient(PoolingHttpClientConnectionManager poolingConnectionManager) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(restTemplateProperties.getConnectionRequestTimeout())
                .setConnectTimeout(restTemplateProperties.getConnectTimeout())
                .setSocketTimeout(restTemplateProperties.getSocketTimeout())
                .build();
        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(poolingConnectionManager)
                .setKeepAliveStrategy(connectionKeepAliveStrategy())
                .build();
    }

    private PoolingHttpClientConnectionManager createPoolingHttpClientConnectionManager(String poolName) {
        try {
            ConnectionSocketFactory connectionSocketFactory = new SSLConnectionSocketFactory(SSLContext.getDefault());
            PoolingHttpClientConnectionManager poolingConnectionManager = this.getPoolingHttpClientConnectionManager(
                    connectionSocketFactory, poolName);
            log.info("Pooling Connection Manager {} Initialisation success", poolName);
            return poolingConnectionManager;
        } catch (Exception e) {
            log.error("Pooling Connection Manager {} Initialisation failure because of {}", poolName, e);
            throw new RuntimeException(e);
        }
    }

    private PoolingHttpClientConnectionManager createSslPoolingHttpClientConnectionManager(KeyStoreProperties keyStoreProperties, String poolName) {
        try {
            SSLContextBuilder builder = SSLContexts.custom();
            // 判断是否需要添加【客户端信任服务端】的证书
            String serverKeyStoreType = keyStoreProperties.getServerKeyStoreType();
            String serverPath = keyStoreProperties.getServerPath();
            String serverPassword = keyStoreProperties.getServerPassword();
            KeyStore serverKeyStore = null;
            if (!StringUtils.isEmpty(serverPath) && !StringUtils.isEmpty(serverPassword)) {
                serverKeyStore = this.newKeyStore(serverKeyStoreType, serverPath, serverPassword);
            }
            if (serverKeyStore != null) {
                builder.loadTrustMaterial(serverKeyStore, new TrustSelfSignedStrategy());
            }
            // 判断是否需要添加【服务端信任客户端】的证书
            String clientKeyStoreType = keyStoreProperties.getClientKeyStoreType();
            String clientPath = keyStoreProperties.getClientPath();
            String clientPassword = keyStoreProperties.getClientPassword();
            KeyStore clientKeyStore = null;
            if (!StringUtils.isEmpty(clientPath) && !StringUtils.isEmpty(clientPassword)) {
                clientKeyStore = this.newKeyStore(clientKeyStoreType, clientPath, clientPassword);
            }
            if (clientKeyStore != null) {
                builder.loadKeyMaterial(clientKeyStore, clientPassword.toCharArray());
            }
            SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(builder.build(), new String[]{"TLSv1"},
                    null, SSLConnectionSocketFactory.getDefaultHostnameVerifier());
            PoolingHttpClientConnectionManager poolingConnectionManager = this.getPoolingHttpClientConnectionManager(
                    sslConnectionSocketFactory, poolName);
            log.info("Pooling Connection Manager {} Initialisation success", poolName);
            return poolingConnectionManager;
        } catch (Exception e) {
            log.error("Pooling Connection Manager {} Initialisation failure because of {}", poolName, e);
            throw new RuntimeException(e);
        }
    }

    private PoolingHttpClientConnectionManager getPoolingHttpClientConnectionManager(ConnectionSocketFactory connectionSocketFactory, String poolName) {
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("https", connectionSocketFactory)
                .register("http", new PlainConnectionSocketFactory())
                .build();
        PoolingHttpClientConnectionManager poolingConnectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        // 设置连接池信息
        poolingConnectionManager.setMaxTotal(restTemplateProperties.getMaxTotal());
        poolingConnectionManager.setDefaultMaxPerRoute(restTemplateProperties.getDefaultMaxPerRoute());
        // 加入监控
        this.idleConnectionMonitor(poolingConnectionManager, poolName);
        return poolingConnectionManager;
    }

    /**
     * 构建 KeyStore
     *
     * @param keyStoreType 证书格式
     * @param path         证书路径
     * @param password     证书访问秘钥
     * @return {@link KeyStore}
     */
    private KeyStore newKeyStore(String keyStoreType, String path, String password) {
        if (StringUtils.isEmpty(keyStoreType)) {
            keyStoreType = KeyStore.getDefaultType();
        }
        KeyStore keyStore;
        try (InputStream inputStream = new FileInputStream(new File(path))) {
            keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(inputStream, password.toCharArray());
        } catch (Exception e) {
            log.error("load key store occur error, keyStoreType is {}, path is {}, password is {}, error is {}", keyStoreType, path, password, e);
            throw new RuntimeException(e);
        }
        return keyStore;
    }

    /**
     * 空闲连接监控策略
     *
     * @param poolingConnectionManager 连接池
     * @param poolName                 连接池名字
     */
    private void idleConnectionMonitor(PoolingHttpClientConnectionManager poolingConnectionManager, String poolName) {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (poolingConnectionManager != null) {
                    log.info("run idleConnectionMonitor - Closing {} expired and idle connections...", poolName);
                    // 关闭过期连接
                    poolingConnectionManager.closeExpiredConnections();
                    // 关闭空闲连接
                    poolingConnectionManager.closeIdleConnections(restTemplateProperties.getIdleTimeout(), TimeUnit.SECONDS);
                } else {
                    log.info("run idleConnectionMonitor - Http Client Connection manager {} is not initialised", poolName);
                }
            }
        }, restTemplateProperties.getInitialDelay(), restTemplateProperties.getDelay(), TimeUnit.MILLISECONDS);
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
