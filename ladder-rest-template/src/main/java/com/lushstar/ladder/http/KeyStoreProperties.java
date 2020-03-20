package com.lushstar.ladder.http;

import lombok.Data;

/**
 * <p>description : KeyStoreProperties
 *
 * <p>blog : https://Blog.csdn.net/masteryourself
 *
 * @author : masteryourself
 * @version : 1.0.0
 * @date : 2020/3/20 23:25
 */
@Data
public class KeyStoreProperties {

    /**
     * 客户端信任服务端的证书格式, 默认为 jks, 即 Java Key Store
     * 用于服务端自己颁发的授权证书场景，未经过 CA 认证：如 Tomcat 搭建自己的 Https 服务
     */
    private String serverKeyStoreType;

    /**
     * 客户端信任服务端证书的存储地址
     */
    private String serverPath;

    /**
     * 客户端信任服务端证书的访问秘钥
     * 注意和 serverPath 为一组配置
     */
    private String serverPassword;

    /**
     * 服务端信任客户端的证书格式
     * 用户服务端校验客户端携带的证书，如微信支付场景
     */
    private String clientKeyStoreType;

    /**
     * 服务端信任客户端的证书存储路径
     */
    private String clientPath;

    /**
     * 服务端信任客户端的证书秘钥
     * 注意和 clientPath 为一组配置
     */
    private String clientPassword;

}
