package net.loginbuddy.common.util;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class CertificateManager {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(CertificateManager.class));

    /**
     * Connect to a host that uses a self-signed (untrusted) certificate
     */
    public final List<Certificate> retrieveTrustedCert(String host, int port) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        SSLSocket socket = (SSLSocket) getOpenSslContext().getSocketFactory().createSocket(host, port);
        socket.startHandshake();
        List<Certificate> certs = new ArrayList<>(Arrays.asList(socket.getSession().getPeerCertificates()));
        return certs;
    }

    /**
     * Adding self-signed certificates as trusted certs (useful during development times)
     */
    public final void addToTruststore(List<Certificate> certs)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        // TODO extract hardcoded values
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] pwdArray = "changeit".toCharArray();
        ks.load(new FileInputStream("/usr/local/openjdk-11/lib/security/cacerts"), pwdArray);
        for (Certificate cert : certs) {
            String certDn = ((X509Certificate) cert).getSubjectX500Principal().getName();
            ks.setCertificateEntry(certDn, cert);
            LOGGER.info(String.format("Accepted certificate with DN: %s", certDn));
        }
        File f = new File("/usr/local/openjdk-11/lib/security/cacerts");
        FileOutputStream fos = new FileOutputStream(f);
        ks.store(fos, pwdArray);
        fos.close();
    }

    /**
     * Used to import self-signed certificates as trusted certs during startup
     */
    private SSLContext getOpenSslContext() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslCtx = SSLContext.getInstance("TLS");
        sslCtx.init(null, new TrustManager[]
                {
                        new X509TrustManager() {

                            private X509Certificate[] accepted;

                            @Override
                            public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                                accepted = xcs;  // done on purpose. This is only used during startup to import specified self-signed certificates
                            }

                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return accepted;
                            }
                        }
                }, new java.security.SecureRandom());
        return sslCtx;
    }
}