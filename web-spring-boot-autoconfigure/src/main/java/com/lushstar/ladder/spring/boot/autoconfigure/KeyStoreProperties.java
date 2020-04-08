package com.lushstar.ladder.spring.boot.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>description : KeyStoreProperties
 *
 * <p>blog : https://blog.csdn.net/masteryourself
 *
 * @author : masteryourself
 * @version : 1.0.0
 * @date : 2020/4/8 11:46
 */
@ConfigurationProperties(prefix = "ladder.http.keystore")
@Data
public class KeyStoreProperties {

    /**
     * 客户端信任服务端的证书格式, 默认为 jks, 即 Java Key Store
     * 用于服务端自己颁发的授权证书场景，未经过 CA 认证：如 Tomcat 搭建自己的 Https 服务
     */
    private String trustType;

    /**
     * 客户端信任服务端证书的存储地址, 只要公钥
     * 如果是公钥 + 私钥混合存储，可用下面命令提取出公钥
     * keytool -export -alias wsria[别名] -keystore wsriakey[公钥+私钥文件] -storepass 123456[秘钥] -file scert.cer[公钥文件]
     */
    private String trustPath;

    /**
     * 服务端信任客户端的证书格式
     * 用户服务端校验客户端携带的证书，如微信支付场景
     */
    private String clientType;

    /**
     * 服务端信任客户端的证书存储路径
     */
    private String clientPath;

    /**
     * 服务端信任客户端的证书秘钥
     * 注意和 clientPath 为一组配置
     */
    private String clientPassword;

    /**
     * 服务端信任地址
     * 在使用 OkHttp 信任服务端证书的情况下, 如果仍然报错：SSLPeerUnverifiedException: Hostname xxx verified，可以尝试把 xxx 域名配置在此属性中
     */
    private String[] hostNames;

}
