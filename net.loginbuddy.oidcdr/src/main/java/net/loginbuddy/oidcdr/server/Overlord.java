package net.loginbuddy.oidcdr.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import net.loginbuddy.common.util.CertificateManager;

public class Overlord extends HttpServlet {

    @Override
    public void init() throws ServletException {
        super.init();
        CertificateManager.loadTrustedServers();
    }
}
