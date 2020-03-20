package com.lushstar.ladder.http;

import org.springframework.web.client.RestTemplate;

/**
 * <p>description : RestTemplateFactory
 *
 * <p>blog : https://Blog.csdn.net/masteryourself
 *
 * @author : masteryourself
 * @version : 1.0.0
 * @date : 2020/3/20 23:21
 */
public interface RestTemplateFactory {

    /**
     * 用于访问 Http 请求和通过 CA 认证的公网 Https 请求
     * 查看通过 CA 认证的地址可以在 %JAVA_HOME%/jre/lib/security/keystore 文件中查看
     * keytool -list -keystore cacerts -storepass changeit
     *
     * @return {@link RestTemplate}
     */
    RestTemplate createRestTemplate();

    /**
     * 定制化的 Https 请求, 具体参考可参考 {@link KeyStoreProperties}
     *
     * @return {@link RestTemplate}
     */
    RestTemplate createSslRestTemplate();

}
