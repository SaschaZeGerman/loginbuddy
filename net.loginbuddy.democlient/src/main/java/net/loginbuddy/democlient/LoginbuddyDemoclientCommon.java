package net.loginbuddy.democlient;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;

import java.util.logging.Logger;

public abstract class LoginbuddyDemoclientCommon extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(LoginbuddyDemoclientCommon.class));

    protected String scheme;
    protected String hostname;
    protected String hostname_loginbuddy;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        scheme = System.getenv("SCHEME_LOGINBUDDY_DEMOCLIENT");
        if("http".equalsIgnoreCase(scheme)) {
            scheme = "http://";
            LOGGER.warning("Loginbuddy Democlient is using http!");
        } else {
            scheme = "https://";
        }
        hostname = System.getenv("HOSTNAME_LOGINBUDDY_DEMOCLIENT");
        hostname_loginbuddy = System.getenv("HOSTNAME_LOGINBUDDY");
    }

}
