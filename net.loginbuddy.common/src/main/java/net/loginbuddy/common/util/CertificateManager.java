package net.loginbuddy.common.util;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;

public class CertificateManager {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(CertificateManager.class));

    /**
     * Connect to a host that uses a self-signed (untrusted) certificate
     */
    public final List<Certificate> retrieveTrustedCert(String host, int port) throws IOException, NoSuchAlgorithmException, KeyManagementException {
        // TODO turn this method into good code
        List<Certificate> certs = new ArrayList<>();
        Certificate[] peerCertificates = null;
        int count = 1;
        int wait = 5000;
        int i = 5;
        while (i >= count) {
            try {
                SSLSocket socket = (SSLSocket) getOpenSslContext().getSocketFactory().createSocket(host, port);
                socket.startHandshake();
                peerCertificates = socket.getSession().getPeerCertificates();
                socket.close();
                certs = new ArrayList<>(Arrays.asList(peerCertificates));
                break;
            } catch (ConnectException e) {
                if(e.getMessage().contains("refused")) {
                    LOGGER.info(String.format("Target server could not be reached: '%s:%d'. Attempt: %d%s", host, port, count, count == i ? ", giving up" : ""));
                    try {
                        Thread.sleep(wait);
                    } catch (InterruptedException e1) {
                        // should never occur
                    }
                } else {
                    throw e;
                }
            }
            count++;
        }
        return certs;
    }

    /**
     * Adding self-signed certificates as trusted certs (useful during development times)
     */
    public final void addToTruststore(List<Certificate> certs)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        // TODO extract hardcoded values
        String ksLocation = "/usr/local/openjdk-11/lib/security/cacerts";
        char[] pwdArray = "changeit".toCharArray();
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(new FileInputStream(ksLocation), pwdArray);
        for (Certificate cert : certs) {
            String certDn = ((X509Certificate) cert).getSubjectX500Principal().getName();
            Collection<List<?>> subjectAlternativeNames = ((X509Certificate) cert).getSubjectAlternativeNames();
            ks.setCertificateEntry(certDn, cert);
            LOGGER.info(String.format("Accepted certificate with DN: %s, SAN: %s", certDn, subjectAlternativeNames == null ? "no san available" : subjectAlternativeNames.toString()));
        }
        File f = new File(ksLocation);
        FileOutputStream fos = new FileOutputStream(f);
        ks.store(fos, pwdArray);
        fos.close();
    }

    /**
     * Generate base64url encoded sha256 value
     * @param input value for which to generate the output value
     */
    public static String generateBase64UrlEncodedSha256(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] encodedHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return new String(Base64.getUrlEncoder().withoutPadding().encode(encodedHash));
    }

    /**
     * Tries to connect to servers to import self-signed SSL certificates as trusted certificates. Intended for development purposes. The list of server is provided via env variable SSL_TRUSTED_SERVER
     */
    public static void loadTrustedServers() {
        String servers = null;
        try {
            servers = System.getenv("SSL_TRUSTED_SERVER");
        } catch (Exception e) {
            LOGGER.warning(String.format("Variable SSL_TRUSTED_SERVER is not accessible and therefore no self-signed certs are imported: %s", e.getMessage()));
        }
        servers = servers == null ? "" : servers;
        try {
            String oidcdr = System.getenv("SUPPORT_OIDCDR");
            if (Boolean.parseBoolean(oidcdr)) {
                servers = "loginbuddy-oidcdr:445,".concat(servers);
            } else {
                LOGGER.info("loginbuddy-oidcdr was not requested");
            }
        } catch (Exception e) {
            LOGGER.warning(String.format("Variable SUPPORT_OIDCDR is not accessible and therefore its certificate is not imported: %s", e.getMessage()));
        }
        try {
            if (servers.trim().length() > 0) {
                LOGGER.info("Connecting to trusted servers!");
                CertificateManager cm = new CertificateManager();
                for (String server : servers.split(",")) {
                    List<Certificate> certificates = cm.retrieveTrustedCert(server.split(":")[0], Integer.parseInt(server.split(":")[1]));
                    cm.addToTruststore(certificates);
                }
            }
        } catch (Exception e) {
            LOGGER.warning(String.format("Trusted server certificate could not be added! : %s", e.getMessage()));
        }
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