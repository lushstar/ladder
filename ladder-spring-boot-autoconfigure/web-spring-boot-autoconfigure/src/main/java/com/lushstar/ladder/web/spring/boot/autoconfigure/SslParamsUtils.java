package com.lushstar.ladder.web.spring.boot.autoconfigure;

import org.springframework.util.StringUtils;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * <p>description : SslParamsUtils
 *
 * <p>blog : https://blog.csdn.net/masteryourself
 *
 * @author : masteryourself
 * @version : 1.0.0
 * @date : 2020/4/8 11:44
 */
public class SslParamsUtils {

    public static class SslParams {
        SSLContext sslContext;
        X509TrustManager trustManager;
    }

    public static TrustManager[] prepareTrustManager(String type, InputStream... certificates) throws Exception {
        if (certificates == null || certificates.length <= 0) {
            return null;
        }
        if (StringUtils.isEmpty(type)) {
            type = KeyStore.getDefaultType();
        }
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        KeyStore keyStore = KeyStore.getInstance(type);
        keyStore.load(null);
        int index = 0;
        for (InputStream certificate : certificates) {
            String certificateAlias = Integer.toString(index++);
            keyStore.setCertificateEntry(certificateAlias, certificateFactory.generateCertificate(certificate));
            if (certificate != null) {
                certificate.close();
            }
        }
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        return trustManagerFactory.getTrustManagers();
    }

    public static KeyManager[] prepareKeyManager(String type, InputStream jksFile, String password) throws Exception {
        if (jksFile == null || password == null) {
            return null;
        }
        if (StringUtils.isEmpty(type)) {
            type = KeyStore.getDefaultType();
        }
        KeyStore clientKeyStore = KeyStore.getInstance(type);
        clientKeyStore.load(jksFile, password.toCharArray());
        KeyManagerFactory keyManagerFactory = KeyManagerFactory
                .getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(clientKeyStore, password.toCharArray());
        return keyManagerFactory.getKeyManagers();
    }

    public static X509TrustManager chooseTrustManager(TrustManager[] trustManagers) {
        for (TrustManager trustManager : trustManagers) {
            if (trustManager instanceof X509TrustManager) {
                return (X509TrustManager) trustManager;
            }
        }
        return null;
    }

    public static SslParams initSslParams(KeyStoreProperties keyStoreProperties) throws Exception {
        SslParams sslParams = new SslParams();
        TrustManager[] trustManagers = null;
        KeyManager[] keyManagers = null;
        String trustPath = keyStoreProperties.getTrustPath();
        if (!StringUtils.isEmpty(trustPath)) {
            InputStream certificates = new FileInputStream(new File(trustPath));
            trustManagers = prepareTrustManager(keyStoreProperties.getTrustType(), certificates);
        }
        String clientPath = keyStoreProperties.getClientPath();
        String password = keyStoreProperties.getClientPassword();
        if (!StringUtils.isEmpty(clientPath) && !StringUtils.isEmpty(password)) {
            InputStream jksFile = new FileInputStream(new File(clientPath));
            keyManagers = SslParamsUtils.prepareKeyManager(keyStoreProperties.getClientType(), jksFile, password);
        }
        SSLContext sslContext = SSLContext.getInstance("TLS");
        X509TrustManager trustManager = null;
        if (trustManagers != null) {
            trustManager = new MyTrustManager(SslParamsUtils.chooseTrustManager(trustManagers));
        }
        sslContext.init(keyManagers, new TrustManager[]{trustManager}, null);
        sslParams.sslContext = sslContext;
        sslParams.trustManager = trustManager;
        return sslParams;
    }

    public static class HostnameVerifierCustomizer implements HostnameVerifier {
        private final KeyStoreProperties keyStoreProperties;

        public HostnameVerifierCustomizer(KeyStoreProperties keyStoreProperties) {
            this.keyStoreProperties = keyStoreProperties;
        }

        @Override
        public boolean verify(String hostname, SSLSession session) {
            String[] hostNames = keyStoreProperties.getHostNames();
            if (hostNames == null) {
                return false;
            }
            for (String tmp : hostNames) {
                if (hostname.equalsIgnoreCase(tmp)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class MyTrustManager implements X509TrustManager {
        private final X509TrustManager defaultTrustManager;
        private final X509TrustManager localTrustManager;

        public MyTrustManager(X509TrustManager localTrustManager) throws NoSuchAlgorithmException, KeyStoreException {
            TrustManagerFactory var4 = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            var4.init((KeyStore) null);
            defaultTrustManager = SslParamsUtils.chooseTrustManager(var4.getTrustManagers());
            this.localTrustManager = localTrustManager;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                defaultTrustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException ce) {
                localTrustManager.checkServerTrusted(chain, authType);
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

}
