package net.loginbuddy.oidcdr.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import net.loginbuddy.common.util.CertificateManager;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.logging.Logger;

public class Overlord extends HttpServlet {

    private static Logger LOGGER = Logger.getLogger(String.valueOf(Overlord.class));

    @Override
    public void init() throws ServletException {
        super.init();

        try {
            loadTrustedServers();
        } catch (Exception e) {
            LOGGER.warning( String.format("Trusted server certificate could not be added! : %s", e.getMessage()));
        }

    }

    private void loadTrustedServers() throws IOException, NoSuchAlgorithmException, KeyManagementException, CertificateException, KeyStoreException {

        String servers = System.getenv("SSL_TRUSTED_SERVER");
        if (servers != null && servers.trim().length() > 0) {
            LOGGER.info("Connecting to trusted servers!");
            CertificateManager cm = new CertificateManager();
            for (String server : servers.split(",")) {
                List<Certificate> certificates = cm.retrieveTrustedCert(server.split(":")[0], Integer.parseInt(server.split(":")[1]));
                cm.addToTruststore(certificates);
            }
        }

    }
}
