package net.loginbuddy.sidecar.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.config.loginbuddy.OverlordMaster;

import java.util.logging.Logger;

public class Overlord extends HttpServlet {

    private static Logger LOGGER = Logger.getLogger(String.valueOf(Overlord.class));

    @Override
    public void init() throws ServletException {
        super.init();
        HttpHelper.loadTrustedServers();
        new OverlordMaster().initializeConfiguration();
    }
}